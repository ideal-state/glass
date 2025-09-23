package team.idealstate.glass.plugin.project.java.extension

import groovy.util.Node
import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.javadoc.Javadoc
import org.gradle.external.javadoc.StandardJavadocDocletOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JavaToolchainService
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.language.jvm.tasks.ProcessResources
import org.gradle.plugins.signing.SigningExtension
import team.idealstate.glass.plugin.project.java.data.InternalJavaManifest
import team.idealstate.glass.plugin.project.java.data.InternalJavaPublication
import team.idealstate.glass.plugin.project.java.data.JavaManifest
import team.idealstate.glass.plugin.project.java.data.JavaPublication
import team.idealstate.glass.plugin.project.java.task.ClassesTask
import team.idealstate.glass.plugin.project.java.task.SourcesTask
import java.nio.charset.Charset
import kotlin.io.path.Path

internal open class InternalJavaExtension(objects: ObjectFactory) : JavaExtension {

    private val mainReleaseVersion = objects.property(Int::class.java).apply { finalizeValueOnRead() }

    private val toolchainVersion = objects.property(Int::class.java).apply { finalizeValueOnRead() }

    override fun withJava(version: Int, toolchainVersion: Int) {
        mainReleaseVersion.set(version)
        this.toolchainVersion.set(toolchainVersion)
    }

    private val withManifest = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun withManifest(action: Action<JavaManifest>) {
        withManifest.set(action)
    }

    private val otherReleaseVersions = objects.listProperty(Int::class.java).apply { finalizeValueOnRead() }

    override fun withMultiRelease(version: Int) {
        otherReleaseVersions.add(version)
    }

