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

package team.idealstate.glass.plugin.project.java.internal.data.integration

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import team.idealstate.glass.plugin.project.java.data.integration.MinecraftIntegration
import team.idealstate.glass.plugin.project.java.data.integration.MinecraftIntegration.Implementation

internal open class InternalMinecraftIntegration(
    objects: ObjectFactory,
    override val minecraftVersion: String,
    override val apiVersion: String,
    override val implementation: Implementation,
) : MinecraftIntegration {
    fun apply(
        project: Project,
        vararg sourceSets: SourceSet,
    ) {
        implementation.applyRepositories(project.repositories)
        val compileDependencies = LinkedHashSet(implementation.getCompileDependencies(minecraftVersion, apiVersion))
        val runtimeDependencies = LinkedHashSet(implementation.getRuntimeDependencies(minecraftVersion, apiVersion))
        val annotationProcessors = LinkedHashSet(implementation.getAnnotationProcessors(minecraftVersion, apiVersion))
        project.dependencies.apply {
            for (sourceSet in sourceSets) {
                for (dependency in compileDependencies) {
                    if (runtimeDependencies.remove(dependency)) {
                        add(sourceSet.implementationConfigurationName, dependency)
                    } else {
                        add(sourceSet.compileOnlyConfigurationName, dependency)
                    }
                    if (annotationProcessors.remove(dependency)) {
                        add(sourceSet.annotationProcessorConfigurationName, dependency)
                    }
                }
                for (dependency in runtimeDependencies) {
                    add(sourceSet.runtimeOnlyConfigurationName, dependency)
                }
                for (dependency in annotationProcessors) {
                    add(sourceSet.compileOnlyConfigurationName, dependency)
                    add(sourceSet.annotationProcessorConfigurationName, dependency)
                }
            }
        }
    }
}
