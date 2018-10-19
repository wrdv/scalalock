
name := "distlock"

version := "1.0.1"

scalaVersion := "2.12.7"

//resolvers +=  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val root = (project in file(".")).configs(IntegrationTest).settings(Defaults.itSettings: _*)
libraryDependencies ++= Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",
  "org.mongodb.scala" %% "mongo-scala-driver" % "2.4.2", //todo move dependency to relevant module

  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test,it",
  "org.specs2" %% "specs2-core" % "4.3.4" % "test,it",
  "org.specs2" %% "specs2-mock" % "4.3.4" % "test,it",

)


scalacOptions in Test ++= Seq("-Yrangepos")