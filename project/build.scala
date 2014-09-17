import sbt._
import Defaults._
import Keys._

object ApplicationBuild extends Build {

  val scalazVersion = "7.1.0"

  lazy val commonSettings = Defaults.defaultSettings ++ Seq(
    organization := "com.timeserieszen",
    credentials += Credentials(Path.userHome / ".ivy2" / ".credentials"),
    scalaVersion := "2.11.0",
    crossScalaVersions  := Seq("2.11.0", "2.10.3"),
    version := "0.01.0",
    resolvers ++= myResolvers,
    name := "timeserieszen",
    //fork := true,
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion,
      "org.scalaz.stream" %% "scalaz-stream" % "0.5a",
      "joda-time" % "joda-time" % "2.4",
      "com.typesafe" % "config" % "1.2.1",
      "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
      "org.scalaz" %% "scalaz-scalacheck-binding" % scalazVersion % "test"
    ),
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
}
