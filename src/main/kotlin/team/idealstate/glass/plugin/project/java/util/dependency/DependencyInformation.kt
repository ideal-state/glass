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

import org.gradle.api.artifacts.Configuration

open class DependencyInformation(
    val groupId: String,
    val artifactId: String,
    val version: String,
) {
    companion object {
        @JvmStatic
        fun resolveDependenciesInformation(configuration: Configuration): Set<DependencyInformation> {
            val dependencies = configuration.resolvedConfiguration.firstLevelModuleDependencies
            val ret = LinkedHashSet<DependencyInformation>(dependencies.size)
            for (dependency in dependencies) {
                ret.add(
                    DependencyInformation(
                        dependency.moduleGroup,
                        dependency.moduleName,
                        dependency.moduleVersion,
                    ),
                )
            }
            return ret
        }
    }

    val id: String
        get() = "$groupId:$artifactId:$version"

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DependencyInformation

        if (groupId != other.groupId) return false
        if (artifactId != other.artifactId) return false
        if (version != other.version) return false

        return true
    }

    override fun hashCode(): Int {
        var result = groupId.hashCode()
        result = 31 * result + artifactId.hashCode()
        result = 31 * result + version.hashCode()
        return result
    }

    override fun toString(): String = "DependencyInformation(id='$id', group='$groupId', name='$artifactId', version='$version')"
}
