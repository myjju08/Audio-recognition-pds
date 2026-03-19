package com.earbrief.app.data.repository

import com.earbrief.app.data.local.db.dao.SessionDao
import com.earbrief.app.data.local.db.entity.SessionEntity
import com.earbrief.app.domain.repository.SessionRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomSessionRepository @Inject constructor(
    private val sessionDao: SessionDao
) : SessionRepository {

    override suspend fun startSession(sessionId: String, startedAtMs: Long) {
        sessionDao.insert(
            SessionEntity(
                id = sessionId,
                startTimeMs = startedAtMs,
            )
        )
    }

    override suspend fun endSession(sessionId: String, endedAtMs: Long) {
        sessionDao.markEnded(sessionId = sessionId, endedAtMs = endedAtMs)
    }
}
