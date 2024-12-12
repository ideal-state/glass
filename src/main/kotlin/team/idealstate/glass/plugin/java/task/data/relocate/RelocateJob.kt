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

import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import team.idealstate.glass.context.filter.Exclude
import team.idealstate.glass.context.mark.MarkedFactory
import team.idealstate.glass.context.parallel.Job
import team.idealstate.glass.context.relocate.Relocator
import team.idealstate.glass.context.relocate.bytecode.ClassRelocator
import team.idealstate.glass.context.util.ClassUtils
import team.idealstate.glass.context.util.PathUtils
import team.idealstate.glass.context.util.Validates
import team.idealstate.glass.plugin.java.task.data.Skip
import java.io.File
import java.util.LinkedList

class RelocateJob(
    private val project: Project,
    private val excludes: ListProperty<Exclude<in RelocateJobDetail>>,
    private val skips: ListProperty<Skip<in RelocateJobDetail>>,
    private val relocators: ListProperty<Relocator>,
    override val mark: File,
) : Job<RelocateJobKey, File, RelocateJobResult> {
    class Factory(
        private val project: Project,
        private val excludes: ListProperty<Exclude<in RelocateJobDetail>>,
        private val skips: ListProperty<Skip<in RelocateJobDetail>>,
        private val relocators: ListProperty<Relocator>,
    ) : MarkedFactory<File, RelocateJob> {
        override fun create(mark: File): RelocateJob = RelocateJob(project, excludes, skips, relocators, mark)
    }

    override val key: RelocateJobKey = RelocateJobKey(mark)
    val file
        get() = mark

    private var _path: String? = null
    val path
        get() = Validates.isPresent(_path, "path")

    fun path(path: String) {
        Validates.notFinal(_path, "path")
        val tmp = PathUtils.normalize(path)
        Validates.isRelative(File(tmp), "path")
        _path = tmp
    }

    private var scope: RelocateJobScope = RelocateJobScope.BOTH

    fun spoce(scope: RelocateJobScope) {
        this.scope = scope
    }

    override fun execute(): RelocateJobResult {
        val file = this.file
        var path = this.path
        val relocationDetail = RelocationJobDetail(file, path, false)
        val excludes = LinkedList(this.excludes.get())
        val exclude = excludes.any { it.exclude(relocationDetail) }
        if (exclude) {
            relocationDetail.exclude()
            return relocationDetail.toRelocateResult()
        }
        val skips = LinkedList(this.skips.get())
        val skip = skips.any { it.skip(relocationDetail) }
        if (skip) {
            return relocationDetail.toRelocateResult()
        }
        val relocators = LinkedList(this.relocators.get())
        when (scope) {
            RelocateJobScope.BOTH, RelocateJobScope.CONTENT -> {
                if (path.endsWith(ClassUtils.CLASS_FILE_EXTENSION_NAME)) {
                    ClassRelocator.relocate(file, relocators)
                } else if (path.startsWith(ClassUtils.SERVICES_DIR_PATH_NAME)) {
                    val content =
                        file.bufferedReader().useLines { lines ->
                            lines
                                .map { line ->
                                    var newline = line
                                    relocators.forEach { relocator -> newline = relocator.relocate(newline) ?: newline }
                                    return@map newline
                                }.toList()
                        }
                    file.writeText(content.joinToString("\n"))
                }
            }
            else -> {}
        }
        when (scope) {
            RelocateJobScope.BOTH, RelocateJobScope.PATH -> {
                relocators.forEach { relocator -> path = relocator.relocate(path) ?: path }
                relocationDetail.path = path
            }
            else -> {}
        }
        return relocationDetail.toRelocateResult()
    }
}
