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

package team.idealstate.glass.plugin

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.bundling.Jar
import shadow

open class ConfigureShadow : Configure() {
    init {
        dependsOn(Plugins.shadow)
    }

    override fun apply() {
        val shadowJar = project.tasks.named("shadowJar", ShadowJar::class.java) { it ->
            it.configurations.set(listOf(project.configurations.named("shadow").get()))
            it.archiveClassifier.set("")
            ConfigureJava.configureJarTask(it)
        }
        project.tasks.named("jar", Jar::class.java) {
            it.enabled = false
            it.finalizedBy(shadowJar)
        }
    }
}
