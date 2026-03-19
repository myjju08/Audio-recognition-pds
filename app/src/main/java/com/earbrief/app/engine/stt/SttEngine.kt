package com.earbrief.app.engine.stt

import com.earbrief.app.engine.stt.model.TranscriptResult
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

enum class SttConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    FAILED
}

interface SttEngine {

    val connectionState: StateFlow<SttConnectionState>

    val transcriptResults: SharedFlow<TranscriptResult>

    suspend fun connect()

    suspend fun sendAudioFrame(pcmData: ShortArray)

    suspend fun endOfStream()

    suspend fun disconnect()

    fun release()
}
