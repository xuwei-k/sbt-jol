package sbtjol

import java.io.File

private[sbtjol] object JolPluginCompat {
  inline def classpathToFiles(classpath: sbt.Keys.Classpath): Seq[File] = {
    val converter = sbt.Keys.fileConverter.value
    classpath.map(x => converter.toPath(x.data).toFile)
  }
}
