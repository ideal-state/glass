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

package team.idealstate.glass.plugin.project.java.task

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SourceSet
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

@DisableCachingByDefault
abstract class Sources
    @Inject
    constructor(
        private val sourceSet: SourceSet,
    ) : DefaultTask() {
        @Suppress("unused")
        @OutputFiles
        protected val destinationFiles =
            project.objects.property(FileCollection::class.java).apply {
                set(project.provider { sourceSet.allSource })
            }
    }
