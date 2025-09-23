package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.Action
import org.gradle.api.java.archives.Manifest
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property

internal open class InternalJavaManifest(objects: ObjectFactory): JavaManifest {

    override val main =  objects.property(String::class.java).apply { finalizeValueOnRead() }
    override val premain = objects.property(String::class.java).apply { finalizeValueOnRead() }
    override val agentmain = objects.property(String::class.java).apply { finalizeValueOnRead() }
    override val canRedefineClasses = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }
    override val canSetNativeMethodPrefix = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }
    override val canRetransformClasses = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }
    private val addActions =  objects.listProperty(Action::class.java).apply { finalizeValueOnRead() }

    override fun add(action: Action<Manifest>) {
        addActions.add(action)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(manifest: Manifest) {
        addAttribute(manifest, "Main-Class", main)
        addAttribute(manifest, "Premain-Class", premain)
        addAttribute(manifest, "Agent-Class", agentmain)
        addAttribute(manifest, "Can-Redefine-Classes", canRedefineClasses)
        addAttribute(manifest, "Can-Set-Native-Method-Prefix", canSetNativeMethodPrefix)
        addAttribute(manifest, "Can-Retransform-Classes", canRetransformClasses)
        for (action in addActions.get()) {
            (action as Action<Manifest>).execute(manifest)
        }
    }

    private fun addAttribute(manifest: Manifest, name: String, value: Property<*>) {
        val any = value.orNull
        if (any != null) {
            if (any is String) {
                manifest.attributes[name] = any
            } else {
                manifest.attributes[name] = any.toString()
            }
        }
    }
}