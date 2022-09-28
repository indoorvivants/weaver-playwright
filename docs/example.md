
```scala mdoc
//> using lib "com.indoorvivants.playwright::weaver:@VERSION@"
//> using lib "com.disneystreaming::weaver-cats:0.8.0"

import com.indoorvivants.weaver.playwright._

object Example extends weaver.IOSuite with PlaywrightIntegration {
  override def sharedResource = PlaywrightRuntime.create()

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

The current version is `@VERSION@`
