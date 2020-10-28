package io.skipn.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.attributes.Attribute
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.JavaExec
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJvmCompilation
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import java.io.File
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import java.util.jar.Manifest
import org.gradle.kotlin.dsl.accessors.runtime.*
import org.jetbrains.kotlin.serialization.KotlinSerializerExtensionBase
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin

class SkipnPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            buildscript {
                repositories {
                    all(project)
                }
            }
            repositories {
                all(project)
            }

            plugins.apply(ApplicationPlugin::class.java)
            plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
            plugins.apply(SerializationGradleSubplugin::class.java)

            configureApplication {
                mainClassName = "ServerKt"
            }

            configureMultiplatform {
                jvm("server") {
                    compilations.all {
                        kotlinOptions.jvmTarget = "1.8"
                    }
                    withJava()
                }
                js("browser") {
                    useCommonJs()

                    browser {
                        binaries.executable()
                        webpackTask {
                            cssSupport.enabled = true
                            if (mode == org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION) {
                                cssSupport.mode = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.EXTRACT
                            }
                        }
                        runTask {
                            cssSupport.enabled = true
                            if (mode == org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.Mode.PRODUCTION) {
                                cssSupport.mode = org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackCssMode.EXTRACT
                            }
                        }
                        testTask {
                            useKarma {
                                useChromeHeadless()
                                webpackConfig.cssSupport.enabled = true
                            }
                        }
                    }
                }

                val kversion = "1.4.1"

                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            implementation("io.skipn:skipn:0.0.1b")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.9")
                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")

                            implementation("io.ktor:ktor-client-core:$kversion")
                            implementation("io.ktor:ktor-client-json:$kversion")
                            implementation("io.ktor:ktor-client-serialization:$kversion")
                            implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.4.10")
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
//                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0-M1-1.4.0-rc")
                        }
                    }
                    val browserMain by getting {
                        dependencies {
                            implementation(devNpm("postcss-loader", "4.0.0"))
                            implementation(devNpm("postcss", "7.0.32"))
                            implementation(devNpm("raw-loader", ""))
                            implementation(npm("tailwindcss", ""))
                            implementation(npm("css-loader", "3.4.2"))
                            implementation(npm("style-loader", "1.1.3"))
                            implementation(npm("google-maps", "4.3.3"))
                        }
                    }
                    val serverMain by getting {
                        dependencies {
                            implementation("io.ktor:ktor-server-netty:$kversion")
                            implementation("io.ktor:ktor-html-builder:$kversion")
                            implementation("io.ktor:ktor-serialization:$kversion")
                        }
                    }
                }
            }

            tasks.register("generateSkipnMeta", SkipnMeta::class.java)
            val generateSkipnMeta = tasks.getByName<SkipnMeta>("generateSkipnMeta")

            val browserBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("browserBrowserProductionWebpack") {
                outputFileName = "output.js"
            }

            tasks.getByName<Jar>("serverJar") {
                dependsOn(
                    tasks.getByName("browserBrowserProductionWebpack"),
                    tasks.getByName("generateSkipnMeta")
                )

                manifest.apply {
                    attributes["Main-Class"] = "ServerKt"
                }

                doFirst {
                    from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
                }

                from(File(browserBrowserProductionWebpack.destinationDirectory, browserBrowserProductionWebpack.outputFileName)) {
                    rename {
                        "${generateSkipnMeta.uuid}.js"
                    }
                    into("public")
                }
                from(File(browserBrowserProductionWebpack.destinationDirectory, "main.css")) {
                    rename {
                        "${generateSkipnMeta.uuid}.css"
                    }
                    into("public")
                }
                from(File(generateSkipnMeta.filePath, generateSkipnMeta.fileName))
            }

            tasks.getByName<JavaExec>("run") .apply {
                dependsOn(tasks.getByName("serverJar"))
                classpath(tasks.getByName("serverJar"))
            }
        }
    }

    fun Project.configureMultiplatform(configure: KotlinMultiplatformExtension.() -> Unit) {
        (this as ExtensionAware).extensions.configure(
            "kotlin",
            configure
        )
    }

    fun Project.configureApplication(configure: JavaApplication.() -> Unit): Unit {
        (this as ExtensionAware).extensions.configure("application", configure)
    }
}

// Providing most repositories so that a beginner doesn't
// have to worry about missing repositories
private fun RepositoryHandler.all(project: Project) {
    mavenCentral()
    jcenter()
    mavenLocal()
    maven {
        url = project.uri("https://plugins.gradle.org/m2/")
    }
    maven {
        url = project.uri("https://dl.bintray.com/kotlin/ktor")
    }
    maven {
        url = project.uri("https://dl.bintray.com/kotlin/kotlinx")
    }
    maven {
        url = project.uri("https://jitpack.io")
    }
//    maven {
//        url = project.uri("https://dl.bintray.com/skipn/skipn")
//    }
}
