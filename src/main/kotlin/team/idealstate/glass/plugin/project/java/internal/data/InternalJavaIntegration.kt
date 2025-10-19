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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.SourceSet
import org.gradle.testing.base.TestingExtension
import team.idealstate.glass.plugin.project.java.data.JavaIntegration
import team.idealstate.glass.plugin.project.java.data.integration.JUnitIntegration
import team.idealstate.glass.plugin.project.java.data.integration.LombokIntegration
import team.idealstate.glass.plugin.project.java.data.integration.MinecraftIntegration
import team.idealstate.glass.plugin.project.java.data.integration.SugarIntegration
import team.idealstate.glass.plugin.project.java.internal.data.integration.InternalJUnitIntegration
import team.idealstate.glass.plugin.project.java.internal.data.integration.InternalLombokIntegration
import team.idealstate.glass.plugin.project.java.internal.data.integration.InternalMinecraftIntegration
import team.idealstate.glass.plugin.project.java.internal.data.integration.InternalSugarIntegration

internal open class InternalJavaIntegration(
    objects: ObjectFactory,
) : JavaIntegration {
    private val sugarApiVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val sugarImplementationVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val sugarImplementation = objects.property(SugarIntegration.Implementation::class.java).apply { finalizeValueOnRead() }
    private val sugarAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun sugar(
        apiVersion: String,
        implementationVersion: String,
        implementation: SugarIntegration.Implementation,
        action: Action<SugarIntegration>,
    ) {
        sugarApiVersion.set(apiVersion)
        sugarImplementationVersion.set(implementationVersion)
        sugarImplementation.set(implementation)
        sugarAction.set(action)
    }

    private val minecraftMinecraftVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val minecraftApiVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val minecraftImplementation = objects.property(MinecraftIntegration.Implementation::class.java).apply { finalizeValueOnRead() }
    private val minecraftAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun minecraft(
        minecraftVersion: String,
        apiVersion: String,
        implementation: MinecraftIntegration.Implementation,
        action: Action<MinecraftIntegration>,
    ) {
        minecraftMinecraftVersion.set(minecraftVersion)
        minecraftApiVersion.set(apiVersion)
        minecraftImplementation.set(implementation)
        minecraftAction.set(action)
    }

    private val lombokVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val lombokAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun lombok(
        version: String,
        action: Action<LombokIntegration>,
    ) {
        lombokVersion.set(version)
        lombokAction.set(action)
    }

    private val junitVersion = objects.property(String::class.java).apply { finalizeValueOnRead() }
    private val junitAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }

    override fun junit(
        version: String,
        action: Action<JUnitIntegration>,
    ) {
        junitVersion.set(version)
        junitAction.set(action)
    }

    @Suppress("UNCHECKED_CAST", "UnstableApiUsage")
    fun apply(
        project: Project,
        testingExtension: TestingExtension,
        mainSourceSet: SourceSet,
        testSourceSet: SourceSet,
        multiSourceSets: Map<Int, SourceSet>,
    ) {
        val sugarAction = sugarAction.orNull as Action<SugarIntegration>?
        val withSugar = sugarAction != null

        val minecraftAction = minecraftAction.orNull as Action<MinecraftIntegration>?
        val withMinecraft = minecraftAction != null

        val lombokAction = lombokAction.orNull as Action<LombokIntegration>?
        val withLombok = lombokAction != null

        val junitAction = junitAction.orNull as Action<JUnitIntegration>?
        val withJUnit = junitAction != null

        // --------------------------------------------------------------------------------------------------

        if (withSugar) {
            val sugarApiVersion = sugarApiVersion.get()
            val sugarImplementationVersion = sugarImplementationVersion.get()
            val sugarImplementation = sugarImplementation.get()
            val javaSugar = InternalSugarIntegration(project.objects, sugarApiVersion, sugarImplementationVersion, sugarImplementation)
            sugarAction.execute(javaSugar)
            javaSugar.apply(project, mainSourceSet, testSourceSet)
        }

        if (withMinecraft) {
            val minecraftMinecraftVersion = minecraftMinecraftVersion.get()
            val minecraftApiVersion = minecraftApiVersion.get()
            val minecraftImplementation = minecraftImplementation.get()
            val javaMinecraft =
                InternalMinecraftIntegration(project.objects, minecraftMinecraftVersion, minecraftApiVersion, minecraftImplementation)
            minecraftAction.execute(javaMinecraft)
            javaMinecraft.apply(project, mainSourceSet, testSourceSet)
        }

        if (withLombok) {
            val lombokVersion = lombokVersion.get()
            val javaLombok = InternalLombokIntegration(project.objects, lombokVersion)
            lombokAction.execute(javaLombok)
            javaLombok.apply(project, mainSourceSet, testSourceSet)
        }

        if (withJUnit) {
            val junitVersion = junitVersion.get()
            val javaJUnit = InternalJUnitIntegration(project.objects, junitVersion)
            junitAction.execute(javaJUnit)
            javaJUnit.apply(project, testingExtension, multiSourceSets)
        }
    }
}
