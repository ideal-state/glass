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

package team.idealstate.glass.plugin.project.java.util.bytecode

import org.objectweb.asm.AnnotationVisitor
import org.objectweb.asm.Type
import java.util.LinkedList

class AnnotationInfoImpl(
    override val annotationType: ClassInfo,
    override val visible: Boolean,
) : AnnotationInfo {
    override val mappings: MutableMap<String, Any> = LinkedHashMap()

    internal abstract class AbstractVisitor(
        api: Int,
        visitor: AnnotationVisitor?,
    ) : AnnotationVisitor(api, visitor) {
        protected abstract fun put(
            name: String?,
            value: Any,
        )

        protected abstract val visible: Boolean

        override fun visit(
            name: String?,
            value: Any,
        ) {
            super.visit(name, value)
            if (value is Type) {
                put(name, ClassInfoImpl(value.internalName))
            } else {
                put(name, value)
            }
        }

        override fun visitEnum(
            name: String?,
            descriptor: String,
            value: String,
        ) {
            super.visitEnum(name, descriptor, value)
            put(
                name,
                EnumInfoImpl(
                    ClassInfoImpl(Type.getType(descriptor).internalName),
                    value,
                ),
            )
        }

        override fun visitAnnotation(
            name: String?,
            descriptor: String,
        ): AnnotationVisitor {
            val annotationVisitor = super.visitAnnotation(name, descriptor)
            val annotationInfo = AnnotationInfoImpl(ClassInfoImpl(Type.getType(descriptor).internalName), visible)
            put(name, annotationInfo)
            return Visitor(api, annotationVisitor, annotationInfo)
        }

        override fun visitArray(name: String?): AnnotationVisitor {
            val annotationVisitor = super.visitArray(name)
            val list = LinkedList<Any>()
            put(name, list)
            return ArrayVisitor(api, annotationVisitor, visible, list)
        }
    }

    internal class Visitor(
        api: Int,
        visitor: AnnotationVisitor?,
        private val annotationInfo: AnnotationInfoImpl,
    ) : AbstractVisitor(api, visitor) {
        override fun put(
            name: String?,
            value: Any,
        ) {
            name ?: return
            annotationInfo.mappings[name] = value
        }

        override val visible: Boolean
            get() = annotationInfo.visible
    }

    internal class ArrayVisitor(
        api: Int,
        annotationVisitor: AnnotationVisitor?,
        override val visible: Boolean,
        private val list: MutableList<Any>,
    ) : AbstractVisitor(api, annotationVisitor) {
        override fun put(
            name: String?,
            value: Any,
        ) {
            list.add(value)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as AnnotationInfoImpl

        if (visible != other.visible) return false
        if (annotationType != other.annotationType) return false
        if (mappings != other.mappings) return false

        return true
    }

    override fun hashCode(): Int {
        var result = visible.hashCode()
        result = 31 * result + annotationType.hashCode()
        result = 31 * result + mappings.hashCode()
        return result
    }

    override fun toString(): String = "AnnotationInfoImpl(annotationType=$annotationType, visible=$visible, mappings=$mappings)"
}
