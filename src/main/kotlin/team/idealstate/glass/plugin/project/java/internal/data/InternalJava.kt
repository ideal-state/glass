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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import team.idealstate.glass.plugin.project.java.data.Java
import team.idealstate.glass.plugin.project.java.data.JavaRelease
import team.idealstate.glass.plugin.project.java.util.JavaUtils
import kotlin.collections.iterator
import kotlin.math.max

internal open class InternalJava(
    objects: ObjectFactory,
    version: Int,
) : InternalJavaRelease(objects, version),
    Java {
    private val multiReleases = objects.mapProperty(Int::class.java, Action::class.java).apply { finalizeValueOnRead() }

    override fun multi(
        version: Int,
        action: Action<JavaRelease>,
    ) {
        multiReleases.put(version, action)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(
        project: Project,
        multiReleases: MutableMap<Int, Action<JavaRelease>>,
    ) {
        val mainReleaseVersion = version
        validate()
        multiReleases.putAll(this.multiReleases.get() as Map<Int, Action<JavaRelease>>)
        var maxVersion = mainReleaseVersion
        for ((multiVersion, _) in multiReleases) {
            if (multiVersion <= mainReleaseVersion) {
                throw IllegalArgumentException(
                    "Multi release version $multiVersion must be greater than main release version $mainReleaseVersion.",
                )
            }
            if (multiVersion < JavaUtils.LEAST_MULTI_RELEASE_SUPPORTED_VERSION) {
                throw IllegalArgumentException("Multi release version $multiVersion must be greater than or equal to 9.")
            }
            maxVersion = multiVersion
        }
        val toolchainVersion = max(toolchainVersion, maxVersion)
        val toolchainVendor = toolchainVendor
        val toolchainImplementation = toolchainImplementation
        val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        javaPluginExtension.apply {
            toolchain {
                it.languageVersion.set(JavaLanguageVersion.of(toolchainVersion))
                it.vendor.set(toolchainVendor)
                it.implementation.set(toolchainImplementation)
            }
        }
    }
}
