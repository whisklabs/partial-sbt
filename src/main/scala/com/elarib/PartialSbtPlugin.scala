package com.elarib

import com.elarib.model.{ChangeGetter, PartialSbParser}
import org.apache.logging.log4j.LogManager
import sbt.*
import sbt.Keys.*
import sbt.internal.BuildDependencies.DependencyMap
import sbt.internal.{BuildDependencies, LoadedBuild}

object BuildKeys {
  val partialSbtExcludedFiles = sbt.settingKey[Seq[sbt.File]]("Files that should be excluded from analysis.")
  val partialSbtOpaqueProject: SettingKey[Unit] =
    sbt.settingKey[Unit]("Changes in this project will not contribute to invalidation of its dependent projects.")
}

object PartialSbtPlugin extends AutoPlugin {

  private[elarib] lazy val logger = {
    val context = LogManager
      .getContext(
        this.getClass.getClassLoader,
        false,
        sys.props
          .get("log4j.configurationFile") match {
          case Some(value) => new File(value).toURI

          case None =>
            val value = getClass.getResource("/log4j2.properties")
            System.setProperty("log4j.configurationFile", value.getPath)
            value.toURI
        }
      )

    context.getLogger(getClass.getName)
  }

  override def globalSettings: Seq[Def.Setting[_]] =
    Seq(
      BuildKeys.partialSbtExcludedFiles := Def.setting(List.empty[sbt.File]).value
    )

  private def hasCompileConfiguration[A <: ProjectReference](dep: ClasspathDep[A]): Boolean =
    dep.configuration.forall(value => value.contains("compile->") || value == "compile")

  private def changedProjectsCommand(name: String)(
      buildDependencies: BuildDependencies,
      baseDirectory: File,
      loadedBuild: LoadedBuild,
      partialSbtExcludedFiles: Seq[File],
      dependencyFilter: ClasspathDep[ProjectRef] => Boolean,
      excludeTestFiles: Boolean
  ) =
    Command(name)(_ => PartialSbParser.changeGetterParser)((st, changeGetter) => {

      val compileDependencyMap: DependencyMap[ClasspathDep[ProjectRef]] =
        buildDependencies.classpath.mapValues(_.filter(dependencyFilter))

      val transitiveCompileDependencyMap =
        BuildDependencies.transitive(compileDependencyMap, BuildDependencies.getID)

      val changedProjects: Seq[ResolvedProject] =
        findChangedModules(changeGetter)(
          baseDirectory,
          loadedBuild.allProjectRefs,
          transitiveCompileDependencyMap,
          partialSbtExcludedFiles,
          excludeTestFiles
        )

      logger.debug(s"${changedProjects.size} projects have been changed")

      changedProjects.foreach { resolvedProject =>
        logger.debug(resolvedProject.id)
      }
      st
    })

  override def projectSettings: Seq[Def.Setting[_]] =
    Seq(
      commands += Command("metaBuildChangedFiles")(_ => PartialSbParser.changeGetterParser)((st, changeGetter) => {
        val metaBuildChangedFiles =
          getMetaBuildChangedFiles(changeGetter)(baseDirectory.value, BuildKeys.partialSbtExcludedFiles.value)

        logger.debug(s"${metaBuildChangedFiles.size} meta build files have been changed.")

        metaBuildChangedFiles.foreach { file =>
          logger.debug(file)
        }
        st
      }),
      commands += changedProjectsCommand("changedProjects")(
        buildDependencies.value,
        baseDirectory.value,
        loadedBuild.value,
        BuildKeys.partialSbtExcludedFiles.value,
        Function.const(true),
        excludeTestFiles = false
      ),
      commands += changedProjectsCommand("changedProjectsInCompile")(
        buildDependencies.value,
        baseDirectory.value,
        loadedBuild.value,
        BuildKeys.partialSbtExcludedFiles.value,
        hasCompileConfiguration,
        excludeTestFiles = true
      )
    )

