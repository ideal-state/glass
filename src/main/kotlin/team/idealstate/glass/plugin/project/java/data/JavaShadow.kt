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

package team.idealstate.glass.plugin.project.java.data

import org.gradle.api.specs.Spec
import team.idealstate.glass.plugin.project.java.util.relocate.Relocator
import java.io.File

interface JavaShadow {
    fun include(includeSpec: Spec<File>)

    fun exclude(excludeSpec: Spec<File>)

    fun internal(source: String)

    fun relocate(
        source: String,
        target: String,
    )

    fun custom(relocator: Relocator)
}
