inThisBuild(
  List(
    scalafixDependencies += "com.github.liancheng" %% "organize-imports" % Versions.OrganizeImports,
    semanticdbEnabled := true,
    organization      := "com.indoorvivants.playwright",
    organizationName  := "Anton Sviridov",
    homepage := Some(
      url("https://github.com/indoorvivants/weaver-playwright")
    ),
    startYear := Some(2022),
    licenses := List(
      "Apache-2.0" -> url("http://www.apache.org/licenses/LICENSE-2.0")
    ),
    developers := List(
      Developer(
        "keynmol",
        "Anton Sviridov",
        "keynmol@gmail.com",
        url("https://blog.indoorvivants.com")
      )
    )
  )
)

organization        := "com.indoorvivants.playwright"
sonatypeProfileName := "com.indoorvivants"

val Versions = new {

  val Scala212 = "2.12.17"

  val Scala213 = "2.13.10"

  val Scala3 = "3.2.2"

  val allScala = Seq(Scala3, Scala213, Scala212)

  val Weaver = "0.8.3"

  val CatsEffect = "3.4.8"

  val Cats = "2.9.0"

  val Playwright = "1.32.0"

  val OrganizeImports = "0.6.0"

  val Keypool = "0.4.8"

}

lazy val root = project
  .in(file("."))
  .aggregate(core.projectRefs*)
  .aggregate(weaver.projectRefs*)
  .settings(
    publish / skip      := true,
    publishLocal / skip := true
  )

lazy val core = projectMatrix
  .in(file("modules/core"))
  .settings(
    moduleName := "core",
    Test / scalacOptions ~= filterConsoleScalacOptions
  )
  .jvmPlatform(Versions.allScala)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"           %% "cats-core"          % Versions.Cats,
      "org.typelevel"           %% "cats-effect"        % Versions.CatsEffect,
      "org.typelevel"           %% "cats-effect-kernel" % Versions.CatsEffect,
      "org.typelevel"           %% "cats-effect-std"    % Versions.CatsEffect,
      "org.typelevel"           %% "keypool"            % Versions.Keypool,
      "com.microsoft.playwright" % "playwright"         % Versions.Playwright
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )

lazy val weaver = projectMatrix
  .in(file("modules/weaver"))
  .dependsOn(core)
  .settings(
    moduleName := "weaver",
    Test / scalacOptions ~= filterConsoleScalacOptions
  )
  .jvmPlatform(Versions.allScala)
  .enablePlugins(BuildInfoPlugin)
  .settings(
    libraryDependencies ++= Seq(
      "com.disneystreaming" %% "weaver-cats"        % Versions.Weaver % Test,
      "com.disneystreaming" %% "weaver-cats-core"   % Versions.Weaver,
      "com.disneystreaming" %% "weaver-core"        % Versions.Weaver,
      "org.typelevel"       %% "cats-core"          % Versions.Cats,
      "org.typelevel"       %% "cats-effect"        % Versions.CatsEffect,
      "org.typelevel"       %% "cats-effect-kernel" % Versions.CatsEffect
    ),
    testFrameworks += new TestFramework("weaver.framework.CatsEffect")
  )

lazy val docs = projectMatrix
  .jvmPlatform(Seq(Versions.Scala213))
  .in(file("myproject-docs"))
  .settings(
    mdocVariables := Map(
      "VERSION" -> version.value
    ),
    publish / skip      := true,
    publishLocal / skip := true
  )
  .dependsOn(weaver)
  .enablePlugins(MdocPlugin)

val scalafixRules = Seq(
  "OrganizeImports",
  "DisableSyntax",
  "LeakingImplicitClassVal",
  "NoValInForComprehension"
).mkString(" ")

val CICommands = Seq(
  "clean",
  "compile",
  "test",
  "docs/mdoc --in README.md",
  "scalafmtCheckAll",
  "scalafmtSbtCheck",
  s"scalafix --check $scalafixRules",
  "headerCheck",
  "undeclaredCompileDependenciesTest",
  "unusedCompileDependenciesTest",
  "missinglinkCheck"
).mkString(";")

val PrepareCICommands = Seq(
  s"scalafix --rules $scalafixRules",
  "scalafmtAll",
  "scalafmtSbt",
  "headerCreate"
).mkString(";")

addCommandAlias("ci", CICommands)

addCommandAlias("preCI", PrepareCICommands)

import ch.epfl.scala.sbtmissinglink.MissingLinkPlugin.missinglinkConflictsTag

ThisBuild / concurrentRestrictions += Tags.limit(missinglinkConflictsTag, 4)
