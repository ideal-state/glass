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

import team.idealstate.glass.context.util.PathUtils
import team.idealstate.glass.context.util.Validates
import java.io.File

class RelocateJobResult(
    val file: File,
    sourcePath: String,
    path: String,
    val exclude: Boolean,
) {
    val sourcePath: String
    val path: String

    init {
        var normalized = PathUtils.normalize(sourcePath)
        Validates.isRelative(File(normalized), "sourcePath")
        this.sourcePath = normalized

        normalized = PathUtils.normalize(path)
        Validates.isRelative(File(normalized), "path")
        this.path = normalized
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as RelocateJobResult

        if (exclude != other.exclude) return false
        if (file != other.file) return false
        if (sourcePath != other.sourcePath) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = exclude.hashCode()
        result = 31 * result + file.hashCode()
        result = 31 * result + sourcePath.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String = "RelocateJobResult(file=$file, exclude=$exclude, sourcePath='$sourcePath', path='$path')"
}
