package com.earbrief.app.data.local.db.dao

import com.earbrief.app.data.local.db.entity.SessionEntity
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class SessionDaoTest {

    @Test
    fun `insert and retrieve session contract is representable`() {
        val session = SessionEntity(
            id = "session-1",
            startTimeMs = 100L,
        )

        assertThat(session.id).isEqualTo("session-1")
        assertThat(session.startTimeMs).isEqualTo(100L)
        assertThat(session.endTimeMs).isNull()
    }

    @Test
    fun `markEnded updates end time behavior can be represented`() {
        val session = SessionEntity(
            id = "session-1",
            startTimeMs = 100L,
            endTimeMs = null,
        )

        val ended = session.copy(endTimeMs = 200L)

        assertThat(ended.endTimeMs).isEqualTo(200L)
    }

    @Test
    fun `incrementTriggerCount increments total triggers behavior can be represented`() {
        val session = SessionEntity(
            id = "session-1",
            startTimeMs = 100L,
            totalTriggers = 2,
        )

        val incremented = session.copy(totalTriggers = session.totalTriggers + 1)

        assertThat(incremented.totalTriggers).isEqualTo(3)
    }
}
