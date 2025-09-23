package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.Action
import org.gradle.api.java.archives.Manifest
import org.gradle.api.provider.Property

interface JavaManifest {

    val main: Property<String>
    val premain: Property<String>
    val agentmain: Property<String>
    val canRedefineClasses: Property<Boolean>
    val canSetNativeMethodPrefix: Property<Boolean>
    val canRetransformClasses: Property<Boolean>

    fun add(action: Action<Manifest>)
}