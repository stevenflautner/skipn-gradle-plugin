import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.util.Date

plugins {
    kotlin("jvm") version "1.6.10"
    id("java-gradle-plugin")
//    id("maven")
    id("org.gradle.kotlin.kotlin-dsl") version "2.2.0"
    id("maven-publish")
//    id("com.jfrog.bintray") version "1.8.4"
}

//publishing {
//    repositories {
//        maven {
//            val releasesRepoUrl = "$buildDir/repos/releases"
//            val snapshotsRepoUrl = "$buildDir/repos/snapshots"
//            url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
//        }
//    }
//}

group = "io.skipn"
version = "0.0.30"

repositories {
    maven {
        url = uri("https://plugins.gradle.org/m2/")
    }

    val mavenUser: String by project
    val mavenPassword: String by project
    maven {
        url = uri("https://maven.pkg.jetbrains.space/nambda/p/tools/skipn")
        credentials {
            username = mavenUser
            password = mavenPassword
        }
    }
}

dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.10")
    implementation("org.jetbrains.kotlin:kotlin-serialization:1.6.10")
}
tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "11"
}

gradlePlugin {
    plugins {
        create("io.skipn.gradle.plugin") {
            id = "io.skipn.gradle.plugin"
            implementationClass = "io.skipn.gradle.SkipnPlugin"
        }
    }
}

// Publications

val artifactName = project.name
val artifactGroup = project.group.toString()
val artifactVersion = project.version.toString()

val bintrayRepo = "skipn"
val owner = "stevenflautner"
val packageName = "skipn-gradle-plugin"
val versionDescription = "Pre-release 0.0.1"
val license = "MIT"
val projVcsUrl = "https://github.com/stevenflautner/skipn-gradle-plugin.git"

//val sourcesJar by tasks.creating(Jar::class) {
//    archiveClassifier.set("sources")
//    from(sourceSets.getByName("main").allSource)
//}

//publishing {
//    publications {
//        create<MavenPublication>("skipn-gradle-plugin") {
//            groupId = artifactGroup
//            artifactId = artifactName
//            version = artifactVersion
//            from(components["java"])
//
//            artifact(sourcesJar)
//
////            pom.withXml {
////                asNode().apply {
////                    appendNode("description", pomDesc)
////                    appendNode("name", rootProject.name)
////                    appendNode("url", pomUrl)
////                    appendNode("licenses").appendNode("license").apply {
////                        appendNode("name", pomLicenseName)
////                        appendNode("url", pomLicenseUrl)
////                        appendNode("distribution", pomLicenseDist)
////                    }
////                    appendNode("developers").appendNode("developer").apply {
////                        appendNode("id", pomDeveloperId)
////                        appendNode("name", pomDeveloperName)
////                    }
////                    appendNode("scm").apply {
////                        appendNode("url", pomScmUrl)
////                    }
////                }
////            }
//        }
//
//        bintray {
//            user = "stevenflautner"
//            key = project.findProperty("bintrayKey").toString()
//            publish = true
//
//            setPublications("skipn-gradle-plugin")
//
//            pkg.apply {
//                repo = bintrayRepo
//                name = packageName
//                userOrg = "skipn"
//                setLicenses("MIT")
//                vcsUrl = projVcsUrl
//                version.apply {
//                    name = artifactVersion
//                    desc = versionDescription
//                    released = Date().toString()
//                    vcsTag = artifactVersion
//                }
//            }
//        }
//    }
//}

publishing {
//    publications {
////        create<MavenPublication>("maven") {
////            groupId = rootProject.group.toString()
////            artifactId = rootProject.name
////            version = version
////            from(components["java"])
////
//////            artifact(sourcesJar)
////        }
//    }

    val mavenUser: String by project
    val mavenPassword: String by project
    repositories {
        maven {
            url = uri("https://maven.pkg.jetbrains.space/nambda/p/tools/skipn-gradle-plugin")
            credentials {
                username = mavenUser
                password = mavenPassword
            }
        }
    }
}