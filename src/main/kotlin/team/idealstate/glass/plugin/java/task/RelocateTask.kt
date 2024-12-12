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
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.work.DisableCachingByDefault
import team.idealstate.glass.context.bytecode.ModuleInfo
import team.idealstate.glass.context.bytecode.ModuleInfoImpl
import team.idealstate.glass.context.filter.Exclude
import team.idealstate.glass.context.relocate.Relocator
import team.idealstate.glass.context.relocate.StringRelocator
import team.idealstate.glass.context.util.ClassUtils
import team.idealstate.glass.context.util.Extensions
import team.idealstate.glass.context.util.PathUtils
import team.idealstate.glass.plugin.java.GlassJavaExtension
import team.idealstate.glass.plugin.java.task.data.JavaClassFile
import team.idealstate.glass.plugin.java.task.data.JavaFile
import team.idealstate.glass.plugin.java.task.data.JavaResourceFile
import team.idealstate.glass.plugin.java.task.data.Skip
import team.idealstate.glass.plugin.java.task.data.relocate.JavaInternalRelocator
import team.idealstate.glass.plugin.java.task.data.relocate.RelocateJob
import team.idealstate.glass.plugin.java.task.data.relocate.RelocateJobContainer
import team.idealstate.glass.plugin.java.task.data.relocate.RelocateJobDetail
import team.idealstate.glass.plugin.java.task.data.relocate.RelocateJobKey
import team.idealstate.glass.plugin.java.task.data.relocate.RelocateJobResult
import team.idealstate.glass.plugin.java.task.data.relocate.RelocationJobDetail
import team.idealstate.glass.task.ParallelTask
import java.io.File
import java.util.LinkedList
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArraySet

@DisableCachingByDefault
open class RelocateTask : ParallelTask<RelocateJobKey, File, RelocateJobResult, RelocateJob, RelocateJobContainer>() {
    object ExcludeManifest : Exclude<RelocateJobDetail> {
        override fun exclude(it: RelocateJobDetail): Boolean = it.path == ClassUtils.MANIFEST_FILE_PATH_NAME
    }

    object SkipCopyright : Skip<RelocateJobDetail> {
        @JvmStatic
        private val skips =
            setOf(
                "COPYRIGHT",
                "LICENSE",
                "NOTICE",
                "COPYRIGHT.txt",
                "LICENSE.txt",
                "NOTICE.txt",
                "Copyright",
                "License",
                "Notice",
                "Copyright.txt",
                "License.txt",
                "Notice.txt",
                "copyright",
                "license",
                "notice",
                "copyright.txt",
                "license.txt",
                "notice.txt",
            )

        override fun skip(it: RelocateJobDetail): Boolean {
            val path = it.path
            if (path.startsWith(PathUtils.normalize("META-INF/${CopyrightTask.ROOT_NAME}/"))) {
                return true
            }
            return skips.any { skip -> path.endsWith(skip) }
        }
    }

    object SkipDependenciesInformation : Skip<RelocateJobDetail> {
        override fun skip(it: RelocateJobDetail): Boolean =
            it.path == PathUtils.normalize("META-INF/${DependenciesInformationTask.ROOT_NAME}/")
    }

