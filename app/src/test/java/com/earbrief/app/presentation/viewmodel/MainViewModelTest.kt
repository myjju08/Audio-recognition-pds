package com.earbrief.app.presentation.viewmodel

import android.app.Application
import app.cash.turbine.test
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.service.PipelineOrchestrator
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class MainViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sessionId mirrors orchestrator sessionId updates`() = runTest {
        val orchestrator = createOrchestrator()
        val viewModel = createViewModel(orchestrator)

        viewModel.sessionId.test {
            assertEquals(null, awaitItem())

            orchestrator.updateSessionId("session-123")

            assertEquals("session-123", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `recentTriggerEvents mirrors orchestrator trigger list`() = runTest {
        val orchestrator = createOrchestrator()
        val viewModel = createViewModel(orchestrator)
        val triggerEvent = TriggerEvent(
            sessionId = "session-abc",
            triggerType = TriggerType.KEYWORD_INSTANT,
            sourceUtteranceId = "utt-1",
            sourceText = "회의 일정 확인해줘",
            whisperText = "다음 회의는 오후 3시입니다"
        )

        viewModel.recentTriggerEvents.test {
            assertEquals(emptyList<TriggerEvent>(), awaitItem())

            orchestrator.publishTriggerEvent(triggerEvent)

            assertEquals(listOf(triggerEvent), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun createOrchestrator(): PipelineOrchestrator {
        return PipelineOrchestrator()
    }

    private fun createViewModel(orchestrator: PipelineOrchestrator): MainViewModel {
        return MainViewModel(
            application = mockk<Application>(relaxed = true),
            orchestrator = orchestrator
        )
    }
}
