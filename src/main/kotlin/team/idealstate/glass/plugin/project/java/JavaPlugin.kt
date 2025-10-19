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

package team.idealstate.glass.plugin.project.java

import team.idealstate.glass.plugin.project.ProjectPlugin
import team.idealstate.glass.plugin.project.java.extension.JavaExtension
import team.idealstate.glass.plugin.project.java.internal.extension.InternalJavaExtension

open class JavaPlugin : ProjectPlugin("java") {
    init {
        dependsOn("java-library", "maven-publish", "signing")
    }

    override fun apply() {
        project.extensions.create(
            JavaExtension::class.java,
            "glassJava",
            InternalJavaExtension::class.java,
            project,
            project.configurations.register("shadow"),
            project.configurations.register("doclet"),
        ) as InternalJavaExtension
    }
}
