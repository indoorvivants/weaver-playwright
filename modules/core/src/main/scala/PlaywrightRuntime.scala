/*
 * Copyright 2022 Anton Sviridov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.indoorvivants.weaver.playwright

import java.nio.file.FileSystemAlreadyExistsException

import scala.concurrent.duration._

import cats.effect._
import cats.effect.std.Semaphore
import cats.syntax.all._
import com.microsoft.playwright._
import org.typelevel.keypool._

import BrowserConfig._

trait PlaywrightRuntime {
  def pageContext: Resource[IO, PageContext]
}

private[playwright] class PooledPlaywrightRuntime(
    pool: Pool[IO, PlaywrightRuntime]
) extends PlaywrightRuntime {
  override def pageContext: Resource[IO, PageContext] =
    pool.take.map(_.value).flatMap(_.pageContext)
}

private[playwright] class SinglePlaywrightRuntime(
    browser: Browser,
    sem: Semaphore[IO]
) extends PlaywrightRuntime {
  def pageContext: Resource[IO, PageContext] = {
    sem.permit >>
      Resource
        .fromAutoCloseable(IO(browser.newContext()))
        .flatMap { ctx =>
          Resource
            .fromAutoCloseable(IO(ctx.newPage()))
            .map { pg =>
              new PageContext(ctx, pg)
            }
        }

  }
}

object PlaywrightRuntime {

  def create(
      browser: BrowserConfig = Chromium(None),
      recoverRetry: PlaywrightRetry = PlaywrightRetry.linear(5, 2.second),
      poolSize: Int = Runtime.getRuntime().availableProcessors()
  ): Resource[IO, PlaywrightRuntime] = {
    val res = single(browser, recoverRetry)
    Pool
      .Builder(res)
      .withMaxTotal(poolSize)
      .build
      .map(new PooledPlaywrightRuntime(_))
  }

  def single(
      browser: BrowserConfig = Chromium(None),
      recoverRetry: PlaywrightRetry = PlaywrightRetry.linear(5, 2.second)
  ): Resource[IO, PlaywrightRuntime] = {
    val withRetries = rerun(
      IO(Playwright.create()),
      {
        case _: FileSystemAlreadyExistsException =>
          true
        // I know. I'm sorry.
        case rm: RuntimeException
            if rm.getMessage() == "Failed to create driver" =>
          true
      },
      recoverRetry
    )
    import BrowserConfig._
    Resource
      .fromAutoCloseable(withRetries)
      .flatMap { pw =>
        def create(f: Playwright => BrowserType) =
          Resource.fromAutoCloseable(IO(f(pw).launch(browser.lo.orNull)))

        val created = browser match {
          case _: Chromium => create(_.chromium())
          case _: Firefox  => create(_.firefox())
          case _: WebKit   => create(_.webkit())
        }

        val sem = Resource.eval(Semaphore[IO](1))

        (created, sem).mapN(new SinglePlaywrightRuntime(_, _))
      }

  }

  private def rerun[A](
      ioa: IO[A],
      errors: PartialFunction[Throwable, Boolean],
      policy: PlaywrightRetry
  ) = {
    val isError = errors.lift.andThen(_.contains(true))

    def go(n: Int, last: IO[A]): IO[A] = {
      policy.next(n) match {
        case None => last
        case Some(delay) =>
          ioa.attempt.flatMap {
            case Left(value) if isError(value) =>
              IO.sleep(delay) *> go(n + 1, IO.raiseError(value))
            case Left(err)    => IO.raiseError(err)
            case Right(value) => IO.pure(value)
          }
      }
    }

    go(1, ioa)
  }
}
