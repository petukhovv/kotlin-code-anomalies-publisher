package org.jetbrains.kotlincodeanomaliespublisher.publishing

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.jetbrains.kotlincodeanomaliespublisher.io.FileWriter
import org.jetbrains.kotlincodeanomaliespublisher.io.JsonFilesReader
import org.jetbrains.kotlincodeanomaliespublisher.structures.*
import org.kohsuke.github.GitHub
import java.io.File
import org.kohsuke.github.GHGist
import org.kohsuke.github.GHRepository
import java.io.FileNotFoundException
import java.lang.System.exit
import java.util.*


class AnomaliesPublisher(private val anomalyType: AnomalyType, private val processMethod: ProcessMethod) {
    private val githubApi = GitHub.connect()
    private val repoAnomaliesSiteIdentifier = "PetukhovVictor/code-anomaly-detection"
    private val repoAnomaliesBranch = "gh-pages"
    private val anomalyExamplesConfigFile = "assets/data/anomaly_examples.json"
    private val anomalyExamplesModifiedConfigFile = "./anomaly_examples.json"
    private var anomaliesObject: MutableMap<String, AnomalyClass>? = null
    private var mapper = ObjectMapper()
    private var siteRepo: GHRepository? = null

    private val publisherName = "Kotlin code anomaly publisher"
    private val publisherEmail = "no-reply@jetbrains.com"
    private val commitMessage = "Add new anomalies"

    init {
        siteRepo = githubApi.getRepository(repoAnomaliesSiteIdentifier)
        when (processMethod) {
            ProcessMethod.APPEND -> {
                val file = siteRepo!!.getFileContent(anomalyExamplesConfigFile, repoAnomaliesBranch)
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

    private fun prepareAnomalyExampleFiles(classInfo: AnomalyClassInfo, files: Set<Pair<File, Boolean>>): AnomalyExampleFiles {
        val anomalyFiles: MutableMap<String, String> = mutableMapOf()
        files.forEach {
            if (it.second) {
                val gist = createGist(classInfo, it.first)
                val gistCommentsUrlComponents = gist.commentsUrl.split("/")

                anomalyFiles[it.first.name] = gistCommentsUrlComponents[gistCommentsUrlComponents.size - 2]
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

    fun prepare(anomalyClass: String, classInfoFile: File, files: Set<Pair<File, Boolean>>) {
        val anomalyClassInfoReference = object: TypeReference<AnomalyClassInfo>() {}
        val classInfo = JsonFilesReader.readFile<AnomalyClassInfo>(classInfoFile, anomalyClassInfoReference)

        if (!anomaliesObject!!.contains(anomalyClass)) {
            val anomalyExampleFiles = prepareAnomalyExampleFiles(classInfo, files)
            val anomalyExample = AnomalyExample(1, classInfo.url, mutableListOf(anomalyExampleFiles))
            val anomalyClassObject = AnomalyClass(classInfo.title, mutableMapOf(anomalyType.toString() to anomalyExample))

            anomaliesObject!![anomalyClass] = anomalyClassObject
            println("ANOMALY NOT EXIST (CLASS NOT EXIST): $files")
        } else {
            val anomalyExamples = anomaliesObject!![anomalyClass]!!.examples
            if (anomalyExamples.contains(anomalyType.toString())) {
                if (!checkExistAnomalyExample(anomalyExamples, files)) {
                    val anomalyExampleFiles = prepareAnomalyExampleFiles(classInfo, files)

                    anomalyExamples[anomalyType.toString()]!!.items.add(anomalyExampleFiles)
                    anomalyExamples[anomalyType.toString()]!!.total++
                    println("ANOMALY NOT EXIST (ANOMALY TYPE EXIST): $files")
                } else {
                    println("ANOMALY ALREADY EXIST: $files")
                }
            } else {
                val anomalyExampleFiles = prepareAnomalyExampleFiles(classInfo, files)

                anomalyExamples[anomalyType.toString()] = AnomalyExample(
                        total = 1,
                        all_url = classInfo.url,
                        items = mutableListOf(anomalyExampleFiles)
                )
                println("ANOMALY NOT EXIST (ANOMALY TYPE NOT EXIST): $files")
            }
        }
    }

    fun write() {
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