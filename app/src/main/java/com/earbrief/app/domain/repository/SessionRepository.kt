package com.earbrief.app.domain.repository

interface SessionRepository {
    suspend fun startSession(sessionId: String, startedAtMs: Long)
    suspend fun endSession(sessionId: String, endedAtMs: Long)
}
