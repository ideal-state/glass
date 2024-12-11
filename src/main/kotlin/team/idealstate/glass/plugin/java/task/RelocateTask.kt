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
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import team.idealstate.glass.context.relocate.Relocator
import team.idealstate.glass.context.relocate.StringRelocator
import team.idealstate.glass.context.relocate.bytecode.JavaInternalRelocator
import team.idealstate.glass.context.util.PathUtils
import team.idealstate.glass.plugin.java.GlassJavaExtension
import team.idealstate.glass.plugin.java.task.data.JavaClassFile
import team.idealstate.glass.plugin.java.task.data.JavaFile
import team.idealstate.glass.plugin.java.task.data.RelocateJob
import team.idealstate.glass.plugin.java.task.data.RelocateJobContainer
import team.idealstate.glass.plugin.java.task.data.RelocateJobKey
import team.idealstate.glass.task.ParallelTask
import java.io.File
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentSkipListSet

open class RelocateTask : ParallelTask<RelocateJobKey, File, RelocateJob, RelocateJobContainer>() {
    companion object {
        const val NAME = "relocate"

        @JvmStatic
        fun register(project: Project): TaskProvider<RelocateTask> = project.tasks.register(NAME, RelocateTask::class.java)

        @JvmStatic
        fun register(
            project: Project,
            action: Action<in RelocateTask>,
        ): TaskProvider<RelocateTask> = project.tasks.register(NAME, RelocateTask::class.java, action)

        @JvmStatic
        fun of(project: Project): TaskProvider<RelocateTask> = project.tasks.named(NAME, RelocateTask::class.java)

        @JvmStatic
        fun of(
            project: Project,
            action: Action<in RelocateTask>,
        ): TaskProvider<RelocateTask> = project.tasks.named(NAME, RelocateTask::class.java, action)
    }

    private val relocators: ListProperty<Relocator> = project.objects.listProperty(Relocator::class.java)
    override val jobs: RelocateJobContainer = RelocateJobContainer(project, relocators)

    private val sourceDirectories = linkedSetOf<Provider<Directory>>()

    init {
        super.shouldRunAfter(UnzipInternalDependenciesTask.NAME)
    }

    fun source(source: File) {
        val directory =
            project.objects.directoryProperty().apply {
                set(source)
            }
        sourceDirectories.add(directory)
    }

    fun source(source: Directory) {
        sourceDirectories.add(project.provider { source })
    }

    fun source(source: Provider<Directory>) {
        sourceDirectories.add(source)
    }

    fun source(source: SourceSet) {
        val compileJavaTask = project.tasks.named(source.compileJavaTaskName, JavaCompile::class.java)
        val processResources = project.tasks.named(source.processResourcesTaskName, ProcessResources::class.java)
        dependsOn(compileJavaTask, processResources)
        source(compileJavaTask.get().destinationDirectory)
        source(processResources.get().destinationDir)
    }

    private val relocatedFilePaths: MutableMap<File, String> = mutableMapOf()

    fun getRelocatedPath(file: File): String? = relocatedFilePaths[file]

    override fun executeJobs() {
        processInternalDependencies()
        processSourceDirectories()
        super.executeJobs()
        val jobs = jobs.all
        for (job in jobs) {
            var path = job.path
            relocators.get().forEach { relocator -> path = relocator.relocate(path) ?: path }
            relocatedFilePaths[job.file] = path
        }
    }

    private fun processSourceDirectories() {
        sourceDirectories.forEach(::processSourceDirectory)
    }

    private fun processSourceDirectory(source: Provider<Directory>) {
        val sourceDir = source.get()
        processSourceDirectory(sourceDir)
    }

    private fun processSourceDirectory(source: Directory) {
        val base = source.asFile
        val files = source.asFileTree.files
        for (file in files) {
            jobs {
                it.relocate(file) { job ->
                    job.path(file.toRelativeString(base))
                }
            }
        }
    }

    private fun processInternalDependencies() {
        val internalDependenciesTask =
            project.tasks.named(UnzipInternalDependenciesTask.NAME, UnzipInternalDependenciesTask::class.java).orNull ?: return
        val internalDestinationDir = internalDependenciesTask.destinationDir.get()
        val glass = GlassJavaExtension.of(project)
        val relocators = collectJavaFiles(internalDestinationDir.asFile, glass.module.get())
        this.relocators.addAll(relocators)
    }

    private fun collectJavaFiles(
        base: File,
        module: String,
    ): Collection<Relocator> {
        val files =
            project.objects
                .fileTree()
                .setDir(base)
                .files
        val relocators = ConcurrentLinkedDeque<Relocator>()
        val parallel = files.parallelStream()
        val sourceDirectories = ConcurrentSkipListSet<File>()
        parallel.forEach { file ->
            val relativePath =
                PathUtils.normalize(file.toRelativeString(base)).run {
                    var start = 0
                    for (i in 1..2) {
                        start = indexOf(PathUtils.NORMAL_DELIMITER, start) + 1
                        if (start == 0 || start >= length) {
                            return@forEach
                        }
                    }
                    return@run substring(start)
                }
            val baseDirPath = PathUtils.normalize(file.absolutePath).substringBeforeLast(relativePath)
            val baseDir = File(baseDirPath)
            sourceDirectories.add(baseDir)
            val javaFile = JavaFile.of(baseDir, file)
            if (javaFile is JavaClassFile && javaFile.type != JavaClassFile.Type.MODULE_INFO) {
                val relocator =
                    JavaInternalRelocator(module, javaFile.className)
                relocators.add(relocator)
            }
        }
        sourceDirectories.forEach {
            val source =
                project.objects.directoryProperty().apply {
                    set(it)
                }
            processSourceDirectory(source)
        }
        return relocators
    }

    fun relocator(substring: String) {
        val glass = GlassJavaExtension.of(project)
        this.relocators.add(JavaInternalRelocator(glass.module.get(), substring))
    }

    fun relocator(
        substring: String,
        replacement: String,
    ) {
        this.relocators.add(StringRelocator(substring, replacement))
    }

    fun relocator(relocator: Relocator) {
        this.relocators.add(relocator)
    }
}
