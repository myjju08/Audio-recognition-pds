package com.earbrief.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.earbrief.app.data.local.db.entity.TriggerEventEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: TriggerEventEntity)

    @Query("SELECT * FROM trigger_events WHERE session_id = :sessionId ORDER BY created_at_ms DESC")
    fun observeBySession(sessionId: String): Flow<List<TriggerEventEntity>>

    @Query("SELECT * FROM trigger_events WHERE session_id = :sessionId ORDER BY created_at_ms DESC")
    suspend fun getBySession(sessionId: String): List<TriggerEventEntity>

    @Query("DELETE FROM trigger_events WHERE created_at_ms < :timestampMs")
    suspend fun deleteOlderThan(timestampMs: Long)

    @Query("SELECT COUNT(*) FROM trigger_events WHERE session_id = :sessionId")
    suspend fun countBySession(sessionId: String): Int
}
