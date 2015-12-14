name := "wiki-scraper"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++=  Seq(
  "org.scalaj" %% "scalaj-http" % "2.2.0",
  "org.jsoup" % "jsoup" % "1.8.3"
)

import sbtassembly.AssemblyPlugin.defaultShellScript

assemblyOption in assembly := (assemblyOption in assembly).value.copy(prependShellScript = Some(defaultShellScript))

assemblyJarName in assembly := s"${name.value}-${version.value}"

target in assembly := file("artifacts")
