package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.Utterance
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

@DisplayName("InMemoryShortTermMemory")
class InMemoryShortTermMemoryTest {

    @Nested
    @DisplayName("Utterance storage")
    inner class Storage {

        @Test
        fun `add utterance recent returns it`() = runTest {
            val memory = createMemory(now = 10_000L)
            val utterance = testUtterance(text = "hello", endTimeMs = 10_000L)

            memory.add(utterance)

            assertEquals(listOf(utterance), memory.recent(1))
        }

        @Test
        fun `add 3 utterances recent 2 returns last 2`() = runTest {
            val memory = createMemory(now = 20_000L)
            val first = testUtterance(text = "1", endTimeMs = 10_000L)
            val second = testUtterance(text = "2", endTimeMs = 15_000L)
            val third = testUtterance(text = "3", endTimeMs = 20_000L)

            memory.add(first)
            memory.add(second)
            memory.add(third)

            assertEquals(listOf(second, third), memory.recent(2))
        }

        @Test
        fun `clear recent returns empty`() = runTest {
            val memory = createMemory(now = 10_000L)
            memory.add(testUtterance(endTimeMs = 10_000L))

            memory.clear()

            assertTrue(memory.recent().isEmpty())
        }
    }

    @Nested
    @DisplayName("10-minute rolling window")
    inner class EvictionTests {

        @Test
        fun `utterance older than 10 minutes is evicted`() = runTest {
            val clock = TestClock(now = 0L)
            val memory = createMemory(clock)
            memory.add(testUtterance(endTimeMs = 0L))
            clock.now = 11 * MINUTE_MS

            val recent = memory.recentInWindow(MINUTE_MS * 10)

            assertTrue(recent.isEmpty())
        }

        @Test
        fun `only utterance within 10-minute window remains`() = runTest {
            val clock = TestClock(now = 0L)
            val memory = createMemory(clock)
            val oldUtterance = testUtterance(text = "old", endTimeMs = 0L)
            val recentUtterance = testUtterance(text = "recent", endTimeMs = 5 * MINUTE_MS)
            memory.add(oldUtterance)
            memory.add(recentUtterance)
            clock.now = 11 * MINUTE_MS

            val recent = memory.recentInWindow(MINUTE_MS * 10)

            assertEquals(listOf(recentUtterance), recent)
        }

        @Test
        fun `utterance within 9 minutes is retained`() = runTest {
            val clock = TestClock(now = 0L)
            val memory = createMemory(clock)
            val utterance = testUtterance(endTimeMs = 0L)
            memory.add(utterance)
            clock.now = 9 * MINUTE_MS

            val recent = memory.recentInWindow(MINUTE_MS * 10)

            assertEquals(listOf(utterance), recent)
        }
    }

    @Nested
    @DisplayName("Entity tracking")
    inner class EntityTests {

        @Test
        fun `datetime entity is tracked`() = runTest {
            val memory = createMemory(now = 10_000L)
            val entity = testEntity(EntityType.DATETIME)

            memory.add(testUtterance(endTimeMs = 10_000L, entities = listOf(entity)))

            assertEquals(listOf(entity), memory.getActiveEntities(EntityType.DATETIME))
        }

        @Test
        fun `no entities returns empty list`() = runTest {
            val memory = createMemory(now = 10_000L)

            memory.add(testUtterance(endTimeMs = 10_000L))

            assertTrue(memory.getActiveEntities(EntityType.DATETIME).isEmpty())
        }
    }

    @Nested
    @DisplayName("Question detection")
    inner class QuestionTests {

        @Test
        fun `question utterance is returned as latest question`() = runTest {
            val memory = createMemory(now = 10_000L)
            val question = testUtterance(endTimeMs = 10_000L, isQuestion = true)

            memory.add(question)

            assertEquals(question, memory.latestQuestion())
        }

        @Test
        fun `non question returns null`() = runTest {
            val memory = createMemory(now = 10_000L)
            memory.add(testUtterance(endTimeMs = 10_000L, isQuestion = false))

            assertNull(memory.latestQuestion())
        }

        @Test
        fun `latestQuestion returns latest question even after non question`() = runTest {
            val memory = createMemory(now = 30_000L)
            val question = testUtterance(endTimeMs = 10_000L, isQuestion = true)
            val statement = testUtterance(endTimeMs = 20_000L, isQuestion = false)
            memory.add(question)
            memory.add(statement)

            assertEquals(question, memory.latestQuestion())
        }
    }

