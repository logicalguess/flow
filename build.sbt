name := "pipeline"

version := "1.0"

scalaVersion := "2.11.7"

lazy val versions = new {
  val spray = "1.1.3"
  val akka = "2.3.9"
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

resolvers += "Job Server Bintray" at "https://dl.bintray.com/spark-jobserver/maven"


libraryDependencies ++= Seq(
  "ch.qos.logback" % "logback-classic" % versions.logback,
  "com.sksamuel.elastic4s" %% "elastic4s-core" % versions.elastic4s,
  "com.sksamuel.elastic4s" %% "elastic4s-jackson" % versions.elastic4sjackson,
  "org.json4s" %% "json4s-native" % versions.json4s,
  "org.sangria-graphql" %% "sangria" % versions.sangria,

  "org.scalaz" %% "scalaz-core" % "7.1.3",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",

  //"com.typesafe.akka"   %%  "akka-actor"    % versions.akka,
  //"com.typesafe.akka"   %%  "akka-testkit"  % versions.akka   % "test",
  //"com.chuusai" %% "shapeless" % "1.2.4",

  "com.twitter.finatra" %% "finatra-http" % versions.finatra,
  "com.twitter.finatra" %% "finatra-httpclient" % versions.finatra,
  "com.twitter.finatra" %% "finatra-slf4j" % versions.finatra,
  "com.twitter.inject" %% "inject-core" % versions.finatra,
  "com.github.spullara.mustache.java" % "compiler" % versions.mustache,
  "com.twitter.finatra" %% "finatra-http" % versions.finatra % "test",
  "com.twitter.finatra" %% "finatra-jackson" % versions.finatra % "test",
  "com.twitter.inject" %% "inject-server" % versions.finatra % "test",
  "com.twitter.inject" %% "inject-app" % versions.finatra % "test",
  "com.twitter.inject" %% "inject-core" % versions.finatra % "test",
  "com.twitter.inject" %% "inject-modules" % versions.finatra % "test",
  "com.google.inject.extensions" % "guice-testlib" % versions.guice % "test",
  "com.twitter.finatra" %% "finatra-http" % versions.finatra % "test" classifier "tests",
  "com.twitter.inject" %% "inject-server" % versions.finatra % "test" classifier "tests",
  "com.twitter.inject" %% "inject-app" % versions.finatra % "test" classifier "tests",
  "com.twitter.inject" %% "inject-core" % versions.finatra % "test" classifier "tests",
  "com.twitter.inject" %% "inject-modules" % versions.finatra % "test" classifier "tests",


  "org.spire-math" %% "cats" % "0.3.0",

  "org.apache.spark" %% "spark-core" % "1.6.0",
  "org.apache.spark" %% "spark-mllib" % "1.6.0",
  "com.databricks" %% "spark-csv" % "1.3.0",


  //"io.spray"            %%  "spray-can"     % versions.spray,
  //"io.spray"            %%  "spray-routing" % versions.spray,
  //"io.spray"            %%  "spray-testkit" % versions.spray  % "test",

  "com.github.intel-hadoop" %% "gearpump-core" % "0.7.1",
  "com.github.intel-hadoop" %% "gearpump-streaming" % "0.7.1",
  "com.github.intel-hadoop" %% "gearpump-core" % "0.7.1" % "test" classifier "tests",
  "com.github.intel-hadoop" %% "gearpump-streaming" % "0.7.1" % "test" classifier "tests",
  "com.typesafe.akka" %% "akka-testkit" % versions.akka % "test",

  "spark.jobserver" %% "job-server-api" % "0.6.1",
  "spark.jobserver" %% "job-server-extras" % "0.6.1",



"org.mockito" % "mockito-core" % versions.mockito % "test",
  "org.scalatest" %% "scalatest" % versions.scalatest % "test",
  "org.specs2" %% "specs2" % versions.specs2 % "test"
).map(
  _.excludeAll(ExclusionRule(organization = "org.mortbay.jetty"))
)