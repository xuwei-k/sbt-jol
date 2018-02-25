import bintray.Keys._

crossSbtVersions := Seq("0.13.17", "1.1.1")

sbtPlugin := true

organization := "pl.project13.sbt"
name := "sbt-jol"

scalacOptions ++= List(
  "-unchecked",
  "-deprecation",
  "-language:_",
  "-target:jvm-1.6",
  "-encoding", "UTF-8"
)

libraryDependencies += Dependencies.jol
libraryDependencies += Dependencies.jolCli

publishTo := {
  if (isSnapshot.value) Some(Classpaths.sbtPluginSnapshots) else Some(Classpaths.sbtPluginReleases)
}

// publishing settings

publishMavenStyle := false
licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html"))
bintrayPublishSettings
repository in bintray := "sbt-plugins"
bintrayOrganization in bintray := None

scriptedSettings
scriptedLaunchOpts += s"-Dproject.version=${version.value}"
