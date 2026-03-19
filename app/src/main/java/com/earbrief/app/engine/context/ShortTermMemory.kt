package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.Utterance

interface ShortTermMemory {
    suspend fun add(utterance: Utterance)
    suspend fun recent(count: Int = 10): List<Utterance>
    suspend fun recentInWindow(windowMs: Long = 10 * 60_000L): List<Utterance>
    suspend fun latestQuestion(windowMs: Long = 10 * 60_000L): Utterance?
    suspend fun getActiveEntities(type: EntityType): List<Entity>
    suspend fun getTimeSinceLastUtteranceMs(): Long
    suspend fun tryConsumeInterventionBudget(): Boolean
    suspend fun getRemainingBudget(): Int
    suspend fun clear()
}
