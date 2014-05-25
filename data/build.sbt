name := "Tipp WM Calculator"

version := "0.1"

scalaVersion := "2.11.0"

sbtVersion := "0.13.0"


libraryDependencies ++= {
  Seq(
    "org.jsoup" % "jsoup" % "1.7.3",
    "org.scalaj" %% "scalaj-http" % "0.3.15",
    "org.scalatest" % "scalatest_2.11" % "2.1.6" % "test",
    "com.typesafe.akka" %% "akka-actor" % "2.3.3",
    "mysql" % "mysql-connector-java" % "5.1.17",
    "org.scala-lang" %% "scala-pickling" % "0.8.0"
  )
}