    private val withSourcesJar = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }

    override fun withSourcesJar() {
        withSourcesJar.set(true)
    }

    private val withJavadocJar = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }

    override fun withJavadocJar() {
        withJavadocJar.set(true)
    }

    private val withPublication = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun withPublication(action: Action<JavaPublication>) {
        withPublication.set(action)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(project: Project) {
        val mainReleaseVersion = mainReleaseVersion.get()
        val toolchainVersion = toolchainVersion.get()
        if (mainReleaseVersion > toolchainVersion) {
            throw IllegalStateException("Main release version must be less than or equal to toolchain version.")
        }

        val withManifest = withManifest.orNull as Action<JavaManifest>?

        val otherReleaseVersions = LinkedHashSet(otherReleaseVersions.get().sortedWith(Int::compareTo))
        for (otherReleaseVersion in otherReleaseVersions) {
            if (otherReleaseVersion <= mainReleaseVersion) {
                throw IllegalStateException("Other release version $otherReleaseVersion must be greater than main release version $mainReleaseVersion.")
            }
            if (otherReleaseVersion > toolchainVersion) {
                throw IllegalStateException("Other release version $otherReleaseVersion must be less than or equal to toolchain version $mainReleaseVersion.")
            }
            if (otherReleaseVersion < 9) {
                throw IllegalArgumentException("Other release version $otherReleaseVersion must be greater than or equal to 9.")
            }
        }

        val withSourcesJar = withSourcesJar.orNull == true
        val withJavadocJar = withJavadocJar.orNull == true
        val withPublication = withPublication.orNull as Action<JavaPublication>?

        // --------------------------------------------------------------------------------------------------

        val javaPluginExtension = project.extensions.getByType(JavaPluginExtension::class.java)
        javaPluginExtension.apply {
            toolchain {
                it.languageVersion.set(JavaLanguageVersion.of(toolchainVersion))
                it.vendor.set(JvmVendorSpec.AZUL)
            }
        }

        val objects = project.objects
        val javaManifest = InternalJavaManifest(objects)
        withManifest?.execute(javaManifest)
        if (!otherReleaseVersions.isEmpty()) {
            javaManifest.add {
                it.attributes["Multi-Release"] = true
            }
        }

        val encoding = Charset.defaultCharset().name()
        val sourceSets = project.extensions.getByType(SourceSetContainer::class.java)
        val mainSourceSet = sourceSets.getByName("main")
        val testSourceSet = sourceSets.getByName("test")
        val otherSourceSets = linkedMapOf<Int, SourceSet>()
        val classes = registerSourceSets(
            project,
            sourceSets,
            encoding,
            mainReleaseVersion,
            mainSourceSet,
            testSourceSet,
            otherSourceSets,
            otherReleaseVersions
        )

        registerJar(project, classes, javaManifest)

        if (withSourcesJar) {
            registerSourcesJar(project, mainSourceSet, otherSourceSets)
        }

        if (withJavadocJar) {
            val doclet = project.configurations.create("doclet")
            registerJavadocJar(project, doclet, encoding)
        }

        registerPublication(project, withSourcesJar, withJavadocJar, withPublication)
    }

    private fun registerSourceSets(
        project: Project,
        sourceSets: SourceSetContainer,
        encoding: String,
        mainReleaseVersion: Int,
        mainSourceSet: SourceSet,
        testSourceSet: SourceSet,
        otherSourceSets: MutableMap<Int, SourceSet>,
        otherReleaseVersions: Set<Int>
    ): List<NamedDomainObjectProvider<ClassesTask>> {
        var lastReleaseVersion: Int = mainReleaseVersion
        var lastSourceSet: SourceSet = mainSourceSet
        for (otherReleaseVersion in otherReleaseVersions) {
            val sourceSet = sourceSets.create("other$otherReleaseVersion") {
                it.compileClasspath += lastSourceSet.compileClasspath
                it.runtimeClasspath += lastSourceSet.runtimeClasspath
            }
            project.tasks.named(sourceSet.compileJavaTaskName, JavaCompile::class.java) {
                it.options.encoding = encoding
                it.options.release.set(otherReleaseVersion)
            }
            project.tasks.named(sourceSet.processResourcesTaskName, ProcessResources::class.java) {
                it.includeEmptyDirs = false
                it.eachFile { each ->
                    each.path = Path("META-INF/versions/$otherReleaseVersion", each.path).normalize().toString()
                }
            }
            otherSourceSets[otherReleaseVersion] = sourceSet
            lastReleaseVersion = otherReleaseVersion
            lastSourceSet = sourceSet
        }
        project.tasks.named(mainSourceSet.compileJavaTaskName, JavaCompile::class.java) {
            it.options.encoding = encoding
            it.options.release.set(mainReleaseVersion)
        }
        if (lastReleaseVersion == mainReleaseVersion) {
            project.tasks.named(testSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                it.options.encoding = encoding
                it.options.release.set(mainReleaseVersion)
            }
        } else {
            testSourceSet.compileClasspath += lastSourceSet.compileClasspath
            testSourceSet.runtimeClasspath += lastSourceSet.runtimeClasspath
            project.tasks.named(testSourceSet.compileJavaTaskName, JavaCompile::class.java) {
                it.options.encoding = encoding
                it.options.release.set(lastReleaseVersion)
            }
        }

        val mainClasses = project.tasks.named("classes", DefaultTask::class.java) {
            it.group = "glass"
            for (entry in otherSourceSets) {
                it.finalizedBy(project.tasks.named("classes${entry.value.name}", ClassesTask::class.java))
            }
        }
        val classes = mutableListOf<NamedDomainObjectProvider<ClassesTask>>()
        for (entry in otherSourceSets) {
            val sourceSet = entry.value
            val otherReleaseVersion = entry.key
            val otherClasses = project.tasks.register("classes${sourceSet.name}", ClassesTask::class.java) {
                it.group = "glass"
                it.sourceSet.set(sourceSet)
                it.includeEmptyDirs = false
                it.eachFile { each ->
                    each.path = Path("META-INF/versions/$otherReleaseVersion", each.path).normalize().toString()
                }
            }
            classes.add(otherClasses)
            mainClasses.configure { it.finalizedBy(otherClasses) }
        }
        return classes
    }

    private fun registerJar(
        project: Project,
        classes: List<NamedDomainObjectProvider<ClassesTask>>,
        javaManifest: InternalJavaManifest
    ) {
        project.tasks.named("jar", Jar::class.java) {
            it.group = "glass"
            it.dependsOn(classes.toTypedArray())
            javaManifest.apply(it.manifest)
        }
    }

    private fun registerSourcesJar(project: Project, mainSourceSet: SourceSet, otherSourceSets: Map<Int, SourceSet>) {
        val sources = mutableListOf<NamedDomainObjectProvider<SourcesTask>>()
        sources.add(project.tasks.register("sources", SourcesTask::class.java) {
            it.group = "glass"
            it.sourceSet.set(mainSourceSet)
        })
        for (entry in otherSourceSets) {
            val sourceSet = entry.value
            val otherReleaseVersion = entry.key
            sources.add(project.tasks.register("sources${sourceSet.name}", SourcesTask::class.java) {
                it.group = "glass"
                it.sourceSet.set(sourceSet)
                it.includeEmptyDirs = false
                it.eachFile { each ->
                    each.path = Path("META-INF/versions/$otherReleaseVersion", each.path).normalize().toString()
                }
            })
        }
        project.tasks.register("sourcesJar", Jar::class.java) {
            it.group = "glass"
            it.archiveClassifier.set("sources")
            it.from(sources.toTypedArray())
            it.destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        }
    }

    private fun registerJavadocJar(project: Project, doclet: Configuration, encoding: String) {
        project.tasks.register("javadocJar", Jar::class.java) {
            it.group = "glass"
            it.archiveClassifier.set("javadoc")
            it.from(project.tasks.named("javadoc", Javadoc::class.java))
            it.destinationDirectory.set(project.layout.buildDirectory.dir("libs"))
        }
        project.tasks.named("javadoc", Javadoc::class.java) {
            val javaToolchains = project.extensions.getByType(JavaToolchainService::class.java)
            it.javadocTool.set(
                javaToolchains.javadocToolFor { tool ->
                    tool.languageVersion.set(JavaLanguageVersion.of(17))
                    tool.vendor.set(JvmVendorSpec.AZUL)
                },
            )
            it.isFailOnError = false
            it.options { options ->
                val docletFiles = doclet.allArtifacts.files.files
                if (docletFiles.isEmpty()) {
                    if (options is StandardJavadocDocletOptions) {
                        options.charSet(encoding)
                        options.docEncoding(encoding)
                        options.author(true)
                        options.version(true)
                        options.addBooleanOption("Xdoclint:none", true)
                    }
                } else {
                    options.docletpath.addAll(docletFiles)
                }
                options.encoding(encoding)
                options.jFlags("-Dfile.encoding=$encoding")
            }
        }
    }

    private fun registerPublication(
        project: Project,
        withSourcesJar: Boolean,
        withJavadocJar: Boolean,
        withPublication: Action<JavaPublication>?
    ) {
        val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
        val signingExtension = project.extensions.getByType(SigningExtension::class.java)
        val publication = publishingExtension.run {
            publications.create("java", MavenPublication::class.java) {
                it.groupId = project.group.toString()
                it.artifactId = project.name
                it.version = project.version.toString()
                if (withSourcesJar) {
                    it.artifact(project.tasks.named("sourcesJar"))
                }
                if (withJavadocJar) {
                    it.artifact(project.tasks.named("javadocJar"))
                }
                it.artifact(project.tasks.named("Jar"))
            }
        }
        signingExtension.apply {
            useGpgCmd()
        }

        val javaPublication = InternalJavaPublication(project.objects)
        withPublication?.execute(javaPublication)
        javaPublication.apply(publication, signingExtension)

        publication.pom { pom ->
            pom.name.set(project.name)
            pom.withXml { xml ->
                val xmlNode = xml.asNode()
                val childrenIterator = xmlNode.children().iterator()
                while (childrenIterator.hasNext()) {
                    val child = childrenIterator.next()
                    if (child is Node) {
                        if (child.name() == "dependencies") {
                            childrenIterator.remove()
                        }
                    }
                }
                val dependencies = dependenciesInformation(project)
                if (dependencies.isNotEmpty()) {
                    val dependenciesNode = xmlNode.appendNode("dependencies")
                    dependencies.forEach { information ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", information.group)
                        dependencyNode.appendNode("artifactId", information.name)
                        dependencyNode.appendNode("version", information.version)
                        if (information.scope != "") {
                            dependencyNode.appendNode("scope", information.scope)
                        }
                    }
                }
            }
        }
    }

    private fun dependenciesInformation(configuration: Configuration): Set<DependencyInformation> {
        val dependencies = configuration.resolvedConfiguration.firstLevelModuleDependencies
        val ret = LinkedHashSet<DependencyInformation>(dependencies.size)
        for (dependency in dependencies) {
            ret.add(
                ScopedDependencyInformation(
                    dependency.moduleGroup,
                    dependency.moduleName,
                    dependency.moduleVersion
                )
            )
        }
        return ret
    }

    private fun dependenciesInformation(project: Project): List<ScopedDependencyInformation> {
        val dependenciesInformation = linkedMapOf<String, ScopedDependencyInformation>()
        var scope = "runtime"
        val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
        for (information in dependenciesInformation(runtimeClasspath)) {
            dependenciesInformation[information.id] =
                ScopedDependencyInformation(information.group, information.name, information.version, scope)
        }
        scope = "compile"
        val compileClasspath = project.configurations.getByName("compileClasspath")
        for (information in dependenciesInformation(compileClasspath)) {
            val id = information.id
            if (dependenciesInformation.containsKey(id)) {
                dependenciesInformation[information.id] =
                    ScopedDependencyInformation(information.group, information.name, information.version, scope)
            }
        }
        return dependenciesInformation.values.toList()
    }

    interface DependencyInformation {
        val id: String
            get() = "$group:$name:$version"
        val group: String
        val name: String
        val version: String
    }

    data class ScopedDependencyInformation(
        override val group: String,
        override val name: String,
        override val version: String,
        val scope: String = "",
    ) : DependencyInformation
}