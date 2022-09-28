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

import com.microsoft.playwright.BrowserType.LaunchOptions

sealed abstract class BrowserConfig(val lo: Option[LaunchOptions])
    extends Product
    with Serializable

object BrowserConfig {
  case class Chromium(override val lo: Option[LaunchOptions])
      extends BrowserConfig(lo)
  case class Firefox(override val lo: Option[LaunchOptions])
      extends BrowserConfig(lo)
  case class WebKit(override val lo: Option[LaunchOptions])
      extends BrowserConfig(lo)
}
