package com.earbrief.app.engine.context

import com.earbrief.app.di.ApplicationScope
import com.earbrief.app.domain.model.ListeningState
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.engine.stt.model.TranscriptResult
import com.earbrief.app.service.SessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@Singleton
class DefaultContextEngine @Inject constructor(
    private val entityExtractor: EntityExtractor,
    private val questionDetector: QuestionDetector,
    private val shortTermMemory: ShortTermMemory,
    private val sessionManager: SessionManager,
    @ApplicationScope private val scope: CoroutineScope
) : ContextEngine {

    private val _utteranceFlow = MutableSharedFlow<Utterance>(extraBufferCapacity = 32)
    override val utteranceFlow: SharedFlow<Utterance> = _utteranceFlow.asSharedFlow()

    override suspend fun onTranscript(result: TranscriptResult) {
        sessionManager.sessionId.value ?: return
        val text = result.transcript.trim()
        if (text.isBlank()) return

        val entities = entityExtractor.extract(text, referenceTimeMs = result.timestampMs)
        val isQuestion = questionDetector.isQuestion(text)
        val startTimeMs = result.timestampMs - result.durationMs
        val endTimeMs = result.timestampMs
        val utterance = Utterance(
            speaker = SpeakerLabel.UNKNOWN,
            text = text,
            startTimeMs = startTimeMs,
            endTimeMs = endTimeMs,
            confidence = result.confidence,
            entities = entities.map { it.copy(sourceUtteranceId = "") },
            isQuestion = isQuestion
        )
        val finalUtterance = utterance.copy(
            entities = utterance.entities.map { it.copy(sourceUtteranceId = utterance.id) }
        )

        if (result.isFinal) {
            shortTermMemory.add(finalUtterance)
        }

        _utteranceFlow.tryEmit(finalUtterance)
    }

    override suspend fun onListeningStateChanged(state: ListeningState) {
        if (state == ListeningState.IDLE) {
            shortTermMemory.clear()
        }
    }
}