    companion object {
        const val NAME = "relocate"
        const val ROOT_NAME = "relocate"

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

    @Suppress("UNCHECKED_CAST")
    private val excludes: ListProperty<Exclude<in RelocateJobDetail>> =
        project.objects.listProperty(
            Exclude::class.java,
        ) as ListProperty<Exclude<in RelocateJobDetail>>

    @Suppress("UNCHECKED_CAST")
    private val skips: ListProperty<Skip<in RelocateJobDetail>> =
        project.objects.listProperty(
            Skip::class.java,
        ) as ListProperty<Skip<in RelocateJobDetail>>
    private val relocators: ListProperty<Relocator> = project.objects.listProperty(Relocator::class.java)
    override val jobs: RelocateJobContainer = RelocateJobContainer(project, excludes, skips, relocators)

    private val sourceDirectories = linkedSetOf<Provider<Directory>>()

    @Input
    val module: Property<String> =
        project.objects.property(String::class.java).apply {
            set(
                project.provider {
                    GlassJavaExtension.of(project).module.get()
                },
            )
        }

    @OutputDirectory
    val destinationDirectory: DirectoryProperty =
        project.objects.directoryProperty().apply {
            set(project.layout.buildDirectory.dir(ROOT_NAME))
        }

    init {
        super.shouldRunAfter(UnzipInternalDependenciesTask.NAME)
        exclude(ExcludeManifest)
        skip(SkipCopyright)
        skip(SkipDependenciesInformation)
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

    private val relocateJobResults: MutableMap<File, RelocateJobResult> = mutableMapOf()

    fun getRelocateResult(file: File): RelocateJobResult? = relocateJobResults[file]

    fun exclude(exclude: Exclude<in RelocateJobDetail>) {
        excludes.add(exclude)
    }

    fun skip(skip: Skip<in RelocateJobDetail>) {
        skips.add(skip)
    }

    override fun executeJobs() {
        processInternalDependencies()
        processSourceDirectories()
        super.executeJobs()
        mappingResults()
        processResults(relocateJobResults)
    }

    private fun processResults(relocateJobResults: MutableMap<File, RelocateJobResult>) {
        val initialCapacity = relocateJobResults.values.size / 2
        val moduleInfoResults = ArrayDeque<Pair<JavaClassFile, RelocateJobResult>>(initialCapacity)
        val serviceResults = ArrayDeque<RelocateJobResult>(initialCapacity)
        relocateJobResults.forEach { (file, result) ->
            if (result.exclude) {
                return@forEach
            }
            val sourcePath = result.sourcePath
            if (ClassUtils.maybeModuleInfoFile(sourcePath)) {
                val baseDirPath = PathUtils.normalize(file.absolutePath).substringBeforeLast(sourcePath)
                val baseDir = File(baseDirPath)
                val javaFile = JavaFile.of(baseDir, file)
                if (javaFile is JavaClassFile && javaFile.type == JavaClassFile.Type.MODULE_INFO) {
                    moduleInfoResults.add(Pair(javaFile, result))
                }
                return@forEach
            }
            if (sourcePath.startsWith(ClassUtils.SERVICES_DIR_PATH_NAME)) {
                val baseDirPath = PathUtils.normalize(file.absolutePath).substringBeforeLast(sourcePath)
                val baseDir = File(baseDirPath)
                val javaFile = JavaFile.of(baseDir, file)
                if (javaFile is JavaResourceFile) {
                    serviceResults.add(result)
                }
            }
        }

        if (moduleInfoResults.isNotEmpty()) {
            val mainSourceSet = Extensions.sourceSets(project).named(SourceSet.MAIN_SOURCE_SET_NAME).get()
            val compileJavaTask = project.tasks.named(mainSourceSet.compileJavaTaskName, JavaCompile::class.java).get()
            val mainRelease = compileJavaTask.options.release.get()
            mergeModuleInfos(moduleInfoResults, mainRelease)
        }

        if (serviceResults.isNotEmpty()) {
            mergeServices(serviceResults)
        }
    }

    private fun mergeModuleInfos(
        moduleInfoResult: ArrayDeque<Pair<JavaClassFile, RelocateJobResult>>,
        mainRelease: Int,
    ) {
        val initialCapacity = moduleInfoResult.size
        val releaseModuleInfos = HashMap<Int, MutableList<ModuleInfo>>(initialCapacity)
        for (pair in moduleInfoResult) {
            val (javaFile, result) = pair
            val file = result.file
            val moduleInfo = ModuleInfo.of(file)
            val detail = RelocationJobDetail(file, result.sourcePath, result.path, true)
            this.relocateJobResults[file] = detail.toRelocateResult()
            val release = javaFile.release ?: moduleInfo.release
            val moduleInfos =
                releaseModuleInfos.computeIfAbsent(release) {
                    mutableListOf()
                }
            moduleInfos.add(moduleInfo)
        }
        val module = this.module.get()
        val destinationDirectory = this.destinationDirectory.get().asFile
        val internalPackageName = ClassUtils.internalize(module, JavaInternalRelocator.INTERNAL_PACKAGE_NAME)
        for ((release, moduleInfos) in releaseModuleInfos) {
            val mainModuleInfo = ModuleInfo.of(release, module, null, false)
            val mergedModuleInfo = ModuleInfo.merge(mainModuleInfo, *moduleInfos.toTypedArray())
            mergedModuleInfo as ModuleInfoImpl
            for (export in mergedModuleInfo.exports) {
                val exportPackageName = export.packageName
                if (exportPackageName.startsWith(internalPackageName)) {
                    mergedModuleInfo.unexport(exportPackageName)
                }
            }
            for (open in mergedModuleInfo.opens) {
                val openPackageName = open.packageName
                if (openPackageName.startsWith(internalPackageName)) {
                    mergedModuleInfo.unopen(openPackageName)
                }
            }
            for (provide in mergedModuleInfo.provides) {
                val service = provide.service
                if (service.startsWith(internalPackageName)) {
                    mergedModuleInfo.unprovide(service)
                }
            }
            for (use in mergedModuleInfo.uses) {
                val service = use.service
                if (service.startsWith(internalPackageName)) {
                    mergedModuleInfo.unuse(service)
                }
            }

            val destinationFile =
                if (release == mainRelease) {
                    File(destinationDirectory, ClassUtils.MODULE_INFO_FILE_NAME)
                } else {
                    File(
                        destinationDirectory,
                        PathUtils.normalize(
                            ClassUtils.MULTI_RELEASE_DIR_PATH_NAME,
                            release.toString(),
                            ClassUtils.MODULE_INFO_FILE_NAME,
                        ),
                    )
                }
            mergedModuleInfo.compileTo(destinationFile)
        }
    }

    private fun mergeServices(serviceResults: ArrayDeque<RelocateJobResult>) {
        val initialCapacity = serviceResults.size
        val serviceProviders = HashMap<String, MutableList<List<String>>>(initialCapacity)
        for (result in serviceResults) {
            val file = result.file
            val detail = RelocationJobDetail(file, result.sourcePath, result.path, true)
            this.relocateJobResults[file] = detail.toRelocateResult()
            val path = result.path
            val resources =
                serviceProviders.computeIfAbsent(path) {
                    mutableListOf()
                }
            resources.add(file.readLines())
        }
        val destinationDirectory = this.destinationDirectory.get().asFile
        for ((path, resources) in serviceProviders) {
            val destinationFile = File(destinationDirectory, path)
            val mergedResources = resources.flatten().distinct()
            if (!destinationFile.exists()) {
                destinationFile.parentFile.mkdirs()
            }
            destinationFile.writeText(mergedResources.joinToString("\n"))
        }
    }

    private fun mappingResults() {
        val relocateJobResults = LinkedList(jobResults)
        for (relocateJobResult in relocateJobResults) {
            val file = relocateJobResult.file
            this.relocateJobResults[file] = relocateJobResult
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
        val relocators = processJavaFiles(internalDestinationDir.asFile, module.get())
        this.relocators.addAll(relocators)
    }

    private fun processJavaFiles(
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
        val sourceDirectories = CopyOnWriteArraySet<File>()
        val packageNames = CopyOnWriteArraySet<String>()
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
                packageNames.add(javaFile.packageName)
                val classLevelRelocator =
                    JavaInternalRelocator(module, javaFile.className)
                relocators.add(classLevelRelocator)
            }
        }
        packageNames.forEach {
            val packageLevelRelocator =
                JavaInternalRelocator(module, it)
            relocators.add(packageLevelRelocator)
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
        this.relocators.add(JavaInternalRelocator(module.get(), substring))
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
