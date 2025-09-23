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

package org.gradle.kotlin.dsl

import org.gradle.api.initialization.Settings
import java.io.File

private const val BUILD_SRC_DIR_NAME = "buildSrc"
private const val BUILD_GROOVY_SCRIPT_NAME = "build.gradle"
private const val BUILD_KOTLIN_SCRIPT_NAME = "build.gradle.kts"
private const val MODULE_ID_DELIMITER = ':'
private const val MODULE_ID_DELIMITER_STR = MODULE_ID_DELIMITER.toString()
private const val MODULE_NAME_DELIMITER = '-'
private const val SPOTLESS_BUILD_MODULE_ID_PREFIX = ":build:spotless-"

private fun Settings.findBuildScripts(
    modulesDirectory: File,
    deep: Boolean = true,
): List<File> {
    val moduleIds = mutableListOf<File>()
    for (file in modulesDirectory.listFiles()!!) {
        val filename = file.name
        if (filename == BUILD_SRC_DIR_NAME) {
            continue
        }
        if (file.isDirectory) {
            if (deep) {
                moduleIds.addAll(findBuildScripts(file))
            }
        } else if (filename == BUILD_GROOVY_SCRIPT_NAME || filename == BUILD_KOTLIN_SCRIPT_NAME) {
            if (file.parentFile == rootProject.projectDir) {
                continue
            }
            moduleIds.add(file)
        }
    }
    return moduleIds
}

private fun normalizePath(vararg parts: String): String = parts.joinToString("/").replace('\\', '/').replace("//", "/")

fun Settings.multiModule(
    base: String = "",
    vararg excludes: String,
) {
    val basePath = normalizePath(base.replace(MODULE_ID_DELIMITER, '/'))

    val modulesDirectory = File(rootProject.projectDir, basePath)
    if (!modulesDirectory.exists()) {
        throw IllegalStateException("Modules directory is not exists.")
    }
    if (!modulesDirectory.isDirectory) {
        throw IllegalStateException("Modules directory file must be a directory.")
    }
    println("\n> Modules: \n> Base Dir: $modulesDirectory")
    val buildScripts = findBuildScripts(modulesDirectory)
    val prefixLength = rootProject.projectDir.absolutePath.length
    var count = 0
    buildScripts.forEach {
        val moduleId =
            normalizePath(it.parentFile.absolutePath)
                .substring(prefixLength)
                .replace('/', MODULE_ID_DELIMITER)
        if (moduleId.isBlank() || moduleId == MODULE_ID_DELIMITER_STR) {
            return@forEach
        }
        if (moduleId.contains(SPOTLESS_BUILD_MODULE_ID_PREFIX)) {
            return@forEach
        }
        for (exclude in excludes) {
            if (moduleId == exclude) {
                return@forEach
            }
        }
        val foundProject = findProject(it.parentFile)
        val projectName =
            "${rootProject.name}${moduleId.substring(basePath.length)}".replace(
                MODULE_ID_DELIMITER,
                MODULE_NAME_DELIMITER,
            )
        println(">> including $moduleId ($projectName)...")
        if (foundProject != null) {
            if (foundProject.name != projectName) {
                throw IllegalStateException("Module $moduleId already exists.")
            }
        } else {
            include(moduleId)
        }
        val project = project(moduleId)
        project.name = projectName
        count++
    }
    println("> $count modules included.\n")
}
