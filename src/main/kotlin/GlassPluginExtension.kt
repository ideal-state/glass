@file:Suppress("unused", "UnusedReceiverParameter")

import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

const val GLASS_GROUP = "team.idealstate.gradle.glass"
fun PluginDependenciesSpec.glass(id: String): PluginDependencySpec = id("$GLASS_GROUP.$id")
fun ObjectConfigurationAction.glass(id: String): ObjectConfigurationAction = plugin("$GLASS_GROUP.$id")

