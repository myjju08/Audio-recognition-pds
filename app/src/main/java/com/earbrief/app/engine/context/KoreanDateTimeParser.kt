package com.earbrief.app.engine.context

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class DateTimeParseResult(
    val value: String,
    val normalizedValue: String,
    val range: IntRange,
    val confidence: Float
)

class KoreanDateTimeParser {
    private val zoneId: ZoneId = ZoneId.systemDefault()
    private val outputFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    private val datePattern =
        """오늘|내일|모레|글피|(?:이번|다음|다다음)\s*주\s*(?:월요일|화요일|수요일|목요일|금요일|토요일|일요일)|\d{4}\s*년\s*\d{1,2}\s*월\s*\d{1,2}\s*일|\d{1,2}\s*월\s*\d{1,2}\s*일"""

    private val timePattern =
        """(?:오전|오후)\s*\d{1,2}\s*시(?:\s*\d{1,2}\s*분|\s*반)?|\d{1,2}\s*시\s*\d{1,2}\s*분|\d{1,2}\s*시\s*반|\d{1,2}\s*시(?!\s*간)|점심|저녁|아침"""

    private val dateTimeRegex = Regex("($datePattern)(?:\\s*($timePattern))?")
    private val relativeTimeRegex = Regex("(\\d+)\\s*(시간|분)\\s*후")
    private val timeOnlyRegex = Regex(timePattern)

