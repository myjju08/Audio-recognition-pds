package com.earbrief.app.engine.context

import com.earbrief.app.domain.model.Entity
import com.earbrief.app.domain.model.EntityType
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.engine.stt.model.TranscriptResult
import com.earbrief.app.service.SessionManager
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DefaultContextEngineTest {

    @Test
    fun `final transcript emits utterance with entities and stores in memory`() = runTest {
        val entityExtractor = mockk<EntityExtractor>()
        val questionDetector = mockk<QuestionDetector>()
        val shortTermMemory = mockk<ShortTermMemory>()
        val sessionManager = mockk<SessionManager>()
        val sessionIdFlow = MutableStateFlow<String?>("session-1")
        every { sessionManager.sessionId } returns sessionIdFlow
        every { questionDetector.isQuestion("내일 오후 3시 미팅 맞나요") } returns true
        every {
            entityExtractor.extract("내일 오후 3시 미팅 맞나요", 20_000L)
        } returns listOf(
            Entity(
                type = EntityType.DATETIME,
                value = "내일 오후 3시",
                normalizedValue = "2026-03-19T15:00",
                confidence = 0.95f,
                position = 0..7,
                sourceUtteranceId = "seed"
            )
        )
        coJustRun { shortTermMemory.add(any()) }

        val engine = DefaultContextEngine(
            entityExtractor = entityExtractor,
            questionDetector = questionDetector,
            shortTermMemory = shortTermMemory,
            sessionManager = sessionManager,
            scope = this
        )
        val emitted = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) { engine.utteranceFlow.first() }
        }
        runCurrent()

        engine.onTranscript(
            transcriptResult(
                transcript = "내일 오후 3시 미팅 맞나요",
                isFinal = true,
                timestampMs = 20_000L,
                durationMs = 1_200L
            )
        )

        val utterance = emitted.await()
        assertEquals("내일 오후 3시 미팅 맞나요", utterance.text)
        assertEquals(18_800L, utterance.startTimeMs)
        assertEquals(20_000L, utterance.endTimeMs)
        assertTrue(utterance.isQuestion)
        assertEquals(1, utterance.entities.size)
        assertEquals(utterance.id, utterance.entities.first().sourceUtteranceId)
        coVerify(exactly = 1) { shortTermMemory.add(match { it.id == utterance.id }) }
    }

    @Test
    fun `interim transcript emits utterance but does not store in memory`() = runTest {
        val entityExtractor = mockk<EntityExtractor>()
        val questionDetector = mockk<QuestionDetector>()
        val shortTermMemory = mockk<ShortTermMemory>()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.sessionId } returns MutableStateFlow<String?>("session-1")
        every { questionDetector.isQuestion(any()) } returns false
        every { entityExtractor.extract(any(), any()) } returns emptyList()
        coJustRun { shortTermMemory.add(any()) }

        val engine = DefaultContextEngine(
            entityExtractor = entityExtractor,
            questionDetector = questionDetector,
            shortTermMemory = shortTermMemory,
            sessionManager = sessionManager,
            scope = this
        )
        val emitted = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) { engine.utteranceFlow.first() }
        }
        runCurrent()

        engine.onTranscript(transcriptResult(transcript = "중간 문장", isFinal = false))

        assertEquals("중간 문장", emitted.await().text)
        coVerify(exactly = 0) { shortTermMemory.add(any()) }
    }

    @Test
    fun `blank transcript emits nothing`() = runTest {
        val entityExtractor = mockk<EntityExtractor>()
        val questionDetector = mockk<QuestionDetector>()
        val shortTermMemory = mockk<ShortTermMemory>()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.sessionId } returns MutableStateFlow<String?>("session-1")
        every { questionDetector.isQuestion(any()) } returns false
        every { entityExtractor.extract(any(), any()) } returns emptyList()
        coJustRun { shortTermMemory.add(any()) }

        val engine = DefaultContextEngine(
            entityExtractor = entityExtractor,
            questionDetector = questionDetector,
            shortTermMemory = shortTermMemory,
            sessionManager = sessionManager,
            scope = this
        )
        val emitted = mutableListOf<com.earbrief.app.domain.model.Utterance>()
        val collectorJob = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            engine.utteranceFlow.collect { emitted += it }
        }
        runCurrent()

        engine.onTranscript(transcriptResult(transcript = "   ", isFinal = true))
        advanceUntilIdle()

        assertTrue(emitted.isEmpty())
        coVerify(exactly = 0) { shortTermMemory.add(any()) }
        collectorJob.cancelAndJoin()
    }

    @Test
    fun `no session id emits nothing`() = runTest {
        val entityExtractor = mockk<EntityExtractor>()
        val questionDetector = mockk<QuestionDetector>()
        val shortTermMemory = mockk<ShortTermMemory>()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.sessionId } returns MutableStateFlow<String?>(null)
        every { questionDetector.isQuestion(any()) } returns false
        every { entityExtractor.extract(any(), any()) } returns emptyList()
        coJustRun { shortTermMemory.add(any()) }

        val engine = DefaultContextEngine(
            entityExtractor = entityExtractor,
            questionDetector = questionDetector,
            shortTermMemory = shortTermMemory,
            sessionManager = sessionManager,
            scope = this
        )
        val emitted = mutableListOf<com.earbrief.app.domain.model.Utterance>()
        val collectorJob = backgroundScope.launch(start = CoroutineStart.UNDISPATCHED) {
            engine.utteranceFlow.collect { emitted += it }
        }
        runCurrent()

        engine.onTranscript(transcriptResult(transcript = "세션 없음", isFinal = true))
        advanceUntilIdle()

        assertTrue(emitted.isEmpty())
        coVerify(exactly = 0) { shortTermMemory.add(any()) }
        collectorJob.cancelAndJoin()
    }

    @Test
    fun `onListeningStateChanged idle clears memory`() = runTest {
        val entityExtractor = mockk<EntityExtractor>()
        val questionDetector = mockk<QuestionDetector>()
        val shortTermMemory = mockk<ShortTermMemory>()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.sessionId } returns MutableStateFlow<String?>("session-1")
        every { questionDetector.isQuestion(any()) } returns false
        every { entityExtractor.extract(any(), any()) } returns emptyList()
        coJustRun { shortTermMemory.clear() }

        val engine = DefaultContextEngine(
            entityExtractor = entityExtractor,
            questionDetector = questionDetector,
            shortTermMemory = shortTermMemory,
            sessionManager = sessionManager,
            scope = this
        )

        engine.onListeningStateChanged(ListeningState.IDLE)

        coVerify(exactly = 1) { shortTermMemory.clear() }
    }

    @Test
    fun `entity sourceUtteranceId matches utterance id`() = runTest {
        val entityExtractor = mockk<EntityExtractor>()
        val questionDetector = mockk<QuestionDetector>()
        val shortTermMemory = mockk<ShortTermMemory>()
        val sessionManager = mockk<SessionManager>()
        every { sessionManager.sessionId } returns MutableStateFlow<String?>("session-1")
        every { questionDetector.isQuestion(any()) } returns false
        every {
            entityExtractor.extract("내일 오후 3시 알람", 10_000L)
        } returns listOf(
            Entity(
                type = EntityType.DATETIME,
                value = "내일 오후 3시",
                normalizedValue = "2026-03-19T15:00",
                confidence = 0.8f,
                position = 0..7,
                sourceUtteranceId = "old-id"
            )
        )
        coJustRun { shortTermMemory.add(any()) }

        val engine = DefaultContextEngine(
            entityExtractor = entityExtractor,
            questionDetector = questionDetector,
            shortTermMemory = shortTermMemory,
            sessionManager = sessionManager,
            scope = this
        )
        val emitted = async(start = CoroutineStart.UNDISPATCHED) {
            withTimeout(1_000) { engine.utteranceFlow.first() }
        }
        runCurrent()

        engine.onTranscript(
            transcriptResult(
                transcript = "내일 오후 3시 알람",
                isFinal = true,
                timestampMs = 10_000L
            )
        )

        val utterance = emitted.await()
        assertNotNull(utterance.entities.firstOrNull())
        assertEquals(utterance.id, utterance.entities.first().sourceUtteranceId)
    }

    private fun transcriptResult(
        transcript: String,
        isFinal: Boolean,
        confidence: Float = 0.9f,
        durationMs: Long = 1_000L,
        timestampMs: Long = 1_000L
    ) = TranscriptResult(
        transcript = transcript,
        confidence = confidence,
        isFinal = isFinal,
        speechFinal = isFinal,
        words = emptyList(),
        durationMs = durationMs,
        timestampMs = timestampMs
    )
}
