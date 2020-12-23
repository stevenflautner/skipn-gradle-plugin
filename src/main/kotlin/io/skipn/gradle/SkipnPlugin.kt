package io.skipn.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.ApplicationPlugin
import org.gradle.api.plugins.ExtensionAware
import org.gradle.api.plugins.JavaApplication
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.*
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinJsCompilation
import org.jetbrains.kotlin.gradle.targets.js.npm.NpmProject
import org.jetbrains.kotlin.gradle.targets.js.npm.packageJson
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack
import org.jetbrains.kotlinx.serialization.gradle.SerializationGradleSubplugin
import java.io.File

class SkipnPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        with(project) {
            extensions.create<SkipnPluginExtension>("skipn")

            buildscript {
                repositories {
                    all(project)
                }
            }
            repositories {
                all(project)
            }
        }
    }

    fun extensionInitialized(project: Project, extension: SkipnPluginExtension) {
        with(project) {
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
                    val serverJar by tasks.getting(org.gradle.jvm.tasks.Jar::class) {
                        doFirst {
                            manifest {
                                attributes["Main-Class"] = "ServerKt"
                            }
                            from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
                        }
                    }
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

                    compilations["main"].packageJson {
                        customField("browserslist", arrayOf("last 2 versions"))
                    }

//                    val main by compilations.getting {
//                        packageJson {
//                            customField("browserslist", arrayOf("last 2 versions"))
////                                devDependencies += arrayOf(
////                                        "css-loader" to "3.2.0",
////                                        "mini-css-extract-plugin" to "0.8.0",
////                                )
//                        }
//                    }
//                    nodejs {
//                    }
                }

                val kversion = "1.4.2"

                sourceSets {
                    val commonMain by getting {
                        dependencies {
                            implementation("io.skipn:skipn:0.0.9952")
                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.0")
                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.0.1")
                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0.1")
//                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")

                            implementation("io.ktor:ktor-client-core:$kversion")
                            implementation("io.ktor:ktor-client-json:$kversion")
                            implementation("io.ktor:ktor-client-serialization:$kversion")
//                            implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.4.10")
                            implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.1.0")
//                            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.0-M1-1.4.0-rc")
                            extension.common.dependency?.invoke(this)
                        }
                    }
                    val browserMain by getting {
                        dependencies {
//                            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core-js:1.4.0")
                            implementation(devNpm("postcss-loader", "4.1.0"))
                            implementation(devNpm("postcss", "8.1.10"))
                            implementation(devNpm("raw-loader", ""))
                            implementation(npm("tailwindcss", "v2.0.1"))
//                            implementation(npm("css-loader", "5.0.1"))
//                            implementation(npm("style-loader", "2.0.0"))
                            implementation(npm("google-maps", "4.3.3"))
                            extension.browser.dependency?.invoke(this)
                        }
                    }
                    val serverMain by getting {
                        dependencies {
                            implementation("io.ktor:ktor-server-netty:$kversion")
//                            implementation("io.ktor:ktor-html-builder:$kversion")
                            implementation("io.ktor:ktor-serialization:$kversion")
                            extension.server.dependency?.invoke(this)
                        }
                    }

                    val commonTest by getting {
                        dependencies {
                            implementation(kotlin("test-common"))
                            implementation(kotlin("test-annotations-common"))
                        }
                    }
                    val serverTest by getting {
                        dependencies {
                            implementation(kotlin("test-junit"))
                        }
                    }
                    val browserTest by getting {
                        dependencies {
                            implementation(kotlin("test-js"))
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

//                manifest.apply {
//                    attributes["Main-Class"] = "ServerKt"
//                }

//                doFirst {
//                    from(configurations.getByName("runtimeClasspath").map { if (it.isDirectory) it else zipTree(it) })
//                }

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
    maven {
        url = project.uri("https://dl.bintray.com/skipn/skipn")
    }
}

fun Project.skipn(configure: SkipnPluginExtension.() -> Unit) {
    (this as ExtensionAware).extensions.configure(
        "skipn",
        configure
    )
    plugins.getAt(SkipnPlugin::class.java).extensionInitialized(
        this,
        extensions.getByType()
    )
}