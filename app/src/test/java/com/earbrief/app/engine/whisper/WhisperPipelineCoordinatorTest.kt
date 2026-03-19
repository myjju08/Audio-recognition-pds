package com.earbrief.app.engine.whisper

import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.engine.trigger.TriggerDetectionEngine
import com.earbrief.app.engine.tts.TtsEngine
import com.earbrief.app.service.PipelineOrchestrator
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WhisperPipelineCoordinator")
class WhisperPipelineCoordinatorTest {

    @Test
    fun `trigger event enqueues and output command synthesizes then plays`() = runTest {
        val orchestrator = createOrchestrator()
        val triggerDetectionEngine = mockk<TriggerDetectionEngine>(relaxed = true)
        val whisperTimingEngine = mockk<WhisperTimingEngine>(relaxed = true)
        val ttsEngine = mockk<TtsEngine>()
        val audioPlayer = mockk<WhisperAudioPlayer>(relaxed = true)
        val outputCommands = MutableSharedFlow<WhisperOutputCommand>(extraBufferCapacity = 1)
        val fadeOutSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
        val playbackState = MutableStateFlow(PlaybackState.IDLE)
        val synthesizedBytes = byteArrayOf(1, 2, 3)

        every { whisperTimingEngine.outputCommands } returns outputCommands
        every { whisperTimingEngine.fadeOutSignal } returns fadeOutSignal
        every { audioPlayer.playbackState } returns playbackState
        coEvery { ttsEngine.synthesize("whisper text") } returns Result.success(synthesizedBytes)
        coEvery {
            audioPlayer.play(
                audioBytes = any(),
                volumeMultiplier = any(),
                vibrateMs = any()
            )
        } returns Result.success(Unit)

        val sut = WhisperPipelineCoordinator(
            pipelineOrchestrator = orchestrator,
            triggerDetectionEngine = triggerDetectionEngine,
            whisperTimingEngine = whisperTimingEngine,
            ttsEngine = ttsEngine,
            whisperAudioPlayer = audioPlayer
        )
        sut.start(this)
        advanceUntilIdle()

        val triggerEvent = createTriggerEvent(whisperText = "whisper text")
        orchestrator.publishTriggerEvent(triggerEvent)
        advanceUntilIdle()

        val command = WhisperOutputCommand(
            triggerEvent = triggerEvent,
            whisperText = "whisper text",
            volumeMultiplier = 0.55f,
            playVibration = false,
            vibrationDurationMs = 0L
        )
        outputCommands.emit(command)
        advanceUntilIdle()
        sut.stop()
        advanceUntilIdle()

        verify(exactly = 1) { whisperTimingEngine.enqueue(triggerEvent, "whisper text") }
        coVerify(exactly = 1) { ttsEngine.synthesize("whisper text") }
        coVerify(exactly = 1) {
            audioPlayer.play(
                audioBytes = match { it.contentEquals(synthesizedBytes) },
                volumeMultiplier = 0.55f,
                vibrateMs = 0L
            )
        }
        verify(exactly = 1) { whisperTimingEngine.onPlaybackStarted() }
        verify(exactly = 1) { whisperTimingEngine.onPlaybackCompleted() }
    }

    @Test
    fun `tts failure does not play audio`() = runTest {
        val orchestrator = createOrchestrator()
        val triggerDetectionEngine = mockk<TriggerDetectionEngine>(relaxed = true)
        val whisperTimingEngine = mockk<WhisperTimingEngine>(relaxed = true)
        val ttsEngine = mockk<TtsEngine>()
        val audioPlayer = mockk<WhisperAudioPlayer>(relaxed = true)
        val outputCommands = MutableSharedFlow<WhisperOutputCommand>(extraBufferCapacity = 1)
        val fadeOutSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        every { whisperTimingEngine.outputCommands } returns outputCommands
        every { whisperTimingEngine.fadeOutSignal } returns fadeOutSignal
        every { audioPlayer.playbackState } returns MutableStateFlow(PlaybackState.IDLE)
        coEvery { ttsEngine.synthesize("fail me") } returns Result.failure(IllegalStateException("tts"))

        val sut = WhisperPipelineCoordinator(
            pipelineOrchestrator = orchestrator,
            triggerDetectionEngine = triggerDetectionEngine,
            whisperTimingEngine = whisperTimingEngine,
            ttsEngine = ttsEngine,
            whisperAudioPlayer = audioPlayer
        )
        sut.start(this)
        advanceUntilIdle()

        outputCommands.emit(
            WhisperOutputCommand(
                triggerEvent = createTriggerEvent(whisperText = "fail me"),
                whisperText = "fail me",
                volumeMultiplier = 0.4f,
                playVibration = false,
                vibrationDurationMs = 0L
            )
        )
        advanceUntilIdle()
        sut.stop()
        advanceUntilIdle()

        coVerify(exactly = 1) { ttsEngine.synthesize("fail me") }
        coVerify(exactly = 0) {
            audioPlayer.play(
                audioBytes = any(),
                volumeMultiplier = any(),
                vibrateMs = any()
            )
        }
    }

    @Test
    fun `fadeout signal invokes player fadeOut`() = runTest {
        val orchestrator = createOrchestrator()
        val triggerDetectionEngine = mockk<TriggerDetectionEngine>(relaxed = true)
        val whisperTimingEngine = mockk<WhisperTimingEngine>(relaxed = true)
        val ttsEngine = mockk<TtsEngine>(relaxed = true)
        val audioPlayer = mockk<WhisperAudioPlayer>(relaxed = true)
        val outputCommands = MutableSharedFlow<WhisperOutputCommand>(extraBufferCapacity = 1)
        val fadeOutSignal = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

        every { whisperTimingEngine.outputCommands } returns outputCommands
        every { whisperTimingEngine.fadeOutSignal } returns fadeOutSignal
        every { audioPlayer.playbackState } returns MutableStateFlow(PlaybackState.IDLE)

        val sut = WhisperPipelineCoordinator(
            pipelineOrchestrator = orchestrator,
            triggerDetectionEngine = triggerDetectionEngine,
            whisperTimingEngine = whisperTimingEngine,
            ttsEngine = ttsEngine,
            whisperAudioPlayer = audioPlayer
        )
        sut.start(this)
        advanceUntilIdle()

        fadeOutSignal.emit(Unit)
        advanceUntilIdle()
        sut.stop()
        advanceUntilIdle()

        coVerify(exactly = 1) { audioPlayer.fadeOut() }
    }

    private fun createOrchestrator(): PipelineOrchestrator {
        return PipelineOrchestrator(scope = CoroutineScope(Dispatchers.Unconfined))
    }

    private fun createTriggerEvent(whisperText: String): TriggerEvent {
        return TriggerEvent(
            sessionId = "session-1",
            triggerType = TriggerType.KEYWORD_INSTANT,
            sourceUtteranceId = "utterance-1",
            sourceText = "source",
            whisperText = whisperText
        )
    }
}
