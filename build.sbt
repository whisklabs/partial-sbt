lazy val root = (project in file("."))
  .enablePlugins(ScriptedPlugin)
  .settings(
    name := "partial-sbt",
    organization := "com.elarib",
    version := "0.2.0-SNAPSHOT",
    sbtPlugin := true,
    scalaVersion := "2.12.19",
    scriptedLaunchOpts := {
      scriptedLaunchOpts.value ++
        Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.5.1.201910021850-r"
    )
  )
