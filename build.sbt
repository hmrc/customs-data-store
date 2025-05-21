import scoverage.ScoverageKeys
import uk.gov.hmrc.DefaultBuildSettings.itSettings

val appName = "customs-data-store"

val silencerVersion = "1.7.14"
val scala3_3_3      = "3.3.3"

val testDirectory            = "test"
val scalaStyleConfigFile     = "scalastyle-config.xml"
val testScalaStyleConfigFile = "test-scalastyle-config.xml"

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := scala3_3_3

lazy val microservice = Project(appName, file("."))
  .enablePlugins(play.sbt.PlayScala, SbtDistributablesPlugin)
  .settings(
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    PlayKeys.playDefaultPort := 9893,
    libraryDependencySchemes ++= Seq("org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always),
    ScoverageKeys.coverageExcludedFiles := "<empty>;Reverse.*;.*filters.*;.*handlers.*;.*components.*;" +
      ".*javascript.*;.*Routes.*;.*GuiceInjector;" +
      ".*ControllerConfiguration;.*LanguageSwitchController;.*Scheduler",
    ScoverageKeys.coverageMinimumStmtTotal := 98,
    ScoverageKeys.coverageMinimumBranchTotal := 90,
    ScoverageKeys.coverageFailOnMinimum := false,
    ScoverageKeys.coverageHighlighting := true,
    scalafmtDetailedError := true,
    scalafmtPrintDiff := true,
    scalafmtFailOnErrors := true,
    scalacOptions := scalacOptions.value.diff(Seq("-Wunused:all")),
    Test / scalacOptions ++= Seq(
      "-Wunused:imports",
      "-Wunused:params",
      "-Wunused:implicits",
      "-Wunused:explicits",
      "-Wunused:privates"
    ),
    libraryDependencies ++= Seq(
      compilerPlugin(
        "com.github.ghik" % "silencer-plugin" % silencerVersion
          cross CrossVersion.for3Use2_13With("", ".12")
      ),
      "com.github.ghik" % "silencer-lib" % silencerVersion % Provided
        cross CrossVersion.for3Use2_13With("", ".12")
    )
  )
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
  .settings(
    libraryDependencies ++= Seq("uk.gov.hmrc" %% "bootstrap-test-play-30" % AppDependencies.bootstrapVersion % Test)
  )

addCommandAlias(
  "runAllChecks",
  ";clean;compile;coverage;test;it/test;scalafmtCheckAll;scalastyle;Test/scalastyle;coverageReport"
)
