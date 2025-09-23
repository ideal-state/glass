package team.idealstate.glass

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginManager
import team.idealstate.glass.extension.GlassExtension
import team.idealstate.glass.ProjectGlassExtension

open class Glass : Plugin<Any> {

    override fun apply(target: Any) {
        when (target) {
            is Settings -> {
                applyPlugins(target.pluginManager,
                    "org.gradle.toolchains.foojay-resolver-convention"
                )
            }
            is Project -> {
                applyPlugins(target.pluginManager,
                    "team.idealstate.glass.java",
                    "team.idealstate.glass.com.diffplug.spotless.gradle",
                    "team.idealstate.glass.com.diffplug.spotless.java"
                )

                target.extensions.create(GlassExtension::class.java, "glass", ProjectGlassExtension::class.java, target)
            }
            else -> {
                throw IllegalStateException("Plugin ${this::class.java.name} can only be applied in settings.gradle(.kts) or build.gradle(.kts).")
            }
        }
    }

    private fun applyPlugins(pluginManager: PluginManager, vararg pluginIds: String) {
        for (pluginId in pluginIds) {
            if (pluginManager.hasPlugin(pluginId)) {
                continue
            }
            pluginManager.apply(pluginId)
        }
    }
}
