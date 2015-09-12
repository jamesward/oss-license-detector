name := "oss-license-detector"

version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  ws,
  cache,
  "org.apache.commons" % "commons-lang3" % "3.4",
  "org.scalatestplus" %% "play" % "1.2.0" % "test"
)


fork in run := true