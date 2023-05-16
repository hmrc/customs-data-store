import uk.gov.hmrc.DefaultBuildSettings.{addTestReportOption, integrationTestSettings, targetJvm}
import scoverage.ScoverageKeys
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import play.core.PlayVersion.{current => currentPlayVersion}

val appName = "customs-data-store"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    majorVersion := 0,
    scalaVersion := "2.13.8",
    targetJvm := "jvm-11",
    libraryDependencies ++= compileDeps ++ testDeps,
    PlayKeys.playDefaultPort := 9893,
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;" +
      ".*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*Scheduler",
    ScoverageKeys.coverageMinimum := 98,
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

val bootstrap = "7.15.0"

val compileDeps = Seq(
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28" % "1.2.0",
  "uk.gov.hmrc" %% "bootstrap-backend-play-28" % bootstrap,
  "com.typesafe.play" %% "play-json-joda" % "2.9.2",
  "org.typelevel" %% "cats-core" % "2.3.0"
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.5" % "test",
  "com.typesafe.play" %% "play-test" % currentPlayVersion % "test",
  "org.pegdown" % "pegdown" % "1.6.0" % "test, it",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0" % "test, it",
  "org.mockito" % "mockito-core" % "4.0.0" % "test,it",
  "org.scalatestplus" %% "mockito-3-12" % "3.2.10.0",
  "com.vladsch.flexmark" % "flexmark-all" % "0.62.2" % "test,it",
  "uk.gov.hmrc" %% "bootstrap-test-play-28" % bootstrap % "test,it"
)
