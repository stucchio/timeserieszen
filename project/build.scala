import sbt._
import Defaults._
import Keys._

object ApplicationBuild extends Build {

  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.timeserieszen",
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    scalaVersion := "2.11.0",
    crossScalaVersions  := Seq("2.11.0", "2.10.3"),
    version := "0.01.0",
    resolvers ++= myResolvers,
    name := "timeserieszen",
    //fork := true,
    libraryDependencies ++= Dependencies.scalazDeps ++ Dependencies.loggingDeps ++ Dependencies.miscDeps ++ Dependencies.http4sDeps ++ Dependencies.rngDeps ++ Dependencies.testDeps,
    publishTo := Some(Resolver.file("file",  new File( "/tmp/injera-publish" )) )
  )

  val myResolvers = Seq(
    "Sonatatype Snapshots" at "http://oss.sonatype.org/content/repositories/snapshots",
    "Sonatatype Releases" at "http://oss.sonatype.org/content/repositories/releases",
    "Typesafe Releases" at "http://repo.typesafe.com/typesafe/releases",
    "Typesafe Snapshots" at "http://repo.typesafe.com/typesafe/snapshots",
    "Scalaz Bintray Repo" at "http://dl.bintray.com/scalaz/releases",
    "Coda Hale" at "http://repo.codahale.com"
  )

  lazy val timeserieszen = Project("timeserieszen", file("."), settings = commonSettings)

  object Dependencies {
    val logbackVersion = "1.0.13"
    val logbackClassic = "ch.qos.logback" % "logback-classic" % logbackVersion
    val logbackCore = "ch.qos.logback" % "logback-core" % logbackVersion
    val slf4j = "org.slf4j" % "slf4j-api" % "1.6.4"

    val metricsVersion = "3.1.0"
    val codahaleMetricsBase = "io.dropwizard.metrics" % "metrics-core" % metricsVersion
    val codahaleMetricsGraphite = "io.dropwizard.metrics" % "metrics-graphite" % metricsVersion
    val loggingDeps = Seq(slf4j, logbackCore, logbackClassic, codahaleMetricsBase, codahaleMetricsGraphite)

    val scalazVersion = "7.1.0"
    val scalaz = "org.scalaz" %% "scalaz-core" % scalazVersion
    val scalazStream = "org.scalaz.stream" %% "scalaz-stream" % "0.6a"
    val scalazDeps = Seq(scalaz, scalazStream)


    // val http4sVersion = "0.5.0-SNAPSHOT" // doesn't work
    val http4sVersion = "0.4.1" // https://github.com/http4s/http4s/issues/75
    val json4sCorerevision = "3.2.10"
    val http4sCore  = "org.http4s" %% "http4s-core"      % http4sVersion
    val http4sServer  = "org.http4s" %% "http4s-server"    % http4sVersion
    val http4sDSL   = "org.http4s" %% "http4s-dsl"         % http4sVersion
    val http4sBlaze = "org.http4s" %% "http4s-blazeserver" % http4sVersion
    val http4sJetty = "org.http4s" %% "http4s-servlet"     % http4sVersion
    val json4sCore          = "org.json4s"               %% "json4s-core"             % json4sCorerevision
    val json4sJackson       = "org.json4s"               %% "json4s-jackson"          % json4sCorerevision
    val json4sNative        = "org.json4s"               %% "json4s-native"           % json4sCorerevision

    val http4sDeps = Seq(http4sCore, http4sServer, http4sServer, http4sDSL, http4sBlaze, http4sJetty, json4sCore, json4sJackson, json4sNative)

    val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.3" % "test"
    val scalazScalacheck = "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion % "test"
    val testDeps = Seq(scalaCheck, scalazScalacheck)

    val rngDeps = Seq("com.nicta" %% "rng" % "1.3.0")

    val guava = "com.google.guava" % "guava" % "14.0"
    val joda = "joda-time" % "joda-time" % "2.4"
    val config = "com.typesafe" % "config" % "1.2.1"
    val miscDeps = Seq(guava, joda, config)
  }
}
