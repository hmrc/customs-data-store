import sbt.*

object AppDependencies {

  val bootstrapVersion = "9.11.0"

  val compile: Seq[ModuleID] = Seq(
    play.sbt.PlayImport.ws,
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.typelevel"     %% "cats-core"                 % "2.12.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % "2.6.0"
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatestplus" %% "mockito-4-11"           % "3.2.18.0",
    "uk.gov.hmrc"       %% "bootstrap-test-play-30" % bootstrapVersion % "test"
  )
}
