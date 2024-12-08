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

fun PluginDependenciesSpec.spotless(id: String): PluginDependencySpec = id(Plugins.spotless(id))

fun ObjectConfigurationAction.spotless(id: String): ObjectConfigurationAction = plugin(Plugins.spotless(id))

val PluginDependenciesSpec.gradle: String
    get() = Plugins.gradle
val ObjectConfigurationAction.gradle: String
    get() = Plugins.gradle

// val PluginDependenciesSpec.java: String
//    get() = Plugins.java
// val ObjectConfigurationAction.java: String
//    get() = Plugins.java

val PluginDependenciesSpec.kotlin: String
    get() = Plugins.kotlin
val ObjectConfigurationAction.kotlin: String
    get() = Plugins.kotlin

val PluginDependenciesSpec.json: String
    get() = Plugins.json
val ObjectConfigurationAction.json: String
    get() = Plugins.json

val PluginDependenciesSpec.yaml: String
    get() = Plugins.yaml
val ObjectConfigurationAction.yaml: String
    get() = Plugins.yaml

val PluginDependenciesSpec.sql: String
    get() = Plugins.sql
val ObjectConfigurationAction.sql: String
    get() = Plugins.sql
