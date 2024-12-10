package team.idealstate.glass.context.util

object ClassUtils {

    const val CLASS_DELIMITER = '.'
    const val INTERNAL_DELIMITER = '/'
    const val CLASS_FILE_EXTENSION_NAME = ".class"
    const val MULTI_RELEASE_DIR_PATH_NAME = "META-INF/versions"
    const val LEAST_MULTI_RELEASE_VERSION = 9

    @JvmStatic
    fun internalize(classNamePart: String, vararg classNameParts: String): String {
        if (classNameParts.isEmpty()) return internalize(classNamePart)
        val className = listOf(classNamePart, *classNameParts).joinToString(INTERNAL_DELIMITER.toString()) {
            internalize(it).trim(INTERNAL_DELIMITER)
        }
        return internalize(className)
    }

    @JvmStatic
    fun normalize(internalNamePart: String, vararg internalNameParts: String): String {
        if (internalNameParts.isEmpty()) return normalize(internalNamePart)
        val internalName = listOf(internalNamePart, *internalNameParts).joinToString(CLASS_DELIMITER.toString()) {
            normalize(it).trim(CLASS_DELIMITER)
        }
        return normalize(internalName)
    }

    @JvmStatic
    fun internalize(className: String): String {
        return className.replace(CLASS_DELIMITER, INTERNAL_DELIMITER).trim(INTERNAL_DELIMITER)
    }

    @JvmStatic
    fun normalize(internalName: String): String {
        return internalName.replace(INTERNAL_DELIMITER, CLASS_DELIMITER).trim(CLASS_DELIMITER)
    }

    @JvmStatic
    fun maybeClassFile(fileName: String): Boolean {
        return fileName.endsWith(CLASS_FILE_EXTENSION_NAME)
    }
}