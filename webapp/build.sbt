name := "tippWM"

version := "1.0-SNAPSHOT"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache,
  "mysql" % "mysql-connector-java" % "5.1.17",
  "org.scala-lang" %% "scala-pickling" % "0.8.0"
)



play.Project.playScalaSettings
