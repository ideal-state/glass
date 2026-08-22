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

val RepositoryHandler.RELEASE
    get() = "release"

val RepositoryHandler.SNAPSHOT
    get() = "snapshot"

fun RepositoryHandler.staging(project: Project): MavenArtifactRepository {
    val name = "staging"
    if (names.contains(name)) {
        return named(name, MavenArtifactRepository::class.java).get()
    }
    return maven {
        this.name = name
        url =
            project.layout.buildDirectory
                .dir(name)
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
            action.execute(this)
        }.get()
    }
    return maven {
        url =
            URI.create(
                urls[type]
                    ?: throw UnsupportedOperationException("Repository \"$name\" does not support type \"$type\"."),
            )
        action.execute(this)
        this.name = actualName
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
            RELEASE to "https://maven.aliyun.com/repository/releases/",
            SNAPSHOT to "https://maven.aliyun.com/repository/snapshots/",
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
            SNAPSHOT to "https://central.sonatype.com/repository/maven-snapshots/",
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
            RELEASE to "https://hub.spigotmc.org/nexus/repository/releases/",
            SNAPSHOT to "https://hub.spigotmc.org/nexus/repository/snapshots/",
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
            RELEASE to "https://artifactory.papermc.io/artifactory/releases/",
            SNAPSHOT to "https://artifactory.papermc.io/artifactory/snapshots/",
        ),
        action,
    )

fun MavenArtifactRepository.login() {
    credentials {
        val id = name.replace(Regex("[ -.]"), "_").uppercase(Locale.ENGLISH)
        username = System.getenv("GLASS_PUBLISHING_${id}_KEY")
        password = System.getenv("GLASS_PUBLISHING_${id}_SECRET")
    }
}

fun MavenArtifactRepository.login(project: Project) {
    credentials {
        val id = name.replace(Regex("[ -_]"), ".").lowercase(Locale.ENGLISH)
        username = project.property("glass.publishing.$id.key") as String
        password = project.property("glass.publishing.$id.secret") as String
    }
}
