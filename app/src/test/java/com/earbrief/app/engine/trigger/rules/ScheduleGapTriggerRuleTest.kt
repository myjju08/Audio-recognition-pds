package com.earbrief.app.engine.trigger.rules

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.TimeRange
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.repository.CalendarRepository
import com.earbrief.app.engine.context.ShortTermMemory
import com.earbrief.app.engine.trigger.WhisperGenerator
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("ScheduleGapTriggerRule")
class ScheduleGapTriggerRuleTest {

    @Test
    fun `question with datetime and free slots emits event`() = runTest {
        val calendarRepository = FakeCalendarRepository(
            freeSlots = listOf(
                TimeRange(startMs = 1_710_000_000_000L, endMs = 1_710_000_900_000L),
                TimeRange(startMs = 1_710_001_000_000L, endMs = 1_710_001_600_000L)
            )
        )
        val shortTermMemory = FakeShortTermMemory(canConsumeBudget = true)
        val whisperGenerator = DeterministicWhisperGenerator()
        val rule = ScheduleGapTriggerRule(calendarRepository, shortTermMemory, whisperGenerator)

        val event = rule.evaluate(
            utterance = createUtterance(
                isQuestion = true,
                entities = listOf(createDateTimeEntity("2026-03-19T14:00"))
            ),
            sessionId = "session-1"
        )

        assertNotNull(event)
        assertEquals(TriggerType.SCHEDULE_GAP, event?.triggerType)
        assertEquals("15", event?.metadata?.get("gapMinutes"))
        assertEquals("1710000000000-1710000900000", event?.metadata?.get("slot1"))
        assertEquals("1710001000000-1710001600000", event?.metadata?.get("slot2"))
    }

    @Test
    fun `no datetime returns null`() = runTest {
        val rule = ScheduleGapTriggerRule(
            calendarRepository = FakeCalendarRepository(freeSlots = listOf(TimeRange(1L, 2L))),
            shortTermMemory = FakeShortTermMemory(canConsumeBudget = true),
            whisperGenerator = DeterministicWhisperGenerator()
        )

        val event = rule.evaluate(
            utterance = createUtterance(isQuestion = true, entities = emptyList()),
            sessionId = "session-1"
        )

        assertNull(event)
    }

    @Test
    fun `datetime but not question returns null`() = runTest {
        val rule = ScheduleGapTriggerRule(
            calendarRepository = FakeCalendarRepository(freeSlots = listOf(TimeRange(1L, 2L))),
            shortTermMemory = FakeShortTermMemory(canConsumeBudget = true),
            whisperGenerator = DeterministicWhisperGenerator()
        )

        val event = rule.evaluate(
            utterance = createUtterance(
                isQuestion = false,
                entities = listOf(createDateTimeEntity("2026-03-19T14:00"))
            ),
            sessionId = "session-1"
        )

        assertNull(event)
    }

    @Test
    fun `no free slots returns null`() = runTest {
        val rule = ScheduleGapTriggerRule(
            calendarRepository = FakeCalendarRepository(freeSlots = emptyList()),
            shortTermMemory = FakeShortTermMemory(canConsumeBudget = true),
            whisperGenerator = DeterministicWhisperGenerator()
        )

        val event = rule.evaluate(
            utterance = createUtterance(
                isQuestion = true,
                entities = listOf(createDateTimeEntity("2026-03-19T14:00"))
            ),
            sessionId = "session-1"
        )

        assertNull(event)
    }

    @Test
    fun `budget exhausted returns null`() = runTest {
        val rule = ScheduleGapTriggerRule(
            calendarRepository = FakeCalendarRepository(freeSlots = listOf(TimeRange(1L, 2L))),
            shortTermMemory = FakeShortTermMemory(canConsumeBudget = false),
            whisperGenerator = DeterministicWhisperGenerator()
        )

        val event = rule.evaluate(
            utterance = createUtterance(
                isQuestion = true,
                entities = listOf(createDateTimeEntity("2026-03-19T14:00"))
            ),
            sessionId = "session-1"
        )

        assertNull(event)
    }

    @Test
    fun `whisper text uses deterministic slot context`() = runTest {
        val rule = ScheduleGapTriggerRule(
            calendarRepository = FakeCalendarRepository(
                freeSlots = listOf(
                    TimeRange(startMs = 10_000L, endMs = 70_000L),
                    TimeRange(startMs = 80_000L, endMs = 140_000L)
                )
            ),
            shortTermMemory = FakeShortTermMemory(canConsumeBudget = true),
            whisperGenerator = DeterministicWhisperGenerator()
        )

        val event = rule.evaluate(
            utterance = createUtterance(
                isQuestion = true,
                entities = listOf(createDateTimeEntity("2026-03-19T14:00"))
            ),
            sessionId = "session-1"
        )

        assertEquals(
            "SCHEDULE_GAP|gapMinutes=1|slot1=10000-70000|slot2=80000-140000",
            event?.whisperText
        )
    }

    private fun createUtterance(isQuestion: Boolean, entities: List<Entity>): Utterance {
        return Utterance(
            id = "u-1",
            speaker = SpeakerLabel.SELF,
            text = "내일 오후 두 시쯤 시간 비어?",
            startTimeMs = 1_000L,
            endTimeMs = 2_000L,
            confidence = 0.95f,
            entities = entities,
            isQuestion = isQuestion
        )
    }

    private fun createDateTimeEntity(normalizedValue: String): Entity {
        return Entity(
            type = EntityType.DATETIME,
            value = "내일 오후 두 시",
            normalizedValue = normalizedValue,
            confidence = 0.9f,
            position = 0..7,
            sourceUtteranceId = "u-1"
        )
    }
}

private class FakeCalendarRepository(
    private val freeSlots: List<TimeRange>
) : CalendarRepository {
    override suspend fun getBusySlots(startMs: Long, endMs: Long): List<TimeRange> = emptyList()

    override suspend fun getFreeSlots(
        startMs: Long,
        endMs: Long,
        minimumDurationMs: Long
    ): List<TimeRange> = freeSlots
}

private class FakeShortTermMemory(
    private val canConsumeBudget: Boolean
) : ShortTermMemory {
    override suspend fun add(utterance: Utterance) = Unit

    override suspend fun recent(count: Int): List<Utterance> = emptyList()

    override suspend fun recentInWindow(windowMs: Long): List<Utterance> = emptyList()

    override suspend fun latestQuestion(windowMs: Long): Utterance? = null

    override suspend fun getActiveEntities(type: EntityType): List<Entity> = emptyList()

    override suspend fun getTimeSinceLastUtteranceMs(): Long = Long.MAX_VALUE

    override suspend fun tryConsumeInterventionBudget(): Boolean = canConsumeBudget

    override suspend fun getRemainingBudget(): Int = if (canConsumeBudget) 1 else 0

    override suspend fun clear() = Unit
}

private class DeterministicWhisperGenerator : WhisperGenerator {
    override fun generate(triggerType: TriggerType, context: Map<String, String>): String {
        return buildString {
            append(triggerType.name)
            context.toSortedMap().forEach { (key, value) ->
                append("|")
                append(key)
                append("=")
                append(value)
            }
        }
    }
}
