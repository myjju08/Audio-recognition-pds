package com.earbrief.app.engine.tts

interface TtsEngine {

    suspend fun synthesize(text: String): Result<ByteArray>

    fun release()
}
