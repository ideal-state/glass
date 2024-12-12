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

package team.idealstate.glass.plugin.java.task.data.relocate

import team.idealstate.glass.context.relocate.ClassReferenceRelocator
import team.idealstate.glass.context.relocate.Relocator
import team.idealstate.glass.context.util.ClassUtils

class JavaInternalRelocator(
    moduleName: String,
    className: String,
) : Relocator {
    companion object {
        const val INTERNAL_PACKAGE_NAME = "internal"
    }

    private val relocator =
        ClassReferenceRelocator(
            className,
            ClassUtils.internalize(moduleName, INTERNAL_PACKAGE_NAME, className),
        )

    override fun relocate(source: String): String? = relocator.relocate(source)
}
