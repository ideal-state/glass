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

package team.idealstate.glass.context.bytecode.module

import java.util.LinkedList

class ModuleProvideInfoImpl(
    override var service: String,
) : ModuleProvideInfo {
    private val _withProviders: LinkedHashSet<String> = linkedSetOf()
    override val withProviders: List<String>
        get() = LinkedList(_withProviders)

    fun with(provider: String) {
        _withProviders.add(provider)
    }

    fun unwith(provider: String) {
        _withProviders.remove(provider)
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModuleProvideInfoImpl

        if (service != other.service) return false
        if (_withProviders != other._withProviders) return false

        return true
    }

    override fun hashCode(): Int {
        var result = service.hashCode()
        result = 31 * result + _withProviders.hashCode()
        return result
    }

    override fun toString(): String = "ModuleProvideInfoImpl(service='$service', withProviders=$_withProviders)"
}
