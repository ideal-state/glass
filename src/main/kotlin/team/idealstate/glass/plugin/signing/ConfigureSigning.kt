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

package team.idealstate.glass.plugin.signing

import org.gradle.kotlin.dsl.main
import team.idealstate.glass.context.util.Extensions
import team.idealstate.glass.context.util.Plugins
import team.idealstate.glass.plugin.Configure

open class ConfigureSigning : Configure() {
    init {
        dependsOn(Plugins.signing)
    }

    override fun apply() {
        val signing = Extensions.signing(project)
        signing.useGpgCmd()
        if (project.pluginManager.hasPlugin(Plugins.glass(Plugins.publishing))) {
            val publishing = Extensions.publishing(project)
            signing.sign(publishing.publications.main().get())
        }
    }
}
