enablePlugins(PlayScala)

name := "oss-license-detector"

scalaVersion := "2.13.3"

libraryDependencies ++= Seq(
  ws,
  ehcache,
  guice,
  "org.apache.commons" % "commons-text" % "1.1",
  "org.scalatestplus.play" %% "scalatestplus-play" % "5.1.0"
)