    private val fullDateRegex = Regex("(\\d{4})\\s*년\\s*(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
    private val monthDayRegex = Regex("(\\d{1,2})\\s*월\\s*(\\d{1,2})\\s*일")
    private val weekDayRegex = Regex("(이번|다음|다다음)\\s*주\\s*(월요일|화요일|수요일|목요일|금요일|토요일|일요일)")

    private val periodHourRegex = Regex("(오전|오후)\\s*(\\d{1,2})\\s*시(?:\\s*(\\d{1,2})\\s*분|\\s*반)?")
    private val hourMinuteRegex = Regex("(\\d{1,2})\\s*시\\s*(\\d{1,2})\\s*분")
    private val halfPastRegex = Regex("(\\d{1,2})\\s*시\\s*반")
    private val hourOnlyRegex = Regex("(\\d{1,2})\\s*시(?!\\s*간)")

    private val relativeDays = mapOf("오늘" to 0L, "내일" to 1L, "모레" to 2L, "글피" to 3L)
    private val weekOffsets = mapOf("이번" to 0L, "다음" to 1L, "다다음" to 2L)
    private val weekDayOffsets = mapOf(
        "월요일" to 0L,
        "화요일" to 1L,
        "수요일" to 2L,
        "목요일" to 3L,
        "금요일" to 4L,
        "토요일" to 5L,
        "일요일" to 6L
    )

    fun parse(text: String, referenceTimeMs: Long = System.currentTimeMillis()): List<DateTimeParseResult> {
        val baseDateTime = Instant.ofEpochMilli(referenceTimeMs).atZone(zoneId).toLocalDateTime()
        val candidates = mutableListOf<DateTimeParseResult>()

        relativeTimeRegex.findAll(text).forEach { match ->
            val amount = match.groupValues[1].toLongOrNull() ?: return@forEach
            val unit = match.groupValues[2]
            val normalized = when (unit) {
                "시간" -> baseDateTime.plusHours(amount)
                else -> baseDateTime.plusMinutes(amount)
            }
            addCandidate(
                candidates,
                DateTimeParseResult(
                    value = match.value,
                    normalizedValue = normalized.format(outputFormatter),
                    range = match.range,
                    confidence = 0.92f
                )
            )
        }

        dateTimeRegex.findAll(text).forEach { match ->
            val datePart = match.groupValues[1]
            val timePart = match.groupValues.getOrNull(2)?.takeIf { it.isNotBlank() }
            if (timePart == null && relativeDays.containsKey(datePart.trim())) return@forEach
            val date = parseDate(datePart, baseDateTime.toLocalDate()) ?: return@forEach
            val time = parseTime(timePart) ?: LocalTime.of(0, 0)
            val confidence = if (timePart != null) 0.95f else 0.84f

            addCandidate(
                candidates,
                DateTimeParseResult(
                    value = match.value.trim(),
                    normalizedValue = date.atTime(time).format(outputFormatter),
                    range = match.range,
                    confidence = confidence
                )
            )
        }

        timeOnlyRegex.findAll(text).forEach { match ->
            if (candidates.any { overlaps(it.range, match.range) }) return@forEach
            val time = parseTime(match.value) ?: return@forEach
            val date = baseDateTime.toLocalDate()

            addCandidate(
                candidates,
                DateTimeParseResult(
                    value = match.value.trim(),
                    normalizedValue = date.atTime(time).format(outputFormatter),
                    range = match.range,
                    confidence = 0.8f
                )
            )
        }

        return candidates.sortedBy { it.range.first }
    }

    private fun parseDate(dateText: String, baseDate: LocalDate): LocalDate? {
        relativeDays[dateText.trim()]?.let { return baseDate.plusDays(it) }

        weekDayRegex.find(dateText)?.let { match ->
            val weekPrefix = match.groupValues[1]
            val dayLabel = match.groupValues[2]
            val weekOffset = weekOffsets[weekPrefix] ?: return null
            val dayOffset = weekDayOffsets[dayLabel] ?: return null
            val mondayThisWeek = baseDate.minusDays((baseDate.dayOfWeek.value - 1).toLong())
            return mondayThisWeek.plusWeeks(weekOffset).plusDays(dayOffset)
        }

        fullDateRegex.find(dateText)?.let { match ->
            val year = match.groupValues[1].toIntOrNull() ?: return null
            val month = match.groupValues[2].toIntOrNull() ?: return null
            val day = match.groupValues[3].toIntOrNull() ?: return null
            return runCatching { LocalDate.of(year, month, day) }.getOrNull()
        }

        monthDayRegex.find(dateText)?.let { match ->
            val month = match.groupValues[1].toIntOrNull() ?: return null
            val day = match.groupValues[2].toIntOrNull() ?: return null
            return runCatching { LocalDate.of(baseDate.year, month, day) }.getOrNull()
        }

        return null
    }

    private fun parseTime(timeText: String?): LocalTime? {
        if (timeText.isNullOrBlank()) return null

        val trimmed = timeText.trim()
        when (trimmed) {
            "아침" -> return LocalTime.of(8, 0)
            "점심" -> return LocalTime.of(12, 0)
            "저녁" -> return LocalTime.of(18, 0)
        }

        periodHourRegex.matchEntire(trimmed)?.let { match ->
            val period = match.groupValues[1]
            val hourRaw = match.groupValues[2].toIntOrNull() ?: return null
            val minute = when {
                match.groupValues[3].isNotBlank() -> match.groupValues[3].toIntOrNull() ?: return null
                trimmed.contains("반") -> 30
                else -> 0
            }

            val normalizedHour = when (period) {
                "오전" -> if (hourRaw == 12) 0 else hourRaw
                "오후" -> if (hourRaw == 12) 12 else hourRaw + 12
                else -> hourRaw
            }
            return runCatching { LocalTime.of(normalizedHour, minute) }.getOrNull()
        }

        hourMinuteRegex.matchEntire(trimmed)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            val minute = match.groupValues[2].toIntOrNull() ?: return null
            return runCatching { LocalTime.of(hour, minute) }.getOrNull()
        }

        halfPastRegex.matchEntire(trimmed)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            return runCatching { LocalTime.of(hour, 30) }.getOrNull()
        }

        hourOnlyRegex.matchEntire(trimmed)?.let { match ->
            val hour = match.groupValues[1].toIntOrNull() ?: return null
            return runCatching { LocalTime.of(hour, 0) }.getOrNull()
        }

        return null
    }

    private fun addCandidate(results: MutableList<DateTimeParseResult>, candidate: DateTimeParseResult) {
        val overlappingIndex = results.indexOfFirst { overlaps(it.range, candidate.range) }
        if (overlappingIndex < 0) {
            results.add(candidate)
            return
        }

        val existing = results[overlappingIndex]
        if (candidate.range.count() > existing.range.count()) {
            results[overlappingIndex] = candidate
        }
    }

    private fun overlaps(first: IntRange, second: IntRange): Boolean {
        return first.first <= second.last && second.first <= first.last
    }
}
