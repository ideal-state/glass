import com.diffplug.gradle.spotless.HasBuiltinDelimiterForLicense

plugins {
    embeddedKotlin("jvm")
    id("java-gradle-plugin")
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    id("org.jetbrains.dokka") version "2.0.0-Beta"
    id("com.gradle.plugin-publish") version "1.3.0"
    id("signing")
}

group = "team.idealstate.glass"
version = "0.1.0"

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

    implementation("org.ow2.asm:asm:9.7.1")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.2")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.2")

    implementation("org.gradle.toolchains:foojay-resolver:0.8.0")
    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.0.BETA4")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
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

val encoding = Charsets.UTF_8

gradlePlugin {
    website.set("https://docs.ideal-state.team/glass/")
    vcsUrl.set("https://gitlab.com/ideal-state/glass")
    plugins {
        create("Glass") {
            id = "team.idealstate.glass"
            implementationClass = "team.idealstate.glass.Glass"
            displayName = "Glass"
            description = ""
            @Suppress("UnstableApiUsage")
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

    encoding(encoding)

    groovyGradle {
        target("*.gradle")
        endWithNewline()
        greclipse()
    }

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
