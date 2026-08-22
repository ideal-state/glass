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

package team.idealstate.glass.plugin.project.java.extension

import org.gradle.api.Action
import org.gradle.api.provider.Property
import team.idealstate.glass.extension.ApplicableExtension
import team.idealstate.glass.plugin.project.java.data.Java
import team.idealstate.glass.plugin.project.java.data.JavaArtifacts
import team.idealstate.glass.plugin.project.java.data.JavaDeploy
import team.idealstate.glass.plugin.project.java.data.JavaIntegration
import team.idealstate.glass.plugin.project.java.data.JavaPublication

interface JavaExtension : ApplicableExtension {
    val module: Property<String>

    fun module(module: String)

    fun release(
        version: Int,
        action: Action<Java> = Action { },
    )

    fun artifacts(action: Action<JavaArtifacts>)

    fun integration(action: Action<JavaIntegration>)

    fun publication(
        name: String = JavaPublication.DEFAULT_PUBLICATION_NAME,
        action: Action<JavaPublication> = Action { },
    )

    fun deploy(
        action: Action<JavaDeploy>,
    )
}
