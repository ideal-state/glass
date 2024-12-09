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

import dependenciesInformation
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import team.idealstate.glass.context.util.Extensions
import team.idealstate.glass.context.util.Validates
import team.idealstate.glass.data.dependency.ScopedDependencyInformation
import team.idealstate.glass.data.release.MultiRelease
import team.idealstate.glass.plugin.java.data.JavaRelease
import team.idealstate.glass.plugin.java.data.JavaReleaseContainer
import team.idealstate.glass.plugin.java.task.CopyrightTask
import team.idealstate.glass.plugin.java.task.DependenciesInformationTask
import team.idealstate.glass.plugin.java.task.MultiReleaseClassesSourceTask
import team.idealstate.glass.plugin.java.task.MultiReleaseClassesTask
import team.idealstate.glass.plugin.java.task.SourcesTask
import java.nio.charset.Charset

open class GlassJavaExtension(
    private val project: Project,
) {
    companion object {
        const val NAME = "glass"

        @JvmStatic
        fun register(project: Project): GlassJavaExtension = project.extensions.create(NAME, GlassJavaExtension::class.java, project)

        @JvmStatic
        fun of(project: Project): GlassJavaExtension = project.extensions.getByName(NAME) as GlassJavaExtension

        @JvmStatic
        fun dependenciesInformation(project: Project): List<ScopedDependencyInformation> {
            val dependenciesInformation = linkedMapOf<String, ScopedDependencyInformation>()
            var scope = "compile"
            val compileClasspath = project.configurations.getByName("runtimeClasspath")
            for (information in compileClasspath.dependenciesInformation) {
                dependenciesInformation[information.id] =
                    ScopedDependencyInformation(information.group, information.name, information.version, scope)
            }
            scope = "provided"
            val runtimeClasspath = project.configurations.getByName("compileClasspath")
            for (information in runtimeClasspath.dependenciesInformation) {
                val id = information.id
                if (!dependenciesInformation.containsKey(id)) {
                    dependenciesInformation[information.id] =
                        ScopedDependencyInformation(information.group, information.name, information.version, scope)
                }
            }
            return dependenciesInformation.values.toList()
        }
    }

    private val appliedSteps = mutableMapOf<String, Boolean>()

    private fun apply(step: String): Boolean {
        val applied = appliedSteps[step] ?: false
        if (!applied) {
            appliedSteps[step] = true
        }
        return applied
    }

    fun withCopyright() {
        if (apply("withCopyright")) return
        val copyrightTask = CopyrightTask.register(project)
        project.tasks.named("processResources", ProcessResources::class.java) {
            it.dependsOn(copyrightTask)
            it.from(copyrightTask) { copy ->
                copy.into("META-INF/${CopyrightTask.ROOT_NAME}/")
            }
        }
        addSources(copyrightTask) { copy ->
            copy.into("META-INF/${CopyrightTask.ROOT_NAME}/")
        }
    }

    fun withDependenciesInformation() {
        if (apply("withDependenciesInformation")) return
        val dependenciesInformationTask = DependenciesInformationTask.register(project)
        project.tasks.named("processResources", ProcessResources::class.java) {
            it.dependsOn(dependenciesInformationTask)
            it.from(dependenciesInformationTask) { copy ->
                copy.into("META-INF/")
            }
        }
        addSources(dependenciesInformationTask) { copy ->
            copy.into("META-INF/")
        }
    }

    private fun addSources(
        task: TaskProvider<*>,
        action: Action<in CopySpec>,
    ) {
        SourcesTask.of(project).configure {
            it.dependsOn(task)
            it.from(task, action)
        }
    }

    fun withSourcesJar() {
        if (apply("withSourcesJar")) return
        project.tasks.named("assemble") {
            it.dependsOn(ConfigureJava.SOURCES_JAR_TASK_NAME)
        }
    }

    fun withJavadocJar() {
        if (apply("withJavadocJar")) return
        project.tasks.named("assemble") {
            it.dependsOn(ConfigureJava.JAVADOC_JAR_TASK_NAME)
        }
    }

    fun withInternal(){
        if (apply("withInternal")) return
        val configurations = project.configurations
        val internal = configurations.named(ConfigureJava.CONFIGURATION_INTERNAL_NAME).get()
        val sourceSets = Extensions.sourceSets(project)
        for (sourceName in arrayOf(SourceSet.MAIN_SOURCE_SET_NAME, SourceSet.TEST_SOURCE_SET_NAME)) {
            val sourceSet = sourceSets.named(sourceName).get()
            sourceSet.compileClasspath += internal
        }
    }

    fun multiRelease(action: Action<MultiRelease>) {
        if (apply("multiRelease")) return
        val multiRelease = MultiRelease(project)
        action.execute(multiRelease)
        configureJavaMultiRelease(multiRelease.java)
    }

    private fun configureJavaMultiRelease(releaseContainer: JavaReleaseContainer) {
        Validates.notEmpty(releaseContainer.artifacts, "artifacts")
        val tasks = project.tasks
        val multiReleaseClassesTask = MultiReleaseClassesTask.register(project)
        val sourceSets = Extensions.sourceSets(project)
        releaseContainer.all.forEach { release ->
            val name = release.name
            val sourceSet: SourceSet
            if (release.isReserved()) {
                sourceSet = sourceSets.named(name).get()
                tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
                    configureMultiReleaseCompileJavaTask(release, it)
                }
                return@forEach
            } else {
                val mainSourceSet = sourceSets.named(JavaRelease.MAIN_NAME).get()
                sourceSet =
                    sourceSets
                        .register(name) {
                            it.compileClasspath += mainSourceSet.compileClasspath + mainSourceSet.output
                            it.runtimeClasspath += mainSourceSet.runtimeClasspath
                            it.annotationProcessorPath += mainSourceSet.annotationProcessorPath
                        }.get()
            }

            tasks.named(SourcesTask.NAME, SourcesTask::class.java) {
                it.sourceSet(name) { copy ->
                    copy.into(release.location)
                }
            }

            tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
                it.doFirst { _ ->
                    val excludes = MultiReleaseClassesSourceTask.sourceSetJavaRelativeFiles(sourceSet, project.objects)
                    populateMultiReleaseCompileJavaTaskSources(sourceSets, releaseContainer, release, excludes, it)
                }
                configureMultiReleaseCompileJavaTask(release, it)
            }

            MultiReleaseClassesSourceTask.register(project, release)

            multiReleaseClassesTask.configure {
                it.source(release)
            }
        }

        releaseContainer.artifacts.forEach { artifact ->
            artifact.configure {
                it.dependsOn(multiReleaseClassesTask)
                it.from(multiReleaseClassesTask)
                it.manifest.attributes(mapOf("Multi-Release" to true))
            }
        }
    }

    private fun configureMultiReleaseCompileJavaTask(
        release: JavaRelease,
        task: JavaCompile,
    ) {
        val version = release.javaLanguageVersion
        val options = task.options
        options.encoding = Charset.defaultCharset().name()
        if (version.canCompileOrRun(JavaRelease.LEAST_MULTI_RELEASE_VERSION)) {
            options.compilerArgs.addAll(
                listOf(
                    "--module-path",
                    task.classpath.asPath,
                ),
            )
        }
        release.apply(options)
        options.release.set(version.asInt())
    }

    private fun populateMultiReleaseCompileJavaTaskSources(
        sourceSets: SourceSetContainer,
        releaseContainer: JavaReleaseContainer,
        release: JavaRelease,
        excludes: Set<String>,
        compileJavaTask: JavaCompile,
    ) {
        if (excludes.isEmpty()) {
            return
        }

        val releases = releaseContainer.canCompileOrRunOn(release.javaLanguageVersion)
        releaseContainer.main.get().also {
            val sourceSet = sourceSets.getByName(it.name)
            populateMultiReleaseCompileJavaTaskSource(excludes, sourceSet, compileJavaTask)
        }
        for (other in releases) {
            if (other.isReserved() || other == release) continue
            val sourceSet = sourceSets.getByName(other.name)
            populateMultiReleaseCompileJavaTaskSource(excludes, sourceSet, compileJavaTask)
        }
    }

    private fun populateMultiReleaseCompileJavaTaskSource(
        excludes: Set<String>,
        sourceSet: SourceSet,
        compileJavaTask: JavaCompile,
    ) {
        for (sourceDirectory in sourceSet.java.sourceDirectories) {
            val directory =
                project.objects
                    .directoryProperty()
                    .apply {
                        set(sourceDirectory)
                    }.get()
            directory.asFileTree.files.forEach { sourceFile ->
                val relativePath = sourceFile.toRelativeString(sourceDirectory)
                if (!excludes.contains(relativePath)) {
                    compileJavaTask.source(sourceFile)
                }
            }
        }
    }
}
