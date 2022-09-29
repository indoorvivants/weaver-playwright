# [Weaver](https://disneystreaming.github.io/weaver-test/) + [Playwright](https://playwright.dev/java/docs/intro)

Love Weaver? Like Playwright? Forced to write tests?

Very small integration between Playwright and Weaver to facilitate browser testing of applications.

Here's an example of usage which is also a [Scala CLI](https://scala-cli.virtuslab.org/) script!

**test.scala**
```scala mdoc
//> using lib "com.indoorvivants.playwright::weaver:0.0.2"
//> using lib "com.disneystreaming::weaver-cats:0.8.0"

import com.indoorvivants.weaver.playwright._
import cats.effect._

object Example extends weaver.IOSuite with PlaywrightSpec {
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

Run it with `scala-cli test test.scala`.

For more common build tools:

**SBT**
```scala
"com.indoorvivants.weaver" %% "playwright" % "<version>" % Test
```

**Mill**
```scala
ivy"com.indoorvivants.weaver::playwright:<version>"
```


## What does it do?

1. Wraps Playwright and browser instantiation into a `Resource`

   - And also handles bonkers exceptions it throws when the drivers are created concurrently

2. Does _minimal_ wrapping of the side-effectful, non-RT APIs that Playwright exposes 

   - As I learn more about frontend testing, the number of wrapped APIs should increase,
       but the goal is to mainly access them via the `.page(...)` combinator

3. Puts semaphores in place to abide by Playwright's [recommendation](https://playwright.dev/java/docs/test-runners#running-tests-in-parallel)

4. Provides a `eventually` combinator for retrying assertions

As you can see, it's intentionally minimal
