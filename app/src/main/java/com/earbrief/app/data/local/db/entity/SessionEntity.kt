package com.earbrief.app.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "start_time_ms") val startTimeMs: Long,
    @ColumnInfo(name = "end_time_ms") val endTimeMs: Long? = null,
    @ColumnInfo(name = "total_triggers") val totalTriggers: Int = 0,
    @ColumnInfo(name = "delivered_triggers") val deliveredTriggers: Int = 0
)
