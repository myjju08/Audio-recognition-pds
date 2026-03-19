package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.KeywordActionType
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.TimeRange
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.domain.repository.CalendarRepository
import com.earbrief.app.domain.repository.CustomKeywordRepository
import com.earbrief.app.domain.repository.TriggerEventRepository
import com.earbrief.app.engine.context.ShortTermMemory
import com.earbrief.app.engine.trigger.rules.KeywordInstantTriggerRule
import com.earbrief.app.engine.trigger.rules.ScheduleGapTriggerRule
import com.earbrief.app.engine.trigger.rules.SilenceDetectTriggerRule
import com.earbrief.app.service.PipelineOrchestrator
import com.earbrief.app.service.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("TriggerMetrics")
class TriggerMetricsTest {
    private val backgroundScope = CoroutineScope(Dispatchers.Default)

    @Test
    fun `schedule gap fixture precision and recall stay above threshold`() = runTest {
        val fixtures = listOf(
            ScheduleGapFixture("question with minute datetime and slots", true, listOf("2026-03-19T14:00"), listOf(TimeRange(1L, 121_000L)), true, true),
            ScheduleGapFixture("question with second precision datetime and slots", true, listOf("2026-03-19T14:00:30"), listOf(TimeRange(2L, 302_000L)), true, true),
            ScheduleGapFixture("question with date-only entity and slots", true, listOf("2026-03-19"), listOf(TimeRange(3L, 603_000L)), true, true),
            ScheduleGapFixture("question with one slot still emits", true, listOf("2026-03-19T10:00"), listOf(TimeRange(4L, 64_000L)), true, true),
            ScheduleGapFixture("non-question with valid datetime", false, listOf("2026-03-19T14:00"), listOf(TimeRange(5L, 65_000L)), true, false),
            ScheduleGapFixture("question with no datetime entities", true, emptyList(), listOf(TimeRange(6L, 66_000L)), true, false),
            ScheduleGapFixture("question with invalid datetime", true, listOf("tomorrow afternoon"), listOf(TimeRange(7L, 67_000L)), true, false),
            ScheduleGapFixture("question with free slots unavailable", true, listOf("2026-03-19T14:00"), emptyList(), true, false),
            ScheduleGapFixture("question with exhausted budget", true, listOf("2026-03-19T14:00"), listOf(TimeRange(8L, 68_000L)), false, false),
            ScheduleGapFixture("first datetime invalid and second valid", true, listOf("invalid", "2026-03-19T14:00"), listOf(TimeRange(9L, 69_000L)), true, false)
        )

        val metrics = measure(fixtures) { fixture ->
            val rule = ScheduleGapTriggerRule(
                calendarRepository = MetricsCalendarRepository(fixture.freeSlots),
                shortTermMemory = MetricsBudgetMemory(mutableListOf(fixture.budgetAvailable)),
                whisperGenerator = MetricsWhisperGenerator()
            )

            val entities = fixture.normalizedDateTimes.mapIndexed { index, value ->
                Entity(
                    type = EntityType.DATETIME,
                    value = "datetime-$index",
                    normalizedValue = value,
                    confidence = 0.9f,
                    position = 0..3,
                    sourceUtteranceId = "schedule-${fixture.name.hashCode()}"
                )
            }

            val utterance = Utterance(
                id = "schedule-${fixture.name.hashCode()}",
                speaker = SpeakerLabel.SELF,
                text = fixture.name,
                startTimeMs = 0L,
                endTimeMs = 1000L,
                confidence = 0.95f,
                entities = entities,
                isQuestion = fixture.isQuestion
            )

            rule.evaluate(utterance, SESSION_ID) != null
        }

        assertTrue(metrics.precision >= 0.90, "schedule gap precision should remain >= 0.90 but was ${metrics.precision}")
        assertTrue(metrics.recall >= 0.90, "schedule gap recall should remain >= 0.90 but was ${metrics.recall}")
        assertEquals(0, metrics.falsePositive)
        assertEquals(0, metrics.falseNegative)
    }

