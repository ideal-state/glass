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

@file:Suppress("unused", "UnusedReceiverParameter")

package org.gradle.kotlin.dsl

import org.gradle.api.Action
import org.gradle.api.artifacts.dsl.RepositoryHandler
import team.idealstate.glass.plugin.project.java.data.JavaIntegration
import team.idealstate.glass.plugin.project.java.data.integration.MinecraftIntegration
import team.idealstate.glass.plugin.project.java.data.integration.SugarIntegration
import team.idealstate.glass.util.PropertiesFormatter

internal abstract class InternalIntegrationImplementation(
    protected val firstVersionKey: String,
    protected val secondVersionKey: String,
    protected val applyRepositories: Action<RepositoryHandler>,
    protected val compileDependencies: List<String>,
    protected val runtimeDependencies: List<String>,
    protected val annotationProcessors: List<String>,
) {
    protected fun format(
        firstVersion: String,
        secondVersion: String,
        dependencies: List<String>,
    ): List<String> {
        dependencies.isEmpty() && return emptyList()
        val properties = mapOf(this.firstVersionKey to firstVersion, this.secondVersionKey to secondVersion)
        return dependencies.map {
            PropertiesFormatter.format(it, properties)
        }
    }
}

// --------------------------------------------------------------------------------------------------

internal class InternalSugarIntegrationImplementation(
    override val name: String,
    firstVersionKey: String,
    secondVersionKey: String,
    applyRepositories: Action<RepositoryHandler>,
    compileDependencies: List<String>,
    runtimeDependencies: List<String>,
    annotationProcessors: List<String>,
) : InternalIntegrationImplementation(
        firstVersionKey,
        secondVersionKey,
        applyRepositories,
        compileDependencies,
        runtimeDependencies,
        annotationProcessors,
    ),
    SugarIntegration.Implementation {
    override fun applyRepositories(repositoryHandler: RepositoryHandler) {
        applyRepositories.execute(repositoryHandler)
    }

    override fun getCompileDependencies(
        apiVersion: String,
        implementationVersion: String,
    ): List<String> = format(apiVersion, implementationVersion, compileDependencies)

    override fun getRuntimeDependencies(
        apiVersion: String,
        implementationVersion: String,
    ): List<String> = format(apiVersion, implementationVersion, runtimeDependencies)

    override fun getAnnotationProcessors(
        apiVersion: String,
        implementationVersion: String,
    ): List<String> = format(apiVersion, implementationVersion, annotationProcessors)
}

val JavaIntegration.HYPER: SugarIntegration.Implementation
    get() =
        InternalSugarIntegrationImplementation(
            "sugar-next",
            "apiVersion",
            "implementationVersion",
            {
                it.sonatype()
                it.sonatype("snapshots")
            },
            listOf(
                $$"team.idealstate.sugar:sugar-next:${apiVersion}",
                $$"team.idealstate.hyper:hyper:${apiVersion}-${implementationVersion}",
            ),
            listOf($$"team.idealstate.hyper:hyper:${apiVersion}-${implementationVersion}"),
            listOf($$"team.idealstate.sugar:sugar:${apiVersion}"),
        )

val JavaIntegration.MINECRAFT_NEXT: SugarIntegration.Implementation
    get() =
        InternalSugarIntegrationImplementation(
            "minecraft-next",
            "apiVersion",
            "implementationVersion",
            {
                it.sonatype()
                it.sonatype("snapshots")
            },
            listOf(
                $$"team.idealstate.sugar:sugar-next:${apiVersion}",
                $$"team.idealstate.minecraft:minecraft-next:${apiVersion}-${implementationVersion}",
            ),
            listOf($$"team.idealstate.minecraft:minecraft-next:${apiVersion}-${implementationVersion}"),
            listOf($$"team.idealstate.sugar:sugar:${apiVersion}"),
        )

// --------------------------------------------------------------------------------------------------

internal class InternalMinecraftIntegrationImplementation(
    override val name: String,
    firstVersionKey: String,
    secondVersionKey: String,
    applyRepositories: Action<RepositoryHandler>,
    compileDependencies: List<String>,
    runtimeDependencies: List<String>,
    annotationProcessors: List<String>,
) : InternalIntegrationImplementation(
        firstVersionKey,
        secondVersionKey,
        applyRepositories,
        compileDependencies,
        runtimeDependencies,
        annotationProcessors,
    ),
    MinecraftIntegration.Implementation {
    override fun applyRepositories(repositoryHandler: RepositoryHandler) {
        applyRepositories.execute(repositoryHandler)
    }

    override fun getCompileDependencies(
        minecraftVersion: String,
        apiVersion: String,
    ): List<String> = format(minecraftVersion, apiVersion, compileDependencies)

    override fun getRuntimeDependencies(
        minecraftVersion: String,
        apiVersion: String,
    ): List<String> = format(minecraftVersion, apiVersion, runtimeDependencies)

    override fun getAnnotationProcessors(
        minecraftVersion: String,
        apiVersion: String,
    ): List<String> = format(minecraftVersion, apiVersion, annotationProcessors)
}

val JavaIntegration.SPIGOT: MinecraftIntegration.Implementation
    get() =
        InternalMinecraftIntegrationImplementation(
            "spigot",
            "minecraftVersion",
            "apiVersion",
            { it.spigotmc() },
            listOf($$"org.spigotmc:spigot-api:${minecraftVersion}-${apiVersion}"),
            listOf($$"org.spigotmc:spigot-api:${minecraftVersion}-${apiVersion}"),
            listOf("org.spigotmc:plugin-annotations:1.3-SNAPSHOT"),
        )

val JavaIntegration.PAPER: MinecraftIntegration.Implementation
    get() =
        InternalMinecraftIntegrationImplementation(
            "paper",
            "minecraftVersion",
            "apiVersion",
            { it.papermc() },
            listOf($$"io.papermc.paper:paper-api:${minecraftVersion}-${apiVersion}"),
            listOf($$"io.papermc.paper:paper-api:${minecraftVersion}-${apiVersion}"),
            listOf("org.spigotmc:plugin-annotations:1.3-SNAPSHOT"),
        )
