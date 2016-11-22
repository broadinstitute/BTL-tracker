name := "tracker"

version := "2.6.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7" // Up to 2.11.8

libraryDependencies ++= Seq(
	"org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",  // Play 2.3
	"org.scalatestplus" %% "play" % "1.2.0" % "test", // Scalatest with play
	"com.assembla.scala-incubator" %% "graph-core" % "1.9.3", // Up to 1.11.0
	"com.assembla.scala-incubator" %% "graph-dot" % "1.10.0", // Up to 1.11.0
	"org.apache.poi" % "poi" % "3.9", // Up to 3.15
	"org.apache.poi" % "poi-ooxml" % "3.9", // Up to 3.15
	"org.broadinstitute" %% "spreadsheets" % "1.0.6",
	"org.broadinstitute" %% "sampleracks" % "1.12.3",
	"org.broadinstitute" %% "mongo" % "1.16.0"
)
