package com.earbrief.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.earbrief.app.data.local.db.entity.SessionEntity

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("UPDATE sessions SET end_time_ms = :endedAtMs WHERE id = :sessionId")
    suspend fun markEnded(sessionId: String, endedAtMs: Long)

    @Query("SELECT * FROM sessions ORDER BY start_time_ms DESC LIMIT :limit")
    suspend fun getRecent(limit: Int = 20): List<SessionEntity>

    @Query("UPDATE sessions SET total_triggers = total_triggers + 1 WHERE id = :sessionId")
    suspend fun incrementTriggerCount(sessionId: String)
}
