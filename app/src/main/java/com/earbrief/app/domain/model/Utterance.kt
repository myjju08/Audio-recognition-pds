package com.earbrief.app.domain.model

import java.util.UUID

data class Utterance(
    val id: String = UUID.randomUUID().toString(),
    val speaker: SpeakerLabel,
    val text: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float,
    val language: String = "ko",
    val entities: List<Entity> = emptyList(),
    val isQuestion: Boolean = false
)
