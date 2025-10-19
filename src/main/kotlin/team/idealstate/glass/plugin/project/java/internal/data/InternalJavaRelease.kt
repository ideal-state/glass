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

import org.gradle.api.model.ObjectFactory
import team.idealstate.glass.plugin.project.java.data.JavaRelease

internal open class InternalJavaRelease(
    objects: ObjectFactory,
    override val version: Int,
    override val defaultToolchainVersion: Int = version,
) : InternalJavaToolchain(objects),
    JavaRelease {
    fun validate() {
        if (version > toolchainVersion) {
            throw IllegalArgumentException(
                "Release version $version must be less than or equal to toolchain version $toolchainVersion.",
            )
        }
    }
}
