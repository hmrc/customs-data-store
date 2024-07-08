import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.{targetJvm, itSettings}

val appName = "customs-data-store"

val silencerVersion = "1.7.16"
val bootstrapVersion = "9.0.0"
val scala3_3_3 = "3.3.3"

val testDirectory = "test"
val scalaStyleConfigFile = "scalastyle-config.xml"
val testScalaStyleConfigFile = "test-scalastyle-config.xml"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala3_3_3

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtAutoBuildPlugin, SbtGitVersioning, SbtDistributablesPlugin)
  .settings(
    targetJvm := "jvm-11",
    libraryDependencies ++= compileDeps ++ testDeps,
    PlayKeys.playDefaultPort := 9893,

    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),

    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;" +
      ".*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*Scheduler",

    ScoverageKeys.coverageMinimumStmtTotal := 98,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := true,
    ScoverageKeys.coverageHighlighting := true,

    scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")),

    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"),

    libraryDependencies ++= Seq(
      compilerPlugin(
        "com.github.ghik" % "silencer-plugin" % silencerVersion
          cross CrossVersion.for3Use2_13With("", ".12")),

        "com.github.ghik" % "silencer-lib" % silencerVersion % Provided
          cross CrossVersion.for3Use2_13With("", ".12")
    )
  )
  .settings(resolvers += Resolver.jcenterRepo)
  .settings(Test / parallelExecution := false)
  .settings(scalastyleSettings)
  .disablePlugins(sbt.plugins.JUnitXmlReportPlugin)

lazy val scalastyleSettings = Seq(
  scalastyleConfig := baseDirectory.value / scalaStyleConfigFile,
  (Test / scalastyleConfig) := baseDirectory.value / testDirectory / testScalaStyleConfigFile
)

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(itSettings())
  .settings(libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test))

val compileDeps = Seq(
  play.sbt.PlayImport.ws,
  "uk.gov.hmrc" %% "bootstrap-backend-play-30" % bootstrapVersion,
  "org.typelevel" %% "cats-core" % "2.12.0",
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % "2.1.0",
)

val testDeps = Seq(
  "org.scalatest" %% "scalatest" % "3.2.19" % "test",
  "org.scalatestplus" %% "mockito-4-11" % "3.2.18.0" ,
  "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % "test" ,
  "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30" % "2.1.0"
)

addCommandAlias("runAllChecks", ";clean;compile;coverage;test;it/test;scalastyle;Test/scalastyle;coverageReport")
