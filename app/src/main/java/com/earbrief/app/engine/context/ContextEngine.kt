package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.engine.stt.model.TranscriptResult
import kotlinx.coroutines.flow.SharedFlow

interface ContextEngine {
    val utteranceFlow: SharedFlow<Utterance>
    suspend fun onTranscript(result: TranscriptResult)
    suspend fun onListeningStateChanged(state: ListeningState)
}
