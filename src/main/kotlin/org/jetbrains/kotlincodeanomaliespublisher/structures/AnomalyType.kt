package org.jetbrains.kotlincodeanomaliespublisher.structures

enum class AnomalyType(private val anomalyTypeName: String, val anomalyTypeDescription: String) {
    CST("cst", "CST"),
    BYTECODE("bytecode", "JVM-bytecode");

    override fun toString(): String {
        return this.anomalyTypeName
    }
}