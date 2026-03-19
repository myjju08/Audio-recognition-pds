package com.earbrief.app.data.repository

import app.cash.turbine.test
import com.earbrief.app.data.local.db.dao.TriggerEventDao
import com.earbrief.app.data.local.db.entity.TriggerEventEntity
import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.TriggerUrgency
import com.google.common.truth.Truth.assertThat
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

class RoomTriggerEventRepositoryTest {

    private val dao: TriggerEventDao = mockk()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    private val repository = RoomTriggerEventRepository(dao, json)

    @Test
    fun `insert maps domain model and serializes json fields`() = runTest {
        val captured = slot<TriggerEventEntity>()
        coEvery { dao.insert(capture(captured)) } just Runs

        repository.insert(
            TriggerEvent(
                id = "event-1",
                sessionId = "session-1",
                triggerType = TriggerType.KEYWORD_INSTANT,
                sourceUtteranceId = "utt-1",
                sourceText = "book a room",
                detectedEntities = listOf(
                    Entity(
                        type = EntityType.PROJECT,
                        value = "alpha",
                        normalizedValue = "ALPHA",
                        confidence = 0.92f,
                        position = 3..7,
                        sourceUtteranceId = "utt-1",
                    )
                ),
                whisperText = "book room",
                confidence = 0.88f,
                priority = 1,
                urgency = TriggerUrgency.HIGH,
                metadata = mapOf("channel" to "voice"),
                createdAtMs = 1234L,
            )
        )

        val inserted = captured.captured
        assertThat(inserted.triggerType).isEqualTo("KEYWORD_INSTANT")
        assertThat(inserted.urgency).isEqualTo("HIGH")

        val entitiesJson = json.parseToJsonElement(inserted.detectedEntities).jsonArray
        assertThat(entitiesJson).hasSize(1)
        val firstEntity = entitiesJson.first().jsonObject
        assertThat(firstEntity.getValue("type").jsonPrimitive.content).isEqualTo("PROJECT")
        assertThat(firstEntity.getValue("start").jsonPrimitive.int).isEqualTo(3)
        assertThat(firstEntity.getValue("endInclusive").jsonPrimitive.int).isEqualTo(7)

        val metadataJson = json.parseToJsonElement(inserted.metadata).jsonObject
        assertThat(metadataJson.getValue("channel").jsonPrimitive.content).isEqualTo("voice")
    }

    @Test
    fun `observeBySession maps entities and falls back on malformed json`() = runTest {
        val daoFlow = MutableStateFlow(
            listOf(
                TriggerEventEntity(
                    id = "event-2",
                    sessionId = "session-1",
                    triggerType = "RISK_DETECT",
                    sourceUtteranceId = "utt-2",
                    sourceText = "text",
                    detectedEntities = "{not valid json",
                    whisperText = "whisper",
                    confidence = 0.7f,
                    priority = 2,
                    urgency = "NORMAL",
                    metadata = "[invalid",
                    createdAtMs = 200L,
                )
            )
        )
        every { dao.observeBySession("session-1") } returns daoFlow

        repository.observeBySession("session-1").test {
            val item = awaitItem().single()
            assertThat(item.id).isEqualTo("event-2")
            assertThat(item.triggerType).isEqualTo(TriggerType.RISK_DETECT)
            assertThat(item.urgency).isEqualTo(TriggerUrgency.NORMAL)
            assertThat(item.detectedEntities).isEmpty()
            assertThat(item.metadata).isEmpty()
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getBySession maps json fields to domain model`() = runTest {
        coEvery { dao.getBySession("session-2") } returns listOf(
            TriggerEventEntity(
                id = "event-3",
                sessionId = "session-2",
                triggerType = "MEMORY_ASSIST",
                sourceUtteranceId = "utt-3",
                sourceText = "source",
                detectedEntities = """
                    [
                      {
                        "type":"PERSON",
                        "value":"Jane",
                        "normalizedValue":"jane",
                        "confidence":0.95,
                        "start":0,
                        "endInclusive":3,
                        "sourceUtteranceId":"utt-3"
                      }
                    ]
                """.trimIndent(),
                whisperText = "whisper",
                confidence = 0.95f,
                priority = 1,
                urgency = "CRITICAL",
                metadata = "{" + "\"foo\":\"bar\"}" ,
                createdAtMs = 300L,
            )
        )

        val result = repository.getBySession("session-2")

        assertThat(result).hasSize(1)
        val event = result.single()
        assertThat(event.triggerType).isEqualTo(TriggerType.MEMORY_ASSIST)
        assertThat(event.urgency).isEqualTo(TriggerUrgency.CRITICAL)
        assertThat(event.detectedEntities).containsExactly(
            Entity(
                type = EntityType.PERSON,
                value = "Jane",
                normalizedValue = "jane",
                confidence = 0.95f,
                position = 0..3,
                sourceUtteranceId = "utt-3",
            )
        )
        assertThat(event.metadata).containsExactly("foo", "bar")
    }

    @Test
    fun `deleteOlderThan delegates to dao`() = runTest {
        coEvery { dao.deleteOlderThan(any()) } just Runs

        repository.deleteOlderThan(999L)

        coVerify(exactly = 1) { dao.deleteOlderThan(999L) }
    }
}
