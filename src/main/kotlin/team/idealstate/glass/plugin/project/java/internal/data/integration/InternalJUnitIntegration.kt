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

package team.idealstate.glass.plugin.project.java.internal.data.integration

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.plugins.jvm.JvmTestSuite
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.testing.junitplatform.JUnitPlatformOptions
import org.gradle.kotlin.dsl.PUBLIC
import org.gradle.kotlin.dsl.sonatype
import org.gradle.testing.base.TestingExtension
import team.idealstate.glass.plugin.project.java.data.integration.JUnitIntegration
import team.idealstate.glass.plugin.project.java.internal.extension.InternalJavaExtension
import kotlin.collections.iterator

internal open class InternalJUnitIntegration(
    objects: ObjectFactory,
    override val version: String,
) : JUnitIntegration {
    private val junitAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun platform(action: Action<JUnitPlatformOptions>) {
        junitAction.set(action)
    }

    private val mockitoVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }

    override fun mockito(version: String) {
        mockitoVersion.set(version)
    }

    @Suppress("UNCHECKED_CAST", "UnstableApiUsage")
    fun apply(
        project: Project,
        testingExtension: TestingExtension,
        multiSourceSets: Map<Int, SourceSet>,
    ) {
        project.repositories.apply {
            sonatype(PUBLIC)
        }
        val junitVersion = version
        var mockito = mockitoVersion.orNull
        if (mockito != null) {
            mockito = "org.mockito:mockito-core:$mockito"
        }
        val junitAction = junitAction.orNull as Action<JUnitPlatformOptions>? ?: Action {}
        val suites = testingExtension.suites
        suites.named("test", JvmTestSuite::class.java) {
            configureTestSuite(project, it, junitVersion, mockito, junitAction)
        }
        for ((version, _) in multiSourceSets) {
            suites.named("test$version", JvmTestSuite::class.java) {
                configureTestSuite(project, it, junitVersion, mockito, junitAction)
            }
        }
    }

    @Suppress("UnstableApiUsage")
    private fun configureTestSuite(
        project: Project,
        suite: JvmTestSuite,
        junitVersion: String,
        mockito: String?,
        junitAction: Action<JUnitPlatformOptions>,
    ) {
        suite.useJUnitJupiter(junitVersion)
        suite.dependencies {
            if (mockito != null) {
                it.implementation.add(mockito)
            }
        }
        suite.targets.all { target ->
            target.testTask.configure { task ->
                task.group = InternalJavaExtension.TASK_GROUP
                task.useJUnitPlatform(junitAction)
                task.failOnNoDiscoveredTests.set(false)
            }
        }
    }
}
