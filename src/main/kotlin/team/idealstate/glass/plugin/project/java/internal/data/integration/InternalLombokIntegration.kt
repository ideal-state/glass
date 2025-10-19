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
import org.gradle.kotlin.dsl.PUBLIC
import org.gradle.kotlin.dsl.sonatype
import team.idealstate.glass.plugin.project.java.data.integration.LombokIntegration

internal open class InternalLombokIntegration(
    objects: ObjectFactory,
    override val version: String,
) : LombokIntegration {
    fun apply(
        project: Project,
        vararg sourceSets: SourceSet,
    ) {
        project.repositories.apply {
            sonatype(PUBLIC)
        }
        val lombok = "org.projectlombok:lombok:$version"
        for (sourceSet in sourceSets) {
            project.dependencies.apply {
                add(sourceSet.compileOnlyConfigurationName, lombok)
                add(sourceSet.annotationProcessorConfigurationName, lombok)
            }
        }
    }
}
