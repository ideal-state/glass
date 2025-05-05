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

import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import team.idealstate.glass.plugin.publishing.ConfigurePublishing

fun PublicationContainer.main(configureAction: Action<MavenPublication> = Action {}): NamedDomainObjectProvider<MavenPublication> {
    if (names.contains(ConfigurePublishing.MAIN_NAME)) {
        return named(ConfigurePublishing.MAIN_NAME, MavenPublication::class.java, configureAction)
    }
    return register(ConfigurePublishing.MAIN_NAME, MavenPublication::class.java, configureAction)
}
