@file:Suppress("unused", "UnusedReceiverParameter")

import org.gradle.api.artifacts.dsl.DependencyHandler

private const val GLASS_ID = "team.idealstate.glass:glass"
private const val GLASS_VERSION = "0.1.0"
fun DependencyHandler.glass(version: String = GLASS_VERSION) = "$GLASS_ID:$version"

private const val SUGAR_ID = "team.idealstate.sugar:sugar"
private const val SUGAR_VERSION = "0.1.0"
fun DependencyHandler.sugar(version: String = SUGAR_VERSION) = "$SUGAR_ID:$version"

private const val WATER_ID = "team.idealstate.water:water"
private const val WATER_VERSION = "0.1.0"
fun DependencyHandler.water(version: String = WATER_VERSION) = "$WATER_ID:$version"
