package org.gradle.kotlin.dsl

import com.diffplug.gradle.spotless.KotlinGradleExtension
import com.diffplug.gradle.spotless.SpotlessExtension
import org.gradle.api.Action

fun SpotlessExtension.gradle(action: Action<KotlinGradleExtension>) {
    format("gradle", KotlinGradleExtension::class.java,action)
}
