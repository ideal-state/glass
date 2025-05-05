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

package team.idealstate.glass.context.util

import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.plugins.signing.SigningExtension
import team.idealstate.glass.plugin.java.GlassJavaExtension

object Extensions {
    @JvmStatic
    fun glass(project: Project): GlassJavaExtension = project.extensions.getByName("glass") as GlassJavaExtension

    @JvmStatic
    fun sourceSets(project: Project): SourceSetContainer = project.extensions.getByName("sourceSets") as SourceSetContainer

    @JvmStatic
    fun java(project: Project): JavaPluginExtension = project.extensions.getByName("java") as JavaPluginExtension

    @JvmStatic
    fun javaToolchains(project: Project): JavaToolchainService = project.extensions.getByName("javaToolchains") as JavaToolchainService

    @JvmStatic
    fun publishing(project: Project): PublishingExtension = project.extensions.getByName("publishing") as PublishingExtension

    @JvmStatic
    fun signing(project: Project): SigningExtension = project.extensions.getByName("signing") as SigningExtension

    @JvmStatic
    fun spotless(project: Project): SpotlessExtension = project.extensions.getByName("spotless") as SpotlessExtension
}
