package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.Action
import org.gradle.api.publish.maven.MavenPom
import org.gradle.plugins.signing.Sign

interface JavaPublication {

    fun pom(action: Action<MavenPom>)

    fun sign()
}