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

import scala.concurrent.duration._

import cats.data.Validated.Invalid
import cats.data.Validated.Valid
import cats.effect._
import weaver._

trait PlaywrightSpec extends PlaywrightIntegration { self: IOSuite =>
  override type Res = PlaywrightRuntime
  override def getPlaywright(res: PlaywrightRuntime): PlaywrightRuntime = res
}

trait PlaywrightIntegration { self: IOSuite =>

  def getPlaywright(res: Res): PlaywrightRuntime

  def retryPolicy: PlaywrightRetry =
    PlaywrightRetry.linear(5, 1.second) // 5 seconds max

  def pageTest(tn: TestName)(f: PageContext => IO[Expectations]): Unit = {
    self.test(tn) { res =>
      getPlaywright(res).pageContext.use(f)
    }
  }

  def getPageContext(res: Res): Resource[IO, PageContext] = getPlaywright(
    res
  ).pageContext

  def eventually[A](
      a: IO[A],
      policy: PlaywrightRetry = retryPolicy
  )(f: A => Expectations): IO[Unit] = {
    def go(n: Int, last: Expectations): IO[Unit] =
      policy.next(n) match {
        case None => last.failFast[IO]
        case Some(delay) =>
          a.flatMap { value =>
            val last = f(value)
            last.run match {
              case Invalid(_) =>
                IO.sleep(delay) >> go(n + 1, last)
              case Valid(_) => IO.unit
            }
          }
      }

    go(1, success)
  }
}
