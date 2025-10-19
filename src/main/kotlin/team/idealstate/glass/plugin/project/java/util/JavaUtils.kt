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

package team.idealstate.glass.plugin.project.java.util

internal object JavaUtils {
    const val CLASS_DELIMITER = '.'
    const val INTERNAL_DELIMITER = '/'
    const val SOURCE_FILE_EXTENSION_NAME = ".java"
    const val CLASS_FILE_EXTENSION_NAME = ".class"
    const val MODULE_INFO_FILE_NAME = "module-info.class"
    const val PACKAGE_INFO_FILE_NAME = "package-info.class"
    const val MANIFEST_FILE_PATH_NAME = "META-INF/MANIFEST.MF"
    const val SERVICES_DIR_PATH_NAME = "META-INF/services/"
    const val MULTI_RELEASE_DIR_PATH_NAME = "META-INF/versions/"
    const val LEAST_MULTI_RELEASE_SUPPORTED_VERSION = 9
    const val DIFF_BETWEEN_VERSION_AND_RELEASE = 44

    fun toVersion(release: Int): Int = release + DIFF_BETWEEN_VERSION_AND_RELEASE

    fun toRelease(version: Int): Int = version - DIFF_BETWEEN_VERSION_AND_RELEASE

    @JvmStatic
    fun internalizeClassName(
        classNamePart: String,
        vararg classNameParts: String,
    ): String {
        if (classNameParts.isEmpty()) return internalizeClassName(classNamePart)
        val className =
            listOf(classNamePart, *classNameParts).joinToString(INTERNAL_DELIMITER.toString()) {
                internalizeClassName(it).trim(INTERNAL_DELIMITER)
            }
        return internalizeClassName(className)
    }

    @JvmStatic
    fun normalizeClassName(
        internalNamePart: String,
        vararg internalNameParts: String,
    ): String {
        if (internalNameParts.isEmpty()) return normalizeClassName(internalNamePart)
        val internalName =
            listOf(internalNamePart, *internalNameParts).joinToString(CLASS_DELIMITER.toString()) {
                normalizeClassName(it).trim(CLASS_DELIMITER)
            }
        return normalizeClassName(internalName)
    }

    @JvmStatic
    fun internalizeClassName(className: String): String = className.replace(CLASS_DELIMITER, INTERNAL_DELIMITER).trim(INTERNAL_DELIMITER)

    @JvmStatic
    fun normalizeClassName(internalName: String): String = internalName.replace(INTERNAL_DELIMITER, CLASS_DELIMITER).trim(CLASS_DELIMITER)

    @JvmStatic
    fun isMaybeSourceFile(fileName: String): Boolean = fileName.endsWith(SOURCE_FILE_EXTENSION_NAME)

    @JvmStatic
    fun isMaybeClassFile(fileName: String): Boolean = fileName.endsWith(CLASS_FILE_EXTENSION_NAME)

    @JvmStatic
    fun isMaybeModuleInfoFile(fileName: String): Boolean = fileName == MODULE_INFO_FILE_NAME

    @JvmStatic
    fun isMaybePackageInfoFile(fileName: String): Boolean = fileName == PACKAGE_INFO_FILE_NAME

    @JvmStatic
    fun isMaybeManifestFile(fileName: String): Boolean = fileName == MANIFEST_FILE_PATH_NAME

    @JvmStatic
    fun isMaybeServicesFile(fileName: String): Boolean = fileName.startsWith(SERVICES_DIR_PATH_NAME)
}
