import play.core.PlayVersion.{current => currentPlayVersion}
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, targetJvm, itSettings}

val appName = "customs-data-store"

val silencerVersion = "1.7.16"
val bootstrapVersion = "8.4.0"
val scala2_13_12 = "2.13.12"

val testDirectory = "test"
val scalaStyleConfigFile = "scalastyle-config.xml"
val testScalaStyleConfigFile = "test-scalastyle-config.xml"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala2_13_12

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    targetJvm := "jvm-11",
    libraryDependencies ++= compileDeps ++ testDeps,
    PlayKeys.playDefaultPort := 9893,

    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;" +
      ".*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*Scheduler",

    ScoverageKeys.coverageMinimumStmtTotal := 98,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,

    scalacOptions ++= Seq(
      "-P:silencer:pathFilters=routes",
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"),

    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:patvars",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"),

    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )
  .configs(IntegrationTest)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(Test / parallelExecution := false)
  .settings(scalastyleSettings)
  .settings(addTestReportOption(IntegrationTest, "int-test-reports"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val scalastyleSettings = Seq(
  scalastyleConfig := baseDirectory.value / scalaStyleConfigFile,
  (Test / scalastyleConfig) := baseDirectory.value/ testDirectory / testScalaStyleConfigFile
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
  .settings(libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-29" % bootstrapVersion % Test))

val compileDeps = Seq(
  play.sbt.PlayImport.ws,
  "uk.gov.hmrc" %% "bootstrap-frontend-play-29" % bootstrapVersion,
  "org.typelevel" %% "cats-core" % "2.10.0",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-29" % "1.6.0",
  "com.typesafe.play" %% "play-json-joda" % "2.9.4"
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.16" % "test",
  "com.typesafe.play" %% "play-test" % currentPlayVersion % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test, it",
  "org.mockito" % "mockito-core" % "5.4.0" % "test,it",
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0",
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % "test,it",
  "uk.gov.hmrc" %% "bootstrap-test-play-29" % bootstrapVersion % "test,it",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-29" % "1.6.0"
)
