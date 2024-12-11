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

package team.idealstate.glass.context.relocate

import team.idealstate.glass.context.util.ClassUtils

class ClassReferenceRelocator(
    className: String,
    replacement: String,
) : Relocator {
    val className: String = ClassUtils.normalize(className)
    val classReplacement: String = ClassUtils.normalize(replacement)
    val internalName: String = ClassUtils.internalize(className)
    val internalReplacement: String = ClassUtils.internalize(replacement)
    private val classNameRelocator = StringRelocator(this.className, classReplacement)
    private val internalNameRelocator = StringRelocator(internalName, internalReplacement)

    override fun relocate(source: String): String? {
        val str = classNameRelocator.relocate(source) ?: source
        return internalNameRelocator.relocate(str) ?: str
    }
}
