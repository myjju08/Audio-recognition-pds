package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.Utterance
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

@Singleton
class InMemoryShortTermMemory @Inject constructor(
    private val config: ShortTermMemoryConfig
) : ShortTermMemory {
    private val mutex = Mutex()
    private val utterances = ArrayDeque<Utterance>()
    private val activeEntities = mutableMapOf<EntityType, MutableList<Entity>>()
    private var interventionBudget = config.maxInterventionsPerWindow
    private var budgetWindowStartMs = 0L

    var timeProvider: () -> Long = { System.currentTimeMillis() }

    override suspend fun add(utterance: Utterance) = mutex.withLock {
        utterances.addLast(utterance)
        utterance.entities.forEach { entity ->
            activeEntities.getOrPut(entity.type) { mutableListOf() }.add(entity)
        }
        while (utterances.size > config.maxUtterances) {
            utterances.removeFirst()
        }
        evictExpiredLocked()
    }

    override suspend fun recent(count: Int): List<Utterance> = mutex.withLock {
        evictExpiredLocked()
        utterances.takeLast(count)
    }

    override suspend fun recentInWindow(windowMs: Long): List<Utterance> = mutex.withLock {
        evictExpiredLocked()
        val cutoff = timeProvider() - windowMs
        utterances.filter { it.endTimeMs >= cutoff }
    }

    override suspend fun latestQuestion(windowMs: Long): Utterance? = mutex.withLock {
        evictExpiredLocked()
        val cutoff = timeProvider() - windowMs
        utterances.lastOrNull { it.isQuestion && it.endTimeMs >= cutoff }
    }

    override suspend fun getActiveEntities(type: EntityType): List<Entity> = mutex.withLock {
        activeEntities[type]?.toList() ?: emptyList()
    }

    override suspend fun getTimeSinceLastUtteranceMs(): Long = mutex.withLock {
        val last = utterances.lastOrNull() ?: return@withLock Long.MAX_VALUE
        timeProvider() - last.endTimeMs
    }

    override suspend fun tryConsumeInterventionBudget(): Boolean = mutex.withLock {
        val now = timeProvider()
        if (now - budgetWindowStartMs > config.windowDurationMs) {
            interventionBudget = config.maxInterventionsPerWindow
            budgetWindowStartMs = now
        }
        if (interventionBudget > 0) {
            interventionBudget--
            if (budgetWindowStartMs == 0L) {
                budgetWindowStartMs = now
            }
            true
        } else {
            false
        }
    }

    override suspend fun getRemainingBudget(): Int = mutex.withLock {
        val now = timeProvider()
        if (now - budgetWindowStartMs > config.windowDurationMs) {
            config.maxInterventionsPerWindow
        } else {
            interventionBudget
        }
    }

    override suspend fun clear() = mutex.withLock {
        utterances.clear()
        activeEntities.clear()
        interventionBudget = config.maxInterventionsPerWindow
        budgetWindowStartMs = 0L
    }

    private fun evictExpiredLocked() {
        val cutoff = timeProvider() - config.windowDurationMs
        while (utterances.isNotEmpty() && utterances.first().endTimeMs < cutoff) {
            utterances.removeFirst()
        }
    }
}