    @Test
    fun `silence detect fixture precision and recall stay above threshold`() = runTest {
        val fixtures = listOf(
            SilenceFixture("question at threshold", true, 2_500L, listOf(true), false, false, true),
            SilenceFixture("question above threshold", true, 3_100L, listOf(true), false, false, true),
            SilenceFixture("question below threshold", true, 2_499L, listOf(true), false, false, false),
            SilenceFixture("non-question above threshold", false, 3_100L, listOf(true), false, false, false),
            SilenceFixture("question but budget exhausted", true, 2_900L, listOf(false), false, false, false),
            SilenceFixture("question and zero silence", true, 0L, listOf(true), false, false, false),
            SilenceFixture("question with speech reset before silence", true, 2_600L, listOf(true), true, false, true),
            SilenceFixture("question with long silence", true, 10_000L, listOf(true), false, false, true),
            SilenceFixture("question with session change", true, 2_600L, listOf(true), false, true, false),
            SilenceFixture("non-question and short silence", false, 1_000L, listOf(true), false, false, false)
        )

        val metrics = measure(fixtures) { fixture ->
            val rule = SilenceDetectTriggerRule(
                shortTermMemory = MetricsBudgetMemory(fixture.budgetResponses.toMutableList()),
                whisperGenerator = MetricsWhisperGenerator()
            )

            val utterance = Utterance(
                id = "silence-${fixture.name.hashCode()}",
                speaker = SpeakerLabel.SELF,
                text = fixture.name,
                startTimeMs = 0L,
                endTimeMs = 1000L,
                confidence = 0.9f,
                isQuestion = fixture.isQuestion
            )

            rule.evaluate(utterance, SESSION_ID)

            val targetSessionId = if (fixture.useDifferentVadSession) "$SESSION_ID-other" else SESSION_ID
            if (fixture.sendSpeechBeforeSilence) {
                rule.onVadStateChanged(VadState.SPEECH, 0L, targetSessionId)
            }

            rule.onVadStateChanged(VadState.SILENCE, fixture.silenceDurationMs, targetSessionId) != null
        }

        assertTrue(metrics.precision >= 0.90, "silence detect precision should remain >= 0.90 but was ${metrics.precision}")
        assertTrue(metrics.recall >= 0.90, "silence detect recall should remain >= 0.90 but was ${metrics.recall}")
        assertEquals(0, metrics.falsePositive)
        assertEquals(0, metrics.falseNegative)
    }

    @Test
    fun `keyword instant fixture precision and recall stay above threshold`() = runTest {
        val fixtures = listOf(
            KeywordFixture("single active keyword exact text", listOf(keyword("k1", "buy milk", true)), "buy milk", true),
            KeywordFixture("keyword match with mixed case and spacing", listOf(keyword("k2", "buy milk", true)), "Please   BUY    milk now", true),
            KeywordFixture("keyword match with punctuation", listOf(keyword("k3", "buy milk", true)), "Could you buy milk?", true),
            KeywordFixture("no enabled keyword", emptyList(), "buy milk", false),
            KeywordFixture("inactive keyword filtered", listOf(keyword("k4", "buy milk", false)), "buy milk", false),
            KeywordFixture("blank utterance", listOf(keyword("k5", "buy milk", true)), "    ", false),
            KeywordFixture("keyword absent in text", listOf(keyword("k6", "book flight", true)), "buy milk", false),
            KeywordFixture("keyword with irregular spaces", listOf(keyword("k7", "save   this", true)), "Please SAVE this", true),
            KeywordFixture("multiple keywords first non-match second match", listOf(keyword("k8", "archive", true), keyword("k9", "set alarm", true)), "please set alarm", true),
            KeywordFixture("blank keyword ignored", listOf(keyword("k10", "   ", true)), "any text", false)
        )

        val metrics = measure(fixtures) { fixture ->
            val repository = MetricsCustomKeywordRepository(fixture.keywords)
            val rule = KeywordInstantTriggerRule(
                customKeywordRepository = repository,
                whisperGenerator = MetricsWhisperGenerator(),
                scope = backgroundScope
            )
            advanceUntilIdle()

            val utterance = Utterance(
                id = "keyword-${fixture.name.hashCode()}",
                speaker = SpeakerLabel.SELF,
                text = fixture.utteranceText,
                startTimeMs = 0L,
                endTimeMs = 1000L,
                confidence = 0.92f
            )

            rule.evaluate(utterance, SESSION_ID) != null
        }

        assertTrue(metrics.precision >= 0.90, "keyword instant precision should remain >= 0.90 but was ${metrics.precision}")
        assertTrue(metrics.recall >= 0.90, "keyword instant recall should remain >= 0.90 but was ${metrics.recall}")
        assertEquals(0, metrics.falsePositive)
        assertEquals(0, metrics.falseNegative)
    }

    @Test
    fun `end to end path from utterance through detection engine emits keyword event`() = runTest {
        val keywordRule = KeywordInstantTriggerRule(
            customKeywordRepository = MetricsCustomKeywordRepository(
                listOf(keyword("e2e-k1", "save this", true, KeywordActionType.BOOKMARK))
            ),
            whisperGenerator = MetricsWhisperGenerator(),
            scope = backgroundScope
        )
        advanceUntilIdle()

        val eventRepository = MetricsTriggerEventRepository()
        val orchestrator = PipelineOrchestrator()
        val engine = TriggerDetectionEngine(
            triggerRules = setOf(keywordRule),
            triggerEventRepository = eventRepository,
            pipelineOrchestrator = orchestrator,
            sessionManager = MetricsSessionManager(SESSION_ID)
        )

        val utterance = Utterance(
            id = "e2e-utterance-1",
            speaker = SpeakerLabel.SELF,
            text = "Please save this for follow up",
            startTimeMs = 0L,
            endTimeMs = 1_200L,
            confidence = 0.94f
        )

        engine.onUtterance(utterance)

        assertEquals(1, eventRepository.insertedEvents.size)
        val event = eventRepository.insertedEvents.first()
        assertNotNull(event)
        assertEquals(TriggerType.KEYWORD_INSTANT, event.triggerType)
        assertEquals(utterance.id, event.sourceUtteranceId)
        assertEquals("save this", event.metadata["keyword"])
        assertEquals(KeywordActionType.BOOKMARK.name, event.metadata["actionType"])
    }

