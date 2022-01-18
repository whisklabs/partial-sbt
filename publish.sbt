ThisBuild / organization := "com.whisk"

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/whisklabs/partial-sbt"),
    "scm:git@github.com:whisklabs/partial-sbt.git"
  )
)

ThisBuild / description := " Apply some sbt task/commands on only the modules/sub-modules (and their reverse dependencies) based on git changes"
ThisBuild / licenses := List("MIT" -> new URL("https://github.com/whisklabs/partial-sbt/blob/master/LICENSE"))
ThisBuild / homepage := Some(url("https://github.com/whisklabs/partial-sbt"))

// Remove all additional repository other than Maven Central from POM
ThisBuild / pomIncludeRepository := { _ => false }
ThisBuild / publishTo := Some(
  "internal.repo.write" at "https://nexus.whisk-dev.com/repository/whisk-maven2/"
)
ThisBuild / publishMavenStyle := true


ThisBuild / credentials += Credentials(Path.userHome / ".m2" / ".credentials")
