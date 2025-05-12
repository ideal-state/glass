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

package team.idealstate.glass.plugin.java.data

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property

class JavaApplication(
    private val project: Project,
) {
    val main: Property<String> = project.objects.property(String::class.java)

    val agent: Property<JavaAgentManifest> = project.objects.property(JavaAgentManifest::class.java)

    fun agent(action: Action<in JavaAgentManifest>) {
        var exists = false
        var agentManifest = this.agent.orNull
        if (agentManifest != null) {
            exists = true
        } else {
            agentManifest = JavaAgentManifest(project)
        }
        action.execute(agentManifest)
        if (!exists) {
            this.agent.set(agentManifest)
        }
    }

    val sugar: Property<Sugar> = project.objects.property(Sugar::class.java)

    fun sugar(action: Action<in Sugar>) {
        var exists = false
        var sugar = this.sugar.orNull
        if (sugar != null) {
            exists = true
        } else {
            sugar = Sugar(project)
        }
        action.execute(sugar)
        if (!exists) {
            this.sugar.set(sugar)
        }
    }
}
