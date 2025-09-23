package team.idealstate.glass.extension

import org.gradle.api.Action
import kotlin.reflect.KClass

interface GlassExtension {

    fun <T: Any> with(type: KClass<T>, action: Action<T>) {
        with(type.java, action)
    }

    fun <T: Any> with(type: Class<T>, action: Action<T>)
}