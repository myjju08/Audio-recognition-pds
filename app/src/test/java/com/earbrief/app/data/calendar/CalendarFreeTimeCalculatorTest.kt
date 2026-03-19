package com.earbrief.app.data.calendar

import com.earbrief.app.domain.model.TimeRange
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("CalendarFreeTimeCalculator")
class CalendarFreeTimeCalculatorTest {

    private val base = 1_000_000L
    private val hour = 3_600_000L
    private val minute = 60_000L
    private val window = TimeRange(base, base + (8 * hour))

    @Nested
    @DisplayName("No busy slots")
    inner class NoBusy {

        @Test
        fun `entire window is free`() {
            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = emptyList(),
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(listOf(window), result)
        }

        @Test
        fun `window shorter than minimum returns empty`() {
            val shortWindow = TimeRange(base, base + (20 * minute))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = emptyList(),
                queryWindow = shortWindow,
                minimumDurationMs = 30 * minute
            )

            assertEquals(emptyList<TimeRange>(), result)
        }
    }

    @Nested
    @DisplayName("Single busy slot")
    inner class SingleBusy {

        @Test
        fun `busy in middle returns two free slots`() {
            val busy = listOf(TimeRange(base + hour, base + (2 * hour)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(
                listOf(
                    TimeRange(base, base + hour),
                    TimeRange(base + (2 * hour), base + (8 * hour))
                ),
                result
            )
        }

        @Test
        fun `busy at start returns one free slot at end`() {
            val busy = listOf(TimeRange(base, base + (2 * hour)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(listOf(TimeRange(base + (2 * hour), base + (8 * hour))), result)
        }

        @Test
        fun `busy at end returns one free slot at start`() {
            val busy = listOf(TimeRange(base + (6 * hour), base + (8 * hour)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(listOf(TimeRange(base, base + (6 * hour))), result)
        }

        @Test
        fun `busy covers entire window returns no free slots`() {
            val busy = listOf(TimeRange(base, base + (8 * hour)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(emptyList<TimeRange>(), result)
        }
    }

    @Nested
    @DisplayName("Overlapping busy slots")
    inner class Overlapping {

        @Test
        fun `two overlapping slots merged and free gaps are correct`() {
            val busy = listOf(
                TimeRange(base + hour, base + (3 * hour)),
                TimeRange(base + (2 * hour), base + (4 * hour))
            )

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(
                listOf(
                    TimeRange(base, base + hour),
                    TimeRange(base + (4 * hour), base + (8 * hour))
                ),
                result
            )
        }

        @Test
        fun `three slots with two overlapping merge correctly`() {
            val busy = listOf(
                TimeRange(base + hour, base + (2 * hour)),
                TimeRange(base + (3 * hour), base + (5 * hour)),
                TimeRange(base + (4 * hour), base + (6 * hour))
            )

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(
                listOf(
                    TimeRange(base, base + hour),
                    TimeRange(base + (2 * hour), base + (3 * hour)),
                    TimeRange(base + (6 * hour), base + (8 * hour))
                ),
                result
            )
        }
    }

    @Nested
    @DisplayName("Adjacent busy slots")
    inner class Adjacent {

        @Test
        fun `back to back busy slots creates no artificial gap`() {
            val busy = listOf(
                TimeRange(base + hour, base + (2 * hour)),
                TimeRange(base + (2 * hour), base + (3 * hour))
            )

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(
                listOf(
                    TimeRange(base, base + hour),
                    TimeRange(base + (3 * hour), base + (8 * hour))
                ),
                result
            )
        }
    }

    @Nested
    @DisplayName("Minimum duration filter")
    inner class MinDuration {

        @Test
        fun `20 minute gap with 30 minute minimum is filtered out`() {
            val shortGapWindow = TimeRange(base, base + (50 * minute))
            val busy = listOf(TimeRange(base + (20 * minute), base + (50 * minute)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = shortGapWindow,
                minimumDurationMs = 30 * minute
            )

            assertEquals(emptyList<TimeRange>(), result)
        }

        @Test
        fun `30 minute gap with 30 minute minimum is included`() {
            val exactGapWindow = TimeRange(base, base + (60 * minute))
            val busy = listOf(TimeRange(base + (30 * minute), base + (60 * minute)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = exactGapWindow,
                minimumDurationMs = 30 * minute
            )

            assertEquals(listOf(TimeRange(base, base + (30 * minute))), result)
        }
    }

    @Nested
    @DisplayName("Edge cases")
    inner class EdgeCases {

        @Test
        fun `empty busy list returns full window`() {
            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = emptyList(),
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(listOf(window), result)
        }

        @Test
        fun `busy slots outside window are ignored`() {
            val busy = listOf(
                TimeRange(base - (5 * hour), base - (3 * hour)),
                TimeRange(base + (9 * hour), base + (10 * hour))
            )

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = window,
                minimumDurationMs = 30 * minute
            )

            assertEquals(listOf(window), result)
        }

        @Test
        fun `all day event across query window returns no free slots`() {
            val busy = listOf(TimeRange(base, base + (24 * hour)))

            val result = CalendarFreeTimeCalculator.calculateFreeSlots(
                busySlots = busy,
                queryWindow = TimeRange(base, base + (24 * hour)),
                minimumDurationMs = 30 * minute
            )

            assertEquals(emptyList<TimeRange>(), result)
        }
    }
}
