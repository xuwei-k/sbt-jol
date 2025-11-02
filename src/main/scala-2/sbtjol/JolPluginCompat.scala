package sbtjol

import java.io.File

private[sbtjol] object JolPluginCompat {
  def classpathToFiles(classpath: sbt.Keys.Classpath): Seq[File] =
    classpath.map(_.data)
}
