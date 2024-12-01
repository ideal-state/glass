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

open class Glass : Plugin<Any> {
    override fun apply(target: Any) {
        when (target) {
            is Settings -> apply(target)
            is Project -> apply(target)
            else -> throw IllegalArgumentException("Unsupported target type: ${target.javaClass}")
        }
    }

    private fun apply(settings: Settings) {
        val foojay = "org.gradle.toolchains.foojay-resolver-convention"
        val plugins = settings.plugins
        if (!plugins.hasPlugin(foojay)) {
            plugins.apply(foojay)
        }
    }

    private fun apply(project: Project) {
        GlassExtension.register(project)
    }
}
