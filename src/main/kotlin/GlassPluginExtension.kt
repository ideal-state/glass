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
import team.idealstate.glass.context.util.Plugins

fun PluginDependenciesSpec.glass(id: String): PluginDependencySpec = id(Plugins.glass(id))

fun ObjectConfigurationAction.glass(id: String): ObjectConfigurationAction = plugin(Plugins.glass(id))

val PluginDependenciesSpec.java: String
    get() = Plugins.java
val ObjectConfigurationAction.java: String
    get() = Plugins.java

val PluginDependenciesSpec.maven_publish: String
    get() = Plugins.maven_publish
val ObjectConfigurationAction.maven_publish: String
    get() = Plugins.maven_publish

val PluginDependenciesSpec.signing: String
    get() = Plugins.signing
val ObjectConfigurationAction.signing: String
    get() = Plugins.signing
