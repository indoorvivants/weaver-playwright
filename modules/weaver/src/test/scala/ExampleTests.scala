import java.nio.file.Paths

import scala.concurrent.duration._

import cats.effect.IO
import cats.effect.Resource
import cats.syntax.all._
import com.indoorvivants.weaver.playwright._
import weaver.GlobalWrite

object SharedResources extends weaver.GlobalResource {
  override def sharedResources(global: GlobalWrite): Resource[IO, Unit] =
    PlaywrightRuntime.create().flatMap(global.putR(_)).void
}

class BasicTests(global: weaver.GlobalRead)
    extends weaver.IOSuite
    with PlaywrightSpec {

  override def sharedResource: Resource[IO, Res] =
    global.getOrFailR[PlaywrightRuntime]()

  val filepath = Paths.get("test.html").toAbsolutePath()
  val uri      = "file://" + filepath.toString

  pageTest("check title") { pc =>
    for {
      _     <- pc.page(_.navigate(uri))
      title <- pc.page(_.title())
    } yield expect(title == "Weaver Playwright")
  }

  pageTest("click button") { pc =>
    val invalidCredentials =
      pc.locator("text=Invalid credentials").map(_.count())

    for {
      _ <- pc.page(_.navigate(uri))
      _ <- eventually(invalidCredentials)(ic => expect(ic == 0))
      _ <- pc.locator("text=submit").map(_.first().click())
      _ <-
        eventually(invalidCredentials)(ic => expect(ic == 1))
          .onError(_ =>
            pc.screenshot(Paths.get("failed-to-see-invalid-credentials.png"))
          )
    } yield success
  }
}
