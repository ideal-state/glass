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
import team.idealstate.glass.plugin.Configure

const val GLASS_GROUP = "team.idealstate.glass"

fun PluginDependenciesSpec.glass(id: String): PluginDependencySpec = id("$GLASS_GROUP.$id")

fun ObjectConfigurationAction.glass(id: String): ObjectConfigurationAction = plugin("$GLASS_GROUP.$id")

private const val JAVA_ID = "java"
val Configure.Plugins.java: String
    get() = JAVA_ID
val PluginDependenciesSpec.java: String
    get() = JAVA_ID
val ObjectConfigurationAction.java: String
    get() = JAVA_ID

private const val JAVA_LIBRARY_ID = "java-library"
val Configure.Plugins.java_library: String
    get() = JAVA_LIBRARY_ID
val PluginDependenciesSpec.java_library: String
    get() = JAVA_LIBRARY_ID
val ObjectConfigurationAction.java_library: String
    get() = JAVA_LIBRARY_ID

private const val MAVEN_PUBLISH_ID = "maven-publish"
val Configure.Plugins.maven_publish: String
    get() = MAVEN_PUBLISH_ID
val PluginDependenciesSpec.maven_publish: String
    get() = MAVEN_PUBLISH_ID
val ObjectConfigurationAction.maven_publish: String
    get() = MAVEN_PUBLISH_ID

private const val SIGNING_ID = "signing"
val Configure.Plugins.signing: String
    get() = SIGNING_ID
val PluginDependenciesSpec.signing: String
    get() = SIGNING_ID
val ObjectConfigurationAction.signing: String
    get() = SIGNING_ID

private const val SPOTLESS_ID = "com.diffplug.spotless"
val Configure.Plugins.spotless: String
    get() = SPOTLESS_ID
val PluginDependenciesSpec.spotless: String
    get() = SPOTLESS_ID
val ObjectConfigurationAction.spotless: String
    get() = SPOTLESS_ID

private const val SHADOW_ID = "com.gradleup.shadow"
val Configure.Plugins.shadow: String
    get() = SHADOW_ID
val PluginDependenciesSpec.shadow: String
    get() = SHADOW_ID
val ObjectConfigurationAction.shadow: String
    get() = SHADOW_ID
