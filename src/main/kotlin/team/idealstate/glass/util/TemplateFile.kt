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

package team.idealstate.glass.util

import java.io.File
import java.nio.charset.Charset
import java.nio.file.Paths

class TemplateFile(
    path: String,
) {
    companion object {
        const val BASE_DIR = "/templates/"
    }

    val path =
        Paths.get(BASE_DIR, path).normalize().toString().run {
            return@run if (File.separatorChar == '/') {
                this
            } else {
                replace(File.separatorChar, '/')
            }
        }

    fun generateTo(
        destinationDir: File,
        charset: Charset = Charsets.UTF_8,
        properties: Map<String, Any?> = emptyMap(),
    ): File = writeTo(File(destinationDir, File(path).name), charset, properties)

    fun writeTo(
        destinationFile: File,
        charset: Charset = Charsets.UTF_8,
        properties: Map<String, Any?> = emptyMap(),
    ): File {
        destinationFile.parentFile.mkdirs()
        this::class.java.getResourceAsStream(path)!!.bufferedReader(charset).use { reader ->
            destinationFile.bufferedWriter(charset).use { writer ->
                writer.write("")
                writer.flush()
                while (true) {
                    writer.appendLine(PropertiesFormatter.format(reader.readLine() ?: break, properties))
                    writer.flush()
                }
            }
        }
        return destinationFile
    }
}
