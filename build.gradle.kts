import com.diffplug.gradle.spotless.HasBuiltinDelimiterForLicense

plugins {
    embeddedKotlin("jvm")
    id("java-gradle-plugin")
    id("com.diffplug.spotless") version "7.0.0.BETA4"
    id("org.jetbrains.dokka") version "2.0.0-Beta"
    id("com.gradle.plugin-publish") version "1.3.0"
    id("signing")
}

group = "team.idealstate.gradle"
version = "0.1.0"

val javaVersion = 8

kotlin {
    coreLibrariesVersion = "1.9.20"
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(javaVersion))
        vendor.set(JvmVendorSpec.AZUL)
    }
    compilerOptions.javaParameters.set(true)
}

configurations {
    api {
        dependencies.remove(project.dependencies.gradleApi())
    }
}

repositories {
    mavenLocal()
    gradlePluginPortal()
    maven {
        name = "sonatype-public"
        url = uri("https://oss.sonatype.org/content/groups/public/")
    }
    mavenCentral()
}

dependencies {
    compileOnly(gradleApi())
    
    implementation("org.ow2.asm:asm:9.7.1")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    if (javaVersion == 8) {
        testRuntimeOnly(
            files(
                File(
                    javaToolchains
                        .compilerFor {
                            languageVersion.set(java.toolchain.languageVersion)
                            vendor.set(java.toolchain.vendor)
                        }.get()
                        .executablePath.asFile.parentFile.parentFile,
                    "lib/tools.jar",
                ),
            ),
        )
    }
}

val encoding = Charsets.UTF_8

gradlePlugin {
    val repoUrl = "https://gitlab.com/ideal-state/glass"
    website.set("https://docs.ideal-state.team/glass/")
    vcsUrl.set(repoUrl)
    plugins {
        create("Glass") {
            id = "team.idealstate.gradle.glass"
            implementationClass = "team.idealstate.gradle.glass.Glass"
            displayName = "Glass"
            description = ""
            tags.set(listOf("java", "repository", "configuration", "publish", "fatjar", "statistics"))
        }
    }
}

publishing {
    repositories {
        maven {
            name = "build"
            url = uri("file://${projectDir}/build/repository/")
        }
        mavenLocal()
    }
}

signing {
    useGpgCmd()
    sign(publishing.publications)
}

tasks.test {
    useJUnitPlatform()
}

val destCopyrightDir = "${projectDir}/build/copyright/META-INF/COPYRIGHT/${project.group.toString().replace('.', '/')}/${project.name}/"
val licenseFilePath = "${projectDir}/LICENSE.txt"
val licensesDirName = "LICENSES"
val licensesDirPath = "${projectDir}/${licensesDirName}/"
val noticeFilePath = "${projectDir}/NOTICE.txt"
tasks.create<Copy>("copyCopyright") {
    from(licenseFilePath, noticeFilePath)
    into(destCopyrightDir)
}

tasks.create<Copy>("copyDependencyCopyright") {
    from(licensesDirPath)
    into("${destCopyrightDir}${licensesDirName}/")
}

tasks.processResources {
    dependsOn("copyCopyright", "copyDependencyCopyright")
    from("$projectDir/build/copyright/")
}

tasks.create<Jar>("sourcesJar") {
    group = "build"
    dependsOn(tasks.processResources)
    archiveClassifier.set("sources")
    val sourceSet = project.sourceSets.main.get()
    val allSource = sourceSet.allSource
    from(allSource)
}

tasks.create<Jar>("javadocJar") {
    group = "build"
    dependsOn(tasks.dokkaGenerate)
    archiveClassifier.set("javadoc")
    from("$projectDir/build/dokka/html/")
}

tasks.assemble {
    dependsOn("sourcesJar", "javadocJar")
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
