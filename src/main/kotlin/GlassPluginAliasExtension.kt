@file:Suppress("unused", "UnusedReceiverParameter")

import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

private const val FOOJAY_ID = "org.gradle.toolchains.foojay-resolver-convention"
private const val FOOJAY_VERSION = "0.8.0"
val PluginDependenciesSpec.foojay: PluginDependencySpec
    get() = id(FOOJAY_ID).version(FOOJAY_VERSION)
val ObjectConfigurationAction.foojay: ObjectConfigurationAction
    get() = plugin(FOOJAY_ID)

private const val SPOTLESS_ID = "com.diffplug.spotless"
private const val SPOTLESS_VERSION = "7.0.0.BETA4"
val PluginDependenciesSpec.spotless: PluginDependencySpec
    get() = id(SPOTLESS_ID).version(SPOTLESS_VERSION)
val ObjectConfigurationAction.spotless: ObjectConfigurationAction
    get() = plugin(SPOTLESS_ID)

private const val SHADOW_ID = "com.gradleup.shadow"
private const val SHADOW_VERSION = "8.3.5"
val PluginDependenciesSpec.shadow: PluginDependencySpec
        get() = id(SHADOW_ID).version(SHADOW_VERSION)
val ObjectConfigurationAction.shadow: ObjectConfigurationAction
        get() = plugin(SHADOW_ID)
