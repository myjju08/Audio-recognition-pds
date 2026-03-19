package com.earbrief.app.data.repository

import com.earbrief.app.data.local.db.dao.SessionDao
import com.earbrief.app.data.local.db.entity.SessionEntity
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class RoomSessionRepositoryTest {

    private val dao: SessionDao = mockk()
    private val repository = RoomSessionRepository(dao)

    @Test
    fun `startSession inserts session entity`() = runTest {
        val captured = slot<SessionEntity>()
        coEvery { dao.insert(capture(captured)) } just Runs

        repository.startSession(sessionId = "session-1", startedAtMs = 100L)

        assertThat(captured.captured).isEqualTo(
            SessionEntity(
                id = "session-1",
                startTimeMs = 100L,
                endTimeMs = null,
                totalTriggers = 0,
                deliveredTriggers = 0,
            )
        )
    }

    @Test
    fun `endSession delegates to markEnded`() = runTest {
        coEvery { dao.markEnded(any(), any()) } just Runs

        repository.endSession(sessionId = "session-2", endedAtMs = 500L)

        coVerify(exactly = 1) { dao.markEnded("session-2", 500L) }
    }
}
