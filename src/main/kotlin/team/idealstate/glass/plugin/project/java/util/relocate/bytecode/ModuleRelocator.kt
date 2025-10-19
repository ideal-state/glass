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

import org.objectweb.asm.ModuleVisitor
import team.idealstate.glass.plugin.project.java.util.relocate.Relocator
import java.util.concurrent.atomic.AtomicBoolean

class ModuleRelocator(
    api: Int,
    writer: ModuleVisitor?,
    override val relocators: List<Relocator>,
    private val changed: AtomicBoolean,
) : ModuleVisitor(api, writer),
    RelocatableVisitor {
    override fun visitMainClass(mainClass: String?) {
        super.visitMainClass(relocate(mainClass, changed))
    }

    override fun visitPackage(packaze: String?) {
        super.visitPackage(relocate(packaze, changed))
    }

    override fun visitRequire(
        module: String?,
        access: Int,
        version: String?,
    ) {
        super.visitRequire(module, access, version)
    }

    override fun visitExport(
        packaze: String?,
        access: Int,
        modules: Array<out String?>?,
    ) {
        if (modules == null || modules.isEmpty()) {
            super.visitExport(relocate(packaze, changed), access)
        } else {
            super.visitExport(relocate(packaze, changed), access, *modules)
        }
    }

    override fun visitOpen(
        packaze: String?,
        access: Int,
        modules: Array<out String?>?,
    ) {
        if (modules == null || modules.isEmpty()) {
            super.visitOpen(relocate(packaze, changed), access)
        } else {
            super.visitOpen(relocate(packaze, changed), access, *modules)
        }
    }

    override fun visitUse(service: String?) {
        super.visitUse(relocate(service, changed))
    }

    override fun visitProvide(
        service: String?,
        providers: Array<out String?>?,
    ) {
        if (providers == null || providers.isEmpty()) {
            super.visitProvide(relocate(service, changed))
        } else {
            super.visitProvide(relocate(service, changed), *relocate(providers, changed)!!)
        }
    }
}
