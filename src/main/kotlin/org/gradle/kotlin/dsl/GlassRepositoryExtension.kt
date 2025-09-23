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
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.io.File
import java.net.URI
import java.util.Locale

fun RepositoryHandler.project(project: Project): MavenArtifactRepository =
    maven {
        it.name = "project"
        it.url = File(project.projectDir, "build/repository").toURI()
    }

fun RepositoryHandler.aliyun(): MavenArtifactRepository =
    maven {
        it.name = "aliyun"
        it.url = URI.create("https://maven.aliyun.com/repository/public/")
    }

fun RepositoryHandler.sonatype(
    snapshot: Boolean = false,
    action: Action<in MavenArtifactRepository> = Action { },
): MavenArtifactRepository {
    val maven =
        maven {
            if (snapshot) {
                it.name = "sonatype-snapshots"
                it.url = URI.create("https://central.sonatype.com/repository/maven-snapshots/")
            } else {
                it.name = "sonatype"
                it.url = URI.create("https://repo1.maven.org/maven2/")
            }
        }
    action.execute(maven)
    return maven
}

fun MavenArtifactRepository.login() {
    credentials {
        val id = name.replace(Regex("[ -.]"), "_").uppercase(Locale.ENGLISH)
        it.username = System.getenv("GLASS_PUBLISHING_${id}_KEY")
        it.password = System.getenv("GLASS_PUBLISHING_${id}_SECRET")
    }
}

fun MavenArtifactRepository.login(project: Project) {
    credentials {
        val id = name.replace(Regex("[ -_]"), ".").lowercase(Locale.ENGLISH)
        it.username = project.property("glass.publishing.$id.key") as String
        it.password = project.property("glass.publishing.$id.secret") as String
    }
}
