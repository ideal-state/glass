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
import java.net.URI
import java.util.Locale

val RepositoryHandler.PUBLIC
    get() = "public"

val RepositoryHandler.RELEASES
    get() = "releases"

val RepositoryHandler.SNAPSHOTS
    get() = "snapshots"

fun RepositoryHandler.local(project: Project): MavenArtifactRepository {
    val name = project.name
    if (names.contains(name)) {
        return named(name, MavenArtifactRepository::class.java).get()
    }
    return maven {
        it.name = name
        it.url =
            project.layout.buildDirectory
                .dir("repository")
                .get()
                .asFile
                .normalize()
                .toURI()
    }
}

private fun RepositoryHandler.remote(
    name: String,
    type: String,
    urls: Map<String, String>,
    action: Action<in MavenArtifactRepository>,
): MavenArtifactRepository {
    val actualName = "$name-$type"
    if (names.contains(actualName)) {
        return named(actualName, MavenArtifactRepository::class.java) {
            action.execute(it)
        }.get()
    }
    return maven {
        it.url =
            URI.create(
                urls[type]
                    ?: throw UnsupportedOperationException("Repository \"$name\" does not support type \"$type\"."),
            )
        action.execute(it)
        it.name = actualName
    }
}

fun RepositoryHandler.aliyun(
    type: String = PUBLIC,
    action: Action<in MavenArtifactRepository> = Action { },
): MavenArtifactRepository =
    remote(
        "aliyun",
        type,
        mapOf(
            PUBLIC to "https://maven.aliyun.com/repository/public/",
            RELEASES to "https://maven.aliyun.com/repository/releases/",
            SNAPSHOTS to "https://maven.aliyun.com/repository/snapshots/",
        ),
        action,
    )

fun RepositoryHandler.sonatype(
    type: String = PUBLIC,
    action: Action<in MavenArtifactRepository> = Action { },
): MavenArtifactRepository =
    remote(
        "sonatype",
        type,
        mapOf(
            PUBLIC to "https://repo1.maven.org/maven2/",
            SNAPSHOTS to "https://central.sonatype.com/repository/maven-snapshots/",
        ),
        action,
    )

fun RepositoryHandler.spigotmc(
    type: String = PUBLIC,
    action: Action<in MavenArtifactRepository> = Action { },
): MavenArtifactRepository =
    remote(
        "spigotmc",
        type,
        mapOf(
            PUBLIC to "https://hub.spigotmc.org/nexus/repository/public/",
            RELEASES to "https://hub.spigotmc.org/nexus/repository/releases/",
            SNAPSHOTS to "https://hub.spigotmc.org/nexus/repository/snapshots/",
        ),
        action,
    )

fun RepositoryHandler.papermc(
    type: String = PUBLIC,
    action: Action<in MavenArtifactRepository> = Action { },
): MavenArtifactRepository =
    remote(
        "papermc",
        type,
        mapOf(
            PUBLIC to "https://artifactory.papermc.io/artifactory/universe/",
            RELEASES to "https://artifactory.papermc.io/artifactory/releases/",
            SNAPSHOTS to "https://artifactory.papermc.io/artifactory/snapshots/",
        ),
        action,
    )

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
