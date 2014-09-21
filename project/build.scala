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
    libraryDependencies ++= Dependencies.scalazDeps ++ Dependencies.loggingDeps ++ Dependencies.miscDeps ++ Dependencies.testDeps,
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
    val scalazStream = "org.scalaz.stream" %% "scalaz-stream" % "0.5a"
    val scalazDeps = Seq(scalaz, scalazStream)

    val scalaCheck = "org.scalacheck" %% "scalacheck" % "1.11.3" % "test"
    val scalazScalacheck = "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion % "test"
    val testDeps = Seq(scalaCheck, scalazScalacheck)

    val guava = "com.google.guava" % "guava" % "14.0"
    val joda = "joda-time" % "joda-time" % "2.4"
    val config = "com.typesafe" % "config" % "1.2.1"
    val miscDeps = Seq(guava, joda, config)
  }
}
