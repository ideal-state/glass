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

package team.idealstate.glass.context.util

import java.io.File

object Validates {
    @JvmStatic
    fun <V : Any> isPresent(
        value: V?,
        name: String,
    ): V {
        if (value == null) throw IllegalStateException("$name not yet set.")
        return value
    }

    @JvmStatic
    fun notPresent(
        value: Any?,
        name: String,
    ) {
        if (value != null) throw IllegalStateException("$name already set. (current: $value)")
    }

    @JvmStatic
    fun isDirectory(
        file: File,
        name: String,
    ): File {
        if (file.exists() && !file.isDirectory) throw IllegalStateException("$name is not a directory.")
        return file
    }

    @JvmStatic
    fun isAbsolute(
        file: File,
        name: String,
    ): File {
        if (!file.isAbsolute) throw IllegalStateException("$name is not a absolute path.")
        return file
    }

    @JvmStatic
    fun isRelative(
        file: File,
        name: String,
    ): File {
        if (file.isAbsolute) throw IllegalStateException("$name is not a relative path.")
        return file
    }

    @JvmStatic
    fun notEmpty(
        value: Collection<*>,
        name: String,
    ) {
        if (value.isEmpty()) throw IllegalStateException("$name is empty.")
    }
}
