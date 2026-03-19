package com.earbrief.app.domain.model

data class Entity(
    val type: EntityType,
    val value: String,
    val normalizedValue: String,
    val confidence: Float,
    val position: IntRange,
    val sourceUtteranceId: String
)
