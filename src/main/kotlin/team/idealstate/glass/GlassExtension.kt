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

import dependenciesInformation
import org.gradle.api.Project
import org.gradle.language.jvm.tasks.ProcessResources
import team.idealstate.glass.data.ScopedDependencyInformation
import team.idealstate.glass.task.CopyrightTask
import team.idealstate.glass.task.DependenciesInformationTask

open class GlassExtension(
    private val project: Project,
) {
    companion object {
        const val NAME = "glass"

        @JvmStatic
        fun register(project: Project): GlassExtension = project.extensions.create(NAME, GlassExtension::class.java, project)

        @JvmStatic
        fun of(project: Project): GlassExtension = project.extensions.getByName(NAME) as GlassExtension

        @JvmStatic
        fun dependenciesInformation(project: Project): List<ScopedDependencyInformation> {
            val dependenciesInformation = linkedMapOf<String, ScopedDependencyInformation>()
            var scope = "compile"
            val compileClasspath = project.configurations.getByName("compileClasspath")
            for (information in compileClasspath.dependenciesInformation) {
                dependenciesInformation[information.id] =
                    ScopedDependencyInformation(information.group, information.name, information.version, scope)
            }
            scope = "runtime"
            val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
            for (information in runtimeClasspath.dependenciesInformation) {
                val id = information.id
                if (!dependenciesInformation.containsKey(id)) {
                    dependenciesInformation[information.id] =
                        ScopedDependencyInformation(information.group, information.name, information.version, scope)
                }
            }
            return dependenciesInformation.values.toList()
        }
    }

    fun withCopyright() {
        val copyrightTask = CopyrightTask.register(project)
        project.tasks.named("processResources", ProcessResources::class.java) {
            it.dependsOn(copyrightTask)
            it.from(copyrightTask) { copy ->
                copy.into("META-INF/${CopyrightTask.ROOT_NAME}/")
            }
        }
    }

    fun withDependenciesInformation() {
        val dependenciesInformationTask = DependenciesInformationTask.register(project)
        project.tasks.named("processResources", ProcessResources::class.java) {
            it.dependsOn(dependenciesInformationTask)
            it.from(dependenciesInformationTask) { copy ->
                copy.into("META-INF/")
            }
        }
    }
}
