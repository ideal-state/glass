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

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.jvm.toolchain.JavaLanguageVersion
import team.idealstate.glass.context.util.Extensions
import java.io.File

fun DependencyHandler.java(
    project: Project,
    vararg libs: String,
): ConfigurableFileCollection {
    val java = Extensions.java(project)
    val ret = project.objects.fileCollection()
    val toolchain = java.toolchain
    val languageVersion = toolchain.languageVersion
    if (!languageVersion.isPresent) {
        return ret
    }
    var javaLanguageVersion = languageVersion.get()
    if (javaLanguageVersion.canCompileOrRun(8)) {
        javaLanguageVersion = JavaLanguageVersion.of(8)
    }
    val javaToolchains = Extensions.javaToolchains(project)
    val javaHome =
        javaToolchains
            .compilerFor {
                it.languageVersion.set(javaLanguageVersion)
                it.vendor.set(toolchain.vendor)
            }.get()
            .executablePath.asFile.parentFile.parentFile
    val libPaths =
        libs
            .map { File(javaHome, "lib/$it.jar") }
            .filter { it.exists() }
            .toTypedArray()
    ret.from(*libPaths)
    return ret
}
