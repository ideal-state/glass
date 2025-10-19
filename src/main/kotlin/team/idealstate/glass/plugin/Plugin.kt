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

package team.idealstate.glass.plugin

import org.gradle.api.Plugin
import org.gradle.api.plugins.PluginAware

abstract class Plugin<T : PluginAware>(
    id: String,
) : Plugin<T> {
    private val id: String = "team.idealstate.glass.$id"
    private var applied: T? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("The plugin '$id' has been applied.")
            }
            field = value
        }
    protected val target: T
        get() {
            applied ?: throw IllegalStateException("The plugin '$id' has not yet been applied.")
            return applied!!
        }
    private val dependencies: MutableList<String> = mutableListOf()

    final override fun apply(target: T) {
        applied = target
        applyDependencies(target, dependencies)
        apply()
    }

    protected abstract fun apply()

    protected fun dependsOn(vararg dependencies: String) {
        this.dependencies.addAll(dependencies)
    }

    private fun applyDependencies(
        target: T,
        dependencies: Collection<String>,
    ) {
        val pluginManager = target.pluginManager
        for (dependency in dependencies) {
            if (pluginManager.hasPlugin(dependency)) {
                continue
            }
            pluginManager.apply(dependency)
        }
    }
}
