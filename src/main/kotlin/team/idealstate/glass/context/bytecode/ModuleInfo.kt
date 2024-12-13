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

package team.idealstate.glass.context.bytecode

import team.idealstate.glass.context.bytecode.module.ModuleExportInfo
import team.idealstate.glass.context.bytecode.module.ModuleOpenInfo
import team.idealstate.glass.context.bytecode.module.ModuleProvideInfo
import team.idealstate.glass.context.bytecode.module.ModuleRequireInfo
import team.idealstate.glass.context.bytecode.module.ModuleUseInfo
import java.io.File

interface ModuleInfo {
    companion object {
        @JvmStatic
        fun of(
            release: Int,
            name: String,
            version: String?,
            open: Boolean,
            mainClass: String?,
        ): ModuleInfo = ModuleInfoImpl.of(release, name, version, open, mainClass)

        @JvmStatic
        fun of(file: File): ModuleInfo = ModuleInfoImpl.of(file)

        @JvmStatic
        fun merge(
            mainModuleInfo: ModuleInfo,
            vararg moduleInfos: ModuleInfo,
        ): ModuleInfo = ModuleInfoImpl.merge(mainModuleInfo, *moduleInfos)
    }

    val release: Int

    val annotations: List<AnnotationInfo>

    val name: String

    val open: Boolean

    val mainClass: String?

    val version: String?

    val requires: List<ModuleRequireInfo>

    val exports: List<ModuleExportInfo>

    val opens: List<ModuleOpenInfo>

    val provides: List<ModuleProvideInfo>

    val uses: List<ModuleUseInfo>

    fun compile(): ByteArray

    fun compileTo(file: File) {
        if (!file.exists()) {
            file.parentFile.mkdirs()
        }
        file.writeBytes(compile())
    }
}
