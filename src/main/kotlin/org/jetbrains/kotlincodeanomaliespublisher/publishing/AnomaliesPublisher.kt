package org.jetbrains.kotlincodeanomaliespublisher.publishing

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlincodeanomaliespublisher.io.FileWriter
import org.jetbrains.kotlincodeanomaliespublisher.io.JsonFilesReader
import org.jetbrains.kotlincodeanomaliespublisher.structures.*
import org.json.JSONObject
import org.kohsuke.github.GitHub
import java.io.File
import java.lang.System.exit
import org.kohsuke.github.GHGist
import org.kohsuke.github.GHRepository
import java.util.*


class AnomaliesPublisher(private val anomalyType: AnomalyType) {
    private val githubApi = GitHub.connect()
    private val repoAnomaliesSiteIdentifier = "PetukhovVictor/code-anomaly-detection"
    private val repoAnomaliesBranch = "gh-pages"
    private val anomalyExamplesConfigFile = "assets/data/anomaly_examples.json"
    private val anomalyExamplesModifiedConfigFile = "./anomaly_examples.json"
    private var anomaliesObject: JSONObject? = null
    private var mapper = ObjectMapper()
    private var siteRepo: GHRepository? = null

    private val publisherName = "Kotlin code anomaly publisher"
    private val publisherEmail = "no-reply@jetbrains.com"
    private val commitMessage = "Add new anomalies"

    init {
        siteRepo = githubApi.getRepository(repoAnomaliesSiteIdentifier)
        val file = siteRepo!!.getFileContent(anomalyExamplesConfigFile, repoAnomaliesBranch)
        val response = khttp.get(file.downloadUrl)
        anomaliesObject = response.jsonObject
    }

    private fun createGist(classInfo: AnomalyClassInfo, file: File): GHGist {
        val fileContentFormatted: String
        if (file.extension == "json") {
            val fileContent = mapper.readValue(file.readText(), Any::class.java)
            fileContentFormatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(fileContent)
        } else {
            fileContentFormatted = file.readText()
        }

        val gistBuilder = githubApi.createGist()
        gistBuilder.description("CAD: ${classInfo.title} on ${anomalyType.anomalyTypeDescription}\n")
        gistBuilder.file(file.name, fileContentFormatted)
        gistBuilder.public_(true)
        return gistBuilder.create()
    }

    fun prepare(anomalyClass: String, classInfoFile: File, files: Set<Pair<File, Boolean>>) {
        if (anomaliesObject!!.isNull(anomalyClass)) {
            val anomalyClassInfoReference = object: TypeReference<AnomalyClassInfo>() {}
            val classInfo = JsonFilesReader.readFile<AnomalyClassInfo>(classInfoFile, anomalyClassInfoReference)
            val anomalyFiles: MutableMap<String, String> = mutableMapOf()

            files.forEach {
                if (it.second) {
                    anomalyFiles[it.first.name] = createGist(classInfo, it.first).toString()
                }
            }
            val anomalyExampleFiles = AnomalyExampleFiles(anomalyFiles)
            val anomalyExample = AnomalyExample(1, classInfo.url, listOf(anomalyExampleFiles))
            val anomalyClassObject = AnomalyClass(classInfo.title, mapOf(anomalyType to anomalyExample))

            anomaliesObject!!.put(anomalyClass, JSONObject(anomalyClassObject))
        } else {
            val anomalyFiles: MutableMap<String, String> = mutableMapOf()
            files.forEach {
                if (it.second) {
                    anomalyFiles[it.first.name] = createGist(AnomalyClassInfo(), it.first).toString()
                }
            }
            val anomalyExampleFiles = AnomalyExampleFiles(anomalyFiles)
            val anomalyExample = AnomalyExample(1, classInfo.url, listOf(anomalyExampleFiles))
        }

        exit(0)
    }

    fun write() {
        println(anomaliesObject)
        FileWriter.write(anomalyExamplesModifiedConfigFile, anomaliesObject!!)
    }

    fun publish() {
        val anomaliesObjectFormatter = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(anomaliesObject)
        val commitBuilder = siteRepo!!.createCommit()
        commitBuilder.committer(publisherName, publisherEmail, Date())
        commitBuilder.message(commitMessage)
        commitBuilder.create()
    }
}