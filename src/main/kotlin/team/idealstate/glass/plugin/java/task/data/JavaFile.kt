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

package team.idealstate.glass.plugin.java.task.data

import org.objectweb.asm.ClassReader
import team.idealstate.glass.context.util.ClassUtils
import team.idealstate.glass.context.util.PathUtils
import java.io.File

interface JavaFile {
    companion object {
        @JvmStatic
        fun of(
            baseDir: File,
            file: File,
        ): JavaFile {
            var processPath =
                PathUtils
                    .normalize(file.toRelativeString(baseDir))
                    .substringBeforeLast(PathUtils.NORMAL_DELIMITER)
            var release: Int? = null
            if (processPath.contains(PathUtils.NORMAL_DELIMITER)) {
                if (processPath.startsWith(ClassUtils.MULTI_RELEASE_DIR_PATH_NAME)) {
                    val path = processPath.substring(ClassUtils.MULTI_RELEASE_DIR_PATH_NAME.length)
                    val version = path.substringBefore(PathUtils.NORMAL_DELIMITER)
                    version.toIntOrNull()?.also {
                        if (it >= ClassUtils.LEAST_MULTI_RELEASE_VERSION) {
                            release = it
                        }
                    }
                    if (release != null) {
                        processPath =
                            if (path.length <= version.length + 1) {
                                ""
                            } else {
                                path.substring(version.length + 1)
                            }
                    }
                }
            } else {
                processPath = ""
            }
            val fullFileName = file.name
            var packageName = PathUtils.normalize(processPath)
            if (ClassUtils.maybeClassFile(fullFileName)) {
                packageName = ClassUtils.internalize(packageName)
                val fileName = fullFileName.substringBeforeLast(ClassUtils.CLASS_FILE_EXTENSION_NAME)
                val internalPath = ClassUtils.internalize(packageName, fileName)
                val internalName: String
                file.inputStream().use { stream ->
                    val classReader = ClassReader(stream)
                    internalName = classReader.className
                }
                if (internalPath == internalName) {
                    return JavaClassFile(baseDir, file, release, packageName, fileName, ClassUtils.CLASS_FILE_EXTENSION_NAME, ClassUtils.normalize(internalName))
                }
            }
            if (ClassUtils.maybeSourceFile(fullFileName)) {
                return JavaSourceFile(baseDir, file, release, packageName, fullFileName.substringBeforeLast(ClassUtils.SOURCE_FILE_EXTENSION_NAME), ClassUtils.SOURCE_FILE_EXTENSION_NAME)
            }
            val fileName = fullFileName.substringBefore('.')
            val extension = '.' + fullFileName.substringAfter('.')
            return JavaResourceFile(baseDir, file, release, packageName, fileName, extension)
        }
    }

    val baseDir: File
    val file: File
    val release: Int?
    val packageName: String
    val name: String
    val extension: String

    val location: String
        get() =
            if (release == null) {
                ClassUtils.internalize(packageName, name) + extension
            } else {
                ClassUtils.internalize(
                    ClassUtils.MULTI_RELEASE_DIR_PATH_NAME,
                    release.toString(),
                    packageName,
                    name,
                ) + extension
            }

    fun location(base: File): File = File(base, location)
}
