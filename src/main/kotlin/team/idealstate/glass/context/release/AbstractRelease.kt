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
import java.io.File

abstract class AbstractRelease<V : Any, O : Any> : Release<V, O> {
    override fun location(base: File): File = base.resolve(location)

    private var configureOptions: Action<O>? = null

    override fun options(configureOptions: Action<O>) {
        this.configureOptions = configureOptions
    }

    override fun apply(options: O) {
        configureOptions?.execute(options)
    }
}
