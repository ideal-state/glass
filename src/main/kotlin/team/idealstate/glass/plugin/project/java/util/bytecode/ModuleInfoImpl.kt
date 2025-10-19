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
import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.ModuleVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import team.idealstate.glass.plugin.project.java.util.JavaUtils
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleExportInfo
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleExportInfoImpl
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleOpenInfo
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleOpenInfoImpl
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleProvideInfo
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleProvideInfoImpl
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleRequireInfo
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleRequireInfoImpl
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleUseInfo
import team.idealstate.glass.plugin.project.java.util.bytecode.module.ModuleUseInfoImpl
import java.io.File
import java.util.LinkedList

class ModuleInfoImpl(
    override var release: Int,
    override var name: String,
    override var version: String?,
    override var open: Boolean,
    override var mainClass: String?,
) : ModuleInfo {
    companion object {
        @JvmStatic
        fun of(
            release: Int,
            name: String,
            version: String?,
            open: Boolean,
            mainClass: String?,
        ): ModuleInfo = ModuleInfoImpl(release, name, version, open, mainClass)

        @JvmStatic
        fun of(file: File): ModuleInfoImpl {
            val moduleInfoClassVisitor = ModuleInfoClassVisitor(Opcodes.ASM9)
            file.inputStream().use { stream ->
                val classReader = ClassReader(stream)
                classReader.accept(moduleInfoClassVisitor, ClassReader.EXPAND_FRAMES)
            }
            return moduleInfoClassVisitor.moduleInfo
        }

        @JvmStatic
        fun merge(
            mainModuleInfo: ModuleInfo,
            vararg moduleInfos: ModuleInfo,
        ): ModuleInfoImpl {
            val queue = listOf(mainModuleInfo, *moduleInfos)
            val main =
                ModuleInfoImpl(
                    mainModuleInfo.release,
                    mainModuleInfo.name,
                    mainModuleInfo.version,
                    mainModuleInfo.open,
                    mainModuleInfo.mainClass,
                )
            val moduleNames = queue.map(ModuleInfo::name).toSet()
            for (moduleInfo in queue) {
                for (annotation in moduleInfo.annotations) {
                    main.annotation(annotation)
                }
                for (require in moduleInfo.requires) {
                    val requireModule = require.module
                    if (requireModule in moduleNames) {
                        continue
                    }
                    val requireTransient = require.transitive
                    val requireStatic = require.static
                    val requireVersion = require.version
                    main.require(requireTransient, requireStatic, requireModule, requireVersion)
                }
                for (export in moduleInfo.exports) {
                    val exportToModules = export.toModules
                    val exportModules = exportToModules.filter { it in moduleNames }
                    if (exportToModules.isNotEmpty() && exportModules.isEmpty()) {
                        continue
                    }
                    val exportPackageName = export.packageName
                    main.export(exportPackageName, exportModules.toTypedArray())
                }
                if (!main.open) {
                    for (open in moduleInfo.opens) {
                        val openToModules = open.toModules
                        val openModules = openToModules.filter { it in moduleNames }
                        if (openToModules.isNotEmpty() && openModules.isEmpty()) {
                            continue
                        }
                        val openPackageName = open.packageName
                        main.open(openPackageName, openModules.toTypedArray())
                    }
                }
                for (provide in moduleInfo.provides) {
                    val provideProviders = provide.withProviders
                    if (provideProviders.isEmpty()) {
                        continue
                    }
                    val provideService = provide.service
                    main.provide(provideService, provideProviders.toTypedArray())
                }
                for (use in moduleInfo.uses) {
                    val useService = use.service
                    main.use(useService)
                }
            }
            return main
        }
    }

    private val _annotations: MutableList<AnnotationInfo> = mutableListOf()
    override val annotations: List<AnnotationInfo>
        get() = LinkedList(_annotations)

    fun annotation(annotationInfo: AnnotationInfo) {
        _annotations.add(annotationInfo)
    }

    private val _requires: MutableSet<ModuleRequireInfo> =
        linkedSetOf(
            ModuleRequireInfoImpl(transitive = false, static = false, "java.base", null),
        )
    override val requires: List<ModuleRequireInfo>
        get() = LinkedList(_requires)

    fun require(
        transitive: Boolean,
        static: Boolean,
        module: String,
        version: String?,
    ) {
        _requires.add(ModuleRequireInfoImpl(transitive, static, module, version))
    }

    fun unrequire(module: String) {
        _requires.removeIf { it.module == module }
    }

    private val _exports: MutableSet<ModuleExportInfoImpl> = linkedSetOf()
    override val exports: List<ModuleExportInfo>
        get() = LinkedList(_exports)

    fun export(
        packageName: String,
        modules: Array<String>,
    ) {
        val moduleExportInfo = ModuleExportInfoImpl(packageName)
        modules.forEach(moduleExportInfo::to)
        _exports.add(moduleExportInfo)
    }

    fun unexport(packageName: String) {
        _exports.removeIf { it.packageName == packageName }
    }

    fun unexport(
        packageName: String,
        vararg modules: String,
    ) {
        modules.isEmpty() && return
        val moduleExportInfo = _exports.find { it.packageName == packageName } ?: return
        for (module in modules) {
            moduleExportInfo.unto(module)
        }
    }

    private val _opens: MutableSet<ModuleOpenInfoImpl> = linkedSetOf()
    override val opens: List<ModuleOpenInfo>
        get() = LinkedList(_opens)

    fun open(
        packageName: String,
        modules: Array<String>,
    ) {
        val moduleOpenInfo = ModuleOpenInfoImpl(packageName)
        modules.forEach(moduleOpenInfo::to)
        _opens.add(moduleOpenInfo)
    }

    fun unopen(packageName: String) {
        _opens.removeIf { it.packageName == packageName }
    }

    fun unopen(
        packageName: String,
        vararg modules: String,
    ) {
        modules.isEmpty() && return
        val moduleOpenInfo = _opens.find { it.packageName == packageName } ?: return
        for (module in modules) {
            moduleOpenInfo.unto(module)
        }
    }

    private val _provides: MutableSet<ModuleProvideInfoImpl> = linkedSetOf()
    override val provides: List<ModuleProvideInfo>
        get() = LinkedList(_provides)

    fun provide(
        service: String,
        providers: Array<String>,
    ) {
        val moduleProvideInfo = ModuleProvideInfoImpl(service)
        providers.forEach(moduleProvideInfo::with)
        _provides.add(moduleProvideInfo)
    }

    fun unprovide(service: String) {
        _provides.removeIf { it.service == service }
    }

    fun unprovide(
        service: String,
        vararg providers: String,
    ) {
        providers.isEmpty() && return
        val moduleProvideInfo = _provides.find { it.service == service } ?: return
        for (provider in providers) {
            moduleProvideInfo.unwith(provider)
        }
    }

    private val _uses: MutableSet<ModuleUseInfo> = linkedSetOf()
    override val uses: List<ModuleUseInfo>
        get() = LinkedList(_uses)

    fun use(service: String) {
        _uses.add(ModuleUseInfoImpl(service))
    }

    fun unuse(service: String) {
        _uses.removeIf { it.service == service }
    }

    private fun visitAnnotationMappings(
        annotationVisitor: AnnotationVisitor,
        key: String?,
        value: Any,
    ) {
        when (value) {
            is ClassInfo -> annotationVisitor.visit(key, Type.getObjectType(value.name))
            is EnumInfo -> {
                annotationVisitor.visitEnum(key, Type.getObjectType(value.enumType.name).descriptor, value.name)
            }
            is AnnotationInfo -> {
                val annotation =
                    annotationVisitor.visitAnnotation(
                        key,
                        Type.getObjectType(value.annotationType.name).descriptor,
                    )
                for ((annotationKey, annotationValue) in value.mappings) {
                    visitAnnotationMappings(annotation, annotationKey, annotationValue)
                }
                annotation.visitEnd()
            }
            is List<*> -> {
                val array = annotationVisitor.visitArray(key)
                for (element in value) {
                    element ?: continue
                    visitAnnotationMappings(array, null, element)
                }
                array.visitEnd()
            }
            else -> annotationVisitor.visit(key, value)
        }
    }

    override fun compile(): ByteArray {
        val classWriter = ClassWriter(0)
        var access = Opcodes.ACC_MODULE
        classWriter.visit(JavaUtils.toVersion(release), access, "module-info", null, null, null)

        for (annotation in annotations) {
            val annotationVisitor =
                classWriter.visitAnnotation(
                    Type.getObjectType(annotation.annotationType.name).descriptor,
                    annotation.visible,
                )
            for ((key, value) in annotation.mappings) {
                visitAnnotationMappings(annotationVisitor, key, value)
            }
            annotationVisitor.visitEnd()
        }

        val moduleVisitor = classWriter.visitModule(name, if (open) Opcodes.ACC_OPEN else 0, version)
        mainClass?.also { moduleVisitor.visitMainClass(JavaUtils.internalizeClassName(it)) }
        for (require in requires) {
            access = 0
            if (require.transitive) {
                access += Opcodes.ACC_TRANSITIVE
            }
            if (require.static) {
                access += Opcodes.ACC_STATIC_PHASE
            }
            moduleVisitor.visitRequire(require.module, access, require.version)
        }
        for (export in exports) {
            access = 0
            moduleVisitor.visitExport(export.packageName, access, *export.toModules.toTypedArray())
        }
        for (open in opens) {
            access = 0
            moduleVisitor.visitOpen(open.packageName, access, *open.toModules.toTypedArray())
        }
        for (provide in provides) {
            moduleVisitor.visitProvide(provide.service, *provide.withProviders.toTypedArray())
        }
        for (use in uses) {
            moduleVisitor.visitUse(use.service)
        }

        moduleVisitor.visitEnd()
        classWriter.visitEnd()
        return classWriter.toByteArray()
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ModuleInfoImpl

        if (release != other.release) return false
        if (open != other.open) return false
        if (name != other.name) return false
        if (version != other.version) return false
        if (_annotations != other._annotations) return false
        if (mainClass != other.mainClass) return false
        if (_requires != other._requires) return false
        if (_exports != other._exports) return false
        if (_opens != other._opens) return false
        if (_provides != other._provides) return false
        if (_uses != other._uses) return false

        return true
    }

    override fun hashCode(): Int {
        var result = release.hashCode()
        result = 31 * result + open.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + (version?.hashCode() ?: 0)
        result = 31 * result + _annotations.hashCode()
        result = 31 * result + (mainClass?.hashCode() ?: 0)
        result = 31 * result + _requires.hashCode()
        result = 31 * result + _exports.hashCode()
        result = 31 * result + _opens.hashCode()
        result = 31 * result + _provides.hashCode()
        result = 31 * result + _uses.hashCode()
        return result
    }

    override fun toString(): String =
        "ModuleInfoImpl(release=$release, name='$name', version=$version, open=$open, annotations=$_annotations, mainClass=$mainClass, requires=$_requires, exports=$_exports, opens=$_opens, provides=$_provides, uses=$_uses)"

    private class ModuleInfoVisitor(
        api: Int,
        visitor: ModuleVisitor?,
        private val moduleInfo: ModuleInfoImpl,
    ) : ModuleVisitor(api, visitor) {
        override fun visitMainClass(mainClass: String?) {
            super.visitMainClass(mainClass)
            moduleInfo.mainClass = mainClass
        }

        override fun visitRequire(
            module: String,
            access: Int,
            version: String?,
        ) {
            super.visitRequire(module, access, version)
            val isTransitive = access and Opcodes.ACC_TRANSITIVE != 0
            val isStatic = access and Opcodes.ACC_STATIC_PHASE != 0
            moduleInfo.require(isTransitive, isStatic, module, version)
        }

        override fun visitExport(
            packaze: String,
            access: Int,
            modules: Array<out String?>?,
        ) {
            if (modules == null || modules.isEmpty()) {
                super.visitExport(packaze, access)
                moduleInfo.export(packaze, emptyArray())
            } else {
                super.visitExport(packaze, access, *modules)
                moduleInfo.export(packaze, modules.filterNotNull().toTypedArray())
            }
        }

        override fun visitOpen(
            packaze: String,
            access: Int,
            modules: Array<out String?>?,
        ) {
            if (modules == null || modules.isEmpty()) {
                super.visitOpen(packaze, access)
                moduleInfo.open(packaze, emptyArray())
            } else {
                super.visitOpen(packaze, access, *modules)
                moduleInfo.open(packaze, modules.filterNotNull().toTypedArray())
            }
        }

        override fun visitUse(service: String) {
            super.visitUse(service)
            moduleInfo.use(service)
        }

        override fun visitProvide(
            service: String,
            providers: Array<out String?>?,
        ) {
            if (providers == null || providers.isEmpty()) {
                super.visitProvide(service)
                moduleInfo.provide(service, emptyArray())
            } else {
                super.visitProvide(service, *providers)
                moduleInfo.provide(service, providers.filterNotNull().toTypedArray())
            }
        }
    }

    private class ModuleInfoClassVisitor(
        api: Int,
    ) : ClassVisitor(api) {
        private var isEnd = false
        private var _moduleInfo: ModuleInfoImpl? = null
        val moduleInfo: ModuleInfoImpl
            get() {
                if (!isEnd) {
                    throw IllegalStateException("The moduleInfo have not yet been read.")
                }
                return _moduleInfo ?: throw IllegalStateException("The moduleInfo has not been read or does not exist.")
            }
        private var release: Int? = null
            get() = field ?: throw IllegalStateException("The moduleInfo class version has not been read or does not exist.")

        override fun visit(
            version: Int,
            access: Int,
            name: String?,
            signature: String?,
            superName: String?,
            interfaces: Array<out String>?,
        ) {
            super.visit(version, access, name, signature, superName, interfaces)
            this.release = JavaUtils.toRelease(version)
        }

        private val annotations = mutableListOf<AnnotationInfo>()

        override fun visitAnnotation(
            descriptor: String,
            visible: Boolean,
        ): AnnotationVisitor {
            val annotationVisitor = super.visitAnnotation(descriptor, visible)
            val annotationInfo = AnnotationInfoImpl(ClassInfoImpl(Type.getType(descriptor).internalName), visible)
            annotations.add(annotationInfo)
            return AnnotationInfoImpl.Visitor(api, annotationVisitor, annotationInfo)
        }

        override fun visitModule(
            name: String,
            access: Int,
            version: String?,
        ): ModuleVisitor {
            val moduleVisitor = super.visitModule(name, access, version)
            val isOpen = access and Opcodes.ACC_OPEN != 0
            val moduleInfo = ModuleInfoImpl(this.release!!, name, version, isOpen, null)
            this._moduleInfo = moduleInfo
            return ModuleInfoVisitor(api, moduleVisitor, moduleInfo)
        }

        override fun visitEnd() {
            super.visitEnd()
            val moduleInfo = this._moduleInfo ?: throw IllegalStateException("The moduleInfo does not exist.")
            annotations.forEach { moduleInfo.annotation(it) }
            this.isEnd = true
        }
    }
}
