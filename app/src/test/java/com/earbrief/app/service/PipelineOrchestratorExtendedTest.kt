package com.earbrief.app.service

import app.cash.turbine.test
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("PipelineOrchestrator extended flows")
class PipelineOrchestratorExtendedTest {

    private fun createOrchestrator(): PipelineOrchestrator {
        return PipelineOrchestrator()
    }

    @Nested
    @DisplayName("Utterance and trigger events")
    inner class EventFlowTests {

        @Test
        fun `publishUtterance emits via utterances SharedFlow`() = runTest {
            val orchestrator = createOrchestrator()
            val utterance = Utterance(
                speaker = SpeakerLabel.SELF,
                text = "hello",
                startTimeMs = 10L,
                endTimeMs = 20L,
                confidence = 0.95f
            )
            orchestrator.utterances.test {
                orchestrator.publishUtterance(utterance)
                assertEquals(utterance, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

        @Test
        fun `publishTriggerEvent emits and adds to recentTriggerEvents`() = runTest {
            val orchestrator = createOrchestrator()
            val event = createTriggerEvent(index = 1)
            orchestrator.triggerEvents.test {
                orchestrator.publishTriggerEvent(event)
                assertEquals(event, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
            assertEquals(listOf(event), orchestrator.recentTriggerEvents.value)
        }

        @Test
        fun `recentTriggerEvents keeps only last 10`() {
            val orchestrator = createOrchestrator()
            repeat(12) { index ->
                orchestrator.publishTriggerEvent(createTriggerEvent(index))
            }

            val recent = orchestrator.recentTriggerEvents.value
            assertEquals(10, recent.size)
            assertEquals("source-2", recent.first().sourceUtteranceId)
            assertEquals("source-11", recent.last().sourceUtteranceId)
        }

        @Test
        fun `clearTriggerEvents resets list`() {
            val orchestrator = createOrchestrator()
            orchestrator.publishTriggerEvent(createTriggerEvent(index = 1))

            orchestrator.clearTriggerEvents()

            assertEquals(emptyList<TriggerEvent>(), orchestrator.recentTriggerEvents.value)
        }
    }

    private fun createTriggerEvent(index: Int): TriggerEvent {
        return TriggerEvent(
            sessionId = "session-1",
            triggerType = TriggerType.KEYWORD_INSTANT,
            sourceUtteranceId = "source-$index",
            sourceText = "text-$index",
            whisperText = "whisper-$index"
        )
    }
}
