![Build Status](https://github.com/whisklabs/partial-sbt/actions/workflows/ci.yml/badge.svg)

# Caution
This is a fork of the [partial-sbt](https://github.com/elarib/) project. While it includes new features not present in the original project, it has been published to a private Maven repository. Feel free to fork the project and modify it per the license.

Partial-sbt
============

The goal is simple: Apply some sbt tasks/commands on only the modules/sub-modules changed between two git commits or two git branches, including their reverse dependencies.

The main purpose behind, is to reduce build, test & integration test times. 

For example: 

 - Test and deploy only modules (and their reverse dependencies) that have been changed between develop branch and feature x branch
 - Package only modules (and their reverse dependencies) that have been changed between two git commits.
 
Project example:

 - Check out : https://github.com/elarib/partial-sbt/blob/master/src/sbt-test/test-projects/multi-module-project/build.sbt 
 - Change scenarios : https://github.com/elarib/partial-sbt/tree/master/src/sbt-test/test-projects/multi-module-project/changes
 - Expected output of each change : https://github.com/elarib/partial-sbt/tree/master/src/sbt-test/test-projects/multi-module-project/expected 

Requirements
------------

* SBT
* Git managed repository.

Setup
-----

### Using Published Plugin

Add sbt-assembly as a dependency in `project/plugins.sbt`:

```scala
addSbtPlugin("com.whisk" % "partial-sbt" % "version")
```

Usage
-----

### Applying the plugin in your build.sbt

```scala
enablePlugins(com.elarib.PartialSbtPlugin)
```

### `changedProjects` task

Now you have the `changedProjects` task, that list you all the modules (and their reverse dependencies) changed.
You have the possibility to get the changed projects  :
- Between two git branches using: 
    ```sbt
    > changedProjects gitBranch sourceLocalBranch targetLocalBranch
    ```
- Between two git commits using: 
    ```scala
    > changedProjects gitCommit oldCommitId newCommitId
    ```


### `metaBuildChangedFiles` task

List you all the changed meta build files  :
- Between two git branches using: 
    ```sbt
    > metaBuildChangedFiles gitBranch sourceLocalBranch targetLocalBranch
    ```
- Between two git commits using: 
    ```scala
    > metaBuildChangedFiles gitCommit oldCommitId newCommitId
    ```

How it works
------------

### 1. Get changed files

`jgit` is used to list the diff between two branches or two commits.

### 2. Check if there is some main metabuild file changed

If yes, the whole project was changed, and they will be a need to reload (or build/test ...)
If no, continue to next step.

### 3. List all modules impacted

List all modules impacted based on the changed files (step1) with all their reverse dependencies.

And finally list them, or apply the command or task need (not implemented yet).


TODO
------------
- [ ] Apply commands to changed modules.
- [ ] Deploy to some maven repo manager.

License
-------

Copyright (c) 2019 [Abdelhamide EL ARIB](https://twitter.com/elarib29) 

Published under The MIT License.
