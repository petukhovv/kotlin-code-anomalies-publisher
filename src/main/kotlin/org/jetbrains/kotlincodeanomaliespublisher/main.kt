package org.jetbrains.kotlincodeanomaliespublisher

import com.xenomachina.argparser.ArgParser
import org.jetbrains.kotlincodeanomaliespublisher.structures.AnomalyType


fun main(args : Array<String>) {
    val parser = ArgParser(args)
    val anomaliesFolder by parser.storing("-d", "--directory", help="path to folder with grouped anomaly source code files")
    val type by parser.mapping("--cst" to AnomalyType.CST, "--bytecode" to AnomalyType.BYTECODE, help = "anomalies type (--cst or --bytecode)")

    Runner.run(anomaliesFolder, type)
}