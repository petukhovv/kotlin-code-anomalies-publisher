package org.jetbrains.kotlincodeanomaliespublisher.publishing

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlincodeanomaliespublisher.io.JsonFilesReader
import org.jetbrains.kotlincodeanomaliespublisher.structures.*
import org.kohsuke.github.GitHub
import java.io.File
import org.kohsuke.github.GHGist
import org.kohsuke.github.GHRepository
import java.util.*


class AnomaliesPublisher(private val anomalyType: AnomalyType, private val processMethod: ProcessMethod) {
    companion object {
        private const val REPO_ANOMALIES_SITE_IDENTIFIER = "PetukhovVictor/code-anomaly-detection"
        private const val REPO_ANOMALIES_BRANCH = "gh-pages"
        private const val ANOMALY_EXAMPLES_CONFIG_PATH = "assets/data/anomaly_examples.json"

        private const val ANOMALY_EXAMPLES_MODIFIED_CONFIG_FILE = "./anomaly_examples.json"

        private const val PUBLISH_PLACE = "https://gist.github.com/"
        private const val PUBLISHER_NAME = "Kotlin code anomaly publisher"
        private const val PUBLISHER_EMAIL = "no-reply@jetbrains.com"
        private const val COMMIT_MESSAGE = "Add new anomalies"
    }

    private val githubApi = GitHub.connect()
    private var anomaliesObject: MutableMap<String, AnomalyClass>? = null
    private var mapper = ObjectMapper()
    private var siteRepo: GHRepository? = null

    init {
        siteRepo = githubApi.getRepository(REPO_ANOMALIES_SITE_IDENTIFIER)
        when (processMethod) {
            ProcessMethod.APPEND -> {
                val file = siteRepo!!.getFileContent(ANOMALY_EXAMPLES_CONFIG_PATH, REPO_ANOMALIES_BRANCH)
                val response = khttp.get(file.downloadUrl)
                anomaliesObject = mapper.readValue(response.jsonObject.toString(), object: TypeReference<Map<String, AnomalyClass>>() {})
            }
            ProcessMethod.OVERWRITE -> {
                anomaliesObject = mutableMapOf()
            }
        }
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

    private fun prepareAnomalyExampleFiles(classInfo: AnomalyClassInfo, files: Set<Pair<File, Boolean>>, publisherName: String): AnomalyExampleFiles {
        val anomalyFiles: MutableMap<String, String> = mutableMapOf()
        files.forEach {
            if (it.second) {
                val gist = createGist(classInfo, it.first)
                val gistCommentsUrlComponents = gist.commentsUrl.split("/")
                val gistId = gistCommentsUrlComponents[gistCommentsUrlComponents.size - 2]

                anomalyFiles[it.first.name] = "$PUBLISH_PLACE$publisherName/$gistId"
            }
        }
        return AnomalyExampleFiles(anomalyFiles)
    }

    private fun checkExistAnomalyExample(anomalyExamples: MutableMap<String, AnomalyExample>, files: Set<Pair<File, Boolean>>): Boolean {
        anomalyExamples.forEach {
            it.value.items.forEach {
                val itemFiles = it.files

                files.forEach {
                    if (itemFiles.contains(it.first.name)) {
                        return@checkExistAnomalyExample true
                    }
                }
            }
        }

        return false
    }

    fun prepare(anomalyClass: String, classInfoFile: File, files: Set<Pair<File, Boolean>>, publisherName: String) {
        val anomalyClassInfoReference = object: TypeReference<AnomalyClassInfo>() {}
        val classInfo = JsonFilesReader.readFile<AnomalyClassInfo>(classInfoFile, anomalyClassInfoReference)

        if (!anomaliesObject!!.contains(anomalyClass)) {
            val anomalyExampleFiles = prepareAnomalyExampleFiles(classInfo, files, publisherName)
            val anomalyExample = AnomalyExample(1, classInfo.url, mutableListOf(anomalyExampleFiles))
            val anomalyClassObject = AnomalyClass(classInfo.title, mutableMapOf(anomalyType.toString() to anomalyExample))

            anomaliesObject!![anomalyClass] = anomalyClassObject
            println("ANOMALY ADDED (+ new class - $anomalyClass): $files")
        } else {
            val anomalyExamples = anomaliesObject!![anomalyClass]!!.examples
            if (anomalyExamples.contains(anomalyType.toString())) {
                if (!checkExistAnomalyExample(anomalyExamples, files)) {
                    val anomalyExampleFiles = prepareAnomalyExampleFiles(classInfo, files, publisherName)

                    anomalyExamples[anomalyType.toString()]!!.items.add(anomalyExampleFiles)
                    anomalyExamples[anomalyType.toString()]!!.total++
                    println("ANOMALY ADDED: $files")
                } else {
                    println("ANOMALY ALREADY EXIST: $files")
                }
            } else {
                val anomalyExampleFiles = prepareAnomalyExampleFiles(classInfo, files, publisherName)

                anomalyExamples[anomalyType.toString()] = AnomalyExample(
                        total = 1,
                        all_url = classInfo.url,
                        items = mutableListOf(anomalyExampleFiles)
                )
                println("ANOMALY ADDED (+ new type - $anomalyType): $files")
            }
        }
    }

    fun write() {
        val anomaliesObjectFormatted = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(anomaliesObject)

        File(ANOMALY_EXAMPLES_MODIFIED_CONFIG_FILE).writeText(anomaliesObjectFormatted)
    }

    fun publish() {
        // TODO: Automatic commit with website configuration in repo
        val anomaliesObjectFormatter = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(anomaliesObject)
        val commitBuilder = siteRepo!!.createCommit()
        commitBuilder.committer(PUBLISHER_NAME, PUBLISHER_EMAIL, Date())
        commitBuilder.message(COMMIT_MESSAGE)
        commitBuilder.create()
    }
}