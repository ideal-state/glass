package team.idealstate.glass.util

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.process.ExecOutput
import org.gradle.process.ExecSpec

object GitUtil {

    private fun exec(project: Project, action: Action<ExecSpec>): ExecOutput {
        var current: Project? = project
        var execOutput: ExecOutput? = null
        var e: Exception? = null
        while (current != null) {
            try {
                execOutput = project.providers.exec {
                    action.execute(this)
                }
                current = null
            } catch (ex: Exception) {
                current = current?.parent
                e = ex
            }
        }
        return execOutput ?: throw IllegalStateException(e)
    }

    fun loadVersionCode(project: Project): Int {
        return exec(project) {
            commandLine("git", "rev-list", "--first-parent", "--count", "HEAD")
        }.standardOutput.asText.getOrElse("0").trim().toInt()
    }

    fun loadVersionName(project: Project, defaultVersion: String? = "0.1.0"): String? {
        return generateVersionName(project) ?: defaultVersion
    }

    private fun generateVersionName(project: Project): String? {
        val versionName = try {
            exec(project) {
                commandLine("git", "describe", "--tags", "--dirty", "--exclude", "*-*")
            }.standardOutput.asText.orNull?.trim()
        } catch (_: Exception) {
            null
        }
        versionName ?: return null
        return if (versionName.contains("-")) {
            versionName.replace(Regex("^(\\d+\\.\\d+\\.)(\\d+)")) {
                val groups = it.groups
                groups[1]!!.value + groups[2]!!.value.toInt().inc()
            }
        } else {
            versionName
        }
    }
}