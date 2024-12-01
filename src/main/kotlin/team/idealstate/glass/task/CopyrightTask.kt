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

package team.idealstate.glass.task

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.*

@CacheableTask
open class CopyrightTask : Copy() {
    companion object {
        const val NAME = "copyright"
        const val ROOT_NAME = "COPYRIGHT"
        const val LICENSE_FILE = "LICENSE.txt"
        const val NOTICE_FILE = "NOTICE.txt"
        const val LICENSES_DIR = "LICENSES/"

        @JvmStatic
        fun register(project: Project): TaskProvider<CopyrightTask> = project.tasks.register(NAME, CopyrightTask::class.java)

        @JvmStatic
        fun of(project: Project): TaskProvider<CopyrightTask> = project.tasks.named(NAME, CopyrightTask::class.java)
    }

    @OutputDirectory
    val destinationDirectory: Directory =
        project.layout.buildDirectory
            .dir("docs/$ROOT_NAME")
            .get()

    init {
        group = "documentation"

        super.into(destinationDirectory.asFile)

        super.from("${project.rootProject.projectDir}/$LICENSE_FILE")
        super.from("${project.rootProject.projectDir}/$NOTICE_FILE")
        super.from("${project.rootProject.projectDir}/$LICENSES_DIR") {
            it.into(LICENSES_DIR)
        }
    }
}
