name := "distlock"

lazy val commonSettings = Seq(
  organization := "com.weirddev",
  organizationName := "WeirdDev",
  organizationHomepage := Some(url("http://weirddev.com")),
  version := "1.0.3",
  scalaVersion := "2.12.7",
  //  crossScalaVersions := Seq(/*"2.10.7",*/"2.11.12", "2.12.7"), //2.11 has compatibility issue with transform not accepting Try mapper func
  scalacOptions in Test ++= Seq("-Yrangepos"),
  description := "Distributed Lock library in scala. currently supported persistence stores: mongodb - depending on mongo-scala-driver",
  publishTo := {
    val nexus = "https://oss.sonatype.org"
    if (isSnapshot.value)
      Some("snapshots" at nexus + "/content/repositories/snapshots")
    else
      Some("releases"  at nexus + "/service/local/staging/deploy/maven2")
  },
  credentials += Credentials(Path.userHome / ".sbt" / ".credentials.txt"),
  scmInfo := Some(
    ScmInfo(
      url("https://github.com/wrdv/distlock"),
      "scm:git@github.com:wrdv/distlock.git"
    )
  ),
  licenses := List("Apache 2" -> new URL("http://www.apache.org/licenses/LICENSE-2.0.txt")),
  homepage := Some(url("https://github.com/wrdv/distlock")), //todo update project docs url
  developers := List(
    Developer(
      id    = "yaronyam",
      name  = "Yaron Yamin",
      email = "yarony@weirddev.com",
      url   = url("https://github.com/yaronyam")
    )
  ),
  publishMavenStyle := true,
) ++ Defaults.itSettings

lazy val commonDependencies  = Seq(
  "org.slf4j" % "slf4j-api" % "1.7.25",

  "ch.qos.logback" % "logback-classic" % "1.2.3" % "test,it",
  "org.specs2" %% "specs2-core" % "4.3.4" % "test,it",
  "org.specs2" %% "specs2-mock" % "4.3.4" % "test,it",
)

lazy val distlock = project
  .in(file("."))
  .settings(
    skip in publish := true,
  )
  .aggregate(`distlock-api`,`distlock-mongo`)

val `distlock-api` = project
  .configs(IntegrationTest)
  .settings(commonSettings,
    libraryDependencies ++= commonDependencies
  )

val `distlock-mongo` = project
  .configs(IntegrationTest)
  .dependsOn(`distlock-api`)
  .settings(commonSettings,
    libraryDependencies ++= commonDependencies ++ Seq(
      "org.mongodb.scala" %% "mongo-scala-driver" % "2.4.2",
    )
  )
