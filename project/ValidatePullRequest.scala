/*
 * Copyright (C) 2009-2022 Lightbend Inc. <https://www.lightbend.com>
 */

package org.apache.pekko

import com.hpe.sbt.ValidatePullRequest
import com.hpe.sbt.ValidatePullRequest.PathGlobFilter
import com.lightbend.paradox.sbt.ParadoxPlugin
import com.lightbend.paradox.sbt.ParadoxPlugin.autoImport.paradox
import com.typesafe.tools.mima.plugin.MimaKeys.mimaReportBinaryIssues
import com.typesafe.tools.mima.plugin.MimaPlugin
import sbtunidoc.BaseUnidocPlugin.autoImport.unidoc
import sbt.Keys._
import sbt._

object PekkoValidatePullRequest extends AutoPlugin {

  object CliOptions {
    val mimaEnabled = CliOption("pekko.mima.enabled", true)
  }

  import ValidatePullRequest.autoImport._

  override def trigger = allRequirements
  override def requires = ValidatePullRequest

  val ValidatePR = config("pr-validation").extend(Test)

  override lazy val projectConfigurations = Seq(ValidatePR)

  val additionalTasks = settingKey[Seq[TaskKey[_]]]("Additional tasks for pull request validation")

  override lazy val globalSettings = Seq(credentials ++= {
      // todo this should probably be supplied properly
      GitHub.envTokenOrThrow.map { token =>
        Credentials("GitHub API", "api.github.com", "", token)
      }
    }, additionalTasks := Seq.empty)

  override lazy val buildSettings = Seq(
    validatePullRequest / includeFilter := {
      val ignoredProjects = List(
        "pekko", // This is the root project
        "serialVersionRemoverPlugin")

      loadedBuild.value.allProjectRefs.collect {
        case (_, project) if !ignoredProjects.contains(project.id) =>
          val directory = project.base.getPath.split("/").last
          PathGlobFilter(s"$directory/**")
      }.fold(new FileFilter { // TODO: Replace with FileFilter.nothing when https://github.com/sbt/io/pull/340 gets released
        override def accept(pathname: File): Boolean = false
      })(_ || _)
    },
    validatePullRequestBuildAll / excludeFilter := PathGlobFilter("project/MiMa.scala"),
    prValidatorGithubRepository := Some("akka/akka"),
    prValidatorTargetBranch := "origin/main")

  override lazy val projectSettings = inConfig(ValidatePR)(Defaults.testTasks) ++ Seq(
    ValidatePR / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "performance"),
    ValidatePR / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "long-running"),
    ValidatePR / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "timing"),
    ValidatePR / testOptions += Tests.Argument(TestFrameworks.ScalaTest, "-l", "gh-exclude"),
    // make it fork just like regular test running
    ValidatePR / fork := (Test / fork).value,
    ValidatePR / testGrouping := (Test / testGrouping).value,
    ValidatePR / javaOptions := (Test / javaOptions).value,
    prValidatorTasks := Seq(ValidatePR / test) ++ additionalTasks.value,
    prValidatorEnforcedBuildAllTasks := Seq(Test / test) ++ additionalTasks.value)
}

/**
 * This autoplugin adds Multi Jvm tests to validatePullRequest task.
 * It is needed, because ValidatePullRequest autoplugin does not depend on MultiNode and
 * therefore test:executeTests is not yet modified to include multi-jvm tests when ValidatePullRequest
 * build strategy is being determined.
 *
 * Making ValidatePullRequest depend on MultiNode is impossible, as then ValidatePullRequest
 * autoplugin would trigger only on projects which have both of these plugins enabled.
 */
object MultiNodeWithPrValidation extends AutoPlugin {
  import PekkoValidatePullRequest._
  import com.typesafe.sbt.MultiJvmPlugin.MultiJvmKeys.MultiJvm

  override def trigger = allRequirements
  override def requires = PekkoValidatePullRequest && MultiNode
  override lazy val projectSettings =
    if (MultiNode.multiNodeTestInTest) Seq(additionalTasks += MultiNode.multiTest)
    else Seq.empty
}

/**
 * This autoplugin adds MiMa binary issue reporting to validatePullRequest task,
 * when a project has MimaPlugin autoplugin enabled.
 */
object MimaWithPrValidation extends AutoPlugin {
  import PekkoValidatePullRequest._

  override def trigger = allRequirements
  override def requires = PekkoValidatePullRequest && MimaPlugin
  override lazy val projectSettings =
    CliOptions.mimaEnabled.ifTrue(additionalTasks += mimaReportBinaryIssues).toList
}

/**
 * This autoplugin adds Paradox doc generation to validatePullRequest task,
 * when a project has ParadoxPlugin autoplugin enabled.
 */
object ParadoxWithPrValidation extends AutoPlugin {
  import PekkoValidatePullRequest._

  override def trigger = allRequirements
  override def requires = PekkoValidatePullRequest && ParadoxPlugin
  override lazy val projectSettings = Seq(additionalTasks += Compile / paradox)
}

object UnidocWithPrValidation extends AutoPlugin {
  import PekkoValidatePullRequest._

  override def trigger = noTrigger
  override lazy val projectSettings = Seq(additionalTasks += Compile / unidoc)
}
