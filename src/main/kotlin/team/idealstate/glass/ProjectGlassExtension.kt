package team.idealstate.glass

import org.gradle.api.Action
import org.gradle.api.Project
import team.idealstate.glass.extension.GlassExtension

internal open class ProjectGlassExtension(
    private val project: Project
): GlassExtension {

    override fun <T : Any> with(type: Class<T>, action: Action<T>) {
        project.extensions.configure(type, action)
    }
}