    private suspend fun <T> measure(fixtures: List<T>, evaluator: suspend (T) -> Boolean): BinaryMetrics {
        var truePositive = 0
        var falsePositive = 0
        var falseNegative = 0

        for (fixture in fixtures) {
            val expectedPositive = when (fixture) {
                is ScheduleGapFixture -> fixture.expectTrigger
                is SilenceFixture -> fixture.expectTrigger
                is KeywordFixture -> fixture.expectTrigger
                else -> error("Unsupported fixture type: ${fixture!!::class.simpleName}")
            }
            val predictedPositive = evaluator(fixture)
            if (predictedPositive && expectedPositive) truePositive += 1
            if (predictedPositive && !expectedPositive) falsePositive += 1
            if (!predictedPositive && expectedPositive) falseNegative += 1
        }

        return BinaryMetrics(truePositive, falsePositive, falseNegative)
    }

    private fun keyword(
        id: String,
        keyword: String,
        isActive: Boolean,
        actionType: KeywordActionType = KeywordActionType.NOTIFY
    ): CustomKeyword {
        return CustomKeyword(
            id = id,
            keyword = keyword,
            actionType = actionType,
            isActive = isActive
        )
    }

    private data class ScheduleGapFixture(
        val name: String,
        val isQuestion: Boolean,
        val normalizedDateTimes: List<String>,
        val freeSlots: List<TimeRange>,
        val budgetAvailable: Boolean,
        val expectTrigger: Boolean
    )

    private data class SilenceFixture(
        val name: String,
        val isQuestion: Boolean,
        val silenceDurationMs: Long,
        val budgetResponses: List<Boolean>,
        val sendSpeechBeforeSilence: Boolean,
        val useDifferentVadSession: Boolean,
        val expectTrigger: Boolean
    )

    private data class KeywordFixture(
        val name: String,
        val keywords: List<CustomKeyword>,
        val utteranceText: String,
        val expectTrigger: Boolean
    )

    private data class BinaryMetrics(
        val truePositive: Int,
        val falsePositive: Int,
        val falseNegative: Int
    ) {
        val precision: Double
            get() {
                val denominator = truePositive + falsePositive
                return if (denominator == 0) 1.0 else truePositive.toDouble() / denominator
            }

        val recall: Double
            get() {
                val denominator = truePositive + falseNegative
                return if (denominator == 0) 1.0 else truePositive.toDouble() / denominator
            }
    }

    private companion object {
        const val SESSION_ID = "metrics-session"
    }
}

private class MetricsWhisperGenerator : WhisperGenerator {
    override fun generate(triggerType: TriggerType, context: Map<String, String>): String {
        return "${triggerType.name}:${context.toSortedMap()}"
    }
}

private class MetricsCalendarRepository(
    private val freeSlots: List<TimeRange>
) : CalendarRepository {
    override suspend fun getBusySlots(startMs: Long, endMs: Long): List<TimeRange> = emptyList()

    override suspend fun getFreeSlots(
        startMs: Long,
        endMs: Long,
        minimumDurationMs: Long
    ): List<TimeRange> = freeSlots
}

private class MetricsBudgetMemory(
    private val responses: MutableList<Boolean>
) : ShortTermMemory {
    override suspend fun add(utterance: Utterance) = Unit

    override suspend fun recent(count: Int): List<Utterance> = emptyList()

    override suspend fun recentInWindow(windowMs: Long): List<Utterance> = emptyList()

    override suspend fun latestQuestion(windowMs: Long): Utterance? = null

    override suspend fun getActiveEntities(type: EntityType): List<Entity> = emptyList()

    override suspend fun getTimeSinceLastUtteranceMs(): Long = Long.MAX_VALUE

    override suspend fun tryConsumeInterventionBudget(): Boolean {
        return if (responses.isEmpty()) {
            true
        } else {
            responses.removeAt(0)
        }
    }

    override suspend fun getRemainingBudget(): Int = 0

    override suspend fun clear() = Unit
}

private class MetricsCustomKeywordRepository(
    keywords: List<CustomKeyword>
) : CustomKeywordRepository {
    private val state = MutableStateFlow(keywords)

    override fun observeEnabledKeywords(): Flow<List<CustomKeyword>> = state

    override suspend fun getAll(): List<CustomKeyword> = state.value

    override suspend fun upsert(keyword: CustomKeyword) = Unit

    override suspend fun delete(id: String) = Unit
}

private class MetricsTriggerEventRepository : TriggerEventRepository {
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

private class MetricsSessionManager(sessionId: String?) : SessionManager {
    private val state = MutableStateFlow(sessionId)

    override val sessionId: StateFlow<String?> = state

    override suspend fun onListeningStateChanged(state: ListeningState) = Unit
}
