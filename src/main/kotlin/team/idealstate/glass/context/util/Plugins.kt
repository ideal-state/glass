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

@file:Suppress("ConstPropertyName")

package team.idealstate.glass.context.util

object Plugins {
    const val GLASS_GROUP = "team.idealstate.glass"

    @JvmStatic
    fun glass(id: String): String = "$GLASS_GROUP.$id"

    const val java: String = "java"
    const val java_library: String = "java-library"

    const val maven_publish: String = "maven-publish"
    const val publishing: String = "publishing"

    const val signing: String = "signing"

    const val spotless: String = "com.diffplug.spotless"

    @JvmStatic
    fun spotless(id: String): String = glass("$spotless.$id")

    const val gradle = "gradle"

//    const val java = "java"

    const val kotlin = "kotlin"

    const val json = "json"

    const val yaml = "yaml"

    const val sql = "sql"
}
