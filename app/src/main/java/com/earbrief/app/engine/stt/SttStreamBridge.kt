package com.earbrief.app.engine.stt

import com.earbrief.app.di.ApplicationScope
import com.earbrief.app.di.IoDispatcher
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.stt.model.TranscriptResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SttStreamBridge @Inject constructor(
    private val sttEngine: SttEngine,
    @ApplicationScope private val scope: CoroutineScope,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {
    private var collectJob: Job? = null
    private var isSpeechActive = false

    private val _transcripts = MutableSharedFlow<TranscriptResult>(extraBufferCapacity = 64)
    val transcripts: SharedFlow<TranscriptResult> = _transcripts.asSharedFlow()

    private val _interimText = MutableStateFlow("")
    val interimText: StateFlow<String> = _interimText.asStateFlow()

    val sttConnectionState: StateFlow<SttConnectionState> = sttEngine.connectionState

    private val _finalizedSegments = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val finalizedSegments: SharedFlow<String> = _finalizedSegments.asSharedFlow()

    fun start() {
        collectJob?.cancel()
        collectJob = scope.launch(ioDispatcher) {
            sttEngine.connect()
            sttEngine.transcriptResults.collect { result ->
                _transcripts.tryEmit(result)

                if (result.isFinal) {
                    _interimText.value = ""
                    if (result.transcript.isNotBlank()) {
                        _finalizedSegments.tryEmit(result.transcript)
                    }
                } else {
                    _interimText.value = result.transcript
                }
            }
        }
    }

    suspend fun onVadStateChanged(state: VadState) {
        when (state) {
            VadState.SPEECH -> {
                if (!isSpeechActive) {
                    isSpeechActive = true
                    if (sttEngine.connectionState.value == SttConnectionState.DISCONNECTED ||
                        sttEngine.connectionState.value == SttConnectionState.FAILED
                    ) {
                        sttEngine.connect()
                    }
                }
            }
            VadState.SILENCE -> {
                if (isSpeechActive) {
                    isSpeechActive = false
                    sttEngine.endOfStream()
                }
            }
            VadState.TRANSITION -> { }
        }
    }

    suspend fun sendAudioFrame(pcmData: ShortArray) {
        if (isSpeechActive) {
            sttEngine.sendAudioFrame(pcmData)
        }
    }

    fun stop() {
        collectJob?.cancel()
        collectJob = null
        isSpeechActive = false
        scope.launch(ioDispatcher) {
            sttEngine.endOfStream()
            sttEngine.disconnect()
        }
    }
}
