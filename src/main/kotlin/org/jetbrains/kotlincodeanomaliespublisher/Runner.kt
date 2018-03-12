package org.jetbrains.kotlincodeanomaliespublisher

import org.jetbrains.kotlincodeanomaliespublisher.io.DirectoryWalker
import org.jetbrains.kotlincodeanomaliespublisher.publishing.AnomaliesPublisher
import org.jetbrains.kotlincodeanomaliespublisher.structures.AnomalyType
import org.jetbrains.kotlincodeanomaliespublisher.structures.ProcessMethod
import java.io.File

object Runner {
    private fun publishBytecodeAnomalies(anomaliesFolder: String, processMethod: ProcessMethod) {
        val anomaliesPublisher = AnomaliesPublisher(AnomalyType.BYTECODE, processMethod)

        try {
            DirectoryWalker(anomaliesFolder, "class").run {
                val anomalyClass = File(it.parent).name
                val classFile = it
                val classJsonFile = File("${it.absolutePath}.json")
                val className = it.nameWithoutExtension.split("$").first()
                val sourceFile = File("${it.parent}/$className.kt")
                val classInfo = File("${it.parent}/info.json")

                anomaliesPublisher.prepare(anomalyClass, classInfo, setOf(Pair(classJsonFile, true), Pair(classFile, false), Pair(sourceFile, true)))
            }
        } catch (e: Exception) {
            println("Exception: $e")
        } finally {
            anomaliesPublisher.write()
        }
    }

    private fun publishCstAnomalies(anomaliesFolder: String, processMethod: ProcessMethod) {

    }

    fun run(anomaliesFolder: String, type: AnomalyType, processMethod: ProcessMethod) {
        when (type) {
            AnomalyType.BYTECODE -> publishBytecodeAnomalies(anomaliesFolder, processMethod)
            AnomalyType.CST -> publishCstAnomalies(anomaliesFolder, processMethod)
        }
    }
}