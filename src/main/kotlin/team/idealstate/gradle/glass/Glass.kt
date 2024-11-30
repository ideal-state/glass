package team.idealstate.gradle.glass

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings

abstract class Glass: Plugin<Any> {

    override fun apply(target: Any) {
        when (target) {
            is Settings -> apply(target)
            is Project -> apply(target)
            else -> throw IllegalArgumentException("Unsupported target type: ${target.javaClass}")
        }
    }

    private fun apply(settings: Settings) {

    }

    private fun apply(project: Project) {

    }
}