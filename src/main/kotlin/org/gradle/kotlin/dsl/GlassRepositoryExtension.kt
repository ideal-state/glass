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
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import team.idealstate.glass.GlassContext
import team.idealstate.glass.data.CredentialsProvider
import team.idealstate.glass.data.repository.EnvironmentsMavenArtifactRepositoryCredentialsProvider
import team.idealstate.glass.data.repository.PropertiesMavenArtifactRepositoryCredentialsProvider
import java.net.URI

fun MavenArtifactRepository.environments(): CredentialsProvider<out MavenArtifactRepository> =
    EnvironmentsMavenArtifactRepositoryCredentialsProvider(this)

fun MavenArtifactRepository.properties(): CredentialsProvider<out MavenArtifactRepository> {
    val project = GlassContext.project
    return PropertiesMavenArtifactRepositoryCredentialsProvider(project, this)
}

fun RepositoryHandler.project(): MavenArtifactRepository {
    val project = GlassContext.project
    return maven {
        it.name = "Project"
        it.url = project.uri("file://${project.projectDir}/build/repository")
    }
}

val RepositoryHandler.SNAPSHOT: String
    get() = "SNAPSHOT"

fun RepositoryHandler.aliyun(): MavenArtifactRepository =
    maven {
        it.name = "Aliyun"
        it.url = URI.create("https://maven.aliyun.com/repository/public/")
    }

fun RepositoryHandler.sonatype(type: String = "", action: Action<in MavenArtifactRepository> = Action { }): MavenArtifactRepository {
    val maven = maven {
        if (SNAPSHOT.equals(type, true)) {
            it.name = "Sonatype-Snapshots"
            it.url = URI.create("https://central.sonatype.com/repository/maven-snapshots/")
        } else {
            it.name = "Sonatype"
            it.url = URI.create("https://repo1.maven.org/maven2/")
        }
    }
    action.execute(maven)
    return maven
}
