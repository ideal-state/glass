import com.diffplug.gradle.spotless.HasBuiltinDelimiterForLicense
import org.jreleaser.model.Active

plugins {
    id("java-gradle-plugin")
    id("signing")
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.jreleaser)
}

group = "team.idealstate.glass"
version = "0.1.0-SNAPSHOT"

configurations {
    api {
        dependencies.remove(project.dependencies.gradleApi())
    }
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())

    implementation(libs.asm)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.foojay.resolver)
    implementation(libs.spotless)
//    implementation(libs.jreleaser)

    testImplementation(platform(libs.junit.bom))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

gradlePlugin {
    website.set("https://docs.ideal-state.team/glass/")
    vcsUrl.set("https://gitlab.com/ideal-state/glass")
    plugins {
        create("Glass") {
            id = "team.idealstate.glass"
            implementationClass = "team.idealstate.glass.Glass"
            displayName = "Glass"
            description = "Configure Java projects simply and quickly."
            tags.set(listOf("java", "repository", "configuration", "publish", "fatjar", "statistics"))
        }
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

val copyrightRootName = "COPYRIGHT"
val copyright by tasks.registering(Copy::class) {
    group = "documentation"
    val licenseFile = "LICENSE.txt"
    val noticeFile = "NOTICE.txt"
    val licensesDir = "LICENSES/"
    val destinationDirectory =
        project.layout.buildDirectory
            .dir("docs/$copyrightRootName")
            .get()
    into(destinationDirectory.asFile)
    val rootProjectDir = project.rootProject.projectDir
    from("$rootProjectDir/$licenseFile")
    from("$rootProjectDir/$noticeFile")
    from("$rootProjectDir/$licensesDir") {
        into(licensesDir)
    }
}

tasks.processResources {
    dependsOn(copyright)
    from(copyright) {
        into("META-INF/$copyrightRootName/")
    }
}

val sourcesJar by tasks.registering(Jar::class) {
    group = "build"
    dependsOn(tasks.processResources)
    archiveClassifier.set("sources")
    from(
        project.sourceSets.main
            .get()
            .kotlin,
        tasks.processResources,
    )
}

val dokkaGeneratedDir = "$projectDir/build/dokka/"
val copyToJavadoc by tasks.registering(Copy::class) {
    group = "documentation"
    dependsOn(tasks.dokkaGenerate)
    from("${dokkaGeneratedDir}html/")
    into("$projectDir/build/docs/javadoc/")
}

tasks.dokkaGenerate {
    finalizedBy(copyToJavadoc)
}

tasks.logLinkDokkaGeneratePublicationHtml {
    enabled = false
}

val javadocJar by tasks.registering(Jar::class) {
    group = "build"
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
    from("${dokkaGeneratedDir}html/")
}

tasks.assemble {
    dependsOn(sourcesJar, javadocJar)
}

spotless {
    fun applyLicenseHeader(it: HasBuiltinDelimiterForLicense) {
        rootProject.file("HEADER.txt").run {
            if (exists()) {
                it.licenseHeaderFile(this)
            }
        }
    }

    encoding(Charsets.UTF_8)

    kotlinGradle {
        target("*.gradle.kts")
        endWithNewline()
        ktlint()
    }

    kotlin {
        target("src/*/kotlin/**/*.kt", "src/*/kotlin/**/*.kts")
        endWithNewline()
        ktlint()
        applyLicenseHeader(this)
    }
}

publishing {
    repositories {
        maven {
            name = "Project"
            url = uri("file://${project.projectDir}/build/repository")
        }
    }
}

jreleaser {
    deploy {
        maven {
            mavenCentral {
                create("release") {
                    active.set(Active.RELEASE)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    sign.set(false)
                    stagingRepository("build/repository")
                }
            }
            nexus2 {
                create("snapshot") {
                    active.set(Active.SNAPSHOT)
                    url.set("https://central.sonatype.com/repository/maven-snapshots")
                    snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                    sign.set(false)
                    applyMavenCentralRules.set(true)
                    snapshotSupported.set(true)
                    closeRepository.set(true)
                    releaseRepository.set(true)
                    verifyPom.set(false)
                    stagingRepository("build/repository")
                }
            }
        }
    }
}

val doDeploy by tasks.registering {
    dependsOn(tasks.test)
    dependsOn(tasks.named("publishAllPublicationsToProjectRepository"))
    finalizedBy(tasks.jreleaserDeploy)
}

val deploy by tasks.registering {
    group = "glass"
    dependsOn(tasks.clean)
    dependsOn(tasks.spotlessApply)
    finalizedBy(doDeploy)
}
