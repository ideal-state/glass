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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.file.CopySpec
import org.gradle.api.provider.Property
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.kotlin.dsl.dependenciesInformation
import org.gradle.language.jvm.tasks.ProcessResources
import team.idealstate.glass.context.util.Extensions
import team.idealstate.glass.context.util.PathUtils
import team.idealstate.glass.context.util.Plugins
import team.idealstate.glass.data.dependency.ScopedDependencyInformation
import team.idealstate.glass.plugin.java.data.JavaApplication
import team.idealstate.glass.plugin.java.data.JavaReleaseProperty
import team.idealstate.glass.plugin.java.task.CopyrightTask
import team.idealstate.glass.plugin.java.task.MavenPomTask
import team.idealstate.glass.plugin.java.task.RelocateTask
import team.idealstate.glass.plugin.java.task.SourcesTask
import team.idealstate.glass.plugin.java.task.UnzipInternalDependenciesTask
import team.idealstate.glass.plugin.java.task.UnzipShadowDependenciesTask

open class GlassJavaExtension(
    private val project: Project,
) {
    companion object {
        const val NAME = "glass"
        const val JUNIT_TEST_DEFAULT_VERSION = "5.11.3"
        const val FEATURE_WITH_COPYRIGHT = "withCopyright"
        const val FEATURE_WITH_DEPENDENCIES_INFORMATION = "withDependenciesInformation"
        const val FEATURE_WITH_INTERNAL = "withInternal"
        const val FEATURE_WITH_SHADOW = "withShadow"
        const val FEATURE_WITH_SOURCES_JAR = "withSourcesJar"
        const val FEATURE_WITH_JAVADOC_JAR = "withJavadocJar"
        const val FEATURE_WITH_JUNIT_TEST = "withJUnitTest"
        const val FEATURE_APPLICATION = "application"
        const val FEATURE_MULTI_RELEASE = "multiRelease"

        @JvmStatic
        fun register(project: Project): GlassJavaExtension = project.extensions.create(NAME, GlassJavaExtension::class.java, project)

        @JvmStatic
        fun of(project: Project): GlassJavaExtension = project.extensions.getByName(NAME) as GlassJavaExtension

        @JvmStatic
        fun dependenciesInformation(project: Project): List<ScopedDependencyInformation> {
            val dependenciesInformation = linkedMapOf<String, ScopedDependencyInformation>()
            var scope = "runtime"
            val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
            for (information in runtimeClasspath.dependenciesInformation) {
                dependenciesInformation[information.id] =
                    ScopedDependencyInformation(information.group, information.name, information.version, scope)
            }
            scope = "compile"
            val compileClasspath = project.configurations.getByName("compileClasspath")
            for (information in compileClasspath.dependenciesInformation) {
                val id = information.id
                if (dependenciesInformation.containsKey(id)) {
                    dependenciesInformation[information.id] =
                        ScopedDependencyInformation(information.group, information.name, information.version, scope)
                }
            }
            return dependenciesInformation.values.toList()
        }
    }

    private val enabledFeatures = mutableMapOf<String, Boolean>()

    private fun enable(feature: String): Boolean {
        val applied = enabledFeatures[feature] ?: false
        if (!applied) {
            enabledFeatures[feature] = true
        }
        return applied
    }

    private fun mustBefore(
        feature: String,
        vararg others: String,
    ) {
        others.forEach { other ->
            if (enabledFeatures[other] == true) {
                throw IllegalStateException("Feature $feature must be enabled before $other.")
            }
        }
    }

    val module: Property<String> =
        project.objects.property(String::class.java).apply {
            set(project.provider { project.group.toString() })
        }

    val release: Property<Int> = JavaReleaseProperty(project, project.objects.property(Int::class.java))

    val application: Property<JavaApplication> = project.objects.property(JavaApplication::class.java)

    fun application(action: Action<in JavaApplication>) {
        if (enable(FEATURE_APPLICATION)) return
        var exists = false
        var application = this.application.orNull
        if (application != null) {
            exists = true
        } else {
            application = JavaApplication(project)
        }
        action.execute(application)
        if (!exists) {
            this.application.set(application)
        }
    }

    fun withCopyright() {
        if (enable(FEATURE_WITH_COPYRIGHT)) return
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

    fun withMavenPom() {
        if (enable(FEATURE_WITH_DEPENDENCIES_INFORMATION)) return
        if (!project.pluginManager.hasPlugin(Plugins.glass(Plugins.publishing))) {
            throw IllegalStateException("plugin glass(${Plugins.publishing}) is required.")
        }
        val tasks = project.tasks
        val mavenPomTask = MavenPomTask.register(project)
        tasks.named("processResources", ProcessResources::class.java) {
            it.dependsOn(mavenPomTask)
            it.from(mavenPomTask) { copy ->
                copy.into("META-INF/maven/${project.group}/${project.name}/")
            }
        }
        addSources(mavenPomTask) { copy ->
            copy.into("META-INF/maven/${project.group}/${project.name}/")
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
        if (enable(FEATURE_WITH_SOURCES_JAR)) return
        project.tasks.named("assemble") {
            it.dependsOn(ConfigureJava.SOURCES_JAR_TASK_NAME)
        }
    }

    fun withJavadocJar() {
        if (enable(FEATURE_WITH_JAVADOC_JAR)) return
        project.tasks.named("assemble") {
            it.dependsOn(ConfigureJava.JAVADOC_JAR_TASK_NAME)
        }
    }

    fun withInternal(
        module: String? = null,
        shadow: Boolean = true,
    ) {
        if (enable(FEATURE_WITH_INTERNAL)) {
            module?.let {
                this.module.set(it)
            }
            return
        }
        mustBefore(FEATURE_WITH_INTERNAL, FEATURE_WITH_SHADOW, FEATURE_MULTI_RELEASE)
        module?.let {
            this.module.set(it)
        }
        val sourceSets = Extensions.sourceSets(project)
        val mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()
        val testSourceSet = sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME).get()
        val configurations = project.configurations
        val internal = configurations.named(ConfigureJava.CONFIGURATION_INTERNAL_NAME).get()
        for (sourceSet in arrayOf(mainSourceSet, testSourceSet)) {
            if (shadow) {
                sourceSet.compileClasspath += internal
                sourceSet.runtimeClasspath += internal
            } else {
                sourceSet.compileClasspath += internal
            }
        }
        val unzipInternalDependenciesTask = UnzipInternalDependenciesTask.register(project)
        val relocateTask =
            RelocateTask.register(project) {
                it.dependsOn(unzipInternalDependenciesTask)
                it.source(mainSourceSet)
            }
        project.tasks.named(ConfigureJava.JAR_TASK_NAME, Jar::class.java) {
            it.dependsOn(unzipInternalDependenciesTask, relocateTask)
            it.from(unzipInternalDependenciesTask) { copy ->
                copy.includeEmptyDirs = false
            }
            it.from(relocateTask)

            it.eachFile { details ->
                val relocateResult = relocateTask.get().getRelocateResult(details.file)
                if (relocateResult != null) {
                    if (relocateResult.exclude) {
                        details.exclude()
                    } else {
                        details.path = relocateResult.path
                    }
                }
            }
        }
    }

    fun withShadow() {
        if (enable(FEATURE_WITH_SHADOW)) return
        mustBefore(FEATURE_WITH_SHADOW, FEATURE_MULTI_RELEASE)
        val sourceSets = Extensions.sourceSets(project)
        val mainSourceSet = sourceSets.named(SourceSet.MAIN_SOURCE_SET_NAME).get()
        val testSourceSet = sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME).get()
        val configurations = project.configurations
        val shadow = configurations.named(ConfigureJava.CONFIGURATION_SHADOW_NAME).get()
        for (sourceSet in arrayOf(mainSourceSet, testSourceSet)) {
            sourceSet.compileClasspath += shadow
            sourceSet.runtimeClasspath += shadow
        }
        val unzipShadowDependenciesTask = UnzipShadowDependenciesTask.register(project)
        project.tasks.named(ConfigureJava.JAR_TASK_NAME, Jar::class.java) {
            it.dependsOn(unzipShadowDependenciesTask)
            it.from(unzipShadowDependenciesTask) { copy ->
                copy.includeEmptyDirs = false
                copy.eachFile { each ->
                    var path = PathUtils.normalize(each.path)
                    if (path[0] == PathUtils.NORMAL_DELIMITER) {
                        path = path.substring(1)
                    }
                    var i = path.indexOf(PathUtils.NORMAL_DELIMITER)
                    if (i < 0 || i == path.length - 1) {
                        each.exclude()
                        return@eachFile
                    }
                    i = path.indexOf(PathUtils.NORMAL_DELIMITER, i + 1)
                    if (i < 0) {
                        each.exclude()
                        return@eachFile
                    }
                    each.path = path.substring(i + 1)
                }
            }
        }
    }

    fun withJUnitTest(
        version: String = JUNIT_TEST_DEFAULT_VERSION,
        action: Action<in JUnitPlatformOptions> = Action {},
    ) {
        if (enable(FEATURE_WITH_JUNIT_TEST)) return
        val sourceSets = Extensions.sourceSets(project)
        val testSourceSet = sourceSets.named(SourceSet.TEST_SOURCE_SET_NAME).get()
        val testImplementation = testSourceSet.implementationConfigurationName
        val testRuntimeOnly = testSourceSet.runtimeOnlyConfigurationName
        project.dependencies.apply {
            add(testImplementation, "org.junit.jupiter:junit-jupiter:$version")
            add(testRuntimeOnly, "org.junit.platform:junit-platform-launcher")
        }
        project.tasks.named("test", Test::class.java) {
            it.useJUnitPlatform(action)
        }
    }

//    fun multiRelease(action: Action<MultiRelease>) {
//        if (enable(FEATURE_MULTI_RELEASE)) return
//        val multiRelease = MultiRelease(project)
//        action.execute(multiRelease)
//
//        val tasks = project.tasks
//        val multiReleaseClassesTask = MultiReleaseClassesTask.register(project)
//        val sourceSets = Extensions.sourceSets(project)
//        val releaseContainer = multiRelease.java
//        val releases =
//            releaseContainer.all.apply {
//                removeFirst()
//            }
//        val mainRelease = releaseContainer.main.get()
//        var last: Triple<SourceSet, MutableList<JavaSourceFile>, TaskProvider<CollectMultiReleaseClassesTask>?> =
//            processMultiReleaseSourceSet(tasks, sourceSets, mainRelease, null)
//        for (release in releases) {
//            last = processMultiReleaseSourceSet(tasks, sourceSets, release, last)
//        }
//
//        RelocateTask.of(project) {
//            it.dependsOn(multiReleaseClassesTask)
//            it.source(
//                project.provider {
//                    project.objects
//                        .directoryProperty()
//                        .apply {
//                            set(multiReleaseClassesTask.get().destinationDir)
//                        }.get()
//                },
//            )
//        }
//
//        tasks.named(ConfigureJava.JAR_TASK_NAME, Jar::class.java) {
//            it.dependsOn(multiReleaseClassesTask)
//            it.from(multiReleaseClassesTask)
//            it.manifest.attributes(mapOf("Multi-Release" to true))
//        }
//    }
//
//    private fun processMultiReleaseSourceSet(
//        tasks: TaskContainer,
//        sourceSets: SourceSetContainer,
//        release: JavaRelease,
//        last: Triple<SourceSet, MutableList<JavaSourceFile>, TaskProvider<CollectMultiReleaseClassesTask>?>?,
//    ): Triple<SourceSet, MutableList<JavaSourceFile>, TaskProvider<CollectMultiReleaseClassesTask>?> {
//        val name = release.name
//        if (release.isReserved()) {
//            val sourceSet = sourceSets.named(name).get()
//            tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
//                configureMultiReleaseCompileJavaTask(release, it)
//            }
//            return Triple(sourceSet, collectMultiReleaseJavaSourceFiles(sourceSet, LinkedList()), null)
//        }
//        last ?: error("The first release must be a reserved release.")
//        val (lastSourceSet, lastJavaSourceFiles, multiReleaseClassesSourceTask) = last
//        val sourceSet =
//            sourceSets
//                .register(name) {
//                    it.compileClasspath += lastSourceSet.compileClasspath + lastSourceSet.output
//                    it.runtimeClasspath += lastSourceSet.runtimeClasspath
//                    it.annotationProcessorPath += lastSourceSet.annotationProcessorPath
//                }.get()
//
//        val location = release.location
//        tasks.named(SourcesTask.NAME, SourcesTask::class.java) {
//            it.sourceSet(name) { copy ->
//                copy.into(location)
//            }
//        }
//        val javaSourceFiles = collectMultiReleaseJavaSourceFiles(sourceSet, lastJavaSourceFiles)
//        val lastJavaSources = lastJavaSourceFiles.map(JavaSourceFile::file)
//        lastJavaSourceFiles.addAll(javaSourceFiles)
//        tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
//            multiReleaseClassesSourceTask?.also { that ->
//                it.dependsOn(that)
//            }
//            it.source(lastJavaSources)
//            configureMultiReleaseCompileJavaTask(release, it)
//        }
//
//        val collectMultiReleaseClassesTask = CollectMultiReleaseClassesTask.register(project, release)
//
//        MultiReleaseClassesTask.of(project) {
//            it.source(release)
//        }
//
//        return Triple(sourceSet, lastJavaSourceFiles, collectMultiReleaseClassesTask)
//    }
//
//    private fun collectMultiReleaseJavaSourceFiles(
//        sourceSet: SourceSet,
//        lastJavaSourceFiles: MutableList<JavaSourceFile>,
//    ): MutableList<JavaSourceFile> {
//        val javaSourceFiles = LinkedList<JavaSourceFile>()
//        for (baseDir in sourceSet.java.srcDirs) {
//            baseDir.exists() || baseDir.isFile || continue
//            val files = project.fileTree(baseDir).files
//            for (file in files) {
//                val javaFile = JavaFile.of(baseDir, file)
//                if (javaFile is JavaSourceFile) {
//                    if (lastJavaSourceFiles.isNotEmpty()) {
//                        val location = javaFile.location
//                        val iterator = lastJavaSourceFiles.iterator()
//                        while (iterator.hasNext()) {
//                            val lastJavaSourceFile = iterator.next()
//                            if (lastJavaSourceFile.location == location) {
//                                iterator.remove()
//                            }
//                        }
//                    }
//                    javaSourceFiles.add(javaFile)
//                }
//            }
//        }
//        return javaSourceFiles
//    }
//
//    private fun configureMultiReleaseCompileJavaTask(
//        release: JavaRelease,
//        task: JavaCompile,
//    ) {
//        val version = release.javaLanguageVersion
//        val options = task.options
//        options.encoding = Charset.defaultCharset().name()
//        if (version.canCompileOrRun(JavaRelease.LEAST_MULTI_RELEASE_VERSION)) {
//            options.compilerArgs.addAll(
//                listOf(
//                    "--module-path",
//                    task.classpath.asPath,
//                ),
//            )
//        }
//        release.apply(options)
//        options.release.set(version.asInt())
//    }
}
