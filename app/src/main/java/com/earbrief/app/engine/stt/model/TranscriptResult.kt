package com.earbrief.app.engine.stt.model

data class TranscriptResult(
    val transcript: String,
    val confidence: Float,
    val isFinal: Boolean,
    val speechFinal: Boolean,
    val words: List<TranscriptWord>,
    val durationMs: Long,
    val timestampMs: Long = System.currentTimeMillis()
)

data class TranscriptWord(
    val word: String,
    val punctuatedWord: String,
    val startSec: Float,
    val endSec: Float,
    val confidence: Float
)
