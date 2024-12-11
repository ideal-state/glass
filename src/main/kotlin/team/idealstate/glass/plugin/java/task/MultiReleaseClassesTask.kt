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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import team.idealstate.glass.plugin.java.data.JavaRelease

@CacheableTask
open class MultiReleaseClassesTask : Copy() {
    companion object {
        const val NAME = "multiReleaseClasses"

        @JvmStatic
        fun register(project: Project): TaskProvider<MultiReleaseClassesTask> =
            project.tasks.register(NAME, MultiReleaseClassesTask::class.java)

        @JvmStatic
        fun of(project: Project): TaskProvider<MultiReleaseClassesTask> = project.tasks.named(NAME, MultiReleaseClassesTask::class.java)

        @JvmStatic
        fun register(
            project: Project,
            action: Action<in MultiReleaseClassesTask>,
        ): TaskProvider<MultiReleaseClassesTask> = project.tasks.register(NAME, MultiReleaseClassesTask::class.java, action)

        @JvmStatic
        fun of(
            project: Project,
            action: Action<in MultiReleaseClassesTask>,
        ): TaskProvider<MultiReleaseClassesTask> = project.tasks.named(NAME, MultiReleaseClassesTask::class.java, action)
    }

    init {
        super.into(
            project.layout.buildDirectory
                .dir("multiRelease/classes")
                .get()
                .asFile,
        )
    }

    fun source(release: JavaRelease) {
        val multiReleaseClassesSourceTask = MultiReleaseClassesSourceTask.of(project, release)
        dependsOn(multiReleaseClassesSourceTask)
        from(multiReleaseClassesSourceTask) {
            it.includeEmptyDirs = false
            it.into(release.location)
        }
    }
}
