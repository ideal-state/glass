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

object PathUtils {
    const val WINDOWS_PATH_DELIMITER = '\\'
    const val LINUX_DELIMITER = '/'
    const val NORMAL_DELIMITER = LINUX_DELIMITER

    @JvmStatic
    fun normalize(
        pathPart: String,
        vararg pathParts: String,
    ): String {
        if (pathParts.isEmpty()) return normalize(pathPart)
        val pathName =
            listOf(pathPart, *pathParts).joinToString(NORMAL_DELIMITER.toString()) {
                normalize(it).trim(NORMAL_DELIMITER)
            }
        return normalize(pathName)
    }

    @JvmStatic
    fun normalize(pathName: String): String = pathName.replace(WINDOWS_PATH_DELIMITER, NORMAL_DELIMITER).trim(NORMAL_DELIMITER)
}
