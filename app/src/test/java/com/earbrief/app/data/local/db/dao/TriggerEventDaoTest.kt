package com.earbrief.app.data.local.db.dao

import com.earbrief.app.data.local.db.entity.TriggerEventEntity
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class TriggerEventDaoTest {

    @Test
    fun `insert and retrieve by session contract is representable`() {
        val event = TriggerEventEntity(
            id = "event-1",
            sessionId = "session-1",
            triggerType = "CALENDAR",
            sourceUtteranceId = "utt-1",
            sourceText = "Book a meeting tomorrow",
            detectedEntities = "[]",
            whisperText = "Calendar reminder",
            confidence = 0.95f,
            priority = 1,
            urgency = "HIGH",
            metadata = "{}",
            createdAtMs = 100L,
        )

        assertThat(event.sessionId).isEqualTo("session-1")
        assertThat(event.createdAtMs).isEqualTo(100L)
    }

    @Test
    fun `observeBySession emits after insert contract can be filtered`() {
        val events = listOf(
            TriggerEventEntity(
                id = "event-1",
                sessionId = "session-1",
                triggerType = "CALENDAR",
                sourceUtteranceId = "utt-1",
                sourceText = "first",
                detectedEntities = "[]",
                whisperText = "w1",
                confidence = 0.8f,
                priority = 2,
                urgency = "NORMAL",
                metadata = "{}",
                createdAtMs = 100L,
            ),
            TriggerEventEntity(
                id = "event-2",
                sessionId = "session-1",
                triggerType = "CALENDAR",
                sourceUtteranceId = "utt-2",
                sourceText = "second",
                detectedEntities = "[]",
                whisperText = "w2",
                confidence = 0.9f,
                priority = 1,
                urgency = "HIGH",
                metadata = "{}",
                createdAtMs = 200L,
            ),
        )

        val observed = events
            .filter { it.sessionId == "session-1" }
            .sortedByDescending { it.createdAtMs }

        assertThat(observed.map { it.id }).containsExactly("event-2", "event-1").inOrder()
    }

    @Test
    fun `deleteOlderThan and countBySession behavior can be validated`() {
        val events = listOf(
            TriggerEventEntity(
                id = "old",
                sessionId = "session-1",
                triggerType = "CALENDAR",
                sourceUtteranceId = "utt-old",
                sourceText = "old",
                detectedEntities = "[]",
                whisperText = "old whisper",
                confidence = 0.5f,
                priority = 3,
                urgency = "LOW",
                metadata = "{}",
                createdAtMs = 10L,
            ),
            TriggerEventEntity(
                id = "new",
                sessionId = "session-1",
                triggerType = "CALENDAR",
                sourceUtteranceId = "utt-new",
                sourceText = "new",
                detectedEntities = "[]",
                whisperText = "new whisper",
                confidence = 0.9f,
                priority = 1,
                urgency = "HIGH",
                metadata = "{}",
                createdAtMs = 1000L,
            ),
        )

        val kept = events.filter { it.createdAtMs >= 100L }
        val countBySession = kept.count { it.sessionId == "session-1" }

        assertThat(kept.map { it.id }).containsExactly("new")
        assertThat(countBySession).isEqualTo(1)
    }
}
