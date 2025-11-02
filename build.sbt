val jol = "org.openjdk.jol" % "jol-core" % "0.17"

lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    scriptedLaunchOpts += s"-Dproject.version=${version.value}",
    organization := "com.github.xuwei-k",
    homepage := Some(url("https://github.com/xuwei-k/sbt-jol")),
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
    publishConfiguration := {
      val javaVersion = System.getProperty("java.specification.version")
      if (javaVersion != "1.8")
        throw new RuntimeException("Cancelling publish, please use JDK 1.8")
      publishConfiguration.value
    },
    libraryDependencies += jol,
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
    publishMavenStyle := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  )
