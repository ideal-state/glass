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

package team.idealstate.glass.plugin.java.data

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.compile.CompileOptions
import team.idealstate.glass.context.mark.MarkedContainer
import team.idealstate.glass.context.mark.MarkedObserver
import team.idealstate.glass.context.mark.MarkedProvider
import team.idealstate.glass.context.release.AbstractReleaseContainer
import team.idealstate.glass.context.release.ReleaseType
import team.idealstate.glass.context.util.Validates
import team.idealstate.glass.plugin.java.data.JavaRelease.Companion.MAIN_NAME
import java.util.SortedSet

class JavaReleaseContainer(
    private val project: Project,
    override val mark: String,
) : AbstractReleaseContainer<Int, CompileOptions, JavaRelease>(
        MarkedContainer.create(JavaRelease.Factory(project)),
    ) {
    private inner class Observer : MarkedObserver<Int, JavaRelease> {
        override fun onAdd(marked: JavaRelease) {
            if (marked.isReserved()) {
                Validates.notPresent(_main, MAIN_NAME)
                return
            }
            val main = main.get()
            if (marked.version <= main.version) {
                throw IllegalArgumentException("Java version must be greater than $MAIN_NAME(${main.version}). (it: ${marked.version})")
            }
        }
    }

    init {
        super.observeBy(Observer())
    }

    private var _main: MarkedProvider<Int, JavaRelease>? = null
    override val main
        get() = Validates.isPresent(_main, MAIN_NAME)

    override val all: SortedSet<JavaRelease>
        get() {
            Validates.isPresent(_main, MAIN_NAME)
            return sortedSetOf<JavaRelease>(Comparator.comparingInt { it.version }).apply {
                addAll(super.all)
            }
        }

    override fun main(version: Int): MarkedProvider<Int, JavaRelease> = add(JavaRelease(project, ReleaseType.MAIN, version))

    override fun main(
        version: Int,
        action: Action<in JavaRelease>,
    ): MarkedProvider<Int, JavaRelease> {
        Validates.notPresent(_main, MAIN_NAME)
        val main = add(JavaRelease(project, ReleaseType.MAIN, version), action)
        this._main = main
        return main
    }
}
