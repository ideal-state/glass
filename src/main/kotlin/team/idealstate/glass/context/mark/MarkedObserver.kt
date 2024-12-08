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

package team.idealstate.glass.context.mark

interface MarkedObserver<T : Any, M : Marked<T>> {
    fun onCreate(mark: T) {}

    fun onAdd(marked: M) {}

    fun onAdded(marked: M) {}

    fun onReplace(
        marked: M,
        oldMarkedProvider: MarkedProvider<out T, out M>?,
    ) {}

    fun onReplaced(
        marked: M,
        oldMarkedProvider: MarkedProvider<out T, out M>?,
    ) {}

    fun onReplace(
        markedProvider: MarkedProvider<out T, out M>,
        oldMarkedProvider: MarkedProvider<out T, out M>?,
    ) {}

    fun onReplaced(
        markedProvider: MarkedProvider<out T, out M>,
        oldMarkedProvider: MarkedProvider<out T, out M>?,
    ) {}

    fun onRemove(mark: T) {}

    fun onRemoved(markedProvider: MarkedProvider<out T, out M>) {}
}
