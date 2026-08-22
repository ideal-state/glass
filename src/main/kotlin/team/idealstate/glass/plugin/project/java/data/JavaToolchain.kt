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

package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.provider.Provider
import org.gradle.jvm.toolchain.JavaCompiler
import org.gradle.jvm.toolchain.JavaLauncher
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JavadocTool
import org.gradle.jvm.toolchain.JvmImplementation
import org.gradle.jvm.toolchain.JvmVendorSpec

interface JavaToolchain {
    companion object {
        @JvmStatic
        val DEFAULT_TOOLCHAIN_VENDOR = JvmVendorSpec.AZUL

        @JvmStatic
        val DEFAULT_TOOLCHAIN_IMPLEMENTATION = JvmImplementation.VENDOR_SPECIFIC
    }

    val toolchainVersion: Int

    val toolchainVendor: JvmVendorSpec

    val toolchainImplementation: JvmImplementation

    fun toolchain(
        version: Int,
        vendor: JvmVendorSpec = DEFAULT_TOOLCHAIN_VENDOR,
        implementation: JvmImplementation = DEFAULT_TOOLCHAIN_IMPLEMENTATION,
    )

    fun launcherFrom(toolchains: JavaToolchainService): Provider<JavaLauncher>

    fun compilerFrom(toolchains: JavaToolchainService): Provider<JavaCompiler>

    fun javadocToolFrom(toolchains: JavaToolchainService): Provider<JavadocTool>
}
