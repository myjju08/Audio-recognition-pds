package com.earbrief.app.engine.whisper

import com.earbrief.app.domain.model.TriggerEvent

data class WhisperOutputCommand(
    val triggerEvent: TriggerEvent,
    val whisperText: String,
    val volumeMultiplier: Float,
    val playVibration: Boolean,
    val vibrationDurationMs: Long
)
