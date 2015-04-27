name := "tracker"

version := "1.11"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.6"

libraryDependencies ++= Seq(
	"org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",  // Play 2.3
	"org.scalatestplus" %% "play" % "1.2.0" % "test", // Scalatest with play
	"com.assembla.scala-incubator" %% "graph-core" % "1.9.1",
	"com.assembla.scala-incubator" %% "graph-dot" % "1.10.0",
	"org.apache.poi" % "poi" % "3.9",
	"org.apache.poi" % "poi-ooxml" % "3.9",
	"org.broadinstitute" %% "spreadsheets" % "1.0.5",
	"org.broadinstitute" %% "sampleracks" % "1.9.10",
	"org.broadinstitute" %% "mongo" % "1.10.0"
)
