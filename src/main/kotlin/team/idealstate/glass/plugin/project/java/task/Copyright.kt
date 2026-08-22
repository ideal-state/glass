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
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

@CacheableTask
abstract class Copyright : DefaultTask() {
    companion object {
        @JvmStatic
        val COPYRIGHT_DIR_NAMES =
            setOf(
                "COPYRIGHTS/",
                "LICENSES/",
                "NOTICES/",
            )

        @JvmStatic
        val COPYRIGHT_FILE_NAMES =
            setOf(
                "COPYRIGHT.txt",
                "LICENSE.txt",
                "NOTICE.txt",
            )
    }

    @OutputDirectory
    protected val destinationDirectory = project.layout.buildDirectory.dir("glass/copyright")

    init {
        super.inputs.apply {
            val rootProjectDir = project.rootProject.projectDir
            for (name in COPYRIGHT_DIR_NAMES) {
                val dirPath = File(rootProjectDir, name)
                !dirPath.isDirectory && continue
                dir(dirPath)
            }
            for (name in COPYRIGHT_FILE_NAMES) {
                val path = File(rootProjectDir, name)
                !path.isFile && continue
                file(path)
            }
        }
    }

    @TaskAction
    fun copy() {
        project.copy {
            into(destinationDirectory)
            val rootProjectDir = project.rootProject.projectDir
            for (name in COPYRIGHT_DIR_NAMES) {
                from(File(rootProjectDir, name)) {
                    into(name)
                }
            }
            for (name in COPYRIGHT_FILE_NAMES) {
                from(File(rootProjectDir, name))
            }
        }
    }
}
