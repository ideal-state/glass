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
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import team.idealstate.glass.context.util.Extensions

@DisableCachingByDefault
open class SourcesTask : Copy() {
    companion object {
        const val NAME = "sources"

        @JvmStatic
        fun register(project: Project): TaskProvider<SourcesTask> =
            project.tasks.register(
                NAME,
                SourcesTask::class.java,
            )

        @JvmStatic
        fun register(
            project: Project,
            action: Action<in SourcesTask>,
        ): TaskProvider<SourcesTask> =
            project.tasks.register(
                NAME,
                SourcesTask::class.java,
                action,
            )

        @JvmStatic
        fun of(project: Project): TaskProvider<SourcesTask> = project.tasks.named(NAME, SourcesTask::class.java)

        @JvmStatic
        fun of(
            project: Project,
            action: Action<in SourcesTask>,
        ): TaskProvider<SourcesTask> = project.tasks.named(NAME, SourcesTask::class.java, action)
    }

    init {
        super.into(project.layout.buildDirectory.dir("sources"))
    }

    fun sourceSet(sourceSetName: String) {
        val sourceSets = Extensions.sourceSets(project)
        super.from(sourceSets.named(sourceSetName).get().allSource)
    }

    fun sourceSet(
        sourceSetName: String,
        action: Action<in CopySpec>,
    ) {
        val sourceSets = Extensions.sourceSets(project)
        super.from(sourceSets.named(sourceSetName).get().allSource, action)
    }
}
