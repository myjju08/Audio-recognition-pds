package com.earbrief.app.engine.whisper

import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.trigger.TriggerDetectionEngine
import com.earbrief.app.engine.tts.TtsEngine
import com.earbrief.app.service.PipelineOrchestrator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Singleton
class WhisperPipelineCoordinator @Inject constructor(
    private val pipelineOrchestrator: PipelineOrchestrator,
    private val triggerDetectionEngine: TriggerDetectionEngine,
    private val whisperTimingEngine: WhisperTimingEngine,
    private val ttsEngine: TtsEngine,
    private val whisperAudioPlayer: WhisperAudioPlayer
) {
    private var utteranceCollectorJob: Job? = null
    private var triggerCollectorJob: Job? = null
    private var outputCollectorJob: Job? = null
    private var fadeOutCollectorJob: Job? = null

    fun start(scope: CoroutineScope) {
        if (isStarted()) return

        utteranceCollectorJob = scope.launch {
            pipelineOrchestrator.utterances.collect { utterance ->
                triggerDetectionEngine.onUtterance(utterance)
            }
        }

        triggerCollectorJob = scope.launch {
            pipelineOrchestrator.triggerEvents.collect { event ->
                whisperTimingEngine.enqueue(event, event.whisperText)
            }
        }

        outputCollectorJob = scope.launch {
            whisperTimingEngine.outputCommands.collect { command ->
                val synthResult = ttsEngine.synthesize(command.whisperText)
                val audioBytes = synthResult.getOrElse { return@collect }

                val vibrateMs = if (command.playVibration) command.vibrationDurationMs else 0L
                val playResult = whisperAudioPlayer.play(
                    audioBytes = audioBytes,
                    volumeMultiplier = command.volumeMultiplier,
                    vibrateMs = vibrateMs
                )

                if (playResult.isSuccess) {
                    whisperTimingEngine.onPlaybackStarted()
                    waitForPlaybackCompletion()
                    whisperTimingEngine.onPlaybackCompleted()
                }
            }
        }

        fadeOutCollectorJob = scope.launch {
            whisperTimingEngine.fadeOutSignal.collect {
                whisperAudioPlayer.fadeOut()
            }
        }
    }

    suspend fun onVadStateChanged(vadState: VadState, silenceDurationMs: Long) {
        triggerDetectionEngine.onVadStateChanged(vadState, silenceDurationMs)
        whisperTimingEngine.onVadStateChanged(vadState, silenceDurationMs)
    }

    fun stop() {
        utteranceCollectorJob?.cancel()
        utteranceCollectorJob = null
        triggerCollectorJob?.cancel()
        triggerCollectorJob = null
        outputCollectorJob?.cancel()
        outputCollectorJob = null
        fadeOutCollectorJob?.cancel()
        fadeOutCollectorJob = null

        whisperAudioPlayer.stop()
        whisperTimingEngine.clear()
        triggerDetectionEngine.reset()
    }

    private suspend fun waitForPlaybackCompletion() {
        whisperAudioPlayer.playbackState.first { state -> state == PlaybackState.IDLE }
    }

    private fun isStarted(): Boolean {
        return utteranceCollectorJob?.isActive == true ||
            triggerCollectorJob?.isActive == true ||
            outputCollectorJob?.isActive == true ||
            fadeOutCollectorJob?.isActive == true
    }
}
