package com.earbrief.app.engine.trigger.rules

import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.engine.context.ShortTermMemory
import com.earbrief.app.engine.trigger.TriggerRule
import com.earbrief.app.engine.trigger.WhisperGenerator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SilenceDetectTriggerRule @Inject constructor(
    private val shortTermMemory: ShortTermMemory,
    private val whisperGenerator: WhisperGenerator
) : TriggerRule {
    private var lastQuestionUtterance: Utterance? = null
    private var hasFiredInCurrentSilenceWindow: Boolean = false
    private var activeSessionId: String? = null

    override suspend fun evaluate(utterance: Utterance, sessionId: String): TriggerEvent? {
        resetIfSessionChanged(sessionId)
        if (utterance.isQuestion) {
            lastQuestionUtterance = utterance
            hasFiredInCurrentSilenceWindow = false
        }
        return null
    }

    override suspend fun onVadStateChanged(
        vadState: VadState,
        silenceDurationMs: Long,
        sessionId: String
    ): TriggerEvent? {
        resetIfSessionChanged(sessionId)

        if (vadState == VadState.SPEECH) {
            hasFiredInCurrentSilenceWindow = false
            return null
        }

        val question = lastQuestionUtterance ?: return null
        if (hasFiredInCurrentSilenceWindow || silenceDurationMs < SILENCE_THRESHOLD_MS) {
            return null
        }

        if (!shortTermMemory.tryConsumeInterventionBudget()) {
            return null
        }

        val whisperText = whisperGenerator.generate(
            triggerType = TriggerType.SILENCE_DETECT,
            context = mapOf(
                KEY_SILENCE_DURATION_MS to silenceDurationMs.toString(),
                KEY_QUESTION_TEXT to question.text
            )
        )

        hasFiredInCurrentSilenceWindow = true
        return TriggerEvent(
            sessionId = sessionId,
            triggerType = TriggerType.SILENCE_DETECT,
            sourceUtteranceId = question.id,
            sourceText = question.text,
            whisperText = whisperText,
            metadata = mapOf(
                KEY_SILENCE_DURATION_MS to silenceDurationMs.toString(),
                KEY_QUESTION_TEXT to question.text
            )
        )
    }

    override fun reset() {
        lastQuestionUtterance = null
        hasFiredInCurrentSilenceWindow = false
        activeSessionId = null
    }

    private fun resetIfSessionChanged(sessionId: String) {
        if (activeSessionId == null || activeSessionId == sessionId) {
            activeSessionId = sessionId
            return
        }
        reset()
        activeSessionId = sessionId
    }

    private companion object {
        const val SILENCE_THRESHOLD_MS = 2_500L
        const val KEY_SILENCE_DURATION_MS = "silenceDurationMs"
        const val KEY_QUESTION_TEXT = "questionText"
    }
}
