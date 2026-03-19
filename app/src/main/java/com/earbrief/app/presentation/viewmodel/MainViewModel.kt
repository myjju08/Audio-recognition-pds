package com.earbrief.app.presentation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.stt.SttConnectionState
import com.earbrief.app.service.AudioCaptureService
import com.earbrief.app.service.PipelineOrchestrator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class MainUiState(
    val listeningState: ListeningState = ListeningState.IDLE,
    val vadState: VadState = VadState.SILENCE,
    val sessionDurationMs: Long = 0L,
    val speechProbability: Float = 0f,
    val whisperCount: Int = 0,
    val budsConnected: Boolean = false,
    val budsName: String = "",
    val batteryLevel: Int = -1
)

@HiltViewModel
class MainViewModel @Inject constructor(
    private val application: Application,
    private val orchestrator: PipelineOrchestrator
) : AndroidViewModel(application) {

    val listeningState: StateFlow<ListeningState> = orchestrator.listeningState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ListeningState.IDLE)

    val vadState: StateFlow<VadState> = orchestrator.vadState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), VadState.SILENCE)

    val speechProbability: StateFlow<Float> = orchestrator.speechProbability
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0f)

    val sttConnectionState: StateFlow<SttConnectionState> = orchestrator.sttConnectionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SttConnectionState.DISCONNECTED)

    val interimTranscript: StateFlow<String> = orchestrator.interimTranscript
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val finalTranscripts: StateFlow<List<String>> = orchestrator.finalTranscripts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val sessionId: StateFlow<String?> = orchestrator.sessionId
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val recentTriggerEvents: StateFlow<List<TriggerEvent>> = orchestrator.recentTriggerEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _whisperCount = MutableStateFlow(0)
    val whisperCount: StateFlow<Int> = _whisperCount.asStateFlow()

    fun toggleListening() {
        val context = application.applicationContext
        when (listeningState.value) {
            ListeningState.IDLE -> {
                AudioCaptureService.start(context)
                orchestrator.updateListeningState(ListeningState.LISTENING)
            }
            ListeningState.LISTENING -> {
                AudioCaptureService.pause(context)
                orchestrator.updateListeningState(ListeningState.PAUSED)
            }
            ListeningState.PAUSED -> {
                AudioCaptureService.resume(context)
                orchestrator.updateListeningState(ListeningState.LISTENING)
            }
            ListeningState.STOPPING -> { }
        }
    }

    fun stopListening() {
        AudioCaptureService.stop(application.applicationContext)
        orchestrator.updateListeningState(ListeningState.IDLE)
    }

    fun getSessionDurationFormatted(): String {
        val ms = orchestrator.getSessionDurationMs()
        val seconds = (ms / 1000) % 60
        val minutes = (ms / 60000) % 60
        val hours = ms / 3600000
        return if (hours > 0) {
            "${hours}h ${minutes}m"
        } else {
            "${minutes}m ${seconds}s"
        }
    }
}
