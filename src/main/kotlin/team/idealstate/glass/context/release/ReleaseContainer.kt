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

package team.idealstate.glass.context.release

import org.gradle.api.Action
import team.idealstate.glass.context.mark.Marked
import team.idealstate.glass.context.mark.MarkedContainer
import team.idealstate.glass.context.mark.MarkedProvider

interface ReleaseContainer<V : Any, O : Any, R : Release<V, O>> :
    MarkedContainer<V, R>,
    Marked<String> {
    override val all: Set<R>

    val main: MarkedProvider<V, R>
    val test: MarkedProvider<V, R>

    fun main(version: V): MarkedProvider<V, R>

    fun main(
        version: V,
        action: Action<in R>,
    ): MarkedProvider<V, R>

    fun test(version: V): MarkedProvider<V, R>

    fun test(
        version: V,
        action: Action<in R>,
    ): MarkedProvider<V, R>

    fun release(version: V): MarkedProvider<V, R>

    fun release(
        version: V,
        action: Action<in R>,
    ): MarkedProvider<V, R>
}
