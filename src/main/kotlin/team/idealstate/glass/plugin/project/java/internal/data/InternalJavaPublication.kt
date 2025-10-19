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
import org.gradle.api.model.ObjectFactory
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension
import team.idealstate.glass.plugin.project.java.data.JavaPublication

internal open class InternalJavaPublication(
    objects: ObjectFactory,
) : JavaPublication {
    private var pomAction = objects.property(Action::class.java).apply { finalizeValueOnRead() }
    private var signing = objects.property(Boolean::class.java).apply { finalizeValueOnRead() }

    override fun pom(action: Action<MavenPom>) {
        pomAction.set(action)
    }

    override fun sign() {
        signing.set(true)
    }

    @Suppress("UNCHECKED_CAST")
    fun apply(
        publication: MavenPublication,
        signingExtension: SigningExtension,
    ) {
        val pomAction = pomAction.orNull as Action<MavenPom>?
        if (pomAction != null) {
            publication.pom(pomAction)
        }
        if (signing.orNull == true) {
            signingExtension.sign(publication)
        }
    }
}
