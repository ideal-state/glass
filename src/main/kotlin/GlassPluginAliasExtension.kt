@file:Suppress("unused", "UnusedReceiverParameter")

import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

private const val SPOTLESS_ID = "com.diffplug.spotless"
val PluginDependenciesSpec.spotless: PluginDependencySpec
    get() = id(SPOTLESS_ID)
val ObjectConfigurationAction.spotless: ObjectConfigurationAction
    get() = plugin(SPOTLESS_ID)

private const val SHADOW_ID = "com.gradleup.shadow"
val PluginDependenciesSpec.shadow: PluginDependencySpec
        get() = id(SHADOW_ID)
val ObjectConfigurationAction.shadow: ObjectConfigurationAction
        get() = plugin(SHADOW_ID)
