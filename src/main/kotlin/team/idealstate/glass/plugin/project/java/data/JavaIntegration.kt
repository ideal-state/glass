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

package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.Action
import team.idealstate.glass.plugin.project.java.data.integration.JUnitIntegration
import team.idealstate.glass.plugin.project.java.data.integration.LombokIntegration
import team.idealstate.glass.plugin.project.java.data.integration.MinecraftIntegration
import team.idealstate.glass.plugin.project.java.data.integration.SugarIntegration

interface JavaIntegration {
    fun sugar(
        apiVersion: String,
        implementationVersion: String,
        implementation: SugarIntegration.Implementation,
        action: Action<SugarIntegration> = Action { },
    )

    fun minecraft(
        minecraftVersion: String,
        apiVersion: String,
        implementation: MinecraftIntegration.Implementation,
        action: Action<MinecraftIntegration> =
            Action {
            },
    )

    fun lombok(
        version: String = LombokIntegration.DEFAULT_LOMBOK_VERSION,
        action: Action<LombokIntegration> = Action { },
    )

    fun junit(
        version: String = JUnitIntegration.DEFAULT_JUNIT_VERSION,
        action: Action<JUnitIntegration> = Action { },
    )
}
