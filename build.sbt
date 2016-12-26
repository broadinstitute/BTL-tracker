name := "tracker"

version := "2.8.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
	"org.reactivemongo" %% "play2-reactivemongo" % "0.10.5.0.akka23",  // Play 2.3
	//"org.scalatestplus.play" %% "scalatestplus-play" % "1.5.1" % "test",
	"org.scalatestplus" %% "play" % "1.2.0" % "test", // Scalatest with play
	"com.assembla.scala-incubator" %% "graph-core" % "1.11.0",
	"com.assembla.scala-incubator" %% "graph-dot" % "1.11.0",
	"org.apache.poi" % "poi" % "3.15",
	"org.apache.poi" % "poi-ooxml" % "3.15",
	"org.broadinstitute" %% "spreadsheets" % "1.0.6", // From https://svn.broadinstitute.org/BTL/barcodeProjects
	"org.broadinstitute" %% "sampleracks" % "1.12.3", // From https://svn.broadinstitute.org/BTL/LIMStales
	"org.broadinstitute" %% "mongo" % "1.16.0" // From https://svn.broadinstitute.org/BTL/LIMStales
)
