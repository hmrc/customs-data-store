import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, integrationTestSettings, scalaSettings}
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import play.core.PlayVersion.{current => currentPlayVersion}

val appName = "customs-data-store"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.12.11",
    libraryDependencies ++= compileDeps ++ testDeps,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;" +
      ".*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController",
    ScoverageKeys.coverageMinimum := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(parallelExecution in Test := false)
  .settings(addTestReportOption(IntegrationTest, "int-test-reports"))
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

val compileDeps = Seq(
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "0.50.0",
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % "5.3.0",
  "com.typesafe.play" %% "play-json-joda" % "2.9.2",
  "org.typelevel" %% "cats-core" % "2.3.0"
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.5" % "test",
  "com.typesafe.play" %% "play-test" % currentPlayVersion % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "4.0.3" % "test, it",
  "org.mockito" % "mockito-core" % "3.8.0" % "test,it",
  "org.scalatestplus" %% "mockito-3-2" % "3.1.2.0",
  "com.vladsch.flexmark" % "flexmark-all" % "0.36.8"
)
