/*
 *    Copyright 2024 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package team.idealstate.glass

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.initialization.Settings
import org.gradle.api.plugins.PluginManager
import team.idealstate.glass.ProjectGlassExtension
import team.idealstate.glass.extension.GlassExtension

open class Glass : Plugin<Any> {
    override fun apply(target: Any) {
        when (target) {
            is Settings -> {
                applyPlugins(
                    target.pluginManager,
                    "org.gradle.toolchains.foojay-resolver-convention",
                )
            }
            is Project -> {
                applyPlugins(
                    target.pluginManager,
                    "team.idealstate.glass.java",
                    "team.idealstate.glass.com.diffplug.spotless.gradle",
                    "team.idealstate.glass.com.diffplug.spotless.java",
                )

                target.extensions.create(GlassExtension::class.java, "glass", ProjectGlassExtension::class.java, target)
            }
            else -> {
                throw IllegalStateException(
                    "Plugin ${this::class.java.name} can only be applied in settings.gradle(.kts) or build.gradle(.kts).",
                )
            }
        }
    }

    private fun applyPlugins(
        pluginManager: PluginManager,
        vararg pluginIds: String,
    ) {
        for (pluginId in pluginIds) {
            if (pluginManager.hasPlugin(pluginId)) {
                continue
            }
            pluginManager.apply(pluginId)
        }
    }
}
