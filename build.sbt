lazy val root = (project in file("."))
  .enablePlugins(SbtPlugin)
  .settings(
    name := "partial-sbt",
    organization := "com.whisk",
    version := "0.11",
    scalaVersion := "2.12.4",
    scriptedLaunchOpts := { scriptedLaunchOpts.value ++
      Seq("-Xmx1024M", "-Dplugin.version=" + version.value)
    },
    scriptedBufferLog := false,
    libraryDependencies ++= Seq(
      "org.eclipse.jgit" % "org.eclipse.jgit" % "5.5.1.201910021850-r"
    )
  )
