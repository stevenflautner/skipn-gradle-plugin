package io.skipn.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.util.*

open class SkipnMeta : DefaultTask() {

    val fileName = "skipn_meta.json"
    val filePath by lazy {
        // Using buildDir instead of project.buildDir results
        // in a Error Inject() annotation required for constructor
        project.buildDir.path
    }
    val uuid: String by lazy {
        val uuid = UUID.randomUUID().toString()
        println("Skipn build meta id: $uuid")
        uuid
    }

    @TaskAction
    fun generateFile() {
        val file = File("$filePath/$fileName")
        if (!file.exists())
            file.createNewFile()

        file.writeText(uuid)
    }
}