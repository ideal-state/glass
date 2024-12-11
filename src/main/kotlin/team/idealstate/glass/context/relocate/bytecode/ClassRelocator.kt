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

package team.idealstate.glass.context.relocate.bytecode

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Attribute
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.FieldVisitor
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.ModuleVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.RecordComponentVisitor
import org.objectweb.asm.TypePath
import team.idealstate.glass.context.relocate.Relocator
import team.idealstate.glass.context.util.ClassUtils
import java.io.File

class ClassRelocator(
    api: Int,
    writer: ClassWriter,
    override val relocators: List<Relocator>,
) : ClassVisitor(api, writer),
    RelocatableVisitor {
    companion object {
        @JvmStatic
        fun relocate(
            file: File,
            relocators: List<Relocator>,
        ) {
            if (!ClassUtils.maybeClassFile(file.name)) return
            val classWriter = ClassWriter(0)
            val classRelocator = ClassRelocator(Opcodes.ASM9, classWriter, relocators)
            file.inputStream().use {
                val classReader = ClassReader(it)
                classReader.accept(classRelocator, ClassReader.EXPAND_FRAMES)
            }
            file.writeBytes(classWriter.toByteArray())
        }
    }

    override fun visit(
        version: Int,
        access: Int,
        name: String?,
        signature: String?,
        superName: String?,
        interfaces: Array<out String>?,
    ) {
        super.visit(version, access, relocate(name), relocate(signature), relocate(superName), relocate(interfaces))
    }

    override fun visitSource(
        source: String?,
        debug: String?,
    ) {
        super.visitSource(relocate(source), relocate(debug))
    }

    override fun visitModule(
        name: String?,
        access: Int,
        version: String?,
    ): ModuleVisitor {
        val moduleVisitor = super.visitModule(name, access, version)
        return ModuleRelocator(api, moduleVisitor, relocators)
    }

    override fun visitNestHost(nestHost: String?) {
        super.visitNestHost(relocate(nestHost))
    }

    override fun visitOuterClass(
        owner: String?,
        name: String?,
        descriptor: String?,
    ) {
        super.visitOuterClass(relocate(owner), name, relocate(descriptor))
    }

    override fun visitAnnotation(
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor = super.visitAnnotation(relocate(descriptor), visible)

    override fun visitTypeAnnotation(
        typeRef: Int,
        typePath: TypePath?,
        descriptor: String?,
        visible: Boolean,
    ): AnnotationVisitor {
        val annotationVisitor = super.visitTypeAnnotation(typeRef, typePath, relocate(descriptor), visible)
        return AnnotationRelocator(api, annotationVisitor, relocators)
    }

    override fun visitAttribute(attribute: Attribute?) {
        super.visitAttribute(attribute)
    }

    override fun visitNestMember(nestMember: String?) {
        super.visitNestMember(relocate(nestMember))
    }

    override fun visitPermittedSubclass(permittedSubclass: String?) {
        super.visitPermittedSubclass(relocate(permittedSubclass))
    }

    override fun visitInnerClass(
        name: String?,
        outerName: String?,
        innerName: String?,
        access: Int,
    ) {
        super.visitInnerClass(relocate(name), relocate(outerName), relocate(innerName), access)
    }

    override fun visitRecordComponent(
        name: String?,
        descriptor: String?,
        signature: String?,
    ): RecordComponentVisitor {
        val recordComponentVisitor = super.visitRecordComponent(name, relocate(descriptor), relocate(signature))
        return RecordComponentRelocator(api, recordComponentVisitor, relocators)
    }

    override fun visitField(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        value: Any?,
    ): FieldVisitor {
        val fieldVisitor = super.visitField(access, name, relocate(descriptor), relocate(signature), relocate(value))
        return FieldRelocator(api, fieldVisitor, relocators)
    }

    override fun visitMethod(
        access: Int,
        name: String?,
        descriptor: String?,
        signature: String?,
        exceptions: Array<out String>?,
    ): MethodVisitor {
        val methodVisitor = super.visitMethod(access, name, relocate(descriptor), relocate(signature), relocate(exceptions))
        return MethodRelocator(api, methodVisitor, relocators)
    }
}
