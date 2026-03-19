package com.earbrief.app.engine.trigger.rules

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.context.ShortTermMemory
import com.earbrief.app.engine.trigger.WhisperGenerator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("SilenceDetectTriggerRule")
class SilenceDetectTriggerRuleTest {

    @Test
    fun `question and 2500ms silence emits event once`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(true, true))
        val whisperGenerator = FakeWhisperGenerator()
        val rule = SilenceDetectTriggerRule(shortTermMemory, whisperGenerator)
        val question = testUtterance(text = "질문이 있습니다", isQuestion = true)

        rule.evaluate(question, SESSION_ID)

        val first = rule.onVadStateChanged(VadState.SILENCE, 2_500L, SESSION_ID)
        val second = rule.onVadStateChanged(VadState.SILENCE, 3_000L, SESSION_ID)

        assertNotNull(first)
        assertEquals(TriggerType.SILENCE_DETECT, first?.triggerType)
        assertEquals(question.id, first?.sourceUtteranceId)
        assertEquals(question.text, first?.sourceText)
        assertEquals("silence:2500|question:${question.text}", first?.whisperText)
        assertNull(second)
        assertEquals(1, shortTermMemory.tryConsumeCalls)
    }

    @Test
    fun `2499ms silence returns null`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(true))
        val rule = SilenceDetectTriggerRule(shortTermMemory, FakeWhisperGenerator())

        rule.evaluate(testUtterance(isQuestion = true), SESSION_ID)

        val event = rule.onVadStateChanged(VadState.SILENCE, 2_499L, SESSION_ID)

        assertNull(event)
        assertEquals(0, shortTermMemory.tryConsumeCalls)
    }

    @Test
    fun `non question then silence returns null`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(true))
        val rule = SilenceDetectTriggerRule(shortTermMemory, FakeWhisperGenerator())

        rule.evaluate(testUtterance(isQuestion = false), SESSION_ID)

        val event = rule.onVadStateChanged(VadState.SILENCE, 2_500L, SESSION_ID)

        assertNull(event)
        assertEquals(0, shortTermMemory.tryConsumeCalls)
    }

    @Test
    fun `speech resets fired flag`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(true, true, true))
        val rule = SilenceDetectTriggerRule(shortTermMemory, FakeWhisperGenerator())
        rule.evaluate(testUtterance(text = "왜 그래?", isQuestion = true), SESSION_ID)

        val first = rule.onVadStateChanged(VadState.SILENCE, 2_500L, SESSION_ID)
        rule.onVadStateChanged(VadState.SPEECH, 0L, SESSION_ID)
        val second = rule.onVadStateChanged(VadState.SILENCE, 2_600L, SESSION_ID)

        assertNotNull(first)
        assertNotNull(second)
        assertEquals(2, shortTermMemory.tryConsumeCalls)
    }

    @Test
    fun `exhausted budget returns null`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(false))
        val rule = SilenceDetectTriggerRule(shortTermMemory, FakeWhisperGenerator())
        rule.evaluate(testUtterance(isQuestion = true), SESSION_ID)

        val event = rule.onVadStateChanged(VadState.SILENCE, 2_500L, SESSION_ID)

        assertNull(event)
        assertEquals(1, shortTermMemory.tryConsumeCalls)
    }

    @Test
    fun `reset clears state`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(true))
        val rule = SilenceDetectTriggerRule(shortTermMemory, FakeWhisperGenerator())
        rule.evaluate(testUtterance(isQuestion = true), SESSION_ID)
        rule.reset()

        val event = rule.onVadStateChanged(VadState.SILENCE, 2_500L, SESSION_ID)

        assertNull(event)
        assertEquals(0, shortTermMemory.tryConsumeCalls)
    }

    @Test
    fun `session change resets internal question state`() = runTest {
        val shortTermMemory = FakeShortTermMemory(mutableListOf(true))
        val rule = SilenceDetectTriggerRule(shortTermMemory, FakeWhisperGenerator())
        rule.evaluate(testUtterance(isQuestion = true), "session-a")

        val event = rule.onVadStateChanged(VadState.SILENCE, 2_500L, "session-b")

        assertNull(event)
        assertEquals(0, shortTermMemory.tryConsumeCalls)
    }

    private fun testUtterance(text: String = "test", isQuestion: Boolean): Utterance {
        return Utterance(
            speaker = SpeakerLabel.SELF,
            text = text,
            startTimeMs = 0L,
            endTimeMs = 1000L,
            confidence = 0.95f,
            isQuestion = isQuestion
        )
    }

    private class FakeWhisperGenerator : WhisperGenerator {
        override fun generate(triggerType: TriggerType, context: Map<String, String>): String {
            return "silence:${context["silenceDurationMs"]}|question:${context["questionText"]}"
        }
    }

    private class FakeShortTermMemory(
        private val budgetResponses: MutableList<Boolean>
    ) : ShortTermMemory {
        var tryConsumeCalls: Int = 0

        override suspend fun add(utterance: Utterance) = Unit

        override suspend fun recent(count: Int): List<Utterance> = emptyList()

        override suspend fun recentInWindow(windowMs: Long): List<Utterance> = emptyList()

        override suspend fun latestQuestion(windowMs: Long): Utterance? = null

        override suspend fun getActiveEntities(type: EntityType): List<Entity> = emptyList()

        override suspend fun getTimeSinceLastUtteranceMs(): Long = Long.MAX_VALUE

        override suspend fun tryConsumeInterventionBudget(): Boolean {
            tryConsumeCalls += 1
            return if (budgetResponses.isEmpty()) {
                true
            } else {
                budgetResponses.removeAt(0)
            }
        }

        override suspend fun getRemainingBudget(): Int = 0

        override suspend fun clear() = Unit
    }

    private companion object {
        const val SESSION_ID = "session-1"
    }
}
