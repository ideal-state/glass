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

import org.gradle.api.Project
import org.gradle.api.provider.Property

class JavaAgentManifest(
    project: Project,
) {
    val premain: Property<String> =
        project.objects.property(String::class.java).apply {
            set("")
        }
    val agentmain: Property<String> =
        project.objects.property(String::class.java).apply {
            set("")
        }
    val canRedefineClasses: Property<Boolean> =
        project.objects.property(Boolean::class.java).apply {
            set(false)
        }
    val canRetransformClasses: Property<Boolean> =
        project.objects.property(Boolean::class.java).apply {
            set(false)
        }
    val canSetNativeMethodPrefix: Property<Boolean> =
        project.objects.property(Boolean::class.java).apply {
            set(false)
        }

    fun toManifestAttributes(): Map<String, *> {
        val attributes = linkedMapOf<String, Any>()
        if (premain.isPresent) {
            attributes["Premain-Class"] = premain.get()
        }
        if (agentmain.isPresent) {
            attributes["Agent-Class"] = agentmain.get()
        }
        if (attributes.isEmpty()) {
            return attributes
        }
        attributes["Can-Redefine-Classes"] = canRedefineClasses.get()
        attributes["Can-Retransform-Classes"] = canRetransformClasses.get()
        attributes["Can-Set-Native-Method-Prefix"] = canSetNativeMethodPrefix.get()
        return attributes
    }
}
