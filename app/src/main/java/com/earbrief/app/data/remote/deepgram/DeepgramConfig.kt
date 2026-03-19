package com.earbrief.app.data.remote.deepgram

object DeepgramConfig {
    const val WS_BASE_URL = "wss://api.deepgram.com/v1/listen"

    val PARAMS = mapOf(
        "model" to "nova-3",
        "language" to "ko",
        "smart_format" to "true",
        "interim_results" to "true",
        "utterance_end_ms" to "1500",
        "vad_events" to "true",
        "encoding" to "linear16",
        "sample_rate" to "16000",
        "channels" to "1",
        "punctuate" to "true",
        "diarize" to "false",
        "numerals" to "true"
    )

    const val HEARTBEAT_INTERVAL_MS = 15_000L
    const val MAX_RECONNECT_ATTEMPTS = 5
    val RECONNECT_DELAYS = listOf(1000L, 2000L, 4000L, 8000L, 16000L)

    fun buildWsUrl(): String {
        val params = PARAMS.entries.joinToString("&") { "${it.key}=${it.value}" }
        return "$WS_BASE_URL?$params"
    }
}
