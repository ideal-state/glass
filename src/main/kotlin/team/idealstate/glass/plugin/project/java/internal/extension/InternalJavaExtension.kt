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

package team.idealstate.glass.plugin.project.java.internal.extension

import groovy.util.Node
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.signing.SigningExtension
import org.gradle.testing.base.TestingExtension
import team.idealstate.glass.plugin.project.java.data.Java
import team.idealstate.glass.plugin.project.java.data.JavaArtifacts
import team.idealstate.glass.plugin.project.java.data.JavaDeploy
import team.idealstate.glass.plugin.project.java.data.JavaIntegration
import team.idealstate.glass.plugin.project.java.data.JavaPublication
import team.idealstate.glass.plugin.project.java.data.JavaRelease
import team.idealstate.glass.plugin.project.java.extension.JavaExtension
import team.idealstate.glass.plugin.project.java.internal.data.InternalJava
import team.idealstate.glass.plugin.project.java.internal.data.InternalJavaArtifacts
import team.idealstate.glass.plugin.project.java.internal.data.InternalJavaDeploy
import team.idealstate.glass.plugin.project.java.internal.data.InternalJavaIntegration
import team.idealstate.glass.plugin.project.java.internal.data.InternalJavaPublication
import team.idealstate.glass.plugin.project.java.internal.data.InternalJavaRelease
import team.idealstate.glass.plugin.project.java.task.Pom
import team.idealstate.glass.plugin.project.java.util.JavaUtils
import team.idealstate.glass.plugin.project.java.util.dependency.ScopedDependencyInformation
import team.idealstate.glass.util.GitUtil
import team.idealstate.glass.util.TemplateFile
import java.io.File
import java.nio.charset.Charset

