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

package team.idealstate.glass.task.parallel.data

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.FileCopyDetails
import team.idealstate.glass.context.mark.MarkedFactory
import team.idealstate.glass.context.parallel.Job
import team.idealstate.glass.context.util.Validates
import java.io.File

class CopyJob(
    private val project: Project,
    destinationBaseDir: File,
    mark: File,
) : Job<CopyJobKey, File> {
    class Factory(
        private val project: Project,
        private val destinationBaseDir: File,
    ) : MarkedFactory<File, CopyJob> {
        override fun create(mark: File): CopyJob = CopyJob(project, destinationBaseDir, mark)
    }

    override val mark: File = Validates.isDirectory(destinationBaseDir.resolve(Validates.isRelative(mark, "into")), "into")

    private val _from = mutableListOf<Any>()
    val from: List<Any>
        get() = ArrayList(_from)

    val into: File
        get() = mark

    override val key: CopyJobKey = CopyJobKey(into)

    fun from(source: Any) {
        _from.add(source)
    }

    fun from(vararg sources: Any) {
        _from.addAll(sources)
    }

    private val eachFiles = mutableListOf<Action<in FileCopyDetails>>()

    fun eachFile(action: Action<in FileCopyDetails>) {
        eachFiles.add(action)
    }

    override fun execute(): Boolean {
        if (_from.isEmpty()) {
            return false
        }
        return project
            .copy {
                it.into(into)
                _from.forEach(it::from)
                eachFiles.forEach(it::eachFile)
            }.didWork
    }
}
