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

@file:Suppress("unused", "UnusedReceiverParameter")

import org.gradle.api.initialization.Settings
import java.io.File

fun Settings.includeModules(root: String = "") {
    fun findBuildScripts(
        modulesDirectory: File,
        deep: Boolean = true,
    ): List<File> {
        val moduleIds = mutableListOf<File>()
        for (file in modulesDirectory.listFiles()!!) {
            val filename = file.name
            if (filename == "buildSrc") {
                continue
            }
            if (file.isDirectory) {
                if (deep) {
                    moduleIds.addAll(findBuildScripts(file))
                }
            } else if (filename == "build.gradle" || filename == "build.gradle.kts") {
                if (file.parentFile == rootProject.projectDir) {
                    continue
                }
                moduleIds.add(file)
            }
        }
        return moduleIds
    }

    val rootPath =
        root
            .replace("\\", "/")
            .replace(":", "/")

    val modulesDirectory = File(rootProject.projectDir, rootPath)
    if (!modulesDirectory.exists()) {
        throw IllegalStateException("Modules directory is not exists.")
    }
    if (!modulesDirectory.isDirectory) {
        throw IllegalStateException("Modules directory file must be a directory.")
    }
    println("\n> Modules: \n> Root Dir: $modulesDirectory")
    val buildScripts = findBuildScripts(modulesDirectory)
    val prefixLength = rootProject.projectDir.absolutePath.length
    var count = 0
    buildScripts.forEach {
        val moduleId =
            it.parentFile.absolutePath
                .substring(prefixLength)
                .replace('\\', ':')
                .replace('/', ':')
        if (moduleId.isBlank() || moduleId == ":") {
            return@forEach
        }
        println(">> including $moduleId ....")
        if (findProject(it.parentFile) != null) {
            throw IllegalStateException("Module $moduleId already exists.")
        }
        include(moduleId)
        val project = project(moduleId)

        project.name =
            "${rootProject.name}${moduleId.substring(rootPath.length)}"
                .replace(':', '-')
        println(">> included $moduleId (${project.name})")
        count++
    }
    if (count == 0) {
        println(">> No modules included.")
    } else {
        println(">> $count modules included.")
    }
    println()
}
