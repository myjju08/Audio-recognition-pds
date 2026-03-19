package com.earbrief.app.engine.whisper

data class WhisperTimingConfig(
    val criticalSilenceMs: Long = 0,
    val highSilenceMs: Long = 0,
    val highMaxWaitMs: Long = 15_000,
    val normalSilenceMs: Long = 800,
    val normalTimeoutMs: Long = 30_000,
    val lowSilenceMs: Long = 1_500,
    val lowTimeoutMs: Long = 45_000,
    val fadeOutDurationMs: Long = 300
)
