package org.jetbrains.kotlincodeanomaliespublisher.structures

data class AnomalyExample (
    val total: Int = 0,
    val all_url: String = "",
    val items: List<AnomalyExampleFiles> = listOf()
)