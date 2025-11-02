package sbtjol

import lmcoursier.internal.shaded.coursier
import org.openjdk.jol.vm.VM
import sbt.*
import sbt.CacheImplicits.*
import sbt.Def.Initialize
import sbt.Keys.*
import sbt.complete.DefaultParsers
import sbt.complete.Parser
import xsbt.api.Discovery
import xsbti.compile.CompileAnalysis

object JolPlugin extends sbt.AutoPlugin {

  import autoImport.*

  override def requires = sbt.plugins.JvmPlugin

  override def trigger = allRequirements

  override def projectSettings: Seq[Def.Setting[?]] = Seq(
    Jol / run := runJolTask(Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    Jol / version := SbtJolBuildInfo.jolVersion,
    Jol / vmDetails := runVmDetailsTask().evaluated,
    Jol / estimates := runJolTask("estimates", Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    Jol / externals := runJolTask("externals", Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    Jol / footprint := runJolTask("footprint", Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    Jol / heapdump := runJolTask("heapdump", Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    Jol / idealpack := runJolTask("idealpack", Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    Jol / internals := runJolTask("internals", Compile / fullClasspath).dependsOn(Compile / compile).evaluated,
    // TODO: stringCompress in jol <<= runJolTask("string-compress", Compile / fullClasspath).dependsOn(Compile / compile),

    Jol / discoveredClasses := Seq.empty,
    // TODO tab auto-completion break if use `:=` and `.value`
    // https://github.com/sbt/sbt/issues/1444
    // `<<=` operator is removed. Use `key
    Jol / discoveredClasses := (Compile / compile)
      .map(discoverAllClasses)
      .storeAs(Jol / discoveredClasses)
      .triggeredBy(Compile / compile)
      .value
  )

  def runJolTask(classpath: Initialize[Task[Classpath]]): Initialize[InputTask[Unit]] = {
    val parser = loadForParser(Jol / discoveredClasses)((s, names) => runJolModesParser(s, modes, names getOrElse Nil))
    Def.inputTask {
      val (mode, className, args) = parser.parsed
      runJol(
        streams.value.log,
        (Jol / version).value,
        JolPluginCompat.classpathToFiles(classpath.value),
        mode :: className :: args.toList,
        (Jol / forkOptions).value
      )
    }
  }
  def runJolTask(mode: String, classpath: Initialize[Task[Classpath]]): Initialize[InputTask[Unit]] = {
    val parser = loadForParser(Jol / discoveredClasses)((s, names) => runJolParser(s, names getOrElse Nil))
    Def.inputTask {
      val (className, args) = parser.parsed
      runJol(
        streams.value.log,
        (Jol / version).value,
        JolPluginCompat.classpathToFiles(classpath.value),
        mode :: className :: args.toList,
        (Jol / forkOptions).value
      )
    }
  }

  def runJol(log: Logger, jolVersion: String, classpath: Seq[File], args: Seq[String], forkOps: ForkOptions): Unit = {

    // TODO not needed, but at least confirms HERE we're able to see the class, sadly if we call JOL classes they won't...
    //      val si = (scalaInstance in console).value
    //      val loader = sbt.classpath.ClasspathUtilities.makeLoader(cpFiles, si)
    //      val clazz = loader.loadClass(className) // make sure we can load it
    //      Thread.currentThread().setContextClassLoader(loader)

    val jolDeps = getArtifact("org.openjdk.jol", "jol-cli", jolVersion)

    val allArg = args ++ cpOption(classpath)
    log.debug(s"jol: ${allArg.mkString(" ")}")

    val javaClasspath = (jolDeps ++ classpath).mkString(":")
    val result = Fork.java.apply(
      forkOps.withOutputStrategy(
        forkOps.outputStrategy.getOrElse(
          OutputStrategy.LoggedOutput(log)
        )
      ),
      Seq(
        "-cp",
        javaClasspath,
        "org.openjdk.jol.Main",
      ) ++ allArg,
    )
    if (result != 0) {
      sys.error(s"jol return ${result}")
    }
    // TODO if anyone can figure out how to make jol not fail with ClassNotFound here that'd be grand (its tricky as it really wants to use the system loader...)
    //      org.openjdk.jol.Main.main("estimates", className, cpOption(cpFiles))
  }

  private def getArtifact(groupId: String, artifactId: String, revision: String): Seq[File] = {
    val dependency = coursier.Dependency(
      coursier.Module(
        coursier.Organization(
          groupId
        ),
        coursier.ModuleName(
          artifactId
        ),
      ),
      revision
    )
    coursier.Fetch().addDependencies(dependency).runResult().files
  }

  private def cpOption(cpFiles: Seq[File]): Seq[String] =
    Seq("-cp", cpFiles.mkString(":"))

  def runVmDetailsTask(): Initialize[InputTask[Unit]] = {
    Def.inputTask {
      streams.value.log.info(VM.current().details())
    }
  }

  private def discoverAllClasses(analysis: CompileAnalysis): Seq[String] =
    Discovery.applications(Tests.allDefs(analysis)).collect({ case (definition, discovered) => definition.name })

  private def runJolParser: (State, Seq[String]) => Parser[(String, Seq[String])] = {
    import DefaultParsers.*
    (state, mainClasses) => Space ~> token(NotSpace.examples(mainClasses.toSet)) ~ spaceDelimited("<arg>")
  }
  private def runJolModesParser: (State, Seq[String], Seq[String]) => Parser[(String, String, Seq[String])] = {
    import DefaultParsers.*
    (state, modes, mainClasses) =>
      val parser = Space ~> (token(NotSpace.examples(modes.toSet)) ~ (Space ~> token(
        NotSpace.examples(mainClasses.toSet)
      ))) ~ spaceDelimited("<arg>")
      parser map { o => (o._1._1, o._1._2, o._2) }
  }

  val modes = List(
    "estimates",
    "externals",
    "footprint",
    "heapdump",
    "idealpack",
    "internals" // ,
    // TODO:   "stringCompress"
  )

  object autoImport {
    // !! Annoying compiler error configuration id must be capitalized
    final val Jol = sbt.config("jol").extend(sbt.Configurations.CompileInternal)

    lazy val vmDetails = inputKey[Unit]("Show vm details")

    lazy val estimates = inputKey[Unit]("Simulate the class layout in different VM modes.")
    lazy val externals = inputKey[Unit]("Show the object externals: the objects reachable from a given instance.")
    lazy val footprint = inputKey[Unit]("Estimate the footprint of all objects reachable from a given instance")
    lazy val heapdump = inputKey[Unit]("Consume the heap dump and estimate the savings in different layout strategies.")
    lazy val idealpack = inputKey[Unit]("Compute the object footprint under different field layout strategies.")
    lazy val internals = inputKey[Unit]("Show the object internals: field layout and default contents, object header")
    // TODO: lazy val stringCompress = inputKey[Unit]("Consume the heap dumps and figures out the savings attainable with compressed strings.")

    lazy val discoveredClasses = TaskKey[Seq[String]]("discovered-classes", "Auto-detects classes.")
  }

}
