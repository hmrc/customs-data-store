import play.core.PlayVersion.{current => currentPlayVersion}
import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, itSettings, targetJvm}

val appName = "customs-data-store"
val bootstrap = "7.22.0"
val silencerVersion = "1.17.13"

val scala2_13_8 = "2.13.8"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala2_13_8

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    targetJvm := "jvm-11",
    libraryDependencies ++= compileDeps ++ testDeps,
    PlayKeys.playDefaultPort := 9893,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;" +
      ".*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*Scheduler",
    ScoverageKeys.coverageMinimumBranchTotal := 98,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
    scalacOptions += "-P:silencer:pathFilters=routes",
    libraryDependencies ++= Seq(
      compilerPlugin("com.github.ghik" % "silencer-plugin" % silencerVersion cross CrossVersion.full),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided cross CrossVersion.full
    )
  )
  .configs(IntegrationTest)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(Test / parallelExecution := false)
  .settings(addTestReportOption(IntegrationTest, "int-test-reports"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test") // the "test->test" allows reusing test code and test dependencies
  .settings(itSettings)
  .settings(libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrap % Test))

val compileDeps = Seq(
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "1.3.0",
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrap,
  "com.typesafe.play" %% "play-json-joda" % "2.9.4",
  "org.typelevel" %% "cats-core" % "2.9.0"
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.16" % "test",
  "com.typesafe.play" %% "play-test" % currentPlayVersion % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test, it",
  "org.mockito" % "mockito-core" % "5.4.0" % "test,it",
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0",
  "com.vladsch.flexmark" % "flexmark-all" % "0.64.8" % "test,it",
  "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrap % "test,it",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "1.3.0"
)
