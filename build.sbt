import uk.gov.hmrc.DefaultBuildSettings.integrationTestSettings
import uk.gov.hmrc.sbtdistributables.SbtDistributablesPlugin.publishingSettings
import play.core.PlayVersion.{current => currentPlayVersion}

val appName = "customs-data-store"

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    majorVersion                     := 0,
    scalaVersion                     := "2.12.11",
    libraryDependencies              ++= compileDeps ++ testDeps
  )
  .settings(publishingSettings: _*)
  .configs(IntegrationTest)
  .settings(integrationTestSettings(): _*)
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(parallelExecution in Test := false)


val compileDeps = Seq(

  "uk.gov.hmrc"             %% "simple-reactivemongo"     % "8.0.0-play-27",
  "uk.gov.hmrc"             %% "bootstrap-backend-play-27"        % "4.1.0"
)

val testDeps = Seq(
  "org.scalatest"           %% "scalatest"                % "3.2.5"                 % "test",
  "com.typesafe.play"       %% "play-test"                % currentPlayVersion      % "test",
  "org.pegdown"             %  "pegdown"                  % "1.6.0"                 % "test, it",
  "org.scalatestplus.play"  %% "scalatestplus-play"       % "4.0.3"                 % "test, it",
  "org.mockito" % "mockito-core" % "3.8.0" % "test,it",
  "org.scalatestplus"      %% "mockito-3-2"          % "3.1.2.0",
  "com.vladsch.flexmark"    % "flexmark-all"           % "0.36.8"
)
