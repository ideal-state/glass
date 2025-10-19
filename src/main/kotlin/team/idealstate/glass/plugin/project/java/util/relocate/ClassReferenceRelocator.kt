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

package team.idealstate.glass.plugin.project.java.util.relocate

import team.idealstate.glass.plugin.project.java.util.JavaUtils
import java.io.Serial
import java.io.Serializable

class ClassReferenceRelocator(
    className: String,
    replacement: String,
) : Relocator,
    Serializable {
    val className: String = JavaUtils.normalizeClassName(className)
    val classReplacement: String = JavaUtils.normalizeClassName(replacement)
    val internalName: String = JavaUtils.internalizeClassName(className)
    val internalReplacement: String = JavaUtils.internalizeClassName(replacement)
    private val classNameRelocator = TextRelocator(this.className, classReplacement)
    private val internalNameRelocator = TextRelocator(internalName, internalReplacement)

    override fun relocate(source: String): String? {
        val str = classNameRelocator.relocate(source) ?: source
        return internalNameRelocator.relocate(str) ?: str
    }

    companion object {
        @Serial
        private const val serialVersionUID: Long = 6143569882681817892L
    }
}
