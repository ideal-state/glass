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
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.CompileOptions
import org.gradle.jvm.toolchain.JavaLanguageVersion
import team.idealstate.glass.context.mark.MarkedContainer
import team.idealstate.glass.context.mark.MarkedProvider
import team.idealstate.glass.context.release.AbstractReleaseContainer
import team.idealstate.glass.context.release.ReleaseType
import team.idealstate.glass.context.util.Validates
import team.idealstate.glass.plugin.java.data.JavaRelease.Companion.MAIN_NAME
import team.idealstate.glass.plugin.java.data.JavaRelease.Companion.TEST_NAME

class JavaReleaseContainer(
    private val project: Project,
    override val mark: String,
) : AbstractReleaseContainer<Int, CompileOptions, JavaRelease>(
        MarkedContainer.create(JavaRelease.Factory(project)),
    ) {
    private object JavaReleaseComparator : Comparator<JavaRelease> {
        override fun compare(
            o1: JavaRelease,
            o2: JavaRelease,
        ): Int {
            val type1 = o1.type
            val type2 = o2.type
            if (type1 == type2) {
                return o1.mark.compareTo(o2.mark)
            }
            if (o1.isReserved()) {
                if (o2.isReserved()) {
                    return if (type1 == ReleaseType.MAIN) {
                        -1
                    } else {
                        1
                    }
                }
                return -1
            }
            if (o2.isReserved()) {
                return 1
            }
            return o1.mark.compareTo(o2.mark)
        }
    }

    private val _artifacts: LinkedHashSet<TaskProvider<out Jar>> = linkedSetOf()

    val artifacts: LinkedHashSet<TaskProvider<out Jar>>
        get() = LinkedHashSet(_artifacts)

    private var _main: MarkedProvider<Int, JavaRelease>? = null
    override val main
        get() = Validates.isPresent(_main, MAIN_NAME)

    private var _test: MarkedProvider<Int, JavaRelease>? = null
    override val test
        get() = Validates.isPresent(_test, TEST_NAME)

    override val all: Set<JavaRelease>
        get() {
            Validates.isPresent(_main, MAIN_NAME)
            val ret = sortedSetOf(JavaReleaseComparator)
            val mainVersion = main.get().javaLanguageVersion
            super.all.forEach {
                if (!it.isReserved()) {
                    val version = it.javaLanguageVersion
                    if (!version.canCompileOrRun(mainVersion)) {
                        throw IllegalArgumentException("Java version must be at least $MAIN_NAME($mainVersion). (it: $version)")
                    }
                }
                ret.add(it)
            }
            return super.all.toSortedSet(JavaReleaseComparator).apply {
                _test?.apply {
                    add(get())
                }
            }
        }

    fun canCompileOrRunOn(version: JavaLanguageVersion): Set<JavaRelease> =
        all
            .filter { version.canCompileOrRun(it.version) }
            .toSortedSet(Comparator.comparingInt { it.version })

    fun artifacts(vararg artifacts: TaskProvider<out Jar>) {
        artifacts.forEach { _artifacts.add(it) }
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

    override fun test(version: Int): MarkedProvider<Int, JavaRelease> = test(version) {}

    override fun test(
        version: Int,
        action: Action<in JavaRelease>,
    ): MarkedProvider<Int, JavaRelease> {
        Validates.notPresent(_test, TEST_NAME)
        this._test =
            MarkedProvider.of(JavaRelease(project, ReleaseType.TEST, version)).apply {
                action.execute(get())
            }
        return _test as MarkedProvider<Int, JavaRelease>
    }
}
