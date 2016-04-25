name := "CaseDAO"

version := "0.1-SNAPSHOT"

scalacOptions ++= Seq("-deprecation", "-unchecked", "-feature")

scalaVersion := "2.11.8"

libraryDependencies ++= Seq("org.reactivemongo" %% "reactivemongo" % "0.11.11")

libraryDependencies ++= Seq("org.reactivemongo" %% "reactivemongo-play-json" % "0.11.11")

libraryDependencies += "com.typesafe.play" %% "play-json" % "2.5.2"


organization := "com.github.agugaillard"

publishMavenStyle := true

publishTo := {
  val nexus = "https://oss.sonatype.org/"
  if (isSnapshot.value)
    Some("snapshots" at nexus + "content/repositories/snapshots")
  else
    Some("releases"  at nexus + "service/local/staging/deploy/maven2")
}

pomExtra := (
  <url>https://github.com/agugaillard/casedao</url>
  <scm>
    <url>git@github.com:agugaillard/casedao.git</url>
    <connection>scm:git:git@github.com:agugaillard/casedao.git</connection>
  </scm>)
