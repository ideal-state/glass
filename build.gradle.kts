import com.diffplug.spotless.LineEnding

plugins {
    `kotlin-dsl`
    signing
    alias(libs.plugins.dokka)
    alias(libs.plugins.spotless)
    alias(libs.plugins.plugin.publish)
}

group = "team.idealstate.glass"
version = "0.2.0"

kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
        vendor.set(JvmVendorSpec.AZUL)
    }
}

configurations {
    api {
        dependencies.remove(project.dependencies.gradleApi())
    }
}

dependencies {
    compileOnly(gradleApi())

    implementation(libs.asm)
    implementation(libs.jackson.databind)
    implementation(libs.jackson.module.kotlin)

    implementation(libs.foojay.resolver)
    implementation(libs.spotless)
    implementation(libs.jreleaser)
}

spotless {
    encoding(Charsets.UTF_8)
    lineEndings = LineEnding.GIT_ATTRIBUTES_FAST_ALLSAME

    kotlinGradle {
        target("*.gradle.kts")
        ktlint()
        endWithNewline()
    }

    kotlin {
        target("src/*/kotlin/**/*.kt", "src/*/kotlin/**/*.kts")
        rootProject.file("HEADER.txt").run {
            if (exists()) {
                licenseHeaderFile(this)
            }
        }
        ktlint()
        endWithNewline()
    }
}

gradlePlugin {
    website.set("https://github.com/ideal-state/glass/")
    vcsUrl.set("https://github.com/ideal-state/glass")
    plugins {
        register("Glass") {
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

val copyrightBaseDirName = "copyright"
val copyright = tasks.register<Copy>("copyright") {
    group = "documentation"
    description = ""
    val licenseFile = "LICENSE.txt"
    val noticeFile = "NOTICE.txt"
    val licensesDir = "LICENSES/"
    val destinationDirectory =
        project.layout.buildDirectory
            .dir("docs/$copyrightBaseDirName")
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
        into("META-INF/$copyrightBaseDirName")
    }
}

val sourcesJar = tasks.register<Jar>("sourcesJar") {
    group = "build"
    description = ""
    dependsOn(tasks.processResources)
    archiveClassifier.set("sources")
    from(
        project.sourceSets.main
            .get()
            .kotlin,
        tasks.processResources,
    )
}

val javadocJar = tasks.register<Jar>("javadocJar") {
    group = "build"
    description = ""
    archiveClassifier.set("javadoc")
}

tasks.assemble {
    dependsOn(sourcesJar, javadocJar)
}
