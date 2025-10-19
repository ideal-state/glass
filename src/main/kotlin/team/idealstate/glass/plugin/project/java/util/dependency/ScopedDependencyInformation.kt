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

package team.idealstate.glass.plugin.project.java.util.dependency

import org.gradle.api.Project

class ScopedDependencyInformation(
    groupId: String,
    artifactId: String,
    version: String,
    val scope: String = "",
) : DependencyInformation(groupId, artifactId, version) {
    companion object {
        @JvmStatic
        fun resolveDependenciesInformation(project: Project): List<ScopedDependencyInformation> {
            val dependenciesInformation = linkedMapOf<String, ScopedDependencyInformation>()
            var scope = "runtime"
            val runtimeClasspath = project.configurations.getByName("runtimeClasspath")
            for (information in DependencyInformation.resolveDependenciesInformation(runtimeClasspath)) {
                dependenciesInformation[information.id] =
                    ScopedDependencyInformation(information.groupId, information.artifactId, information.version, scope)
            }
            scope = "compile"
            val compileClasspath = project.configurations.getByName("compileClasspath")
            for (information in DependencyInformation.resolveDependenciesInformation(compileClasspath)) {
                val id = information.id
                if (dependenciesInformation.containsKey(id)) {
                    dependenciesInformation[information.id] =
                        ScopedDependencyInformation(information.groupId, information.artifactId, information.version, scope)
                }
            }
            return dependenciesInformation.values.toList()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        if (!super.equals(other)) return false

        other as ScopedDependencyInformation

        return scope == other.scope
    }

    override fun hashCode(): Int {
        var result = super.hashCode()
        result = 31 * result + scope.hashCode()
        return result
    }

    override fun toString(): String =
        "ScopedDependencyInformation(id='$id', group='$groupId', name='$artifactId', version='$version', scope='$scope')"
}
