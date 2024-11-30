@file:Suppress("unused", "UnusedReceiverParameter")

import org.gradle.api.initialization.Settings
import java.io.File

fun Settings.includeModules(root: String = "") {
    fun findBuildScripts(modulesDirectory: File, deep: Boolean = true): List<File> {
        val moduleIds = mutableListOf<File>()
        for (file in modulesDirectory.listFiles()!!) {
            val filename = file.name
            if (filename == "buildSrc") {
                continue
            }
            if (file.isDirectory) {
                if (deep) {
                    moduleIds.addAll(findBuildScripts(file))
                }
            } else if (filename == "build.gradle" || filename == "build.gradle.kts") {
                if (file.parentFile == rootProject.projectDir) {
                    continue
                }
                moduleIds.add(file)
            }
        }
        return moduleIds
    }

    val rootPath = root
        .replace("\\", "/")
        .replace(":", "/")

    val modulesDirectory = File(rootProject.projectDir, rootPath)
    if (!modulesDirectory.exists()) {
        throw IllegalStateException("Modules directory is not exists.")
    }
    if (!modulesDirectory.isDirectory) {
        throw IllegalStateException("Modules directory file must be a directory.")
    }
    println("\n> Modules: \n> Root Dir: $modulesDirectory")
    val buildScripts = findBuildScripts(modulesDirectory)
    val prefixLength = rootProject.projectDir.absolutePath.length
    var count = 0
    buildScripts.forEach {
        val moduleId = it.parentFile.absolutePath
            .substring(prefixLength).replace('\\', ':').replace('/', ':')
        if (moduleId.isBlank() || moduleId == ":") {
            return@forEach
        }
        println(">> including $moduleId ....")
        if (findProject(it.parentFile) != null) {
            throw IllegalStateException("Module $moduleId already exists.")
        }
        include(moduleId)
        val project = project(moduleId)

        project.name = "${rootProject.name}${moduleId.substring(rootPath.length)}"
            .replace(':', '-')
        println(">> included $moduleId (${project.name})")
        count++
    }
    if (count == 0) {
        println(">> No modules include.")
    } else {
        println(">> $count modules include.")
    }
    println()
}
