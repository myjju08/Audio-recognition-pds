package com.earbrief.app.domain.repository

import com.earbrief.app.domain.model.TimeRange

interface CalendarRepository {
    suspend fun getBusySlots(startMs: Long, endMs: Long): List<TimeRange>
    suspend fun getFreeSlots(startMs: Long, endMs: Long, minimumDurationMs: Long = 30 * 60_000L): List<TimeRange>
}
