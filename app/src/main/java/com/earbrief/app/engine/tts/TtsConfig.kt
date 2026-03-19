package com.earbrief.app.engine.tts

data class TtsConfig(
    val voiceId: String = "korean_whisper_01",
    val modelId: String = "eleven_multilingual_v2",
    val stability: Float = 0.75f,
    val similarityBoost: Float = 0.80f,
    val style: Float = 0.15f,
    val speakingRate: Float = 1.1f,
    val outputFormat: String = "mp3_22050_32"
)
