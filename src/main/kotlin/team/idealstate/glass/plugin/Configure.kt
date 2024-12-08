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
import org.gradle.api.Project

abstract class Configure : Plugin<Project> {
    private var applied: Project? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("Already applied to ${field!!.name}.")
            }
            field = value
        }
    protected val project: Project
        get() {
            applied ?: throw IllegalStateException("Not applied.")
            return applied!!
        }
    private val depends: MutableList<String> = mutableListOf()

    final override fun apply(target: Project) {
        applied = target
        applyDepends()
        apply()
    }

    protected abstract fun apply()

    protected fun dependsOn(vararg plugins: String) {
        depends.addAll(plugins)
    }

    private fun applyDepends() {
        val pluginManager = project.pluginManager
        for (depend in depends) {
            if (pluginManager.hasPlugin(depend)) {
                continue
            }
            pluginManager.apply(depend)
        }
    }
}
