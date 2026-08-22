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
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import team.idealstate.glass.plugin.project.java.data.JavaShadow
import team.idealstate.glass.plugin.project.java.util.JavaUtils
import team.idealstate.glass.plugin.project.java.util.bytecode.ModuleInfo
import team.idealstate.glass.plugin.project.java.util.bytecode.ModuleInfoImpl
import team.idealstate.glass.plugin.project.java.util.relocate.ClassReferenceRelocator
import team.idealstate.glass.plugin.project.java.util.relocate.Relocator
import team.idealstate.glass.plugin.project.java.util.relocate.bytecode.ClassRelocator
import java.io.File
import java.nio.charset.Charset
import java.util.Deque
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedDeque
import javax.inject.Inject

@CacheableTask
abstract class Shadow
    @Inject
    constructor(
        private val module: String,
        private val encoding: String,
        private val artifact: TaskProvider<Jar>,
        private val configuration: NamedDomainObjectProvider<Configuration>,
    ) : DefaultTask(),
        JavaShadow {
        @OutputDirectory
        val destinationDirectory = project.layout.buildDirectory.dir("glass/shadow")

        private val servicesContents = ConcurrentHashMap<String, Deque<String>>()

        private val moduleInfoContents = ConcurrentHashMap<String, Deque<ModuleInfo>>()

        init {
            super.dependsOn(artifact)
            inputs.file(project.provider { artifact.get().archiveFile })
        }

        @InputFiles
        @PathSensitive(PathSensitivity.RELATIVE)
        protected fun getDependencies(): Set<File> =
            LinkedHashSet(
                configuration.get().resolvedConfiguration.resolvedArtifacts.map {
                    it.file
                },
            )

        @TaskAction
        fun execute() {
            val relocators = relocators
            val destinationDirectory = destinationDirectory.get().asFile
            project.copy {
                into(destinationDirectory)
                includeEmptyDirs = false
                val charset = Charset.forName(encoding)
                eachFile {
                    path = relocate(path, relocators)
                    val path = path
                    val name = name
                    if (JavaUtils.isMaybeClassFile(name)) {
                        if (JavaUtils.isMaybeModuleInfoFile(name)) {
                            moduleInfoContents
                                .computeIfAbsent(path) {
                                    ConcurrentLinkedDeque()
                                }.add(ModuleInfo.of(file))
                            exclude()
                        }
                    } else if (JavaUtils.isMaybeServicesFile(path)) {
                        servicesContents
                            .computeIfAbsent(path) {
                                ConcurrentLinkedDeque()
                            }.addAll(open().bufferedReader(charset).readLines())
                        exclude()
                    }
                }
                for (dependency in getDependencies()) {
                    from(project.zipTree(dependency)) {
                        duplicatesStrategy = DuplicatesStrategy.WARN
                        eachFile {
                            val path = path
                            if (path.startsWith("META-INF/maven/") || JavaUtils.isMaybeManifestFile(path)) {
                                exclude()
                            }
                        }
                    }
                }
                from(project.zipTree(artifact.get().archiveFile.get())) {
                    duplicatesStrategy = DuplicatesStrategy.INCLUDE
                }
            }

            generateModuleInfoFiles(relocators, destinationDirectory)
            generateServicesFiles(relocators, destinationDirectory)

            relocateFilesContent(relocators, destinationDirectory)
        }

        protected open fun relocateFilesContent(
            relocators: List<Relocator>,
            destinationDirectory: File,
        ) {
            val charset = Charset.forName(encoding)
            for (file in destinationDirectory.walkTopDown()) {
                file.isDirectory && continue
                val name = file.name
                if (JavaUtils.isMaybeClassFile(name)) {
                    if (JavaUtils.isMaybeModuleInfoFile(name)) {
                        !ClassRelocator.relocate(file, relocators) && continue
                        val moduleInfo = ModuleInfo.of(file)
                        !excludeInternalReference(moduleInfo) && continue
                        moduleInfo.compileTo(file)
                    } else {
                        ClassRelocator.relocate(file, relocators)
                    }
                } else if (isRequiresRelocation(file)) {
                    val lines = ArrayList(file.readLines(charset))
                    lines.isEmpty() && continue
                    var changed = false
                    for ((index, line) in lines.withIndex()) {
                        val relocated = relocate(line, relocators)
                        line == relocated && continue
                        lines[index] = relocated
                        changed = true
                    }
                    !changed && continue
                    file.bufferedWriter(charset).use { writer ->
                        writer.write(lines.first())
                        writer.flush()
                        var skip = true
                        for (line in lines) {
                            if (skip) {
                                skip = false
                                continue
                            }
                            writer.appendLine(line)
                            writer.flush()
                        }
                    }
                }
            }
        }

        protected open fun generateModuleInfoFiles(
            relocators: List<Relocator>,
            destinationDirectory: File,
        ) {
            val mainClass = artifact.get().manifest.attributes["Main-Class"] as String?
            for ((path, moduleInfos) in moduleInfoContents) {
                var release = JavaUtils.LEAST_MULTI_RELEASE_SUPPORTED_VERSION
                for (moduleInfo in moduleInfos) {
                    val moduleInfoRelease = moduleInfo.release
                    if (moduleInfoRelease > release) {
                        release = moduleInfoRelease
                    }
                }
                val mainModuleInfo = ModuleInfo.of(release, module, project.version.toString(), false, mainClass)
                val mergedModuleInfo = ModuleInfo.merge(mainModuleInfo, *moduleInfos.toTypedArray())
                excludeInternalReference(mergedModuleInfo)
                val destinationFile = File(destinationDirectory, path)
                mergedModuleInfo.compileTo(destinationFile)
            }
        }

        protected open fun excludeInternalReference(moduleInfo: ModuleInfo): Boolean {
            var changed = false
            val internalPackageName = JavaUtils.internalizeClassName(module, "internal")
            moduleInfo as ModuleInfoImpl
            for (export in moduleInfo.exports) {
                val exportPackageName = export.packageName
                if (exportPackageName.startsWith(internalPackageName)) {
                    moduleInfo.unexport(exportPackageName)
                    changed = true
                }
            }
            for (open in moduleInfo.opens) {
                val openPackageName = open.packageName
                if (openPackageName.startsWith(internalPackageName)) {
                    moduleInfo.unopen(openPackageName)
                    changed = true
                }
            }
            for (provide in moduleInfo.provides) {
                val service = provide.service
                if (service.startsWith(internalPackageName)) {
                    moduleInfo.unprovide(service)
                    changed = true
                }
            }
            for (use in moduleInfo.uses) {
                val service = use.service
                if (service.startsWith(internalPackageName)) {
                    moduleInfo.unuse(service)
                    changed = true
                }
            }
            return changed
        }

        protected open fun generateServicesFiles(
            relocators: List<Relocator>,
            destinationDirectory: File,
        ) {
            val charset = Charset.forName(encoding)
            for (entry in servicesContents) {
                val path = entry.key
                var providers: Collection<String> = entry.value
                providers.isEmpty() && continue
                providers = LinkedHashSet(providers)
                val file = File(destinationDirectory, path)
                file.parentFile.mkdirs()
                file.createNewFile()
                file.bufferedWriter(charset).use { writer ->
                    for (provider in providers) {
                        writer.appendLine(provider)
                        writer.flush()
                    }
                }
            }
        }

        protected open fun relocate(
            source: String,
            relocators: List<Relocator>,
        ): String {
            relocators.isEmpty() && return source
            var process = source
            for (relocator in relocators) {
                val relocated = relocator.relocate(process) ?: continue
                process = relocated
            }
            return process
        }

        private val relocators
            get() = mutableListOf<Relocator>() + internalRelocators.get().values + normalRelocators.get().values + customRelocators.get()

        @Suppress("UNCHECKED_CAST")
        protected fun isRequiresRelocation(file: File): Boolean {
            for (exclude in excludes.get()) {
                if ((exclude as Spec<File>).isSatisfiedBy(file)) {
                    return false
                }
            }
            for (include in includes.get()) {
                if ((include as Spec<File>).isSatisfiedBy(file)) {
                    return true
                }
            }
            return false
        }

        @Input
        protected val includes = project.objects.listProperty(Spec::class.java).apply { finalizeValueOnRead() }

        override fun include(includeSpec: Spec<File>) {
            includes.add(includeSpec)
        }

        @Input
        protected val excludes = project.objects.listProperty(Spec::class.java).apply { finalizeValueOnRead() }

        override fun exclude(excludeSpec: Spec<File>) {
            excludes.add(excludeSpec)
        }

        @Input
        protected val internalRelocators =
            project.objects.mapProperty(String::class.java, Relocator::class.java).apply { finalizeValueOnRead() }

        override fun internal(source: String) {
            val className = JavaUtils.normalizeClassName(source)
            internalRelocators.put(
                className,
                ClassReferenceRelocator(
                    className,
                    JavaUtils.normalizeClassName(module, "internal", className),
                ),
            )
        }

        @Input
        protected val normalRelocators =
            project.objects.mapProperty(String::class.java, Relocator::class.java).apply { finalizeValueOnRead() }

        override fun relocate(
            source: String,
            target: String,
        ) {
            val className = JavaUtils.normalizeClassName(source)
            normalRelocators.put(
                className,
                ClassReferenceRelocator(
                    className,
                    JavaUtils.normalizeClassName(target),
                ),
            )
        }

        @Input
        protected val customRelocators = project.objects.listProperty(Relocator::class.java).apply { finalizeValueOnRead() }

        override fun custom(relocator: Relocator) {
            customRelocators.add(relocator)
        }
    }
