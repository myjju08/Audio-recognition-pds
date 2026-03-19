package com.earbrief.app.data.calendar

import android.content.Context
import android.provider.CalendarContract
import com.earbrief.app.di.IoDispatcher
import com.earbrief.app.domain.model.TimeRange
import com.earbrief.app.domain.repository.CalendarRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContentResolverCalendarRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : CalendarRepository {

    override suspend fun getBusySlots(startMs: Long, endMs: Long): List<TimeRange> =
        withContext(ioDispatcher) {
            val busySlots = mutableListOf<TimeRange>()
            val projection = arrayOf(
                CalendarContract.Instances.BEGIN,
                CalendarContract.Instances.END
            )
            val selection = "${CalendarContract.Instances.BEGIN} < ? AND ${CalendarContract.Instances.END} > ?"
            val selectionArgs = arrayOf(endMs.toString(), startMs.toString())

            try {
                val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
                    .appendPath(startMs.toString())
                    .appendPath(endMs.toString())
                    .build()

                context.contentResolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                    val beginIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
                    val endIdx = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)

                    while (cursor.moveToNext()) {
                        val eventStart = cursor.getLong(beginIdx)
                        val eventEnd = cursor.getLong(endIdx)
                        busySlots.add(TimeRange(eventStart, eventEnd))
                    }
                }
            } catch (_: SecurityException) {
                busySlots.clear()
            } catch (_: Exception) {
                busySlots.clear()
            }

            busySlots
        }

    override suspend fun getFreeSlots(
        startMs: Long,
        endMs: Long,
        minimumDurationMs: Long
    ): List<TimeRange> {
        val busySlots = getBusySlots(startMs, endMs)

        return CalendarFreeTimeCalculator.calculateFreeSlots(
            busySlots = busySlots,
            queryWindow = TimeRange(startMs, endMs),
            minimumDurationMs = minimumDurationMs
        )
    }
}
