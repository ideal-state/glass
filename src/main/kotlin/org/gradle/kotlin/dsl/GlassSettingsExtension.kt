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
import java.nio.file.Paths

private const val BUILD_SRC_DIR_NAME = "buildSrc"
private const val BUILD_KOTLIN_SCRIPT_NAME = "build.gradle.kts"
private const val MODULE_ID_DELIMITER_CHAR = ':'
private const val MODULE_ID_DELIMITER = MODULE_ID_DELIMITER_CHAR.toString()
private const val SPOTLESS_BUILD_MODULE_ID_PREFIX = ":build:spotless-"

fun Settings.multiModule(
    base: String = MODULE_ID_DELIMITER,
    vararg excludes: String,
) {
    val basePath = Paths.get(base.replace(MODULE_ID_DELIMITER_CHAR, File.separatorChar)).normalize().toString()

    val rootProjectDir = rootProject.projectDir
    val buildSrcDir = File(rootProjectDir, BUILD_SRC_DIR_NAME)
    val baseDir = File(rootProjectDir, basePath)
    if (!baseDir.exists()) {
        throw IllegalArgumentException("Base dir \"$baseDir\" is not exists.")
    }
    if (!baseDir.isDirectory) {
        throw IllegalArgumentException("Base dir \"$baseDir\" must be a directory.")
    }
    if (baseDir == buildSrcDir) {
        throw IllegalArgumentException("Base dir \"$baseDir\" cannot be buildSrc.")
    }
    try {
        baseDir.relativeTo(rootProjectDir)
    } catch (e: IllegalArgumentException) {
        throw IllegalArgumentException("Base dir \"$baseDir\" must be specified relative to \"$rootProjectDir\".", e)
    }

    println("\n> Modules: \n> Base Dir: \"$baseDir\"")

    val buildScriptFiles = mutableListOf<File>()
    for (file in baseDir.listFiles()) {
        !file.isDirectory && continue
        file == buildSrcDir && continue
        for (item in file.listFiles()) {
            item.isDirectory && continue
            val name = item.name
            if (name == BUILD_KOTLIN_SCRIPT_NAME) {
                buildScriptFiles.add(item)
            }
        }
    }

    var count = 0
    for (buildScriptFile in buildScriptFiles) {
        val projectDir = buildScriptFile.parentFile
        val moduleId =
            MODULE_ID_DELIMITER_CHAR +
                projectDir
                    .relativeTo(rootProjectDir)
                    .toPath()
                    .normalize()
                    .toString()
                    .replace(File.separatorChar, MODULE_ID_DELIMITER_CHAR)
        val projectName =
            projectDir
                .relativeTo(baseDir)
                .toPath()
                .normalize()
                .toString()
        if (moduleId.contains(SPOTLESS_BUILD_MODULE_ID_PREFIX)) {
            continue
        }
        for (exclude in excludes) {
            moduleId == exclude && continue
        }
        println(">> including \"$projectName\" ($moduleId)...")
        include(moduleId)
        count++
    }

    println("> $count modules included.\n")
}
