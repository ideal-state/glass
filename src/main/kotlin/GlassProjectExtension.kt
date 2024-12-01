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

import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaToolchainService
import java.io.File

fun Project.java(vararg libs: String): ConfigurableFileCollection {
    val java = extensions.getByName("java") as JavaPluginExtension
    val ret = objects.fileCollection()
    val toolchain = java.toolchain
    val languageVersion = toolchain.languageVersion
    if (!languageVersion.isPresent || !languageVersion.get().canCompileOrRun(8)) {
        return ret
    }
    val javaToolchains = extensions.getByName("javaToolchains") as JavaToolchainService
    val javaHome =
        javaToolchains
            .compilerFor {
                it.languageVersion.set(languageVersion)
                it.vendor.set(toolchain.vendor)
            }.get()
            .executablePath.asFile.parentFile.parentFile
    val libPaths = libs.map { File(javaHome, "lib/$it.jar") }.toTypedArray()
    ret.from(*libPaths)
    return ret
}
