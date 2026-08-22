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

package team.idealstate.glass.plugin.project.java.internal.data

import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.publish.PublishingExtension
import org.gradle.kotlin.dsl.staging
import org.jreleaser.gradle.plugin.JReleaserExtension
import org.jreleaser.model.Active
import team.idealstate.glass.plugin.project.java.data.JavaDeploy
import kotlin.io.path.toPath

internal open class InternalJavaDeploy(
    objects: ObjectFactory,
) : JavaDeploy {
    private var sonatype = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }

    override fun sonatype() {
        sonatype.set(true)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(
        project: Project
    ) {
        if (sonatype.orNull == true) {
            val publishingExtension = project.extensions.getByType(PublishingExtension::class.java)
            val staging = publishingExtension.repositories.staging(project)
            val stagingDir = staging.url.toPath().toFile().absoluteFile.path
            val jreleaserExtension = project.extensions.getByType(JReleaserExtension::class.java)
            jreleaserExtension.apply {
                deploy {
                    maven {
                        mavenCentral {
                            register("sonatypeRelease") {
                                active.set(Active.RELEASE)
                                url.set("https://central.sonatype.com/api/v1/publisher")
                                sign.set(false)
                                stagingRepository(stagingDir)
                            }
                        }
                        nexus2 {
                            register("sonatypeSnapshot") {
                                active.set(Active.SNAPSHOT)
                                url.set("https://central.sonatype.com/repository/maven-snapshots")
                                snapshotUrl.set("https://central.sonatype.com/repository/maven-snapshots")
                                sign.set(false)
                                applyMavenCentralRules.set(true)
                                snapshotSupported.set(true)
                                closeRepository.set(true)
                                releaseRepository.set(true)
                                verifyPom.set(false)
                                stagingRepository(stagingDir)
                            }
                        }
                    }
                }
            }
        }
    }
}
