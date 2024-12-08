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

package team.idealstate.glass.plugin.publishing

import groovy.util.Node
import main
import org.gradle.api.tasks.bundling.Jar
import project
import team.idealstate.glass.context.util.Extensions
import team.idealstate.glass.context.util.Plugins
import team.idealstate.glass.plugin.Configure
import team.idealstate.glass.plugin.java.ConfigureJava
import team.idealstate.glass.plugin.java.GlassJavaExtension

open class ConfigureMavenPublish : Configure() {
    init {
        dependsOn(Plugins.maven_publish)
    }

    override fun apply() {
        val publishing = Extensions.publishing(project)
        publishing.repositories.apply {
            project(project)
        }
        publishing.publications.main {
            val tasks = project.tasks
            val pluginManager = project.pluginManager
            if (pluginManager.hasPlugin(Plugins.glass(Plugins.java))) {
                it.artifact(tasks.named(ConfigureJava.JAR_TASK_NAME, Jar::class.java))
                it.artifact(tasks.named(ConfigureJava.SOURCES_JAR_TASK_NAME, Jar::class.java))
                it.artifact(tasks.named(ConfigureJava.JAVADOC_JAR_TASK_NAME, Jar::class.java))
            }
            it.pom { pom ->
                pom.name.set(project.name)
                pom.withXml { xml ->
                    val xmlNode = xml.asNode()
                    removeChildren(xmlNode, "dependencies")
                    val dependenciesNode = xmlNode.appendNode("dependencies")
                    val dependencies = GlassJavaExtension.dependenciesInformation(project)
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
    }

    private fun removeChildren(
        parent: Node,
        childName: String,
    ) {
        val childrenIterator = parent.children().iterator()
        while (childrenIterator.hasNext()) {
            val child = childrenIterator.next()
            if (child is Node) {
                if (child.name() == childName) {
                    childrenIterator.remove()
                }
            }
        }
    }
}
