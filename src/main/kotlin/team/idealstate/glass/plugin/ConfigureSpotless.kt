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

package team.idealstate.glass.plugin

import com.diffplug.gradle.spotless.HasBuiltinDelimiterForLicense
import com.diffplug.gradle.spotless.SpotlessExtension
import spotless
import java.nio.charset.Charset

open class ConfigureSpotless : Configure() {
    companion object {
        const val HEADER_FILE = "HEADER.txt"
    }

    init {
        dependsOn(Plugins.spotless)
    }

    override fun apply() {
        val spotless = project.extensions.getByName("spotless") as SpotlessExtension
        spotless.apply {
            encoding(Charset.defaultCharset())

            groovyGradle {
                it.target("**/*.gradle")
                it.endWithNewline()
                it.greclipse()
            }

            kotlinGradle {
                it.target("**/*.gradle.kts")
                it.endWithNewline()
                it.ktlint().editorConfigOverride(mapOf("ktlint_standard_no-wildcard-imports" to "disabled"))
            }

            java {
                it.target("src/*/java/**/*.java")
                it.endWithNewline()
                it
                    .googleJavaFormat()
                    .aosp()
                    .reflowLongStrings(true)
                    .formatJavadoc(true)
                it.formatAnnotations()
                applyLicenseHeader(it)
            }

            kotlin {
                it.target("src/*/kotlin/**/*.kt", "src/*/kotlin/**/*.kts")
                it.endWithNewline()
                it.ktlint().editorConfigOverride(mapOf("ktlint_standard_no-wildcard-imports" to "disabled"))
                applyLicenseHeader(it)
            }

            sql {
                it.target("src/*/resources/**/*.sql")
                it.dbeaver()
            }

            json {
                it.target("src/*/resources/**/*.json")
                it.jackson()
            }

            yaml {
                it.target("src/*/resources/**/*.yml", "src/*/resources/**/*.yaml")
                it.jackson()
            }
        }
    }

    private fun applyLicenseHeader(it: HasBuiltinDelimiterForLicense) {
        val headerFile = project.rootProject.file(HEADER_FILE)
        if (headerFile.exists()) {
            it.licenseHeaderFile(headerFile)
        }
    }
}
