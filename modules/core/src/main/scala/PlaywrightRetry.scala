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

trait PlaywrightRetry {
  def next(attempt: Int): Option[FiniteDuration]
}

object PlaywrightRetry {
  def no = new PlaywrightRetry {
    override def next(attempt: Int): Option[FiniteDuration] = None
  }

  def linear(max: Int, delay: FiniteDuration) = new PlaywrightRetry {
    override def next(attempt: Int): Option[FiniteDuration] =
      if (attempt <= max) Some(delay) else None
  }

  def exponential(
      max: Int,
      step: FiniteDuration
  ) = new PlaywrightRetry {
    override def next(attempt: Int): Option[FiniteDuration] =
      if (attempt <= max) Some {
        ((1L << (attempt - 1)) * step.toMillis).millis
      }
      else None
  }
}
