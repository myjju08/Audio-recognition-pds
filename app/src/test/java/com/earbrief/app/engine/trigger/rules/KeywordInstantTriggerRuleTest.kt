package com.earbrief.app.engine.trigger.rules

import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.model.KeywordActionType
import com.earbrief.app.domain.model.SpeakerLabel
import com.earbrief.app.domain.model.TriggerType
import com.earbrief.app.domain.model.Utterance
import com.earbrief.app.domain.repository.CustomKeywordRepository
import com.earbrief.app.engine.trigger.WhisperGenerator
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("KeywordInstantTriggerRule")
class KeywordInstantTriggerRuleTest {

    @Test
    fun `enabled keyword present emits event`() = runTest {
        val repository = FakeCustomKeywordRepository(
            enabledKeywords = listOf(createKeyword(id = "k-1", keyword = "buy milk"))
        )
        val rule = KeywordInstantTriggerRule(repository, FakeWhisperGenerator(), backgroundScope)
        advanceUntilIdle()

        val event = rule.evaluate(createUtterance(id = "u-1", text = "Please   BUY    milk today"), "session-1")

        assertNotNull(event)
        assertEquals(TriggerType.KEYWORD_INSTANT, event?.triggerType)
    }

    @Test
    fun `disabled keyword absent from enabled flow returns null`() = runTest {
        val repository = FakeCustomKeywordRepository(enabledKeywords = emptyList())
        val rule = KeywordInstantTriggerRule(repository, FakeWhisperGenerator(), backgroundScope)
        advanceUntilIdle()

        val event = rule.evaluate(createUtterance(id = "u-1", text = "buy milk"), "session-1")

        assertNull(event)
    }

    @Test
    fun `duplicate same utterance and keyword emits once`() = runTest {
        val repository = FakeCustomKeywordRepository(
            enabledKeywords = listOf(createKeyword(id = "k-1", keyword = "remind me"))
        )
        val rule = KeywordInstantTriggerRule(repository, FakeWhisperGenerator(), backgroundScope)
        advanceUntilIdle()
        val utterance = createUtterance(id = "u-dup", text = "please remind me now")

        val firstEvent = rule.evaluate(utterance, "session-1")
        val secondEvent = rule.evaluate(utterance, "session-1")

        assertNotNull(firstEvent)
        assertNull(secondEvent)
    }

    @Test
    fun `different utterance same keyword emits again`() = runTest {
        val repository = FakeCustomKeywordRepository(
            enabledKeywords = listOf(createKeyword(id = "k-1", keyword = "bookmark"))
        )
        val rule = KeywordInstantTriggerRule(repository, FakeWhisperGenerator(), backgroundScope)
        advanceUntilIdle()

        val firstEvent = rule.evaluate(createUtterance(id = "u-1", text = "bookmark this"), "session-1")
        val secondEvent = rule.evaluate(createUtterance(id = "u-2", text = "bookmark that"), "session-1")

        assertNotNull(firstEvent)
        assertNotNull(secondEvent)
    }

    @Test
    fun `metadata includes matched keyword and action type`() = runTest {
        val repository = FakeCustomKeywordRepository(
            enabledKeywords = listOf(
                createKeyword(
                    id = "k-1",
                    keyword = "save this",
                    actionType = KeywordActionType.BOOKMARK
                )
            )
        )
        val rule = KeywordInstantTriggerRule(repository, FakeWhisperGenerator(), backgroundScope)
        advanceUntilIdle()

        val event = rule.evaluate(createUtterance(id = "u-meta", text = "please save this for later"), "session-1")

        assertNotNull(event)
        assertEquals("save this", event?.metadata?.get("keyword"))
        assertEquals(KeywordActionType.BOOKMARK.name, event?.metadata?.get("actionType"))
    }

    private fun createKeyword(
        id: String,
        keyword: String,
        actionType: KeywordActionType = KeywordActionType.NOTIFY
    ): CustomKeyword {
        return CustomKeyword(
            id = id,
            keyword = keyword,
            actionType = actionType,
            isActive = true
        )
    }

    private fun createUtterance(id: String, text: String): Utterance {
        return Utterance(
            id = id,
            speaker = SpeakerLabel.SELF,
            text = text,
            startTimeMs = 0L,
            endTimeMs = 1000L,
            confidence = 0.9f
        )
    }
}

private class FakeCustomKeywordRepository(
    enabledKeywords: List<CustomKeyword>
) : CustomKeywordRepository {
    private val enabledKeywordsFlow = MutableStateFlow(enabledKeywords)

    override fun observeEnabledKeywords(): Flow<List<CustomKeyword>> = enabledKeywordsFlow

    override suspend fun getAll(): List<CustomKeyword> = enabledKeywordsFlow.value

    override suspend fun upsert(keyword: CustomKeyword) = Unit

    override suspend fun delete(id: String) = Unit
}

private class FakeWhisperGenerator : WhisperGenerator {
    override fun generate(triggerType: TriggerType, context: Map<String, String>): String {
        return "trigger=${triggerType.name};keyword=${context["keyword"] ?: ""}"
    }
}
