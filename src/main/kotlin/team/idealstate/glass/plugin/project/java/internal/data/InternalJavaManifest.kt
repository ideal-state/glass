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

package team.idealstate.glass.plugin.project.java.internal.data

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.java.archives.Manifest
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import team.idealstate.glass.plugin.project.java.data.JavaManifest

internal open class InternalJavaManifest(
    objects: ObjectFactory,
) : JavaManifest {
    override val main = objects.property(String::class.java).apply { finalizeValueOnRead() }
    override val premain = objects.property(String::class.java).apply { finalizeValueOnRead() }
    override val agentmain = objects.property(String::class.java).apply { finalizeValueOnRead() }
    override val canRedefineClasses = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }
    override val canSetNativeMethodPrefix = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }
    override val canRetransformClasses = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }
    private val addActions = objects.listProperty(Action::class.java).apply { finalizeValueOnRead() }

    override fun add(action: Action<Manifest>) {
        addActions.add(action)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(
        project: Project,
        manifest: Manifest,
    ) {
        addAttribute(manifest, "Main-Class", main)
        addAttribute(manifest, "Premain-Class", premain)
        addAttribute(manifest, "Agent-Class", agentmain)
        addAttribute(manifest, "Can-Redefine-Classes", canRedefineClasses)
        addAttribute(manifest, "Can-Set-Native-Method-Prefix", canSetNativeMethodPrefix)
        addAttribute(manifest, "Can-Retransform-Classes", canRetransformClasses)
        manifest.attributes["groupId"] = project.group
        manifest.attributes["artifactId"] = project.name
        manifest.attributes["version"] = project.version
        for (action in addActions.get()) {
            (action as Action<Manifest>).execute(manifest)
        }
    }

    private fun addAttribute(
        manifest: Manifest,
        name: String,
        value: Property<*>,
    ) {
        val any = value.orNull
        if (any != null) {
            if (any is String) {
                manifest.attributes[name] = any
            } else {
                manifest.attributes[name] = any.toString()
            }
        }
    }
}
