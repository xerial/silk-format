import sbt._
import sbt.Keys._
import xerial.sbt.Pack._

object Build extends sbt.Build {

  lazy val root = Project(
    id = "silk-text",
    base = file("."),
    settings = Defaults.defaultSettings ++ packSettings ++
      Seq(
        scalaVersion := "2.10.3",
        // Mapping from program name -> Main class
        packMain := Map("silkp" -> "xerial.silk.text.SilkTextMain"),
        // custom settings here
        crossPaths := false,
	libraryDependencies ++= Seq(
	  "org.xerial" % "xerial-core" % "3.2.2",
	  "org.scalatest" % "scalatest_2.10" % "2.0" % "test"
	)  
      )
  )
}
