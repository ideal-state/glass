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

package team.idealstate.glass.plugin.project.java.util.relocate.bytecode

import org.objectweb.asm.Handle
import org.objectweb.asm.Type
import team.idealstate.glass.plugin.project.java.util.relocate.Relocator
import java.util.concurrent.atomic.AtomicBoolean

interface RelocatableVisitor {
    val relocators: List<Relocator>

    fun relocate(
        source: Any?,
        changed: AtomicBoolean,
    ): Any? {
        source ?: return null
        return when (source) {
            is String -> relocate(source, changed)
            is Type -> relocate(source, changed)
            is Handle -> relocate(source, changed)
            else -> source
        }
    }

    fun relocate(
        source: Type?,
        changed: AtomicBoolean,
    ): Type? {
        source ?: return null
        val internalName = source.internalName
        val relocate = relocate(internalName, changed)
        if (internalName != relocate && relocate != null) {
            if (relocate.contains("(") && relocate.contains(")")) {
                return Type.getMethodType(relocate)
            }
            return Type.getObjectType(relocate)
        }
        return source
    }

    fun relocate(
        source: Handle?,
        changed: AtomicBoolean,
    ): Handle? {
        source ?: return null
        val owner = source.owner
        val desc = source.desc
        val relocateOwner = relocate(owner, changed)
        val relocateDesc = relocate(desc, changed)
        if ((owner != relocateOwner && relocateOwner != null) || (desc != relocateDesc && relocateDesc != null)) {
            return Handle(source.tag, relocateOwner, source.name, relocateDesc, source.isInterface)
        }
        return source
    }

    fun relocate(
        source: String?,
        changed: AtomicBoolean,
    ): String? {
        source ?: return null
        var ret: String = source
        for (relocator in relocators) {
            ret = relocator.relocate(ret) ?: ret
        }
        if (source != ret) {
            changed.set(true)
        }
        return ret
    }

    fun relocate(
        source: Array<out Any?>?,
        changed: AtomicBoolean,
    ): Array<out Any?>? {
        source ?: return null
        source.isEmpty() && return source
        return source.map { relocate(it, changed) }.toTypedArray()
    }

    fun relocate(
        source: Array<out String?>?,
        changed: AtomicBoolean,
    ): Array<out String?>? {
        source ?: return null
        source.isEmpty() && return source
        return source.map { relocate(it, changed) }.toTypedArray()
    }
}
