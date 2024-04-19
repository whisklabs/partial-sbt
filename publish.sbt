ThisBuild / organization := "com.whisk"
ThisBuild / publishMavenStyle := true
ThisBuild / credentials += Credentials(Path.userHome / ".m2" / ".credentials")
ThisBuild / publishTo := Some("internal.repo" at sys.env.getOrElse("NEXUS_DEST", Resolver.SonatypeRepositoryRoot))

ThisBuild / scmInfo := Some(
  ScmInfo(
    url("https://github.com/whisklabs/partial-sbt"),
    "scm:git@github.com:whisklabs/partial-sbt.git"
  )
)

ThisBuild / developers := List(
  Developer(
    id = "elarib",
    name = "Abdelhamide EL ARIB",
    email = "elarib.abdelhamide@gmail.com",
    url = url("http://elarib.com")
  )
)

ThisBuild / description := " Apply some sbt task/commands on only the modules/sub-modules (and their reverse dependencies) based on git changes"
ThisBuild / licenses := List("MIT" -> new URL("https://github.com/whisklabs/partial-sbt/blob/master/LICENSE"))
ThisBuild / homepage := Some(url("https://github.com/whisklabs/partial-sbt"))
