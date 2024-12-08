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

package team.idealstate.glass.plugin.java.task

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import team.idealstate.glass.plugin.java.GlassJavaExtension

@CacheableTask
open class DependenciesInformationTask : DefaultTask() {
    companion object {
        const val NAME = "dependenciesInformation"
        const val ROOT_NAME = "dependencies-information.json"

        @JvmStatic
        fun register(project: Project): TaskProvider<DependenciesInformationTask> =
            project.tasks.register(NAME, DependenciesInformationTask::class.java)

        @JvmStatic
        fun of(project: Project): TaskProvider<DependenciesInformationTask> =
            project.tasks.named(NAME, DependenciesInformationTask::class.java)
    }

    @OutputFile
    val destinationFile: Provider<RegularFile> =
        project.layout.buildDirectory.file("docs/$ROOT_NAME")

    init {
        group = "documentation"
    }

    @TaskAction
    protected fun generate() {
        val dependencyInformation = GlassJavaExtension.dependenciesInformation(project)
        val objectMapper =
            ObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .writerWithDefaultPrettyPrinter()
        objectMapper.writeValue(destinationFile.get().asFile, dependencyInformation)
    }
}
