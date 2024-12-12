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

package team.idealstate.glass.plugin.java.task.data.relocate

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import team.idealstate.glass.context.filter.Exclude
import team.idealstate.glass.context.mark.MarkedContainer
import team.idealstate.glass.context.mark.MarkedProvider
import team.idealstate.glass.context.parallel.AbstractJobContainer
import team.idealstate.glass.context.relocate.Relocator
import team.idealstate.glass.plugin.java.task.data.Skip
import java.io.File

class RelocateJobContainer(
    project: Project,
    excludes: ListProperty<Exclude<in RelocateJobDetail>>,
    skips: ListProperty<Skip<in RelocateJobDetail>>,
    relocators: ListProperty<Relocator>,
) : AbstractJobContainer<RelocateJobKey, File, RelocateJobResult, RelocateJob>(
        MarkedContainer.create(RelocateJob.Factory(project, excludes, skips, relocators)),
    ) {
    fun relocate(
        file: File,
        action: Action<RelocateJob>,
    ): MarkedProvider<File, RelocateJob> = register(file, action)
}
