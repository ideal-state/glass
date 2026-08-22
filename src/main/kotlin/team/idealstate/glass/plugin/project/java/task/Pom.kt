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

package team.idealstate.glass.plugin.project.java.task

import org.gradle.api.DefaultTask
import org.gradle.api.publish.maven.tasks.GenerateMavenPom
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import java.util.Locale.getDefault
import java.util.Properties
import javax.inject.Inject

@CacheableTask
abstract class Pom
    @Inject
    constructor(
        private val publication: String,
    ) : DefaultTask() {
        @OutputDirectory
        protected val destinationDirectory = project.layout.buildDirectory.dir("glass/maven")

        private val generateMavenPom: TaskProvider<GenerateMavenPom>
            get() =
                project.tasks.named(
                    "generatePomFileFor${publication.replaceFirstChar {
                        if (it.isLowerCase()) {
                            it.titlecase(
                                getDefault(),
                            )
                        } else {
                            it.toString()
                        }
                    }}Publication",
                    GenerateMavenPom::class.java,
                )

        init {
            super.dependsOn(generateMavenPom)
            super.inputs.file(project.provider { generateMavenPom.get().destination })
        }

        @TaskAction
        fun copy() {
            project.copy {
                into(destinationDirectory)
                from(generateMavenPom) {
                    include("pom-default.xml")
                    rename("pom-default.xml", "pom.xml")
                }
            }
            generatePomProperties()
        }

        protected open fun generatePomProperties() {
            val properties = Properties(3)
            properties.setProperty("artifactId", project.name)
            properties.setProperty("groupId", project.group.toString())
            properties.setProperty("version", project.version.toString())
            val destinationFile =
                project.layout.buildDirectory
                    .dir("glass/maven")
                    .get()
                    .file("pom.properties")
            val file = destinationFile.asFile
            file.parentFile.mkdirs()
            file.exists() || file.createNewFile()
            file.writer().use {
                properties.store(it, null)
            }
        }
    }
