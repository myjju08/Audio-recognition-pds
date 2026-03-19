package com.earbrief.app.engine.trigger

import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState

interface TriggerRule {
    suspend fun evaluate(utterance: Utterance, sessionId: String): TriggerEvent?
    suspend fun onVadStateChanged(vadState: VadState, silenceDurationMs: Long, sessionId: String): TriggerEvent? = null
    fun reset()
}
