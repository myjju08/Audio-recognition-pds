package com.earbrief.app.service

import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.VadState
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("AudioCaptureService")
class AudioCaptureServiceTest {

    @Nested
    @DisplayName("Service companion")
    inner class ServiceCompanion {

        @Test
        fun `isRunning defaults to false`() {
            assertFalse(AudioCaptureService.isRunning)
        }

        @Test
        fun `action constants are correctly defined`() {
            assertEquals("com.earbrief.action.START", AudioCaptureService.ACTION_START)
            assertEquals("com.earbrief.action.PAUSE", AudioCaptureService.ACTION_PAUSE)
            assertEquals("com.earbrief.action.RESUME", AudioCaptureService.ACTION_RESUME)
            assertEquals("com.earbrief.action.STOP", AudioCaptureService.ACTION_STOP)
        }
    }

    @Nested
    @DisplayName("PipelineOrchestrator")
    inner class OrchestratorTests {

        @Test
        fun `initial listening state is IDLE`() {
            val orchestrator = PipelineOrchestrator(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
            assertEquals(ListeningState.IDLE, orchestrator.listeningState.value)
        }

        @Test
        fun `initial vad state is SILENCE`() {
            val orchestrator = PipelineOrchestrator(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
            assertEquals(VadState.SILENCE, orchestrator.vadState.value)
        }

        @Test
        fun `updateListeningState changes state`() {
            val orchestrator = PipelineOrchestrator(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
            orchestrator.updateListeningState(ListeningState.LISTENING)
            assertEquals(ListeningState.LISTENING, orchestrator.listeningState.value)
        }

        @Test
        fun `session duration starts at 0`() {
            val orchestrator = PipelineOrchestrator(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
            assertEquals(0L, orchestrator.getSessionDurationMs())
        }

        @Test
        fun `session duration tracks after LISTENING state`() {
            val orchestrator = PipelineOrchestrator(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
            orchestrator.updateListeningState(ListeningState.LISTENING)
            Thread.sleep(50)
            val duration = orchestrator.getSessionDurationMs()
            assert(duration >= 40)
        }

        @Test
        fun `session resets on IDLE`() {
            val orchestrator = PipelineOrchestrator(
                scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.Unconfined)
            )
            orchestrator.updateListeningState(ListeningState.LISTENING)
            orchestrator.updateListeningState(ListeningState.IDLE)
            assertEquals(0L, orchestrator.getSessionDurationMs())
        }
    }
}
