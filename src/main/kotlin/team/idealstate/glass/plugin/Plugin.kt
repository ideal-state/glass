package team.idealstate.glass.plugin

import org.gradle.api.Plugin
import org.gradle.api.plugins.PluginAware

abstract class Plugin<T: PluginAware>(id: String) : Plugin<T> {
    private val id: String = "team.idealstate.glass.$id"
    private var applied: T? = null
        set(value) {
            if (field != null) {
                throw IllegalStateException("The plugin '$id' has been applied.")
            }
            field = value
        }
    protected val target: T
        get() {
            applied ?: throw IllegalStateException("The plugin '$id' has not yet been applied.")
            return applied!!
        }
    private val dependencies: MutableList<String> = mutableListOf()

    final override fun apply(target: T) {
        applied = target
        applyDependencies(target, dependencies)
        apply()
    }

    protected abstract fun apply()

    protected fun dependsOn(vararg dependencies: String) {
        this.dependencies.addAll(dependencies)
    }

    private fun applyDependencies(target: T, dependencies: Collection<String>) {
        val pluginManager = target.pluginManager
        for (dependency in dependencies) {
            if (pluginManager.hasPlugin(dependency)) {
                continue
            }
            pluginManager.apply(dependency)
        }
    }
}