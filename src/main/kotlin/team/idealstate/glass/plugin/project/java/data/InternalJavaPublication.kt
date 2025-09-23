package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.Sign
import org.gradle.plugins.signing.SigningExtension

internal open class InternalJavaPublication(objects: ObjectFactory): JavaPublication {

    private var pomAction  = objects.property(Action::class.java).apply { finalizeValueOnRead() }
    private var signing = objects.property(Boolean::class.java).apply {  finalizeValueOnRead() }

    override fun pom(action: Action<MavenPom>) {
        pomAction.set(action)
    }

    override fun sign() {
        signing.set(true)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(publication: MavenPublication, signingExtension: SigningExtension) {
        pomAction.orNull?.let {  publication.pom(it as  Action<MavenPom>) }
        if (signing.orNull == true) {
            signingExtension.sign(publication)
        }
    }
}