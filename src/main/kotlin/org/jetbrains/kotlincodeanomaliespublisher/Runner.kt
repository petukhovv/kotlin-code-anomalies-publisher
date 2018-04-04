package org.jetbrains.kotlincodeanomaliespublisher

import org.jetbrains.kotlincodeanomaliespublisher.io.DirectoryWalker
import org.jetbrains.kotlincodeanomaliespublisher.publishing.AnomaliesPublisher
import org.jetbrains.kotlincodeanomaliespublisher.structures.AnomalyType
import org.jetbrains.kotlincodeanomaliespublisher.structures.ProcessMethod
import java.io.File

object Runner {
    private fun publishBytecodeAnomalies(anomaliesFolder: String, processMethod: ProcessMethod, publisherName: String) {
        val anomaliesPublisher = AnomaliesPublisher(AnomalyType.BYTECODE, processMethod)

        try {
            DirectoryWalker(anomaliesFolder, "class").run {
                val anomalyClass = File(it.parent).name
                val classFile = it
                val classJsonFile = File("${it.absolutePath}.json")
                val className = it.nameWithoutExtension.split("$").first()
                val sourceFile = File("${it.parent}/$className.kt")
                val classInfo = File("${it.parent}/info.json")

                anomaliesPublisher.prepare(
                        anomalyClass,
                        classInfo,
                        setOf(Pair(classJsonFile, true), Pair(classFile, false), Pair(sourceFile, true)),
                        publisherName)
            }
        } catch (e: Exception) {
            println("Exception: $e")
        } finally {
            anomaliesPublisher.write()
        }
    }

    private fun publishCstAnomalies(anomaliesFolder: String, processMethod: ProcessMethod, publisherName: String) {
        val anomaliesPublisher = AnomaliesPublisher(AnomalyType.CST, processMethod)

        try {
            DirectoryWalker(anomaliesFolder, "kt").run {
                val anomalyClass = File(it.parent).name
                val classInfo = File("${it.parent}/info.json")

                println("!!!!!!!")
                anomaliesPublisher.prepare(anomalyClass, classInfo, setOf(Pair(it, true)), publisherName)
            }
        } catch (e: Exception) {
            println("Exception: $e")
        } finally {
            anomaliesPublisher.write()
        }
    }

    fun run(anomaliesFolder: String, type: AnomalyType, processMethod: ProcessMethod, publisherName: String) {
        when (type) {
            AnomalyType.BYTECODE -> publishBytecodeAnomalies(anomaliesFolder, processMethod, publisherName)
            AnomalyType.CST -> publishCstAnomalies(anomaliesFolder, processMethod, publisherName)
        }
    }
}