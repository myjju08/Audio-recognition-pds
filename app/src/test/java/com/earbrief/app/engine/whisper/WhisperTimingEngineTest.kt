package com.earbrief.app.engine.whisper

import app.cash.turbine.test
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.TriggerUrgency
import com.earbrief.app.domain.model.VadState
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("WhisperTimingEngine")
class WhisperTimingEngineTest {

    @Test
    fun `CRITICAL event emits immediately`() = runTest {
        val now = MutableNow(1_000L)
        val engine = createEngine(now)
        val event = createEvent(priority = 10, urgency = TriggerUrgency.CRITICAL)

        engine.outputCommands.test {
            engine.enqueue(event, whisperText = "critical")

            val command = awaitItem()
            assertEquals(event.id, command.triggerEvent.id)
            assertEquals("critical", command.whisperText)
            assertEquals(true, command.playVibration)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NORMAL event emits at 800ms silence`() = runTest {
        val now = MutableNow(2_000L)
        val engine = createEngine(now)
        val event = createEvent(priority = 5, urgency = TriggerUrgency.NORMAL)
        engine.enqueue(event, whisperText = "normal")

        engine.outputCommands.test {
            engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 800)

            val command = awaitItem()
            assertEquals(event.id, command.triggerEvent.id)
            assertEquals("normal", command.whisperText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NORMAL event does not emit at 799ms silence`() = runTest {
        val now = MutableNow(3_000L)
        val engine = createEngine(now)
        engine.enqueue(createEvent(priority = 5, urgency = TriggerUrgency.NORMAL), whisperText = "normal")

        engine.outputCommands.test {
            engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 799)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `LOW event emits at 1500ms silence`() = runTest {
        val now = MutableNow(4_000L)
        val engine = createEngine(now)
        val event = createEvent(priority = 4, urgency = TriggerUrgency.LOW)
        engine.enqueue(event, whisperText = "low")

        engine.outputCommands.test {
            engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 1_500)

            val command = awaitItem()
            assertEquals(event.id, command.triggerEvent.id)
            assertEquals("low", command.whisperText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `NORMAL event is discarded at 30s timeout`() = runTest {
        val now = MutableNow(10_000L)
        val engine = createEngine(now)
        engine.enqueue(createEvent(priority = 5, urgency = TriggerUrgency.NORMAL), whisperText = "normal")

        now.value += 30_000L

        engine.outputCommands.test {
            engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 5_000)

            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `higher priority event preempts lower priority`() = runTest {
        val now = MutableNow(20_000L)
        val engine = createEngine(now)
        val lowPriority = createEvent(priority = 1, urgency = TriggerUrgency.NORMAL)
        val highPriority = createEvent(priority = 10, urgency = TriggerUrgency.NORMAL)

        engine.enqueue(lowPriority, whisperText = "low-priority")
        engine.enqueue(highPriority, whisperText = "high-priority")

        engine.outputCommands.test {
            engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 800)

            val command = awaitItem()
            assertEquals(highPriority.id, command.triggerEvent.id)
            assertEquals("high-priority", command.whisperText)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `speech during playback emits fadeout signal`() = runTest {
        val now = MutableNow(30_000L)
        val engine = createEngine(now)

        engine.onPlaybackStarted()

        engine.fadeOutSignal.test {
            engine.onVadStateChanged(VadState.SPEECH, silenceDurationMs = 0)
            awaitItem()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `clear empties queue`() = runTest {
        val now = MutableNow(40_000L)
        val engine = createEngine(now)
        engine.enqueue(createEvent(priority = 3, urgency = TriggerUrgency.LOW), whisperText = "queued")

        engine.clear()

        engine.outputCommands.test {
            engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 2_000)
            expectNoEvents()
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createEngine(now: MutableNow): WhisperTimingEngine {
        return WhisperTimingEngine(WhisperTimingConfig()).also {
            it.setNowProviderForTest { now.value }
        }
    }

    private fun createEvent(priority: Int, urgency: TriggerUrgency): TriggerEvent {
        return TriggerEvent(
            sessionId = "session-1",
            triggerType = TriggerType.KEYWORD_INSTANT,
            sourceUtteranceId = "utterance-1",
            sourceText = "source",
            whisperText = "whisper",
            priority = priority,
            urgency = urgency
        )
    }

    private data class MutableNow(var value: Long)
}
