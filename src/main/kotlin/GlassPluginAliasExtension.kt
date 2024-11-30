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
