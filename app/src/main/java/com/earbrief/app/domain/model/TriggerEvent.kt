package com.earbrief.app.domain.model

import java.util.UUID

data class TriggerEvent(
    val id: String = UUID.randomUUID().toString(),
    val sessionId: String,
    val triggerType: TriggerType,
    val sourceUtteranceId: String,
    val sourceText: String,
    val detectedEntities: List<Entity> = emptyList(),
    val whisperText: String,
    val confidence: Float = 0f,
    val priority: Int = 3,
    val urgency: TriggerUrgency = TriggerUrgency.NORMAL,
    val metadata: Map<String, String> = emptyMap(),
    val createdAtMs: Long = System.currentTimeMillis()
)

enum class TriggerUrgency { LOW, NORMAL, HIGH, CRITICAL }