  private def findChangedModules(changeGetter: ChangeGetter)(
      baseDir: sbt.File,
      allProjectRefs: Seq[(ProjectRef, ResolvedProject)],
      buildDeps: DependencyMap[ProjectRef],
      excludedFiles: Seq[sbt.File],
      excludeTestFiles: Boolean
  ): Seq[ResolvedProject] = {

    val projectMap: Map[ProjectRef, ResolvedProject] = allProjectRefs.toMap

    getMetaBuildChangedFiles(changeGetter)(baseDir, excludedFiles) match {
      case _ :: _ =>
        logger.debug(s"Metabuild files have changed. Need to reload all the ${projectMap.size} projects")
        projectMap.values.toSeq
          .sortBy(_.id)
      case Nil =>
        def isTransparentProject(resolvedProject: ResolvedProject): Boolean =
          !resolvedProject.settings.exists(_.key.key == BuildKeys.partialSbtOpaqueProject.key)

        val reverseDependencyMap: DependencyMap[ResolvedProject] = buildDeps
          .foldLeft[DependencyMap[ResolvedProject]](Map.empty) { (acc, dependency) =>
            val (ref, dependsOnList) = dependency

            dependsOnList.foldLeft(acc) { (dependencyMap, key) =>
              val resolvedProjects = dependencyMap.getOrElse(key, Nil)
              val newValue: Seq[ResolvedProject] =
                projectMap
                  .get(ref)
                  .fold(resolvedProjects)(_ +: resolvedProjects)
              dependencyMap + (key -> newValue)
            }
          }
          .filterKeys { projectRef =>
            projectMap.get(projectRef).exists(isTransparentProject)
          }

        val modulesWithPath: Seq[(ProjectRef, ResolvedProject)] =
          allProjectRefs.filter { case (_, resolvedProject) =>
            resolvedProject.base != baseDir
          }

        val diffsFiles: Seq[sbt.File] = changeGetter.changes.filterNot(f => isFileExcluded(baseDir)(f, excludedFiles))

        def isExcludedTestFile(file: sbt.File, resolvedProject: ResolvedProject): Boolean =
          excludeTestFiles && file.getAbsolutePath.startsWith((resolvedProject.base / "src" / "test").getAbsolutePath)

        def findContainingProject(file: File): Option[(ProjectRef, ResolvedProject)] =
          modulesWithPath
            .filter { case (_, resolvedProject) =>
              file.getAbsolutePath.contains(resolvedProject.base.getAbsolutePath)
            }
            .sortBy(_._2.base.getAbsolutePath.length)
            .lastOption
            .filterNot { case (_, resolvedProject) =>
              isExcludedTestFile(file, resolvedProject)
            }

        diffsFiles
          .flatMap(findContainingProject)
          .distinct
          .flatMap { case (projectRef, resolvedProject) =>
            reverseDependencyMap
              .get(projectRef)
              .map(_ :+ resolvedProject)
              .getOrElse(Seq(resolvedProject))
          }
          .distinct
          .sortBy(_.id)
    }

  }

  private def getMetaBuildChangedFiles(
      changeGetter: ChangeGetter
  )(baseDir: sbt.File, excludedFiles: Seq[sbt.File]): List[sbt.File] = {

    lazy val metaBuildFiles: Seq[(sbt.File, (sbt.File, sbt.File) => Boolean)] =
      PartialSbtConf.metaBuildFiles(baseDir)

    for {
      fileChanged <- changeGetter.changes
        .flatMap(_.relativeTo(baseDir))
        .filterNot(f => isFileExcluded(baseDir)(f, excludedFiles))
      (metaFile, metaFileChecker) <- metaBuildFiles.flatMap { case (metaFile, metaFileChecker) =>
        metaFile.relativeTo(baseDir).map((_, metaFileChecker))
      }
      if metaFileChecker(metaFile, fileChanged)
    } yield fileChanged

  }

  private def isFileExcluded(baseDir: sbt.File)(file: sbt.File, toExclude: Seq[sbt.File]): Boolean =
    toExclude.exists {
      case ef if ef.isAbsolute =>
        (for {
          efRel <- ef.relativeTo(baseDir)
          fRel <- if (file.isAbsolute) file.relativeTo(baseDir) else Some(file)
        } yield efRel.getCanonicalPath == fRel.getCanonicalPath).getOrElse(false)
      case ef =>
        if (file.isAbsolute)
          file.relativeTo(baseDir).exists(_.getCanonicalPath == ef.getCanonicalPath)
        else
          file.getCanonicalPath == ef.getCanonicalPath
    }
}
