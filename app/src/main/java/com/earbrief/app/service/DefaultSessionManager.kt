package com.earbrief.app.service

import com.earbrief.app.domain.model.ListeningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultSessionManager @Inject constructor() : SessionManager {
    private val _sessionId = MutableStateFlow<String?>(null)
    override val sessionId: StateFlow<String?> = _sessionId.asStateFlow()

    override suspend fun onListeningStateChanged(state: ListeningState) {
        when (state) {
            ListeningState.LISTENING -> {
                if (_sessionId.value == null) {
                    _sessionId.value = UUID.randomUUID().toString()
                }
            }

            ListeningState.IDLE -> {
                _sessionId.value = null
            }

            else -> Unit
        }
    }
}
