package com.earbrief.app.domain.repository

import com.earbrief.app.domain.model.TriggerEvent
import kotlinx.coroutines.flow.Flow

interface TriggerEventRepository {
    suspend fun insert(event: TriggerEvent)
    fun observeBySession(sessionId: String): Flow<List<TriggerEvent>>
    suspend fun getBySession(sessionId: String): List<TriggerEvent>
    suspend fun deleteOlderThan(timestampMs: Long)
}
