package team.idealstate.glass

import org.gradle.api.Project
import org.gradle.api.provider.Property

open class GlassExtension(private val project: Project) {

    public val withCopyright: Property<Boolean> = project.objects.property(Boolean::class.java)

    fun withCopyright() {
        withCopyright.set(true)
    }
}
