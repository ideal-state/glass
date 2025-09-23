package team.idealstate.glass.plugin.project.java

import team.idealstate.glass.plugin.project.ProjectPlugin
import team.idealstate.glass.plugin.project.java.extension.InternalJavaExtension
import team.idealstate.glass.plugin.project.java.extension.JavaExtension

open class JavaPlugin : ProjectPlugin("java") {

    init {
        dependsOn("java-library", "maven-publish", "signing")
    }

    override fun apply() {
        val javaExtension = project.extensions.create(
            JavaExtension::class.java,
            "glassJava",
            InternalJavaExtension::class.java,
            project.objects
        ) as InternalJavaExtension
        javaExtension.apply(project)
    }
}