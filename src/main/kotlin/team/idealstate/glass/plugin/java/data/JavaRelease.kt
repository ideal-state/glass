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

package team.idealstate.glass.plugin.java.data

import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import team.idealstate.glass.context.mark.MarkedFactory
import team.idealstate.glass.context.release.AbstractRelease
import team.idealstate.glass.context.release.ReleaseType
import team.idealstate.glass.context.util.Extensions

class JavaRelease(
    private val project: Project,
    override val type: ReleaseType,
    override val mark: Int,
) : AbstractRelease<Int, CompileOptions>() {
    companion object {
        const val MAIN_NAME = "main"
        const val TEST_NAME = "test"
        const val EXTEND_NAME_PREFIX = "java"
        const val LEAST_MULTI_RELEASE_VERSION = 9
    }

    class Factory(
        private val project: Project,
    ) : MarkedFactory<Int, JavaRelease> {
        override fun create(mark: Int): JavaRelease = JavaRelease(project, ReleaseType.EXTEND, mark)
    }

    private val toolchainJavaVersion: Provider<JavaLanguageVersion>
        get() {
            val java = Extensions.java(project)
            return java.toolchain.languageVersion
        }

    override val location: String
        get() = "META-INF/versions/$mark/"

    val javaLanguageVersion: JavaLanguageVersion
        get() = JavaLanguageVersion.of(mark)

    val name
        get() =
            when (type) {
                ReleaseType.MAIN -> MAIN_NAME
                ReleaseType.TEST -> TEST_NAME
                else -> EXTEND_NAME_PREFIX + version
            }

    init {
        validateJavaLanguageVersion(mark)
    }

    private fun validateJavaLanguageVersion(mark: Int) {
        val version = JavaLanguageVersion.of(mark)
        if (!isReserved()) {
            if (!version.canCompileOrRun(LEAST_MULTI_RELEASE_VERSION)) {
                throw IllegalArgumentException(
                    "Java version must be at least ${JavaLanguageVersion.of(
                        LEAST_MULTI_RELEASE_VERSION,
                    )}. (current: $version)",
                )
            }
        }
        val toolchainJavaVersion = this.toolchainJavaVersion.get()
        if (!toolchainJavaVersion.canCompileOrRun(version)) {
            throw IllegalArgumentException("Java version must be at most $toolchainJavaVersion. (current: $version)")
        }
    }
}
