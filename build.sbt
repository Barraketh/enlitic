name := "Enlitic Wiki Classifier"

scalaVersion := "2.10.6"

libraryDependencies ++=  Seq(
  "org.scalaj" %% "scalaj-http" % "2.2.0",
  "org.jsoup" % "jsoup" % "1.8.3",
  "org.apache.spark" % "spark-mllib_2.10" % "1.5.2"


)
