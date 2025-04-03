resolvers += "HMRC-open-artefacts-maven2" at "https://open.artefacts.tax.service.gov.uk/maven2"

resolvers += Resolver.url("HMRC-open-artefacts-ivy", url("https://open.artefacts.tax.service.gov.uk/ivy2"))(
  Resolver.ivyStylePatterns
)

resolvers += Resolver.typesafeRepo("releases")

// Resolves versions conflict
ThisBuild / libraryDependencySchemes += "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always

addSbtPlugin("uk.gov.hmrc"       % "sbt-auto-build"     % "3.24.0")
addSbtPlugin("uk.gov.hmrc"       % "sbt-distributables" % "2.6.0")
addSbtPlugin("org.playframework" % "sbt-plugin"         % "3.0.3")
addSbtPlugin("org.scoverage"     % "sbt-scoverage"      % "2.0.9")

addSbtPlugin(
  "org.scalastyle" %% "scalastyle-sbt-plugin" %
    "1.0.0" exclude ("org.scala-lang.modules", "scala-xml_2.12")
)

addSbtPlugin("com.typesafe.sbt" % "sbt-uglify"   % "2.0.0")
addSbtPlugin("org.scalameta"    % "sbt-scalafmt" % "2.5.2")
