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

package team.idealstate.glass.plugin.java

import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import team.idealstate.glass.context.util.Plugins
import team.idealstate.glass.plugin.Configure
import team.idealstate.glass.plugin.java.task.SourcesTask
import java.nio.charset.Charset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

open class ConfigureJava : Configure() {
    companion object {
        const val JAR_TASK_NAME = "jar"
        const val SOURCES_JAR_TASK_NAME = "sourcesJar"
        const val JAVADOC_JAR_TASK_NAME = "javadocJar"
        const val CONFIGURATION_INTERNAL_NAME = "internal"
        const val CONFIGURATION_DOCLET_NAME = "doclet"

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
        configureCompileJavaTask()
        configureSourcesJarTask()
        configureJavadocTask()
        configureJavadocJarTask()
        configureJarTask()

        GlassJavaExtension.register(project)
    }

    private fun configureCompileJavaTask() {
        val configurations = project.configurations
        val internal = configurations.register(CONFIGURATION_INTERNAL_NAME).get()
        project.tasks.named("compileJava", JavaCompile::class.java) {
            it.doFirst { _ ->
                it.options.encoding = Charset.defaultCharset().name()
            }
        }
    }

    private fun configureJarTask() {
        project.tasks.named("jar", Jar::class.java) {
            configureJarTask(it)
            it.doFirst { _ ->
                val glass = GlassJavaExtension.of(project)
                glass.application.orNull?.also { application ->
                    application.main.orNull?.also { main ->
                        it.manifest.attributes(mapOf("Main-Class" to main))
                    }
                    application.agent.orNull?.also { agent ->
                        it.manifest.attributes(agent.toManifestAttributes())
                    }
                }
            }
        }
    }

    private fun configureSourcesJarTask() {
        SourcesTask.register(project) {
            it.sourceSet(SourceSet.MAIN_SOURCE_SET_NAME)
        }
        project.tasks.register("sourcesJar", Jar::class.java) {
            it.group = "build"
            it.archiveClassifier.set("sources")
            it.from(project.tasks.named("sources", SourcesTask::class.java))
            it.destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
            configureJarTask(it)
        }
    }

    private fun configureJavadocJarTask() {
        project.tasks.register("javadocJar", Jar::class.java) {
            it.group = "build"
            it.archiveClassifier.set("javadoc")
            it.from(project.tasks.named("javadoc", Javadoc::class.java))
            it.destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
            configureJarTask(it)
        }
    }

    private fun configureJavadocTask() {
        val encoding = Charset.defaultCharset().name()
        val doclet = project.configurations.register(CONFIGURATION_DOCLET_NAME)
        project.tasks.named("javadoc", Javadoc::class.java) {
            it.options { options ->
                val docletFiles =
                    doclet
                        .get()
                        .allArtifacts.files.files
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
