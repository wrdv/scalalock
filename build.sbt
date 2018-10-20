name := "distlock"
organization := "com.weirddev.scala"
version := "1.0.1"

//resolvers +=  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"

lazy val settings = Seq(
  scalaVersion := "2.12.7",
  scalacOptions in Test ++= Seq("-Yrangepos"),

) ++ Defaults.itSettings

lazy val commonDependencies  = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",

  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test,it",
  "org.specs2" %% "specs2-core" % "4.3.4" % "test,it",
  "org.specs2" %% "specs2-mock" % "4.3.4" % "test,it",
)

lazy val distlock = project
  .in(file("."))
  .aggregate(`distlock-api`,`distlock-mongo`)

val `distlock-api` = project
  .configs(IntegrationTest)
  .settings(settings,
    libraryDependencies ++= commonDependencies
  )

val `distlock-mongo` = project
  .configs(IntegrationTest)
  .dependsOn(`distlock-api`)
  .settings(settings,
    libraryDependencies ++= commonDependencies ++ Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.4.2",
    )
  )
