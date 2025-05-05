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
import org.gradle.api.file.ConfigurableFileTree
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTreeElement
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.work.DisableCachingByDefault
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.Opcodes
import team.idealstate.glass.context.util.Extensions
import team.idealstate.glass.context.util.Validates
import team.idealstate.glass.plugin.java.data.JavaRelease
import java.io.File

@DisableCachingByDefault
open class CollectMultiReleaseClassesTask : DefaultTask() {
    companion object {
        @JvmStatic
        fun nameof(release: JavaRelease): String {
            val multiReleaseName =
                release.name.replaceFirstChar {
                    if (it.isLowerCase()) it.titlecase() else it.toString()
                }
            return "collectMultiRelease${multiReleaseName}Classes"
        }

        @JvmStatic
        fun sourceSetJavaRelativeFiles(
            sourceSet: SourceSet,
            objects: ObjectFactory,
        ): Set<String> {
            val files = mutableSetOf<String>()
            sourceSet.java.sourceDirectories.forEach { sourceDirectory ->
                val directory =
                    objects
                        .directoryProperty()
                        .apply {
                            set(sourceDirectory)
                        }.get()
                directory.asFileTree.files.forEach { sourceFile ->
                    val relativePath = sourceFile.toRelativeString(sourceDirectory)
                    files.add(relativePath)
                }
            }
            return files
        }

        @JvmStatic
        fun register(
            project: Project,
            release: JavaRelease,
        ): TaskProvider<CollectMultiReleaseClassesTask> =
            project.tasks.register(
                nameof(release),
                CollectMultiReleaseClassesTask::class.java,
            ) {
                it.setup(release)
            }

        @JvmStatic
        fun of(
            project: Project,
            release: JavaRelease,
        ): TaskProvider<CollectMultiReleaseClassesTask> =
            project.tasks.named(
                nameof(release),
                CollectMultiReleaseClassesTask::class.java,
            )

        @JvmStatic
        fun of(
            project: Project,
            release: JavaRelease,
            action: Action<in CollectMultiReleaseClassesTask>,
        ): TaskProvider<CollectMultiReleaseClassesTask> =
            project.tasks.named(
                nameof(release),
                CollectMultiReleaseClassesTask::class.java,
                action,
            )
    }

    private var _release: JavaRelease? = null
    @get:Internal
    val release: JavaRelease
        get() = Validates.isPresent(_release, "release")

    private var _classesBaseDir: File? = null
    @get:Internal
    val classesBaseDir: File
        get() = Validates.isPresent(_classesBaseDir, "classesBaseDir")

    @Optional
    @OutputFiles
    val classes: Property<ConfigurableFileTree> = project.objects.property(ConfigurableFileTree::class.java)

    @Optional
    @OutputDirectory
    val resources: DirectoryProperty = project.objects.directoryProperty()

    @Suppress("unused")
    @Input
    protected val alwaysRun: Property<Long> =
        project.objects.property(Long::class.java).apply {
            set(project.provider { System.currentTimeMillis() })
        }

    private val includes: Set<String> = mutableSetOf()

    @TaskAction
    protected open fun populateIncludes() {
        val multiRelease = this.release
        if (multiRelease.isReserved()) {
            return
        }
        val suffix = ".java"
        val sourceSets = Extensions.sourceSets(project)
        val sourceSet = sourceSets.named(multiRelease.name).get()
        (this.includes as MutableSet<String>).addAll(
            sourceSetJavaRelativeFiles(sourceSet, project.objects).map { path ->
                if (path.endsWith(suffix)) {
                    return@map path.substring(0, path.length - suffix.length)
                }
                return@map path
            },
        )
    }

    private fun setup(release: JavaRelease) {
        if (release.isReserved()) {
            return
        }
        val sourceSets = Extensions.sourceSets(project)
        val sourceSet = sourceSets.named(release.name).get()
        dependsOn(sourceSet.classesTaskName)
        this._release = release
        resources.set(
            project.provider {
                val processResourcesTask = project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources::class.java)
                return@provider project.objects
                    .directoryProperty()
                    .apply {
                        set(processResourcesTask.get().destinationDir)
                    }.get()
            },
        )
        val tasks = project.tasks
        val compileJavaTask = tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java)
        val base =
            compileJavaTask
                .get()
                .destinationDirectory
                .get()
                .asFile
        val classes =
            this.classes
                .apply {
                    set(project.objects.fileTree())
                }.get()
        this._classesBaseDir = base
        classes.setDir(base)
        classes.exclude(ExcludeSpec(base, includes))
    }

    private class ExcludeSpec(
        private val base: File,
        private val includes: Set<String>,
    ) : Spec<FileTreeElement> {
        companion object {
            const val CLASS_SUFFIX = ".class"
        }

        override fun isSatisfiedBy(element: FileTreeElement): Boolean {
            if (element.isDirectory) {
                return false
            }
            if (includes.isEmpty()) {
                return true
            }
            val file = element.file
            val relativePath =
                file.toRelativeString(base).run {
                    if (endsWith(CLASS_SUFFIX)) {
                        return@run substring(0, length - CLASS_SUFFIX.length)
                    }
                    return@run this
                }
            if (includes.contains(relativePath)) {
                return false
            }
            val matched = includes.filter(relativePath::startsWith)
            if (matched.isNotEmpty()) {
                val classReader = ClassReader(file.inputStream())
                val owner = matched.first().replace('\\', '/')
                val ownership = ClassOwnership(Opcodes.ASM9, owner)
                classReader.accept(
                    ownership,
                    ClassReader.SKIP_CODE +
                        ClassReader.SKIP_DEBUG +
                        ClassReader.SKIP_FRAMES +
                        ClassReader.EXPAND_FRAMES,
                )
                if (ownership.isOwner) {
                    return false
                }
            }
            return true
        }

        private class ClassOwnership(
            api: Int,
            private val owner: String,
        ) : ClassVisitor(api) {
            var isOwner: Boolean = false
                private set

            override fun visitOuterClass(
                owner: String?,
                name: String?,
                descriptor: String?,
            ) {
                super.visitOuterClass(owner, name, descriptor)
                isOwner = owner == this.owner
            }
        }
    }
}
