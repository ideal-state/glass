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

import org.gradle.api.Action
import org.gradle.api.Project
import team.idealstate.glass.extension.ApplicableExtension
import team.idealstate.glass.extension.GlassExtension

internal open class ProjectGlassExtension(
    private val project: Project,
) : GlassExtension {
    override fun <T : Any> with(
        type: Class<T>,
        action: Action<T>,
    ) {
        project.extensions.configure(type, action)
    }

    override fun <T : ApplicableExtension> apply(
        type: Class<T>,
        action: Action<T>,
    ) {
        project.extensions.configure(type) {
            action.execute(it)
            it.apply()
        }
    }
}
