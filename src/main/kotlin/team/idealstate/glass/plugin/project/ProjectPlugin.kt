package team.idealstate.glass.plugin.project

import org.gradle.api.Project
import team.idealstate.glass.plugin.Plugin

abstract class ProjectPlugin(id: String) : Plugin<Project>(id) {

    protected val project: Project
        get() = target
}