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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import team.idealstate.glass.context.util.PathUtils
import team.idealstate.glass.plugin.java.ConfigureJava
import team.idealstate.glass.task.parallel.ParallelCopy
import java.io.File

@DisableCachingByDefault
open class UnzipInternalDependenciesTask : ParallelCopy() {
    companion object {
        const val NAME = "unzipInternalDependencies"
        const val ROOT_NAME = "dependencies"

        @JvmStatic
        fun register(project: Project): TaskProvider<UnzipInternalDependenciesTask> =
            project.tasks.register(NAME, UnzipInternalDependenciesTask::class.java)

        @JvmStatic
        fun of(project: Project): TaskProvider<UnzipInternalDependenciesTask> =
            project.tasks.named(NAME, UnzipInternalDependenciesTask::class.java)

        @JvmStatic
        fun register(
            project: Project,
            action: Action<in UnzipInternalDependenciesTask>,
        ): TaskProvider<UnzipInternalDependenciesTask> = project.tasks.register(NAME, UnzipInternalDependenciesTask::class.java, action)

        @JvmStatic
        fun of(
            project: Project,
            action: Action<in UnzipInternalDependenciesTask>,
        ): TaskProvider<UnzipInternalDependenciesTask> = project.tasks.named(NAME, UnzipInternalDependenciesTask::class.java, action)
    }

    @Suppress("unused")
    @Input
    protected val alwaysRun: Property<Long> =
        project.objects.property(Long::class.java).apply {
            set(project.provider { System.currentTimeMillis() })
        }

    init {
        destinationDir.set(
            project.layout.buildDirectory
                .dir(ROOT_NAME)
                .get()
                .dir(ConfigureJava.CONFIGURATION_INTERNAL_NAME),
        )
        super.jobs {
            val internal = project.configurations.named(ConfigureJava.CONFIGURATION_INTERNAL_NAME).get()
            val resolvedArtifacts = internal.resolvedConfiguration.resolvedArtifacts
            resolvedArtifacts.forEach { artifact ->
                val artifactFile = artifact.file
                if (!artifactFile.name.endsWith(".jar")) {
                    return@forEach
                }
                val id = artifact.moduleVersion.id
                val group = id.group
                val name = id.name
                val path = PathUtils.normalize("$group/$name/")
                it.into(File(path)) { copy ->
                    copy.from(project.zipTree(artifactFile))
                }
            }
        }
    }
}
