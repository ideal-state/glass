/*
 *    Copyright 2024 ideal-state
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

@file:Suppress("unused", "UnusedReceiverParameter")

package org.gradle.kotlin.dsl

import org.gradle.api.plugins.ObjectConfigurationAction
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

val PluginDependenciesSpec.glass: PluginDependencySpec
    get() = id("team.idealstate.glass")

val ObjectConfigurationAction.glass: ObjectConfigurationAction
    get() = plugin("team.idealstate.glass")

fun PluginDependenciesSpec.glass(id: String): PluginDependencySpec = id("team.idealstate.glass.$id")

fun ObjectConfigurationAction.glass(id: String): ObjectConfigurationAction = plugin("team.idealstate.glass.$id")

fun PluginDependenciesSpec.spotless(id: String): PluginDependencySpec = id("team.idealstate.glass.com.diffplug.spotless.$id")

fun ObjectConfigurationAction.spotless(id: String): ObjectConfigurationAction = plugin("team.idealstate.glass.com.diffplug.spotless.$id")
