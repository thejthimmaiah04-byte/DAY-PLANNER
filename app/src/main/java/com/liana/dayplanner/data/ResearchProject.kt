package com.liana.dayplanner.data

import java.util.UUID

data class ResearchItem(
    val id: String = UUID.randomUUID().toString(),
    val text: String,
    val done: Boolean = false
)

data class ResearchProject(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val tasks: List<ResearchItem> = emptyList(),
    val targets: List<ResearchItem> = emptyList(),
    val notes: String = ""
)
