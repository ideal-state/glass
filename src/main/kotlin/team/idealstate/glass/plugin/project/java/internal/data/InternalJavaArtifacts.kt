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

package team.idealstate.glass.plugin.project.java.internal.data

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaToolchainService
import team.idealstate.glass.plugin.project.java.data.JavaArtifacts
import team.idealstate.glass.plugin.project.java.data.JavaDocumentation
import team.idealstate.glass.plugin.project.java.data.JavaManifest
import team.idealstate.glass.plugin.project.java.data.JavaShadow
import team.idealstate.glass.plugin.project.java.data.JavaSources
import team.idealstate.glass.plugin.project.java.internal.extension.InternalJavaExtension.Companion.TASK_GROUP
import team.idealstate.glass.plugin.project.java.task.Copyright
import team.idealstate.glass.plugin.project.java.task.Shadow
import team.idealstate.glass.plugin.project.java.task.Sources
import team.idealstate.glass.plugin.project.java.util.JavaUtils
import java.util.Locale
import kotlin.collections.iterator

internal open class InternalJavaArtifacts(
    objects: ObjectFactory,
) : JavaArtifacts {
    private val manifestAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun manifest(action: Action<JavaManifest>) {
        manifestAction.set(action)
    }

    private val shadowJarAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun shadowJar(action: Action<JavaShadow>) {
        shadowJarAction.set(action)
    }

    private val sourcesJarAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun sourcesJar(action: Action<JavaSources>) {
        sourcesJarAction.set(action)
    }

    private val javadocJarAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun javadocJar(action: Action<JavaDocumentation>) {
        javadocJarAction.set(action)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(
        project: Project,
        module: String,
        encoding: String,
        toolchainVersion: Int,
        shadow: NamedDomainObjectProvider<Configuration>,
        doclet: NamedDomainObjectProvider<Configuration>,
        toolchains: JavaToolchainService,
        mainSourceSet: SourceSet,
        multiSourceSets: Map<Int, SourceSet>,
    ) {
        val manifestAction = manifestAction.orNull as Action<JavaManifest>?

        val shadowJarAction = shadowJarAction.orNull as Action<JavaShadow>?
        val withShadowJar = shadowJarAction != null
        val sourcesJarAction = sourcesJarAction.orNull as Action<JavaSources>?
        val withSourcesJar = sourcesJarAction != null
        val javadocJarAction = javadocJarAction.orNull as Action<JavaDocumentation>?
        val withJavadocJar = javadocJarAction != null

        val jar = registerJar(project, multiSourceSets, manifestAction)

        if (withShadowJar) {
            registerShadowJar(project, module, encoding, shadow, mainSourceSet, jar, shadowJarAction)
        }

        if (withSourcesJar) {
            registerSourcesJar(project, mainSourceSet, multiSourceSets, sourcesJarAction)
        }

        if (withJavadocJar) {
            registerJavadocJar(project, encoding, toolchainVersion, doclet, toolchains, javadocJarAction)
        }
    }

    private fun registerJar(
        project: Project,
        multiSourceSets: Map<Int, SourceSet>,
        manifestAction: Action<JavaManifest>?,
    ): TaskProvider<Jar> {
        val javaManifest = InternalJavaManifest(project.objects)
        manifestAction?.execute(javaManifest)
        if (!multiSourceSets.isEmpty()) {
            javaManifest.add {
                it.attributes["Multi-Release"] = true
            }
        }
        val copyright =
            project.tasks.register("copyright", Copyright::class.java) {
                it.group = TASK_GROUP
            }
        return project.tasks.named("jar", Jar::class.java) {
            it.group = TASK_GROUP
            for ((multiVersion, sourceSet) in multiSourceSets) {
                it.dependsOn(sourceSet.classesTaskName)
                val base = "${JavaUtils.MULTI_RELEASE_DIR_PATH_NAME}$multiVersion"
                it.from(project.tasks.named(sourceSet.compileJavaTaskName)) { copy ->
                    copy.into(base)
                    copy.exclude { element ->
                        element.path == "previous-compilation-data.bin"
                    }
                }
                it.from(project.tasks.named(sourceSet.processResourcesTaskName)) { copy ->
                    copy.into(base)
                }

                it.dependsOn(copyright)
                it.from(copyright) { copy ->
                    copy.duplicatesStrategy = DuplicatesStrategy.INCLUDE
                    copy.into("META-INF/copyright/")
                }
            }
            javaManifest.apply(project, it.manifest)
        }
    }

    private fun registerShadowJar(
        project: Project,
        module: String,
        encoding: String,
        shadow: NamedDomainObjectProvider<Configuration>,
        mainSourceSet: SourceSet,
        jar: TaskProvider<Jar>,
        shadowJarAction: Action<JavaShadow>,
    ) {
        project.configurations.named(mainSourceSet.implementationConfigurationName) {
            it.extendsFrom(shadow.get())
        }
        val shadowTask =
            project.tasks.register("shadow", Shadow::class.java, module, encoding, jar, shadow).apply {
                configure {
                    it.group = TASK_GROUP
                    shadowJarAction.execute(it)
                }
            }
        val shadowJar =
            project.tasks.register("shadowJar", Jar::class.java) {
                it.group = TASK_GROUP
                it.dependsOn(shadowTask)
                it.from(shadowTask)
                it.archiveBaseName.set(project.provider { jar.get().archiveBaseName.orNull })
                it.archiveAppendix.set(project.provider { jar.get().archiveAppendix.orNull })
                it.archiveVersion.set(project.provider { jar.get().archiveVersion.orNull })
                it.archiveClassifier.set(project.provider { jar.get().archiveClassifier.orNull })
                it.archiveExtension.set(project.provider { jar.get().archiveExtension.get() })
                it.destinationDirectory.set(project.provider { jar.get().destinationDirectory.get() })
                it.manifest { manifest ->
                    manifest.attributes.putAll(jar.get().manifest.attributes)
                }
            }
        jar.configure {
            it.finalizedBy(shadowJar)
        }
    }

    @Suppress("unused")
    private fun registerSourcesJar(
        project: Project,
        mainSourceSet: SourceSet,
        multiSourceSets: Map<Int, SourceSet>,
        sourcesJarAction: Action<JavaSources>,
    ) {
        val javaSources = InternalJavaSources(project.objects)
        sourcesJarAction.execute(javaSources)
        val mainSources =
            project.tasks.register("sources", Sources::class.java, mainSourceSet).apply {
                configure {
                    it.group = TASK_GROUP
                }
            }
        val multiSources = linkedMapOf<Int, TaskProvider<Sources>>()
        for ((multiVersion, sourceSet) in multiSourceSets) {
            multiSources[multiVersion] =
                project.tasks
                    .register(
                        "${
                            sourceSet.name.replaceFirstChar {
                                if (it.isUpperCase()) {
                                    it.lowercase(
                                        Locale.ENGLISH,
                                    )
                                } else {
                                    it.toString()
                                }
                            }
                        }Sources",
                        Sources::class.java,
                        sourceSet,
                    ).apply {
                        configure {
                            it.group = TASK_GROUP
                        }
                    }
        }
        project.tasks.register("sourcesJar", Jar::class.java) {
            it.group = TASK_GROUP
            it.archiveClassifier.set("sources")
            it.dependsOn(mainSources)
            it.from(mainSources)
            for ((multiVersion, sources) in multiSources) {
                it.dependsOn(sources)
                val base = "${JavaUtils.MULTI_RELEASE_DIR_PATH_NAME}$multiVersion"
                it.from(sources) { copy ->
                    copy.into(base)
                }
            }
            it.destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        }
    }

    private fun registerJavadocJar(
        project: Project,
        encoding: String,
        toolchainVersion: Int,
        doclet: NamedDomainObjectProvider<Configuration>,
        toolchains: JavaToolchainService,
        javadocJarAction: Action<JavaDocumentation>,
    ) {
        val javaDocumentation = InternalJavaDocumentation(project.objects, toolchainVersion)
        javadocJarAction.execute(javaDocumentation)
        project.tasks.register("javadocJar", Jar::class.java) {
            it.group = TASK_GROUP
            it.archiveClassifier.set("javadoc")
            it.from(project.tasks.named("javadoc", Javadoc::class.java))
            it.destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        }
        project.tasks.named("javadoc", Javadoc::class.java) {
            it.group = TASK_GROUP
            it.isFailOnError = false
            it.options { options ->
                val docletFiles =
                    doclet
                        .get()
                        .allArtifacts.files.files
                if (options is StandardJavadocDocletOptions) {
                    options.charSet(encoding)
                    options.docEncoding(encoding)
                    options.author(true)
                    options.version(true)
                    options.addBooleanOption("Xdoclint:none", true)
                }
                if (!docletFiles.isEmpty()) {
                    options.docletpath.addAll(docletFiles)
                }
                options.encoding(encoding)
                options.jFlags("-Dfile.encoding=$encoding")
            }
            javaDocumentation.apply(it, toolchains)
        }
    }
}
