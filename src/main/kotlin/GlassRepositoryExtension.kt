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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.publish.PublicationContainer
import org.gradle.api.publish.maven.MavenPublication
import java.net.URI

private const val PREFIX = "glass.publish"
private const val KEY = "key"
private const val SECRET = "secret"

val PublicationContainer.default: MavenPublication
    get() = named("glass", MavenPublication::class.java).get()

fun MavenArtifactRepository.login() {
    val id = name.replace(' ', '-').lowercase()
    credentials {
        it.username =
            System.getProperty(
                "$PREFIX.$id.$KEY",
            )
        it.password =
            System.getProperty(
                "$PREFIX.$id.$SECRET",
            )
    }
}

fun MavenArtifactRepository.login(project: Project) {
    val id = name.replace(' ', '-').lowercase()
    credentials {
        it.username =
            project.property(
                "$PREFIX.$id.$KEY",
            ) as String
        it.password =
            project.property(
                "$PREFIX.$id.$SECRET",
            ) as String
    }
}

fun RepositoryHandler.aliyun(): MavenArtifactRepository =
    maven {
        it.name = "Aliyun"
        it.url = URI.create("https://maven.aliyun.com/repository/public/")
    }

fun RepositoryHandler.sonatype(): MavenArtifactRepository =
    maven {
        it.name = "Sonatype"
        it.url = URI.create("https://s01.oss.sonatype.org/content/groups/public/")
    }

fun RepositoryHandler.sonatypeReleases(action: Action<MavenArtifactRepository> = Action {}): MavenArtifactRepository =
    maven {
        it.name = "Sonatype-Releases"
        it.url = URI.create("https://s01.oss.sonatype.org/content/repositories/releases/")
        action.execute(it)
    }

fun RepositoryHandler.sonatypeSnapshots(action: Action<MavenArtifactRepository> = Action {}): MavenArtifactRepository =
    maven {
        it.name = "Sonatype-Snapshots"
        it.url = URI.create("https://s01.oss.sonatype.org/content/repositories/snapshots/")
        action.execute(it)
    }

fun RepositoryHandler.sonatypeStaging(action: Action<MavenArtifactRepository>): MavenArtifactRepository =
    maven {
        it.name = "Sonatype-Staging"
        it.url = URI.create("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
        action.execute(it)
    }
