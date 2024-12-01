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

import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.language.jvm.tasks.ProcessResources
import java
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class ConfigureJava : Configure() {

    companion object {
        @JvmStatic
        fun configureJarTask(jar: Jar) {
            jar.doFirst {
                it as Jar
                it.manifest.attributes(
                    mapOf(
                        "group" to it.project.group,
                        "name" to it.project.name,
                        "version" to it.project.version,
                        "timestamp" to ZonedDateTime.now().format(DateTimeFormatter.ISO_ZONED_DATE_TIME),
                    ),
                )
            }
        }
    }

    init {
        dependsOn(Plugins.java)
    }

    override fun apply() {
        val encoding = Charset.defaultCharset().name()

        configureJavaPluginExtension()

        configureJavaCompileTask(encoding)
        configureJavadocTask(encoding)
        configureSourcesJarTask()
        configureJavadocJarTask()
        configureJarTask()
    }

    private fun configureJarTask() {
        project.tasks.named("jar", Jar::class.java) {
            configureJarTask(it)
        }
    }

    private fun configureSourcesJarTask() {
        project.tasks.named("sourcesJar", Jar::class.java) {
            it.dependsOn("processResources")
            it.from(project.tasks.named("processResources", ProcessResources::class.java))
            configureJarTask(it)
        }
    }

    private fun configureJavadocJarTask() {
        project.tasks.named("javadocJar", Jar::class.java) {
            configureJarTask(it)
        }
    }

    private fun configureJavaPluginExtension() {
        val java = project.extensions.getByName("java") as JavaPluginExtension
        java.apply {
            withSourcesJar()
            withJavadocJar()
        }
    }

    private fun configureJavaCompileTask(encoding: String) {
        project.tasks.whenTaskAdded {
            if (it is JavaCompile) {
                it.options.also { compileOptions ->
                    compileOptions.isFork = true
                    compileOptions.encoding = encoding
                    compileOptions.compilerArgs.also { compileArgs ->
                        compileArgs.add("-parameters")
                    }
                    compileOptions.forkOptions.also { forkOptions ->
                        forkOptions.jvmArgs!!.add("-J-Dfile.encoding=$encoding")
                        forkOptions.executable =
                            it.javaCompiler
                                .get()
                                .executablePath.asFile.absolutePath
                    }
                }
            }
        }
    }

    private fun configureJavadocTask(encoding: String) {
        val doclet = project.configurations.register("doclet").get()
        project.tasks.named("javadoc", Javadoc::class.java) {
            it.options { options ->
                val docletFiles = doclet.allArtifacts.files.files
                if (docletFiles.isEmpty()) {
                    if (options is StandardJavadocDocletOptions) {
                        options.charSet(encoding)
                        options.docEncoding(encoding)
                        options.author(true)
                        options.version(true)
                    }
                } else {
                    options.docletpath.addAll(docletFiles)
                }
                options.encoding(encoding)
                options.jFlags("-Dfile.encoding=$encoding")
            }
        }
    }
}
