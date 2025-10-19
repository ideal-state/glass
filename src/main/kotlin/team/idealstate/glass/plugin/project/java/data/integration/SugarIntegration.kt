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

package team.idealstate.glass.plugin.project.java.data.integration

import org.gradle.api.artifacts.dsl.RepositoryHandler

interface SugarIntegration {
    interface Implementation {
        val name: String

        fun applyRepositories(repositoryHandler: RepositoryHandler)

        fun getCompileDependencies(
            apiVersion: String,
            implementationVersion: String,
        ): List<String>

        fun getRuntimeDependencies(
            apiVersion: String,
            implementationVersion: String,
        ): List<String>

        fun getAnnotationProcessors(
            apiVersion: String,
            implementationVersion: String,
        ): List<String>
    }

    val apiVersion: String

    val implementationVersion: String

    val implementation: Implementation
}
