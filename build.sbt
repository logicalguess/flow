name := "pipeline"

version := "1.0"

scalaVersion := "2.11.8"

lazy val versions = new {
  val finatra = "2.1.2"
  val mustache = "0.9.1"
  val logback = "1.0.13"
  val guice = "4.0"
  val mockito = "1.9.5"
  val elastic4s = "2.2.0"
  val elastic4sjackson = "2.2.0"
  val sangria = "0.4.3"
  var json4s = "3.2.11"
  val scalatest = "2.2.3"
  val specs2 = "2.3.12"
}

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  "Twitter Maven" at "https://maven.twttr.com",
  "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases/",
  Resolver.url("scala sbt",  url("http://repo.scala-sbt.org/scalasbt/sbt-plugin-releases"))(Resolver.ivyStylePatterns),
  Resolver.url("typesafe ivy",  url("http://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)
)

libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % versions.logback,
  "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
  "com.sksamuel.elastic4s" %% "elastic4s-jackson" % versions.elastic4sjackson,
  "org.json4s" %% "json4s-native" % versions.json4s,
  "org.sangria-graphql" %% "sangria" % versions.sangria,

  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  "com.github.mpilquist" %% "simulacrum" % "0.3.0",
  "org.spire-math" %% "cats" % "0.3.0",

  "org.apache.spark" %% "spark-core" % "1.6.0",
  "org.apache.spark" %% "spark-mllib" % "1.6.0",
  "com.databricks" %% "spark-csv" % "1.3.0",

  "org.mockito" % "mockito-core" % versions.mockito % "test",
  "org.scalatest" %% "scalatest" % versions.scalatest % "test",
  "org.specs2" %% "specs2" % versions.specs2 % "test"
).map(
  _.excludeAll(ExclusionRule(organization = "org.mortbay.jetty"))
)