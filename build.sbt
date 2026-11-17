import ReleaseTransformations.*

val sbt2 = "2.1.0"
def sbt1 = "1.13.1"
val Scala3 = "3.9.0"
val Scala212 = "2.12.21"

val jol = "org.openjdk.jol" % "jol-core" % "0.17"

val root = rootProject.autoAggregate.settings(
  publish / skip := true,
  releaseProcess := Seq[ReleaseStep](
    checkSnapshotDependencies,
    inquireVersions,
    runClean,
    setReleaseVersion,
    commitReleaseVersion,
    tagRelease,
    releaseStepCommandAndRemaining("publishSigned"),
    releaseStepCommandAndRemaining("sonaRelease"),
    setNextVersion,
    commitNextVersion,
    pushChanges
  )
)

lazy val `sbt-jol-plugin` = (projectMatrix in file("sbt-jol"))
  .enablePlugins(SbtPlugin)
  .defaultAxes(VirtualAxis.jvm)
  .jvmPlatform(
    scalaVersions = Seq(Scala212, Scala3)
  )
  .settings(
    pluginCrossBuild / sbtVersion := {
      scalaBinaryVersion.value match {
        case "2.12" =>
          sbt1
        case _ =>
          sbt2
      }
    },
    scriptedLaunchOpts += s"-Dproject.version=${version.value}",
    scriptedBufferLog := false,
    organization := "com.github.xuwei-k",
    homepage := Some(uri("https://github.com/xuwei-k/sbt-jol")),
    pomExtra := (
      <developers>
        <developer>
          <id>xuwei-k</id>
          <name>Kenji Yoshida</name>
          <url>https://github.com/xuwei-k</url>
        </developer>
      </developers>
      <scm>
        <url>git@github.com:xuwei-k/sbt-jol.git</url>
        <connection>scm:git:git@github.com:xuwei-k/sbt-jol.git</connection>
      </scm>
    ),
    scalacOptions ++= {
      if (scalaBinaryVersion.value == "2.12") Seq("-Xsource:3", "-release:8")
      else Seq("-release:17")
    },
    libraryDependencies += jol,
    libraryDependencies += "io.get-coursier" % "interface" % "1.0.30",
    libraryDependencies += "org.scala-lang.modules" %% "scala-collection-compat" % "2.14.0",
    name := "sbt-jol",
    scalacOptions ++= List(
      "-unchecked",
      "-deprecation",
      "-encoding",
      "UTF-8"
    ),
    Compile / sourceGenerators += task {
      val f = (Compile / sourceManaged).value / "SbtJolBuildInfo.scala"
      IO.write(
        f,
        Seq(
          "package sbtjol",
          "",
          "private[sbtjol] object SbtJolBuildInfo {",
          s"""  def jolVersion: String = "${jol.revision}"""",
          "}",
          "",
        ).mkString("\n")
      )
      Seq(f)
    },
    publishTo := (if (isSnapshot.value) None else localStaging.value),
    licenses += ("Apache-2.0", uri("http://www.apache.org/licenses/LICENSE-2.0.html")),
  )
