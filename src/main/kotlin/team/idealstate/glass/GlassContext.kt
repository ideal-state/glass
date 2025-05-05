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

package team.idealstate.glass

import org.gradle.api.Project
import org.gradle.api.provider.Property

data class GlassContext(
    val project: Project,
) {
//    internal companion object {
//        @JvmStatic
//        private fun <V : Any> isInjected(
//            value: V?,
//            name: String,
//        ): V =
//            value
//                ?: throw IllegalStateException(
//                    "GlassContext value '$name' is not injected. (Please call Project#GlassContext(...) before using it.)",
//                )
//
//        private const val CONTEXT = "context"
//
//        @Volatile
//        private var contextSingleton: GlassContext? = null
//            set(value) =
//                synchronized(this) {
//                    field = value
//                }
//
//        private val context: GlassContext
//            get() = isInjected(contextSingleton, CONTEXT)
//
//        private const val PROJECT = "project"
//
//        val project: Project
//            get() = context.project
//
//        private const val GROUP = "group"
//
//        val group: String
//            get() = context.group.get()
//
//        private const val VERSION = "version"
//
//        val version: String
//            get() = context.version.get()
//    }

    val group: Property<String> =
        project.objects.property(String::class.java).apply {
            set(project.provider { project.group.toString() })
        }

    val version: Property<String> =
        project.objects.property(String::class.java).apply {
            set(project.provider { project.version.toString() })
        }

//    init {
//        contextSingleton = this
//    }

    fun apply() {
        project.group = group.get()
        project.version = version.get()
    }
}