internal open class InternalJavaExtension(
    private val project: Project,
    private val shadow: NamedDomainObjectProvider<Configuration>,
    private val doclet: NamedDomainObjectProvider<Configuration>,
) : JavaExtension {
    companion object {
        const val MODULE_NAME_PATTERN = "^[a-zA-Z][a-zA-Z0-9_$]*(?:\\.[a-zA-Z][a-zA-Z0-9_$]*)*$"
        const val TASK_GROUP = "glass"
    }

    override val module = project.objects.property(String::class.java).apply { finalizeValueOnRead() }

    override fun module(module: String) {
        this.module.set(module)
    }

    private val javaVersion = project.objects.property(Int::class.java).apply { finalizeValueOnRead() }
    private val javaAction = project.objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun release(
        version: Int,
        action: Action<Java>,
    ) {
        javaVersion.set(version)
        javaAction.set(action)
    }

    private val artifactsAction = project.objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun artifacts(action: Action<JavaArtifacts>) {
        artifactsAction.set(action)
    }

    private val integrationAction = project.objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun integration(action: Action<JavaIntegration>) {
        integrationAction.set(action)
    }

    private val publicationName = project.objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val publicationAction = project.objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun publication(
        name: String,
        action: Action<JavaPublication>,
    ) {
        publicationName.set(name)
        publicationAction.set(action)
    }

    private val deployAction = project.objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun deploy(
        action: Action<JavaDeploy>,
    ) {
        deployAction.set(action)
    }

    @Suppress("UNCHECKED_CAST", "UnstableApiUsage")
    override fun apply() {
        val encoding = Charset.defaultCharset().name()
        val objects = project.objects
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val toolchains = project.extensions.getByType(JavaToolchainService::class.java)
        val configurations = project.configurations
        val testingExtension = project.extensions.getByType(TestingExtension::class.java)

        // --------------------------------------------------------------------------------------------------

        val module = module.get()
        if (!Regex(MODULE_NAME_PATTERN).matches(module)) {
            throw IllegalArgumentException("Module name \"$module\" does not match required pattern: \"$MODULE_NAME_PATTERN\".")
        }

        val java = InternalJava(objects, javaVersion.get())
        (this.javaAction.get() as Action<Java>).execute(java)
        val multiReleases = sortedMapOf<Int, Action<JavaRelease>>(Comparator.comparingInt { it })
        java.apply(project, multiReleases)
        val toolchainVersion = java.toolchainVersion

        // --------------------------------------------------------------------------------------------------

        val artifactsAction = artifactsAction.orNull as Action<JavaArtifacts>?
        val withArtifacts = artifactsAction != null

        val integrationAction = integrationAction.orNull as Action<JavaIntegration>?
        val withIntegration = integrationAction != null

        val publicationAction = publicationAction.orNull as Action<JavaPublication>?
        val withPublication = publicationAction != null

        val deployAction = deployAction.orNull as Action<JavaDeploy>?
        val withDeploy = deployAction != null

        // --------------------------------------------------------------------------------------------------

        val mainSourceSet = sourceSets.getByName("main")
        generateSources(project, module, encoding, mainSourceSet)

        val testSourceSet = sourceSets.getByName("test")
        val multiSourceSets =
            registerSourceSets(
                project,
                sourceSets,
                configurations,
                testingExtension,
                toolchains,
                encoding,
                java,
                mainSourceSet,
                testSourceSet,
                multiReleases,
            )

        if (withArtifacts) {
            val javaArtifacts = InternalJavaArtifacts(objects)
            artifactsAction.execute(javaArtifacts)
            javaArtifacts.apply(
                project,
                module,
                encoding,
                toolchainVersion,
                shadow,
                doclet,
                toolchains,
                mainSourceSet,
                multiSourceSets,
            )
        }

        if (withIntegration) {
            val javaIntegration = InternalJavaIntegration(objects)
            integrationAction.execute(javaIntegration)
            javaIntegration.apply(project, testingExtension, mainSourceSet, testSourceSet, multiSourceSets)
        }

        if (withPublication) {
            val jar = project.tasks.named("jar", Jar::class.java)
            val shadowJar =
                if (project.tasks.names.contains("shadowJar")) {
                    project.tasks.named("shadowJar", Jar::class.java)
                } else {
                    null
                }
            val sourcesJar =
                if (project.tasks.names.contains("sourcesJar")) {
                    project.tasks.named("sourcesJar", Jar::class.java)
                } else {
                    null
                }
            val javadocJar =
                if (project.tasks.names.contains("javadocJar")) {
                    project.tasks.named("javadocJar", Jar::class.java)
                } else {
                    null
                }
            registerPublication(project, jar, shadowJar, sourcesJar, javadocJar, publicationAction)
        }

        if (withDeploy) {
            val javaDeploy = InternalJavaDeploy(objects)
            deployAction.execute(javaDeploy)
            javaDeploy.apply(project)
        }
    }

    private fun generateSources(
        project: Project,
        module: String,
        encoding: String,
        mainSourceSet: SourceSet,
    ) {
        val generatedSourcesDir =
            project.layout.buildDirectory
                .dir("generated/sources/glass/java/main")
                .get()
                .asFile
        mainSourceSet.java.srcDirs(generatedSourcesDir)
        TemplateFile("Module.java")
            .generateTo(
                File(generatedSourcesDir, module.replace('.', File.separatorChar)),
                Charset.forName(encoding),
                mapOf(
                    "module" to module,
                    "groupId" to project.group,
                    "artifactId" to project.name,
                    "version" to project.version,
                    "version_code" to GitUtil.loadVersionCode(project),
                ),
            )
    }

    @Suppress("UnstableApiUsage", "DuplicatedCode")
    private fun registerSourceSets(
        project: Project,
        sourceSets: SourceSetContainer,
        configurations: ConfigurationContainer,
        testingExtension: TestingExtension,
        toolchains: JavaToolchainService,
        encoding: String,
        mainRelease: JavaRelease,
        mainSourceSet: SourceSet,
        testSourceSet: SourceSet,
        multiReleases: Map<Int, Action<JavaRelease>>,
    ): Map<Int, SourceSet> {
        val suites = testingExtension.suites
        val multiSourceSets = linkedMapOf<Int, SourceSet>()
        var lastMainSourceSet: SourceSet = mainSourceSet
        var lastTestSuite = suites.named("test", JvmTestSuite::class.java)
        val check = project.tasks.named("check")
        for ((multiVersion, multiReleaseAction) in multiReleases) {
            val javaRelease = InternalJavaRelease(project.objects, multiVersion)
            multiReleaseAction.execute(javaRelease)
            javaRelease.validate()
            val finalLastMainSourceSet = lastMainSourceSet
            val finalLastTestSuite = lastTestSuite
            val sourceSet =
                sourceSets.create("main$multiVersion") {
                    val output = finalLastMainSourceSet.output
                    compileClasspath += output
                    runtimeClasspath += output
                    configurations.named(compileClasspathConfigurationName) {
                        extendsFrom(configurations.getByName(finalLastMainSourceSet.compileClasspathConfigurationName))
                    }
                    configurations.named(runtimeClasspathConfigurationName) {
                        extendsFrom(configurations.getByName(finalLastMainSourceSet.runtimeClasspathConfigurationName))
                    }
                    configurations.named(annotationProcessorConfigurationName) {
                        extendsFrom(configurations.getByName(finalLastMainSourceSet.annotationProcessorConfigurationName))
                    }
                }
            val javaCompiler = javaRelease.compilerFrom(toolchains)
            project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
                group = TASK_GROUP
                shouldRunAfter(finalLastTestSuite)
                this.javaCompiler.set(javaCompiler)
                options.encoding = encoding
                options.release.set(multiVersion)
                options.compilerArgs.add("-parameters")
            }
            val testSuite =
                suites.register("test$multiVersion", JvmTestSuite::class.java) {
                    sources {
                        val output = sourceSet.output
                        compileClasspath += output
                        runtimeClasspath += output
                        val finalLastTestSourceSet = finalLastTestSuite.get().sources
                        configurations.named(compileClasspathConfigurationName) {
                            extendsFrom(configurations.getByName(finalLastTestSourceSet.compileClasspathConfigurationName))
                        }
                        configurations.named(runtimeClasspathConfigurationName) {
                            extendsFrom(configurations.getByName(finalLastTestSourceSet.runtimeClasspathConfigurationName))
                        }
                        configurations.named(annotationProcessorConfigurationName) {
                            extendsFrom(configurations.getByName(finalLastTestSourceSet.annotationProcessorConfigurationName))
                        }
                        project.tasks.named(compileJavaTaskName, JavaCompile::class.java) {
                            group = TASK_GROUP
                            this.javaCompiler.set(javaCompiler)
                            options.encoding = encoding
                            options.release.set(multiVersion)
                            options.compilerArgs.add("-parameters")
                        }
                    }
                    targets.all {
                        testTask.configure {
                            group = TASK_GROUP
                            shouldRunAfter(finalLastTestSuite)
                        }
                    }
                }
            check.configure {
                dependsOn(testSuite)
            }
            multiSourceSets[multiVersion] = sourceSet
            lastMainSourceSet = sourceSet
            lastTestSuite = testSuite
        }

        val javaCompiler = mainRelease.compilerFrom(toolchains)
        val mainVersion = mainRelease.version
        project.tasks.named(mainSourceSet.compileJavaTaskName, JavaCompile::class.java) {
            group = TASK_GROUP
            options.encoding = encoding
            this.javaCompiler.set(javaCompiler)
            if (mainVersion >= JavaUtils.LEAST_MULTI_RELEASE_SUPPORTED_VERSION) {
                options.release.set(mainVersion)
            }
            options.compilerArgs.add("-parameters")
        }
        project.tasks.named(testSourceSet.compileJavaTaskName, JavaCompile::class.java) {
            group = TASK_GROUP
            options.encoding = encoding
            this.javaCompiler.set(javaCompiler)
            if (mainVersion >= JavaUtils.LEAST_MULTI_RELEASE_SUPPORTED_VERSION) {
                options.release.set(mainVersion)
            }
            options.compilerArgs.add("-parameters")
        }

        lastMainSourceSet = mainSourceSet
        for ((_, sourceSet) in multiSourceSets) {
            val lastClasses = project.tasks.named(lastMainSourceSet.classesTaskName)
            project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
                dependsOn(lastClasses)
            }

            project.tasks.named(sourceSet.classesTaskName) {
                group = TASK_GROUP
            }

            project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources::class.java) {
                group = TASK_GROUP
                includeEmptyDirs = false
            }
        }
        return multiSourceSets
    }

    private fun registerPublication(
        project: Project,
        jar: TaskProvider<Jar>,
        shadowJar: TaskProvider<Jar>?,
        sourcesJar: TaskProvider<Jar>?,
        javadocJar: TaskProvider<Jar>?,
        publicationAction: Action<JavaPublication>,
    ) {
        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
        val signingExtension = project.extensions.getByType(SigningExtension::class.java)
        val publication =
            publishingExtension.publications.create("java", MavenPublication::class.java) {
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
                if (sourcesJar != null) {
                    artifact(sourcesJar)
                }
                if (javadocJar != null) {
                    artifact(javadocJar)
                }
                if (shadowJar != null) {
                    artifact(shadowJar)
                } else {
                    artifact(jar)
                }
            }
        signingExtension.apply {
            useGpgCmd()
        }

        val javaPublication = InternalJavaPublication(project.objects)
        publicationAction.execute(javaPublication)
        javaPublication.apply(publication, signingExtension)

        publication.pom {
            name.set(project.name)
            withXml {
                val xmlNode = asNode()
                val childrenIterator = xmlNode.children().iterator()
                while (childrenIterator.hasNext()) {
                    val child = childrenIterator.next()
                    if (child is Node) {
                        if (child.name() == "dependencies") {
                            childrenIterator.remove()
                        }
                    }
                }
                val dependencies = ScopedDependencyInformation.Companion.resolveDependenciesInformation(project)
                if (dependencies.isNotEmpty()) {
                    val dependenciesNode = xmlNode.appendNode("dependencies")
                    dependencies.forEach { information ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", information.groupId)
                        dependencyNode.appendNode("artifactId", information.artifactId)
                        dependencyNode.appendNode("version", information.version)
                        if (information.scope != "") {
                            dependencyNode.appendNode("scope", information.scope)
                        }
                    }
                }
            }
        }

        val pom =
            project.tasks.register("pom", Pom::class.java, publication.name).apply {
                configure {
                    group = TASK_GROUP
                }
            }
        jar.configure {
            dependsOn(pom)
            from(pom) {
                into("META-INF/maven/${project.group}/${project.name}/")
            }
        }
    }
}
