package org.jetbrains.kotlincodeanomaliespublisher.structures

data class AnomalyExample (
    var total: Int = 0,
    val all_url: String = "",
    val items: MutableList<AnomalyExampleFiles> = mutableListOf()
)