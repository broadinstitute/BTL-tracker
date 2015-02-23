name := "tracker"

version := "0.4"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.5"

libraryDependencies ++= Seq(
  "org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",
  "org.broadinstitute" %% "sampleracks" % "1.9.8",
  "org.broadinstitute" %% "mongo" % "1.9.7"
)
