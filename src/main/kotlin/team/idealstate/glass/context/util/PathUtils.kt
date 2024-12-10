package team.idealstate.glass.context.util

object PathUtils {
    const val WINDOWS_PATH_DELIMITER = '\\'
    const val LINUX_DELIMITER = '/'
    const val NORMAL_DELIMITER = LINUX_DELIMITER

    @JvmStatic
    fun normalize(pathPart: String, vararg pathParts: String): String {
        if (pathParts.isEmpty()) return normalize(pathPart)
        val pathName = listOf(pathPart, *pathParts).joinToString(NORMAL_DELIMITER.toString()) {
            normalize(it).trim(NORMAL_DELIMITER)
        }
        return normalize(pathName)
    }

    @JvmStatic
    fun normalize(pathName: String): String {
        return pathName.replace(WINDOWS_PATH_DELIMITER, NORMAL_DELIMITER).trim(NORMAL_DELIMITER)
    }
}