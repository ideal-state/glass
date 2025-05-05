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

package team.idealstate.glass.plugin.java.task

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.work.DisableCachingByDefault
import team.idealstate.glass.plugin.publishing.ConfigurePublishing
import java.util.Locale.getDefault
import java.util.Properties

@DisableCachingByDefault
open class MavenPomTask : DefaultTask() {
    companion object {
        const val NAME = "mavenPom"
        const val ROOT_NAME = "mavenPom"

        @JvmStatic
        fun register(project: Project): TaskProvider<MavenPomTask> =
            project.tasks.register(NAME, MavenPomTask::class.java)

        @JvmStatic
        fun of(project: Project): TaskProvider<MavenPomTask> =
            project.tasks.named(NAME, MavenPomTask::class.java)

        @JvmStatic
        fun register(
            project: Project,
            action: Action<in MavenPomTask>,
        ): TaskProvider<MavenPomTask> = project.tasks.register(NAME, MavenPomTask::class.java, action)

        @JvmStatic
        fun of(
            project: Project,
            action: Action<in MavenPomTask>,
        ): TaskProvider<MavenPomTask> = project.tasks.named(NAME, MavenPomTask::class.java, action)
    }

    @OutputDirectory
    val destinationDirectory: DirectoryProperty =
        project.objects.directoryProperty().apply {
            set(project.layout.buildDirectory.dir("$ROOT_NAME/"))
        }

    private val pomTaskName by lazy {
        "generatePomFileFor${ConfigurePublishing.MAIN_NAME.replaceFirstChar { if (it.isLowerCase()) it.titlecase(getDefault()) else it.toString() }}Publication"
    }

    init {
        group = "documentation"
        dependsOn(pomTaskName)
    }

    @TaskAction
    protected fun generatePomProperties() {
        val pomTask = project.tasks.named(pomTaskName).get()
        project.copy {
            it.from(pomTask.outputs) { into ->
                into.rename("pom-default.xml", "pom.xml")
            }
            it.into(destinationDirectory)
        }
        val properties = Properties(3)
        properties.setProperty("artifactId", project.name)
        properties.setProperty("groupId", project.group.toString())
        properties.setProperty("version", project.version.toString())
        val destinationFile = destinationDirectory.file("pom.properties")
        val file = destinationFile.get().asFile
        file.parentFile.mkdirs()
        file.exists() || file.createNewFile()
        file.writer().use {
            properties.store(it, null)
        }
    }
}
