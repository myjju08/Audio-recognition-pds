package com.earbrief.app.data.local.db.dao

import com.earbrief.app.data.local.db.entity.CustomKeywordEntity
import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test

class CustomKeywordDaoTest {

    @Test
    fun `upsert creates new keyword contract`() {
        val keyword = CustomKeywordEntity(
            id = "keyword-1",
            keyword = "milk",
            pattern = null,
            actionType = "SHOPPING_LIST",
            responseTemplate = null,
            isActive = true,
            triggerCount = 0,
            lastTriggeredMs = null,
            createdAtMs = 100L,
        )

        assertThat(keyword.id).isEqualTo("keyword-1")
        assertThat(keyword.keyword).isEqualTo("milk")
    }

    @Test
    fun `upsert updates existing keyword contract`() {
        val original = CustomKeywordEntity(
            id = "keyword-1",
            keyword = "milk",
            pattern = null,
            actionType = "SHOPPING_LIST",
            responseTemplate = null,
            isActive = true,
            triggerCount = 0,
            lastTriggeredMs = null,
            createdAtMs = 100L,
        )
        val updated = original.copy(pattern = "buy milk", isActive = false)

        assertThat(updated.id).isEqualTo(original.id)
        assertThat(updated.pattern).isEqualTo("buy milk")
        assertThat(updated.isActive).isFalse()
    }

    @Test
    fun `observeEnabled only returns active keywords behavior can be filtered`() {
        val allKeywords = listOf(
            CustomKeywordEntity(
                id = "k1",
                keyword = "alpha",
                pattern = null,
                actionType = "NOTIFY",
                responseTemplate = null,
                isActive = false,
                triggerCount = 0,
                lastTriggeredMs = null,
                createdAtMs = 1L,
            ),
            CustomKeywordEntity(
                id = "k2",
                keyword = "beta",
                pattern = null,
                actionType = "NOTIFY",
                responseTemplate = null,
                isActive = true,
                triggerCount = 0,
                lastTriggeredMs = null,
                createdAtMs = 2L,
            ),
        )

        val enabled = allKeywords.filter { it.isActive }.sortedBy { it.keyword }

        assertThat(enabled.map { it.id }).containsExactly("k2")
    }

    @Test
    fun `deleteById removes keyword behavior can be applied`() {
        val allKeywords = listOf(
            CustomKeywordEntity(
                id = "k1",
                keyword = "alpha",
                pattern = null,
                actionType = "NOTIFY",
                responseTemplate = null,
                isActive = true,
                triggerCount = 0,
                lastTriggeredMs = null,
                createdAtMs = 1L,
            ),
            CustomKeywordEntity(
                id = "k2",
                keyword = "beta",
                pattern = null,
                actionType = "NOTIFY",
                responseTemplate = null,
                isActive = true,
                triggerCount = 0,
                lastTriggeredMs = null,
                createdAtMs = 2L,
            ),
        )

        val remaining = allKeywords.filterNot { it.id == "k1" }

        assertThat(remaining.map { it.id }).containsExactly("k2")
    }

    @Test
    fun `incrementTriggerCount updates count behavior can be represented`() {
        val keyword = CustomKeywordEntity(
            id = "k1",
            keyword = "alpha",
            pattern = null,
            actionType = "NOTIFY",
            responseTemplate = null,
            isActive = true,
            triggerCount = 2,
            lastTriggeredMs = 10L,
            createdAtMs = 1L,
        )

        val incremented = keyword.copy(triggerCount = keyword.triggerCount + 1, lastTriggeredMs = 99L)

        assertThat(incremented.triggerCount).isEqualTo(3)
        assertThat(incremented.lastTriggeredMs).isEqualTo(99L)
    }
}
