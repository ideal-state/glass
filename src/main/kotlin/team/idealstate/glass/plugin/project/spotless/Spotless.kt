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

package team.idealstate.glass.plugin.project.spotless

import com.diffplug.gradle.spotless.FormatExtension
import com.diffplug.gradle.spotless.HasBuiltinDelimiterForLicense
import com.diffplug.gradle.spotless.SpotlessExtension
import team.idealstate.glass.plugin.project.ProjectPlugin

abstract class Spotless<T : FormatExtension>(
    private val name: String,
    private val type: Class<T>,
) : ProjectPlugin("com.diffplug.spotless.$name") {
    init {
        dependsOn("com.diffplug.spotless")
    }

    final override fun apply() {
        project.extensions.configure(SpotlessExtension::class.java) {
            it.format(name, type) { format ->
                doApply(format)
            }
        }
    }

    private fun doApply(format: T) {
        format.endWithNewline()
        if (format is HasBuiltinDelimiterForLicense) {
            val headerFile = project.rootProject.file("HEADER.txt")
            if (headerFile.exists()) {
                format.licenseHeaderFile(headerFile)
            }
        }
        apply(format)
    }

    abstract fun apply(format: T)
}
