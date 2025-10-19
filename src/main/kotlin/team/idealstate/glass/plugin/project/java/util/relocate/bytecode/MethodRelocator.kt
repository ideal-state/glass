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
import org.objectweb.asm.Attribute
import org.objectweb.asm.Handle
import org.objectweb.asm.Label
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.TypePath
import team.idealstate.glass.plugin.project.java.util.relocate.Relocator
import java.util.concurrent.atomic.AtomicBoolean

class MethodRelocator(
    api: Int,
    writer: MethodVisitor?,
    override val relocators: List<Relocator>,
    private val changed: AtomicBoolean,
) : MethodVisitor(api, writer),
    RelocatableVisitor {
    override fun visitParameter(
        name: String?,
        access: Int,
    ) {
        super.visitParameter(name, access)
    }

    override fun visitAnnotationDefault(): AnnotationVisitor {
        val annotationVisitor = super.visitAnnotationDefault()
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitAnnotation(
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitAnnotation(relocate(descriptor, changed), visible)
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitTypeAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitTypeAnnotation(typeRef, typePath, relocate(descriptor, changed), visible)
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitAnnotableParameterCount(
        parameterCount: Int,
        visible: Boolean,
    ) {
        super.visitAnnotableParameterCount(parameterCount, visible)
    }

    override fun visitParameterAnnotation(
        parameter: Int,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitParameterAnnotation(parameter, relocate(descriptor, changed), visible)
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitAttribute(attribute: Attribute?) {
        super.visitAttribute(attribute)
    }

    override fun visitCode() {
        super.visitCode()
    }

    override fun visitFrame(
        type: Int,
        numLocal: Int,
        local: Array<out Any>?,
        numStack: Int,
        stack: Array<out Any>?,
    ) {
        super.visitFrame(type, numLocal, relocate(local, changed), numStack, relocate(stack, changed))
    }

    override fun visitInsn(opcode: Int) {
        super.visitInsn(opcode)
    }

    override fun visitIntInsn(
        opcode: Int,
        operand: Int,
    ) {
        super.visitIntInsn(opcode, operand)
    }

    override fun visitVarInsn(
        opcode: Int,
        varIndex: Int,
    ) {
        super.visitVarInsn(opcode, varIndex)
    }

    override fun visitTypeInsn(
        opcode: Int,
        type: String?,
    ) {
        super.visitTypeInsn(opcode, relocate(type, changed))
    }

    override fun visitFieldInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
    ) {
        super.visitFieldInsn(opcode, relocate(owner, changed), name, relocate(descriptor, changed))
    }

    @Suppress("OVERRIDE_DEPRECATION", "DEPRECATION")
    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
    ) {
        super.visitMethodInsn(opcode, relocate(owner, changed), name, relocate(descriptor, changed))
    }

    override fun visitMethodInsn(
        opcode: Int,
        owner: String?,
        name: String?,
        descriptor: String?,
        isInterface: Boolean,
    ) {
        super.visitMethodInsn(opcode, relocate(owner, changed), name, relocate(descriptor, changed), isInterface)
    }

    override fun visitInvokeDynamicInsn(
        name: String?,
        descriptor: String?,
        bootstrapMethodHandle: Handle?,
        bootstrapMethodArguments: Array<out Any?>?,
    ) {
        if (bootstrapMethodArguments == null || bootstrapMethodArguments.isEmpty()) {
            super.visitInvokeDynamicInsn(name, relocate(descriptor, changed), relocate(bootstrapMethodHandle, changed))
        } else {
            super.visitInvokeDynamicInsn(
                name,
                relocate(descriptor, changed),
                relocate(bootstrapMethodHandle, changed),
                *relocate(bootstrapMethodArguments, changed)!!,
            )
        }
    }

    override fun visitJumpInsn(
        opcode: Int,
        label: Label?,
    ) {
        super.visitJumpInsn(opcode, label)
    }

    override fun visitLabel(label: Label?) {
        super.visitLabel(label)
    }

    override fun visitLdcInsn(value: Any?) {
        super.visitLdcInsn(relocate(value, changed))
    }

    override fun visitIincInsn(
        varIndex: Int,
        increment: Int,
    ) {
        super.visitIincInsn(varIndex, increment)
    }

    override fun visitTableSwitchInsn(
        min: Int,
        max: Int,
        dflt: Label?,
        labels: Array<out Label?>?,
    ) {
        if (labels == null || labels.isEmpty()) {
            super.visitTableSwitchInsn(min, max, dflt)
        } else {
            super.visitTableSwitchInsn(min, max, dflt, *labels)
        }
    }

    override fun visitLookupSwitchInsn(
        dflt: Label?,
        keys: IntArray?,
        labels: Array<out Label>?,
    ) {
        super.visitLookupSwitchInsn(dflt, keys, labels)
    }

    override fun visitMultiANewArrayInsn(
        descriptor: String?,
        numDimensions: Int,
    ) {
        super.visitMultiANewArrayInsn(relocate(descriptor, changed), numDimensions)
    }

    override fun visitInsnAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitInsnAnnotation(typeRef, typePath, relocate(descriptor, changed), visible)
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitTryCatchBlock(
        start: Label?,
        end: Label?,
        handler: Label?,
        type: String?,
    ) {
        super.visitTryCatchBlock(start, end, handler, relocate(type, changed))
    }

    override fun visitTryCatchAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitTryCatchAnnotation(typeRef, typePath, relocate(descriptor, changed), visible)
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitLocalVariable(
        name: String?,
        descriptor: String?,
        signature: String?,
        start: Label?,
        end: Label?,
        index: Int,
    ) {
        super.visitLocalVariable(name, relocate(descriptor, changed), relocate(signature, changed), start, end, index)
    }

    override fun visitLocalVariableAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        start: Array<out Label>?,
        end: Array<out Label>?,
        index: IntArray?,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor =
            super.visitLocalVariableAnnotation(
                typeRef,
                typePath,
                start,
                end,
                index,
                relocate(descriptor, changed),
                visible,
            )
        return AnnotationRelocator(api, annotationVisitor, relocators, changed)
    }

    override fun visitLineNumber(
        line: Int,
        start: Label?,
    ) {
        super.visitLineNumber(line, start)
    }

    override fun visitMaxs(
        maxStack: Int,
        maxLocals: Int,
    ) {
        super.visitMaxs(maxStack, maxLocals)
    }
}
