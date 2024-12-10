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
import team.idealstate.glass.context.mark.MarkedContainer
import team.idealstate.glass.context.mark.MarkedObserver
import team.idealstate.glass.context.mark.MarkedProvider

abstract class AbstractReleaseContainer<V : Any, O : Any, R : Release<V, O>>(
    private val delegate: MarkedContainer<V, R>,
) : ReleaseContainer<V, O, R>,
    MarkedContainer<V, R> by delegate {
    private inner class Observer : MarkedObserver<V, R> {
        override fun onRemoved(markedProvider: MarkedProvider<out V, out R>) {
            val marked = markedProvider.get()
            if (marked.isReserved()) {
                throw IllegalStateException("Cannot remove reserved release. (${marked.type})")
            }
        }
    }

    init {
        delegate.observeBy(Observer())
    }

    override fun release(version: V): MarkedProvider<V, R> = delegate.register(version)

    override fun release(
        version: V,
        action: Action<in R>,
    ): MarkedProvider<V, R> = delegate.register(version, action)
}
