package com.earbrief.app.data.calendar

import com.earbrief.app.domain.model.TimeRange

object CalendarFreeTimeCalculator {
    fun calculateFreeSlots(
        busySlots: List<TimeRange>,
        queryWindow: TimeRange,
        minimumDurationMs: Long = 30 * 60_000L
    ): List<TimeRange> {
        if (busySlots.isEmpty()) {
            return if (queryWindow.durationMs >= minimumDurationMs) {
                listOf(queryWindow)
            } else {
                emptyList()
            }
        }

        val sorted = busySlots.sortedBy { it.startMs }
        val merged = mutableListOf<TimeRange>()
        var current = sorted.first()

        for (i in 1 until sorted.size) {
            val next = sorted[i]
            current = if (next.startMs <= current.endMs) {
                TimeRange(current.startMs, maxOf(current.endMs, next.endMs))
            } else {
                merged.add(current)
                next
            }
        }
        merged.add(current)

        val freeSlots = mutableListOf<TimeRange>()
        var cursor = queryWindow.startMs

        for (busy in merged) {
            val busyStart = maxOf(busy.startMs, queryWindow.startMs)
            val busyEnd = minOf(busy.endMs, queryWindow.endMs)

            if (busyStart > queryWindow.endMs || busyEnd < queryWindow.startMs) {
                continue
            }

            if (cursor < busyStart) {
                val gap = TimeRange(cursor, busyStart)
                if (gap.durationMs >= minimumDurationMs) {
                    freeSlots.add(gap)
                }
            }

            cursor = maxOf(cursor, busyEnd)
        }

        if (cursor < queryWindow.endMs) {
            val gap = TimeRange(cursor, queryWindow.endMs)
            if (gap.durationMs >= minimumDurationMs) {
                freeSlots.add(gap)
            }
        }

        return freeSlots
    }
}
