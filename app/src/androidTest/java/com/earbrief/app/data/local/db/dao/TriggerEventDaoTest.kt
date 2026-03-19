package com.earbrief.app.data.local.db.dao

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.earbrief.app.data.local.db.EarBriefDatabase
import com.earbrief.app.data.local.db.entity.TriggerEventEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TriggerEventDaoTest {

    private lateinit var db: EarBriefDatabase
    private lateinit var dao: TriggerEventDao

    @Before
    fun setup() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        db = Room.inMemoryDatabaseBuilder(context, EarBriefDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.triggerEventDao()
    }

    @After
    fun teardown() {
        db.close()
    }

    private fun entity(
        id: String = "event-1",
        sessionId: String = "session-1",
        createdAtMs: Long = 100L,
    ) = TriggerEventEntity(
        id = id,
        sessionId = sessionId,
        triggerType = "CALENDAR",
        sourceUtteranceId = "utt-$id",
        sourceText = "text for $id",
        detectedEntities = "[]",
        whisperText = "whisper $id",
        confidence = 0.9f,
        priority = 1,
        urgency = "HIGH",
        metadata = "{}",
        createdAtMs = createdAtMs,
    )

    @Test
    fun insertAndRetrieveBySession() = runBlocking {
        val event = entity()
        dao.insert(event)

        val results = dao.getBySession("session-1")

        assertEquals(1, results.size)
        assertEquals("event-1", results[0].id)
        assertEquals("session-1", results[0].sessionId)
    }

    @Test
    fun observeBySessionEmitsAfterInsert() = runBlocking {
        dao.insert(entity(id = "e1", createdAtMs = 100L))
        dao.insert(entity(id = "e2", createdAtMs = 200L))

        val observed = dao.observeBySession("session-1").first()

        assertEquals(2, observed.size)
        assertEquals("e2", observed[0].id)
        assertEquals("e1", observed[1].id)
    }

    @Test
    fun deleteOlderThanRemovesOldKeepsNew() = runBlocking {
        dao.insert(entity(id = "old", createdAtMs = 10L))
        dao.insert(entity(id = "new", createdAtMs = 1000L))

        dao.deleteOlderThan(100L)

        val remaining = dao.getBySession("session-1")
        assertEquals(1, remaining.size)
        assertEquals("new", remaining[0].id)
    }

    @Test
    fun countBySessionReturnsCorrectCount() = runBlocking {
        dao.insert(entity(id = "e1"))
        dao.insert(entity(id = "e2"))
        dao.insert(entity(id = "e3", sessionId = "session-other"))

        assertEquals(2, dao.countBySession("session-1"))
        assertEquals(1, dao.countBySession("session-other"))
    }

    @Test
    fun getBySessionReturnsEmptyForUnknownSession() = runBlocking {
        dao.insert(entity())

        assertTrue(dao.getBySession("nonexistent").isEmpty())
    }

    @Test
    fun insertWithSameIdReplaces() = runBlocking {
        dao.insert(entity(id = "e1", createdAtMs = 100L))
        dao.insert(entity(id = "e1", createdAtMs = 200L))

        val results = dao.getBySession("session-1")
        assertEquals(1, results.size)
        assertEquals(200L, results[0].createdAtMs)
    }
}
