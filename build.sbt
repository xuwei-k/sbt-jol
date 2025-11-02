lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    scriptedLaunchOpts += s"-Dproject.version=${version.value}",
    organization := "com.github.xuwei-k",
    publishConfiguration := {
      val javaVersion = System.getProperty("java.specification.version")
      if (javaVersion != "1.8")
        throw new RuntimeException("Cancelling publish, please use JDK 1.8")
      publishConfiguration.value
    },
    libraryDependencies += Dependencies.jol,
    libraryDependencies += Dependencies.jolCli,
    libraryDependencies += Dependencies.scriptedPlugin,
    name := "sbt-jol",
    scalacOptions ++= List(
      "-unchecked",
      "-deprecation",
      "-encoding",
      "UTF-8"
    ),
    publishMavenStyle := false,
    licenses += ("Apache-2.0", url("http://www.apache.org/licenses/LICENSE-2.0.html")),
  )