    @Nested
    @DisplayName("Intervention budget")
    inner class BudgetTests {

        @Test
        fun `first three consumes return true and fourth returns false`() = runTest {
            val memory = createMemory(now = 1_000L)

            assertTrue(memory.tryConsumeInterventionBudget())
            assertTrue(memory.tryConsumeInterventionBudget())
            assertTrue(memory.tryConsumeInterventionBudget())
            assertFalse(memory.tryConsumeInterventionBudget())
        }

        @Test
        fun `budget resets after window expires`() = runTest {
            val clock = TestClock(now = 1_000L)
            val memory = createMemory(clock)
            repeat(3) { memory.tryConsumeInterventionBudget() }
            assertFalse(memory.tryConsumeInterventionBudget())

            clock.now += 10 * MINUTE_MS + 1

            assertTrue(memory.tryConsumeInterventionBudget())
        }

        @Test
        fun `remaining budget decrements correctly`() = runTest {
            val memory = createMemory(now = 1_000L)

            assertEquals(3, memory.getRemainingBudget())
            memory.tryConsumeInterventionBudget()
            assertEquals(2, memory.getRemainingBudget())
            memory.tryConsumeInterventionBudget()
            assertEquals(1, memory.getRemainingBudget())
        }

        @Test
        fun `clear resets budget`() = runTest {
            val memory = createMemory(now = 1_000L)
            memory.tryConsumeInterventionBudget()
            memory.tryConsumeInterventionBudget()

            memory.clear()

            assertEquals(3, memory.getRemainingBudget())
        }
    }

    @Nested
    @DisplayName("Silence tracking")
    inner class SilenceTests {

        @Test
        fun `no utterances returns max value`() = runTest {
            val memory = createMemory(now = 3_000L)

            assertEquals(Long.MAX_VALUE, memory.getTimeSinceLastUtteranceMs())
        }

        @Test
        fun `time since last utterance is calculated from clock`() = runTest {
            val clock = TestClock(now = 1_000L)
            val memory = createMemory(clock)
            memory.add(testUtterance(endTimeMs = 1_000L))
            clock.now = 3_000L

            assertEquals(2_000L, memory.getTimeSinceLastUtteranceMs())
        }
    }

    @Nested
    @DisplayName("Thread safety")
    inner class ThreadSafetyTests {

        @Test
        fun `concurrent adds preserve all utterances`() = runTest {
            val clock = TestClock(now = 100_000L)
            val memory = createMemory(clock)

            coroutineScope {
                (1..200).map { idx ->
                    launch {
                        memory.add(testUtterance(text = "u$idx", endTimeMs = 100_000L + idx))
                    }
                }.forEach { it.join() }
            }

            assertEquals(200, memory.recent(500).size)
        }

        @Test
        fun `concurrent budget consumes allow only three successes`() = runTest {
            val memory = createMemory(now = 5_000L)
            val results = ConcurrentLinkedQueue<Boolean>()

            coroutineScope {
                (1..20).map {
                    async {
                        results.add(memory.tryConsumeInterventionBudget())
                    }
                }.awaitAll()
            }

            assertEquals(3, results.count { it })
            assertEquals(17, results.count { !it })
            assertEquals(0, memory.getRemainingBudget())
            assertNotNull(results)
        }
    }

    private fun createMemory(now: Long): InMemoryShortTermMemory {
        val clock = TestClock(now)
        return createMemory(clock)
    }

    private fun createMemory(clock: TestClock): InMemoryShortTermMemory {
        return InMemoryShortTermMemory(ShortTermMemoryConfig()).also { memory ->
            memory.timeProvider = { clock.now }
        }
    }

    private fun testUtterance(
        text: String = "test",
        endTimeMs: Long = System.currentTimeMillis(),
        isQuestion: Boolean = false,
        entities: List<Entity> = emptyList()
    ) = Utterance(
        speaker = SpeakerLabel.UNKNOWN,
        text = text,
        startTimeMs = endTimeMs - 1_000,
        endTimeMs = endTimeMs,
        confidence = 0.95f,
        entities = entities,
        isQuestion = isQuestion
    )

    private fun testEntity(type: EntityType) = Entity(
        type = type,
        value = "tomorrow",
        normalizedValue = "2026-03-19",
        confidence = 0.9f,
        position = 0..7,
        sourceUtteranceId = "u1"
    )

    private data class TestClock(var now: Long)

    private companion object {
        private const val MINUTE_MS = 60_000L
    }
}
