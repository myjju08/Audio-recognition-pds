package com.earbrief.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "trigger_events")
data class TriggerEventEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "trigger_type") val triggerType: String,
    @ColumnInfo(name = "source_utterance_id") val sourceUtteranceId: String,
    @ColumnInfo(name = "source_text") val sourceText: String,
    @ColumnInfo(name = "detected_entities") val detectedEntities: String,
    @ColumnInfo(name = "whisper_text") val whisperText: String,
    val confidence: Float,
    val priority: Int,
    val urgency: String,
    val metadata: String,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long
)
