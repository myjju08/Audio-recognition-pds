package com.earbrief.app.engine.trigger.rules

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.TimeRange
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.repository.CalendarRepository
import com.earbrief.app.engine.context.ShortTermMemory
import com.earbrief.app.engine.trigger.TriggerRule
import com.earbrief.app.engine.trigger.WhisperGenerator
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScheduleGapTriggerRule @Inject constructor(
    private val calendarRepository: CalendarRepository,
    private val shortTermMemory: ShortTermMemory,
    private val whisperGenerator: WhisperGenerator
) : TriggerRule {

    override suspend fun evaluate(
        utterance: com.earbrief.app.domain.model.Utterance,
        sessionId: String
    ): TriggerEvent? {
        if (!utterance.isQuestion) return null

        val datetimeEntities = utterance.entities.filter { it.type == EntityType.DATETIME }
        val firstDateTimeEntity = datetimeEntities.firstOrNull() ?: return null
        val queryWindow = resolveQueryWindow(firstDateTimeEntity) ?: return null

        val freeSlots = calendarRepository.getFreeSlots(
            startMs = queryWindow.startMs,
            endMs = queryWindow.endMs
        )
        if (freeSlots.isEmpty()) return null
        if (!shortTermMemory.tryConsumeInterventionBudget()) return null

        val selectedSlots = freeSlots.take(2)
        val firstGapMinutes = (selectedSlots.first().durationMs / 60_000L).coerceAtLeast(1L)
        val whisperContext = mapOf(
            KEY_GAP_MINUTES to firstGapMinutes.toString(),
            KEY_SLOT_1 to slotToString(selectedSlots[0])
        ) + if (selectedSlots.size > 1) mapOf(KEY_SLOT_2 to slotToString(selectedSlots[1])) else emptyMap()

        return TriggerEvent(
            sessionId = sessionId,
            triggerType = TriggerType.SCHEDULE_GAP,
            sourceUtteranceId = utterance.id,
            sourceText = utterance.text,
            detectedEntities = datetimeEntities,
            whisperText = whisperGenerator.generate(TriggerType.SCHEDULE_GAP, whisperContext),
            confidence = 0.86f,
            priority = 2,
            metadata = whisperContext
        )
    }

    override fun reset() = Unit

    private fun resolveQueryWindow(entity: Entity): TimeRange? {
        val normalized = entity.normalizedValue.trim()

        parseLocalDateTime(normalized)?.let { dateTime ->
            val centerMs = dateTime.atZone(zoneId).toInstant().toEpochMilli()
            return TimeRange(
                startMs = centerMs - TWO_HOURS_MS,
                endMs = centerMs + TWO_HOURS_MS
            )
        }

        parseLocalDate(normalized)?.let { date ->
            val startMs = date.atTime(LocalTime.of(9, 0)).atZone(zoneId).toInstant().toEpochMilli()
            val endMs = date.atTime(LocalTime.of(18, 0)).atZone(zoneId).toInstant().toEpochMilli()
            return TimeRange(startMs = startMs, endMs = endMs)
        }

        return null
    }

    private fun parseLocalDateTime(value: String): LocalDateTime? {
        return runCatching { LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME) }
            .recoverCatching { LocalDateTime.parse(value, minutePrecisionFormatter) }
            .getOrNull()
    }

    private fun parseLocalDate(value: String): LocalDate? {
        return runCatching { LocalDate.parse(value, DateTimeFormatter.ISO_LOCAL_DATE) }.getOrNull()
    }

    private fun slotToString(slot: TimeRange): String = "${slot.startMs}-${slot.endMs}"

    private companion object {
        const val KEY_GAP_MINUTES = "gapMinutes"
        const val KEY_SLOT_1 = "slot1"
        const val KEY_SLOT_2 = "slot2"
        const val TWO_HOURS_MS = 2 * 60 * 60_000L
        val minutePrecisionFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        val zoneId: ZoneId = ZoneId.systemDefault()
    }
}
