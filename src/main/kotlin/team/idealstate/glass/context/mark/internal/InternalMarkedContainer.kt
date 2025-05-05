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

package team.idealstate.glass.context.mark.internal

import org.gradle.api.Action
import org.gradle.api.internal.provider.DefaultProvider
import team.idealstate.glass.context.mark.Marked
import team.idealstate.glass.context.mark.MarkedContainer
import team.idealstate.glass.context.mark.MarkedFactory
import team.idealstate.glass.context.mark.MarkedObserver
import team.idealstate.glass.context.mark.MarkedProvider

internal class InternalMarkedContainer<T : Any, M : Marked<T>>(
    private val factory: MarkedFactory<T, M>,
) : MarkedContainer<T, M> {
    private val providers: MutableMap<T, MarkedProvider<T, M>> = mutableMapOf()
    override val all: Set<M>
        get() = LinkedHashSet(providers.values.map { it.get() })

    private fun createAndAdd(
        mark: T,
        action: Action<in M>,
    ): M {
        observers.forEach {
            it.onCreate(mark)
        }
        val marked = factory.create(mark)
        observers.forEach {
            it.onAdd(marked)
        }

        action.execute(marked)

        observers.forEach {
            it.onAdded(marked)
        }
        return marked
    }

    override fun register(mark: T): MarkedProvider<T, M> = register(mark) {}

    override fun register(
        mark: T,
        action: Action<in M>,
    ): MarkedProvider<T, M> {
        validateDuplicate(mark)
        val provider =
            DefaultProvider {
                createAndAdd(mark, action)
            }
        val markedProvider = MarkedProvider.create(mark, provider)
        providers[mark] = markedProvider
        return markedProvider
    }

    override fun create(mark: T): M = create(mark) {}

    override fun create(
        mark: T,
        action: Action<in M>,
    ): M = register(mark, action).get()

    override fun add(marked: M): MarkedProvider<T, M> = add(marked) {}

    override fun add(
        marked: M,
        action: Action<in M>,
    ): MarkedProvider<T, M> {
        val mark = marked.mark
        validateDuplicate(mark)
        val markedProvider = MarkedProvider.of(marked)
        observers.forEach {
            it.onAdd(marked)
        }

        action.execute(marked)
        providers[mark] = markedProvider

        observers.forEach {
            it.onAdded(marked)
        }
        return markedProvider
    }

    override fun add(markedProvider: MarkedProvider<out T, out M>): MarkedProvider<T, M> = add(markedProvider) {}

    override fun add(
        markedProvider: MarkedProvider<out T, out M>,
        action: Action<in M>,
    ): MarkedProvider<T, M> {
        val mark = markedProvider.mark
        validateDuplicate(mark)
        return add(markedProvider.get(), action)
    }

    override fun replace(marked: M): MarkedProvider<T, M>? = replace(marked) {}

    override fun replace(
        marked: M,
        action: Action<in M>,
    ): MarkedProvider<T, M>? {
        val mark = marked.mark
        val removingMarkedProvider = providers[mark]

        observers.forEach {
            it.onReplace(marked, removingMarkedProvider)
        }

        remove(mark)
        add(marked)

        observers.forEach {
            it.onReplaced(marked, removingMarkedProvider)
        }
        return removingMarkedProvider
    }

    override fun replace(mark: T): MarkedProvider<T, M>? = replace(mark) {}

    override fun replace(
        mark: T,
        action: Action<in M>,
    ): MarkedProvider<T, M>? {
        val provider =
            DefaultProvider {
                createAndAdd(mark, action)
            }
        val markedProvider = MarkedProvider.create(mark, provider)
        val removingMarkedProvider = providers[mark]

        observers.forEach {
            it.onReplace(markedProvider, removingMarkedProvider)
        }

        remove(mark)
        add(markedProvider)

        observers.forEach {
            it.onReplaced(markedProvider, removingMarkedProvider)
        }
        return removingMarkedProvider
    }

    private fun validateDuplicate(mark: T) {
        if (providers.containsKey(mark)) {
            throw IllegalStateException("There are duplicate marked: $mark.")
        }
    }

    override fun marked(mark: T): MarkedProvider<T, M> = marked(mark) {}

    override fun marked(
        mark: T,
        action: Action<in M>,
    ): MarkedProvider<T, M> {
        val markedProvider = providers[mark]!!
        val marked = markedProvider.get()
        action.execute(marked)
        return markedProvider
    }

    override fun remove(mark: T): Boolean {
        observers.forEach { it.onRemove(mark) }
        val removedMarkedProvider = providers.remove(mark) ?: return false
        observers.forEach {
            it.onRemoved(removedMarkedProvider)
        }
        return true
    }

    private val observers = linkedSetOf<MarkedObserver<T, in M>>()

    override fun observeBy(observer: MarkedObserver<T, in M>) {
        observers.add(observer)
    }
}
