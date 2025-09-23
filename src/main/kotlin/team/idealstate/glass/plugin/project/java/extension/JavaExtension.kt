package team.idealstate.glass.plugin.project.java.extension

import org.gradle.api.Action
import team.idealstate.glass.plugin.project.java.data.JavaManifest
import team.idealstate.glass.plugin.project.java.data.JavaPublication

interface JavaExtension {

    fun withJava(version: Int, toolchainVersion: Int = version)

    fun withManifest(action: Action<JavaManifest>)

    fun withMultiRelease(version: Int)

    fun withSourcesJar()

    fun withJavadocJar()

    fun withPublication(action: Action<JavaPublication> = Action { })
}