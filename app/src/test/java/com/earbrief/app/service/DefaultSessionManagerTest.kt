package com.earbrief.app.service

import com.earbrief.app.domain.model.ListeningState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("DefaultSessionManager")
class DefaultSessionManagerTest {

    private fun createManager(): DefaultSessionManager {
        return DefaultSessionManager()
    }

    @Nested
    @DisplayName("Session lifecycle")
    inner class SessionLifecycle {

        @Test
        fun `LISTENING generates non-null UUID`() = runTest {
            val manager = createManager()

            manager.onListeningStateChanged(ListeningState.LISTENING)

            assertNotNull(manager.sessionId.value)
        }

        @Test
        fun `IDLE clears sessionId to null`() = runTest {
            val manager = createManager()
            manager.onListeningStateChanged(ListeningState.LISTENING)

            manager.onListeningStateChanged(ListeningState.IDLE)

            assertNull(manager.sessionId.value)
        }

        @Test
        fun `PAUSED keeps sessionId unchanged`() = runTest {
            val manager = createManager()
            manager.onListeningStateChanged(ListeningState.LISTENING)
            val initialSessionId = manager.sessionId.value

            manager.onListeningStateChanged(ListeningState.PAUSED)

            assertEquals(initialSessionId, manager.sessionId.value)
        }

        @Test
        fun `repeated LISTENING does not rotate session ID`() = runTest {
            val manager = createManager()
            manager.onListeningStateChanged(ListeningState.LISTENING)
            val initialSessionId = manager.sessionId.value

            manager.onListeningStateChanged(ListeningState.LISTENING)

            assertEquals(initialSessionId, manager.sessionId.value)
        }

        @Test
        fun `IDLE then LISTENING generates new session ID`() = runTest {
            val manager = createManager()
            manager.onListeningStateChanged(ListeningState.LISTENING)
            val firstSessionId = manager.sessionId.value
            manager.onListeningStateChanged(ListeningState.IDLE)

            manager.onListeningStateChanged(ListeningState.LISTENING)

            val secondSessionId = manager.sessionId.value
            assertNotNull(secondSessionId)
            assertNotEquals(firstSessionId, secondSessionId)
        }
    }
}
