package team.idealstate.glass.plugin.java.data

import org.gradle.api.Project
import org.gradle.api.provider.Property

class Sugar(
    project: Project
) {
    val release: Property<String> =
        project.objects.property(String::class.java).apply {
            set("")
        }

    fun toManifestAttributes(): Map<String, *> {
        val attributes = linkedMapOf<String, Any>()
        if (release.isPresent || release.orNull.isNullOrBlank()) {
            attributes["sugar-release"] = release.get()
        }
        if (attributes.isNotEmpty()) {
            attributes.putFirst("sugar", true)
        }
        return attributes
    }
}