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

package team.idealstate.glass.task.parallel

import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import team.idealstate.glass.task.ParallelTask
import team.idealstate.glass.task.parallel.data.CopyJob
import team.idealstate.glass.task.parallel.data.CopyJobContainer
import team.idealstate.glass.task.parallel.data.CopyJobKey
import java.io.File

@CacheableTask
abstract class ParallelCopy : ParallelTask<CopyJobKey, File, CopyJob, CopyJobContainer>() {
    @OutputDirectory
    val destinationDir: DirectoryProperty = project.objects.directoryProperty()

    override val jobs: CopyJobContainer by lazy {
        CopyJobContainer(project, destinationDir.get().asFile)
    }
}
