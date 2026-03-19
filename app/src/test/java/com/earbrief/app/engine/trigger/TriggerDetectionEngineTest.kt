package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.domain.repository.TriggerEventRepository
import com.earbrief.app.service.PipelineOrchestrator
import com.earbrief.app.service.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TriggerDetectionEngine")
class TriggerDetectionEngineTest {

    @Test
    fun `onUtterance with no sessionId does not publish`() = runTest {
        val rule = FakeTriggerRule(utteranceEvent = createTriggerEvent("session-1"))
        val repository = FakeTriggerEventRepository()
        val orchestrator = createOrchestrator()
        val sessionManager = FakeSessionManager(sessionId = null)
        val engine = TriggerDetectionEngine(setOf(rule), repository, orchestrator, sessionManager)

        engine.onUtterance(createUtterance())

        assertEquals(0, rule.evaluateCalls)
        assertTrue(repository.insertedEvents.isEmpty())
        assertTrue(orchestrator.recentTriggerEvents.value.isEmpty())
    }

    @Test
    fun `onUtterance publishes when a rule emits`() = runTest {
        val emittedEvent = createTriggerEvent("session-abc")
        val firstRule = FakeTriggerRule(utteranceEvent = null)
        val secondRule = FakeTriggerRule(utteranceEvent = emittedEvent)
        val repository = FakeTriggerEventRepository()
        val orchestrator = createOrchestrator()
        val sessionManager = FakeSessionManager(sessionId = "session-abc")
        val engine = TriggerDetectionEngine(setOf(firstRule, secondRule), repository, orchestrator, sessionManager)

        engine.onUtterance(createUtterance())

        assertEquals(listOf(emittedEvent), repository.insertedEvents)
        assertEquals(listOf(emittedEvent), orchestrator.recentTriggerEvents.value)
    }

    @Test
    fun `reset calls reset on all rules`() {
        val firstRule = FakeTriggerRule()
        val secondRule = FakeTriggerRule()
        val engine = TriggerDetectionEngine(
            triggerRules = setOf(firstRule, secondRule),
            triggerEventRepository = FakeTriggerEventRepository(),
            pipelineOrchestrator = createOrchestrator(),
            sessionManager = FakeSessionManager(sessionId = "session-abc")
        )

        engine.reset()

        assertEquals(1, firstRule.resetCalls)
        assertEquals(1, secondRule.resetCalls)
    }

    @Test
    fun `onVadStateChanged publishes emitted event`() = runTest {
        val emittedEvent = createTriggerEvent("session-abc")
        val rule = FakeTriggerRule(vadEvent = emittedEvent)
        val repository = FakeTriggerEventRepository()
        val orchestrator = createOrchestrator()
        val engine = TriggerDetectionEngine(
            triggerRules = setOf(rule),
            triggerEventRepository = repository,
            pipelineOrchestrator = orchestrator,
            sessionManager = FakeSessionManager(sessionId = "session-abc")
        )

        engine.onVadStateChanged(VadState.SILENCE, silenceDurationMs = 3000L)

        assertEquals(listOf(emittedEvent), repository.insertedEvents)
        assertEquals(listOf(emittedEvent), orchestrator.recentTriggerEvents.value)
        assertEquals(1, rule.vadCalls)
    }

    private fun createOrchestrator(): PipelineOrchestrator {
        return PipelineOrchestrator()
    }

    private val backgroundScope = CoroutineScope(Dispatchers.Default)

    private fun createUtterance(): Utterance {
        return Utterance(
            speaker = SpeakerLabel.SELF,
            text = "테스트 발화",
            startTimeMs = 0L,
            endTimeMs = 1000L,
            confidence = 0.95f
        )
    }

    private fun createTriggerEvent(sessionId: String): TriggerEvent {
        return TriggerEvent(
            sessionId = sessionId,
            triggerType = TriggerType.KEYWORD_INSTANT,
            sourceUtteranceId = "u-1",
            sourceText = "테스트 발화",
            whisperText = "핵심 키워드가 감지되었습니다."
        )
    }
}

private class FakeTriggerRule(
    private val utteranceEvent: TriggerEvent? = null,
    private val vadEvent: TriggerEvent? = null
) : TriggerRule {
    var evaluateCalls: Int = 0
    var vadCalls: Int = 0
    var resetCalls: Int = 0

    override suspend fun evaluate(utterance: Utterance, sessionId: String): TriggerEvent? {
        evaluateCalls += 1
        return utteranceEvent
    }

    override suspend fun onVadStateChanged(
        vadState: VadState,
        silenceDurationMs: Long,
        sessionId: String
    ): TriggerEvent? {
        vadCalls += 1
        return vadEvent
    }

    override fun reset() {
        resetCalls += 1
    }
}

private class FakeTriggerEventRepository : TriggerEventRepository {
    val insertedEvents = mutableListOf<TriggerEvent>()

    override suspend fun insert(event: TriggerEvent) {
        insertedEvents += event
    }

    override fun observeBySession(sessionId: String): Flow<List<TriggerEvent>> {
        return flowOf(insertedEvents.filter { it.sessionId == sessionId })
    }

    override suspend fun getBySession(sessionId: String): List<TriggerEvent> {
        return insertedEvents.filter { it.sessionId == sessionId }
    }

    override suspend fun deleteOlderThan(timestampMs: Long) = Unit
}

private class FakeSessionManager(sessionId: String?) : SessionManager {
    private val state = MutableStateFlow(sessionId)

    override val sessionId: StateFlow<String?> = state

    override suspend fun onListeningStateChanged(state: ListeningState) = Unit
}
