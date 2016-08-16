name := """play-slick-multitenant-seed"""

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  "org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % Test,
//  JWT
  "com.pauldijou" %% "jwt-play" % "0.8.0",
//  PostGres
  "org.postgresql" % "postgresql" % "9.4.1208.jre7",
//  Play-Slick Plug in
  "com.typesafe.play" %% "play-slick" % "2.0.0",
  "com.typesafe.play" %% "play-slick-evolutions" % "2.0.0",

//  Joda time
  "joda-time" % "joda-time" % "2.7",
  "org.joda" % "joda-convert" % "1.7",

//  Scala-Time, wrapper of Joda Time
  "com.github.nscala-time" %% "nscala-time" % "2.12.0",
//  Mapping for joda-time to slick column
  "com.github.tototoshi" %% "slick-joda-mapper" % "2.2.0",
// Scala-Z
  "org.scalaz" %% "scalaz-core" % "7.2.4"
)

fork in run := false
