lazy val root = (project in file(".")).enablePlugins(PlayScala)

name := "oss-license-detector"

scalaVersion := "2.12.4"

libraryDependencies ++= Seq(
  ws,
  ehcache,
  guice,
  "org.apache.commons" % "commons-text" % "1.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "3.1.2"
)
