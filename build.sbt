name := """sbt-apidoc"""
organization := "com.culpin.team"
version := "0.5.3-SNAPSHOT"

sbtPlugin := true

scalacOptions ++= Seq("-deprecation", "-feature")
licenses := Seq("MIT License" -> url("http://opensource.org/licenses/mit-license.php/"))


libraryDependencies ++= Seq(
  "com.lihaoyi"         %%     "upickle"        %    "0.6.6",
  "org.scalatest"       %%     "scalatest"      %    "3.0.5"   % "test"
)

bintrayPackageLabels := Seq("sbt","plugin")
bintrayVcsUrl := Some("""git@github.com:org.example/sbt-apidocjs.git""")

initialCommands in console := """import org.example.sbt._"""

// set up 'scripted; sbt plugin for testing sbt plugins
scriptedLaunchOpts ++=
  Seq("-Xmx1024M", "-Dplugin.version=" + version.value)

scriptedBufferLog := false
