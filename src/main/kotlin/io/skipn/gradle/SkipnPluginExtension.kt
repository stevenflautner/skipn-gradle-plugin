package io.skipn.gradle

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.jetbrains.kotlin.gradle.plugin.KotlinDependencyHandler

open class SkipnPluginExtension {
    val common = SkipnSource()
    val browser = SkipnSource()
    val server = SkipnSource()
}

open class SkipnSource() {
    var dependency: (KotlinDependencyHandler.() -> Unit)? = null
}