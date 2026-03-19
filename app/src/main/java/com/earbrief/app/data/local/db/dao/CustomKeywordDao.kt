package com.earbrief.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.earbrief.app.data.local.db.entity.CustomKeywordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CustomKeywordDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(keyword: CustomKeywordEntity)

    @Query("SELECT * FROM custom_keywords WHERE is_active = 1 ORDER BY keyword ASC")
    fun observeEnabled(): Flow<List<CustomKeywordEntity>>

    @Query("SELECT * FROM custom_keywords ORDER BY keyword ASC")
    suspend fun getAll(): List<CustomKeywordEntity>

    @Query("DELETE FROM custom_keywords WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("UPDATE custom_keywords SET trigger_count = trigger_count + 1, last_triggered_ms = :triggeredAtMs WHERE id = :id")
    suspend fun incrementTriggerCount(id: String, triggeredAtMs: Long)
}
