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

import org.objectweb.asm.AnnotationVisitor
import team.idealstate.glass.plugin.project.java.util.relocate.Relocator
import java.util.concurrent.atomic.AtomicBoolean

class AnnotationRelocator(
    api: Int,
    writer: AnnotationVisitor?,
    override val relocators: List<Relocator>,
    private val changed: AtomicBoolean,
) : AnnotationVisitor(api, writer),
    RelocatableVisitor {
    override fun visit(
        name: String?,
        value: Any?,
    ) {
        super.visit(name, relocate(value, changed))
    }

    override fun visitEnum(
        name: String?,
        descriptor: String?,
        value: String?,
    ) {
        super.visitEnum(name, relocate(descriptor, changed), value)
    }

    override fun visitAnnotation(
        name: String?,
        descriptor: String?,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitAnnotation(name, relocate(descriptor, changed))
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitArray(name: String?): AnnotationVisitor {
        val annotationVisitor = super.visitArray(name)
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }
}
