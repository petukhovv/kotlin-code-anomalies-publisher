package org.jetbrains.kotlincodeanomaliespublisher

import org.jetbrains.kotlincodeanomaliespublisher.io.DirectoryWalker
import org.jetbrains.kotlincodeanomaliespublisher.publishing.AnomaliesPublisher
import org.jetbrains.kotlincodeanomaliespublisher.structures.AnomalyType
import java.io.File

object Runner {
    private fun publicBytecodeAnomalies(anomaliesFolder: String) {
        val anomaliesPublisher = AnomaliesPublisher(AnomalyType.BYTECODE)

        DirectoryWalker(anomaliesFolder, "class").run {
            val anomalyClass = File(it.parent).name
            val classFile = it
            val classJsonFile = File("${it.absolutePath}.json")
            val className = it.nameWithoutExtension.split("$").first()
            val sourceFile = File("${it.parent}/$className.kt")
            val classInfo = File("${it.parent}/info.json")

            anomaliesPublisher.prepare(anomalyClass, classInfo, setOf(Pair(classJsonFile, true), Pair(classFile, false), Pair(sourceFile, true)))
        }

        anomaliesPublisher.write()
    }

    private fun publicCstAnomalies(anomaliesFolder: String) {

    }

    fun run(anomaliesFolder: String, type: AnomalyType) {
        when (type) {
            AnomalyType.BYTECODE -> publicBytecodeAnomalies(anomaliesFolder)
            AnomalyType.CST -> publicCstAnomalies(anomaliesFolder)
        }
    }
}