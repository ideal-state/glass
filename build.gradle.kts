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

    implementation("com.diffplug.spotless:spotless-plugin-gradle:7.0.0.BETA4")
    implementation("com.gradleup.shadow:shadow-gradle-plugin:9.0.0-beta2")

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

val destCopyrightRootDir = "$projectDir/build/copyright/"
val destCopyrightDir = "${destCopyrightRootDir}META-INF/COPYRIGHT/${project.group.toString().replace('.', '/')}/${project.name}/"

val copyCopyright by tasks.registering(Copy::class) {
    from("$projectDir/LICENSE.txt", "$projectDir/NOTICE.txt")
    into(destCopyrightDir)
}

val copyDependencyCopyright by tasks.registering(Copy::class) {
    from("$projectDir/LICENSES/")
    into("${destCopyrightDir}LICENSES/")
}

tasks.processResources {
    dependsOn(copyCopyright, copyDependencyCopyright)
    from(destCopyrightRootDir)
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

    java {
        target("src/*/java/**/*.java")

        endWithNewline()

        googleJavaFormat().aosp().reflowLongStrings(true).formatJavadoc(true)

        formatAnnotations()

        applyLicenseHeader(this)
    }

    kotlin {
        target("src/*/kotlin/**/*.kt", "src/*/kotlin/**/*.kts")

        endWithNewline()

        ktlint()

        applyLicenseHeader(this)
    }

    sql {
        target("src/*/resources/**/*.sql")

        dbeaver()
    }

    json {
        target("src/*/resources/**/*.json")

        jackson()
    }

    yaml {
        target("src/*/resources/**/*.yml", "src/*/resources/**/*.yaml")

        jackson()
    }
}
