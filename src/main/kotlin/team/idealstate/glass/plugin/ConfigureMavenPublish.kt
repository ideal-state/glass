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

import default
import dependenciesInformation
import groovy.util.Node
import maven_publish
import org.gradle.api.publish.PublishingExtension
import org.gradle.plugins.signing.SigningExtension
import project
import signing
import team.idealstate.glass.GlassExtension

open class ConfigureMavenPublish : Configure() {
    init {
        dependsOn(Plugins.maven_publish)
    }

    override fun apply() {
        val publishing = project.extensions.getByName("publishing") as PublishingExtension
        publishing.repositories.apply {
            project(project)
        }
        val publication = publishing.publications.default
        publication.apply {
            groupId = project.group.toString()
            artifactId = project.name
            version = project.version.toString()
            pom { pom ->
                pom.name.set(project.name)
                pom.withXml { xml ->
                    val dependenciesNode: Node = xml.asNode().appendNode("dependencies")
                    val dependencies = GlassExtension.dependenciesInformation(project)
                    dependencies.forEach { information ->
                        val dependencyNode = dependenciesNode.appendNode("dependency")
                        dependencyNode.appendNode("groupId", information.group)
                        dependencyNode.appendNode("artifactId", information.name)
                        dependencyNode.appendNode("version", information.version)
                        dependencyNode.appendNode("scope", information.scope)
                    }
                }
            }
        }

        if (project.plugins.hasPlugin(Plugins.signing)) {
            val signing = project.extensions.getByType(SigningExtension::class.java)
            signing.sign(publication)
        }
    }
}
