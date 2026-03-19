package com.earbrief.app.engine.trigger.rules

import com.earbrief.app.di.ApplicationScope
import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.model.TriggerEvent
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.model.VadState
import com.earbrief.app.domain.repository.CustomKeywordRepository
import com.earbrief.app.engine.trigger.TriggerRule
import com.earbrief.app.engine.trigger.WhisperGenerator
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Singleton
class KeywordInstantTriggerRule @Inject constructor(
    private val customKeywordRepository: CustomKeywordRepository,
    private val whisperGenerator: WhisperGenerator,
    @ApplicationScope private val scope: CoroutineScope
) : TriggerRule {

    @Volatile
    private var enabledKeywords: List<KeywordCandidate> = emptyList()
    @Volatile
    private var initialLoadComplete: Boolean = false
    private val emittedKeys = mutableSetOf<String>()

    init {
        scope.launch {
            customKeywordRepository.observeEnabledKeywords().collect { keywords ->
                enabledKeywords = keywords.toKeywordCandidates()
                initialLoadComplete = true
            }
        }
    }

    override suspend fun evaluate(utterance: Utterance, sessionId: String): TriggerEvent? {
        if (!initialLoadComplete) {
            enabledKeywords = customKeywordRepository.getAll().toKeywordCandidates()
            initialLoadComplete = true
        }

        val normalizedText = normalizeText(utterance.text)
        if (normalizedText.isBlank()) {
            return null
        }

        val matchedKeyword = enabledKeywords.firstOrNull { candidate ->
            normalizedText.contains(candidate.normalizedKeyword)
        } ?: return null

        val emissionKey = "${utterance.id}:${matchedKeyword.id}"
        synchronized(emittedKeys) {
            if (!emittedKeys.add(emissionKey)) {
                return null
            }
        }

        val context = mapOf(
            KEY_KEYWORD to matchedKeyword.rawKeyword,
            KEY_ACTION_TYPE to matchedKeyword.actionType
        )

        return TriggerEvent(
            sessionId = sessionId,
            triggerType = TriggerType.KEYWORD_INSTANT,
            sourceUtteranceId = utterance.id,
            sourceText = utterance.text,
            whisperText = whisperGenerator.generate(TriggerType.KEYWORD_INSTANT, context),
            confidence = utterance.confidence,
            metadata = context
        )
    }

    override suspend fun onVadStateChanged(
        vadState: VadState,
        silenceDurationMs: Long,
        sessionId: String
    ): TriggerEvent? = null

    override fun reset() {
        synchronized(emittedKeys) {
            emittedKeys.clear()
        }
    }

    private fun normalizeText(text: String): String {
        return text
            .lowercase()
            .replace(WHITESPACE_REGEX, " ")
            .trim()
    }

    private fun List<CustomKeyword>.toKeywordCandidates(): List<KeywordCandidate> {
        return this
            .filter { it.isActive }
            .mapNotNull { keyword ->
                val normalizedKeyword = normalizeText(keyword.keyword)
                if (normalizedKeyword.isBlank()) {
                    null
                } else {
                    KeywordCandidate(
                        id = keyword.id,
                        rawKeyword = keyword.keyword,
                        normalizedKeyword = normalizedKeyword,
                        actionType = keyword.actionType.name
                    )
                }
            }
    }

    private data class KeywordCandidate(
        val id: String,
        val rawKeyword: String,
        val normalizedKeyword: String,
        val actionType: String
    )

    private companion object {
        val WHITESPACE_REGEX = Regex("\\s+")
        const val KEY_KEYWORD = "keyword"
        const val KEY_ACTION_TYPE = "actionType"
    }
}
