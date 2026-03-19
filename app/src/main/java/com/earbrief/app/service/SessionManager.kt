package com.earbrief.app.service

import com.earbrief.app.domain.model.ListeningState
import kotlinx.coroutines.flow.StateFlow

interface SessionManager {
    val sessionId: StateFlow<String?>
    suspend fun onListeningStateChanged(state: ListeningState)
}
