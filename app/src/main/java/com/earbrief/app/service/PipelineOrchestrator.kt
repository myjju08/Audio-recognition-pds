package com.earbrief.app.service

import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.stt.SttConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PipelineOrchestrator @Inject constructor() {
    private val _listeningState = MutableStateFlow(ListeningState.IDLE)
    val listeningState: StateFlow<ListeningState> = _listeningState.asStateFlow()

    private val _vadState = MutableStateFlow(VadState.SILENCE)
    val vadState: StateFlow<VadState> = _vadState.asStateFlow()

    private val _sessionStartTimeMs = MutableStateFlow(0L)
    val sessionStartTimeMs: StateFlow<Long> = _sessionStartTimeMs.asStateFlow()

    private val _speechProbability = MutableStateFlow(0f)
    val speechProbability: StateFlow<Float> = _speechProbability.asStateFlow()

    private val _sttConnectionState = MutableStateFlow(SttConnectionState.DISCONNECTED)
    val sttConnectionState: StateFlow<SttConnectionState> = _sttConnectionState.asStateFlow()

    private val _interimTranscript = MutableStateFlow("")
    val interimTranscript: StateFlow<String> = _interimTranscript.asStateFlow()

    private val _finalTranscripts = MutableStateFlow<List<String>>(emptyList())
    val finalTranscripts: StateFlow<List<String>> = _finalTranscripts.asStateFlow()

    private val _transcriptEvents = MutableSharedFlow<String>(extraBufferCapacity = 16)
    val transcriptEvents: SharedFlow<String> = _transcriptEvents.asSharedFlow()

    private val _sessionId = MutableStateFlow<String?>(null)
    val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    private val _utterances = MutableSharedFlow<Utterance>(extraBufferCapacity = 32)
    val utterances: SharedFlow<Utterance> = _utterances.asSharedFlow()

    private val _triggerEvents = MutableSharedFlow<TriggerEvent>(extraBufferCapacity = 16)
    val triggerEvents: SharedFlow<TriggerEvent> = _triggerEvents.asSharedFlow()

    private val _recentTriggerEvents = MutableStateFlow<List<TriggerEvent>>(emptyList())
    val recentTriggerEvents: StateFlow<List<TriggerEvent>> = _recentTriggerEvents.asStateFlow()

    fun updateListeningState(state: ListeningState) {
        _listeningState.value = state
        if (state == ListeningState.LISTENING && _sessionStartTimeMs.value == 0L) {
            _sessionStartTimeMs.value = System.currentTimeMillis()
        }
        if (state == ListeningState.IDLE) {
            _sessionStartTimeMs.value = 0L
            clearTranscripts()
            clearTriggerEvents()
        }
    }

    fun updateVadState(state: VadState) {
        _vadState.value = state
    }

    fun updateSpeechProbability(probability: Float) {
        _speechProbability.value = probability
    }

    fun updateSttConnectionState(state: SttConnectionState) {
        _sttConnectionState.value = state
    }

    fun updateInterimTranscript(text: String) {
        _interimTranscript.value = text
    }

    fun appendFinalTranscript(text: String) {
        if (text.isBlank()) return
        _finalTranscripts.value = (_finalTranscripts.value + text).takeLast(20)
        _transcriptEvents.tryEmit(text)
    }

    fun clearTranscripts() {
        _interimTranscript.value = ""
        _finalTranscripts.value = emptyList()
    }

    fun updateSessionId(sessionId: String?) {
        _sessionId.value = sessionId
    }

    fun publishUtterance(utterance: Utterance) {
        _utterances.tryEmit(utterance)
    }

    fun publishTriggerEvent(event: TriggerEvent) {
        _triggerEvents.tryEmit(event)
        _recentTriggerEvents.value = (_recentTriggerEvents.value + event).takeLast(10)
    }

    fun clearTriggerEvents() {
        _recentTriggerEvents.value = emptyList()
    }

    fun getSessionDurationMs(): Long {
        val start = _sessionStartTimeMs.value
        if (start == 0L) return 0L
        return System.currentTimeMillis() - start
    }
}
