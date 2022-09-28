## [Weaver](https://disneystreaming.github.io/weaver-test/) + [Playwright](https://playwright.dev/java/docs/intro)

Love Weaver? Like Playwright? Forced to write tests?

Very small integration between Playwright and Weaver to facilitate browser testing of applications.

Here's an example of usage which is also a Scala CLI script!

**test.scala**
```scala mdoc
//> using lib "com.indoorvivants.weaver::playwright:0.0.1"
//> using lib "com.disneystreaming::weaver-cats:0.8.0"

import com.indoorvivants.weaver.playwright._
import cats.effect._

object Example extends weaver.IOSuite with PlaywrightIntegration {
  def sharedResource: Resource[IO, Res] = PlaywrightRuntime.create()

  pageTest("hello playwright!") { pc =>
    for {
      _ <- pc.page(_.navigate("https://playwright.dev"))

      _ <- pc.locator("text=Get Started").map(_.first().click())

      _ <- eventually(pc.page(_.url())) { url =>
        expect(url.contains("intro"))
      }
    } yield success
  }
}
```

Run it with `scala-cli test test.scala`
