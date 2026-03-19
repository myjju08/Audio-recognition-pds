package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.domain.repository.TriggerEventRepository
import com.earbrief.app.service.PipelineOrchestrator
import com.earbrief.app.service.SessionManager
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerDetectionEngine @Inject constructor(
    private val triggerRules: Set<@JvmSuppressWildcards TriggerRule>,
    private val triggerEventRepository: TriggerEventRepository,
    private val pipelineOrchestrator: PipelineOrchestrator,
    private val sessionManager: SessionManager
) {
    private var lastSilenceDurationMs: Long = 0L

    suspend fun onUtterance(utterance: Utterance) {
        val sessionId = sessionManager.sessionId.first() ?: return

        for (rule in triggerRules) {
            val event = rule.evaluate(utterance, sessionId) ?: continue
            persistAndPublish(event)
            return
        }
    }

    suspend fun onVadStateChanged(vadState: VadState, silenceDurationMs: Long) {
        lastSilenceDurationMs = silenceDurationMs
        val sessionId = sessionManager.sessionId.first() ?: return

        for (rule in triggerRules) {
            val event = rule.onVadStateChanged(vadState, silenceDurationMs, sessionId) ?: continue
            persistAndPublish(event)
        }
    }

    fun getLastSilenceDurationMs(): Long = lastSilenceDurationMs

    fun reset() {
        lastSilenceDurationMs = 0L
        triggerRules.forEach { it.reset() }
    }

    private suspend fun persistAndPublish(event: TriggerEvent) {
        triggerEventRepository.insert(event)
        pipelineOrchestrator.publishTriggerEvent(event)
    }
}
