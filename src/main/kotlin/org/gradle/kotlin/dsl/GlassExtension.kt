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

@file:Suppress("UnusedReceiverParameter")

package org.gradle.kotlin.dsl

import team.idealstate.glass.extension.ApplicableExtension
import team.idealstate.glass.extension.GlassExtension

inline fun <reified T : Any> GlassExtension.with(noinline action: T.() -> Unit) {
    with(T::class, action)
}

inline fun <reified T : ApplicableExtension> GlassExtension.apply(noinline action: T.() -> Unit) {
    apply(T::class, action)
}
