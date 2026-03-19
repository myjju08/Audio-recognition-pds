package com.earbrief.app.data.repository

import app.cash.turbine.test
import com.earbrief.app.data.local.db.dao.CustomKeywordDao
import com.earbrief.app.data.local.db.entity.CustomKeywordEntity
import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.model.KeywordActionType
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
import org.junit.jupiter.api.Test

class RoomCustomKeywordRepositoryTest {

    private val dao: CustomKeywordDao = mockk()
    private val repository = RoomCustomKeywordRepository(dao)

    @Test
    fun `observeEnabledKeywords maps dao entities to domain`() = runTest {
        val daoFlow = MutableStateFlow(
            listOf(
                CustomKeywordEntity(
                    id = "k-1",
                    keyword = "milk",
                    pattern = "buy milk",
                    actionType = "SHOPPING_LIST",
                    responseTemplate = "added",
                    isActive = true,
                    triggerCount = 3,
                    lastTriggeredMs = 123L,
                    createdAtMs = 10L,
                )
            )
        )
        every { dao.observeEnabled() } returns daoFlow

        repository.observeEnabledKeywords().test {
            assertThat(awaitItem()).containsExactly(
                CustomKeyword(
                    id = "k-1",
                    keyword = "milk",
                    pattern = "buy milk",
                    actionType = KeywordActionType.SHOPPING_LIST,
                    responseTemplate = "added",
                    isActive = true,
                    triggerCount = 3,
                    lastTriggeredMs = 123L,
                    createdAtMs = 10L,
                )
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getAll maps dao entities to domain`() = runTest {
        coEvery { dao.getAll() } returns listOf(
            CustomKeywordEntity(
                id = "k-2",
                keyword = "pause",
                pattern = null,
                actionType = "PAUSE",
                responseTemplate = null,
                isActive = false,
                triggerCount = 0,
                lastTriggeredMs = null,
                createdAtMs = 30L,
            )
        )

        val result = repository.getAll()

        assertThat(result).containsExactly(
            CustomKeyword(
                id = "k-2",
                keyword = "pause",
                pattern = null,
                actionType = KeywordActionType.PAUSE,
                responseTemplate = null,
                isActive = false,
                triggerCount = 0,
                lastTriggeredMs = null,
                createdAtMs = 30L,
            )
        )
    }

    @Test
    fun `upsert maps domain model to dao entity`() = runTest {
        val captured = slot<CustomKeywordEntity>()
        coEvery { dao.upsert(capture(captured)) } just Runs

        repository.upsert(
            CustomKeyword(
                id = "k-3",
                keyword = "bookmark",
                pattern = "save this",
                actionType = KeywordActionType.BOOKMARK,
                responseTemplate = "saved",
                isActive = true,
                triggerCount = 9,
                lastTriggeredMs = 42L,
                createdAtMs = 12L,
            )
        )

        assertThat(captured.captured).isEqualTo(
            CustomKeywordEntity(
                id = "k-3",
                keyword = "bookmark",
                pattern = "save this",
                actionType = "BOOKMARK",
                responseTemplate = "saved",
                isActive = true,
                triggerCount = 9,
                lastTriggeredMs = 42L,
                createdAtMs = 12L,
            )
        )
    }

    @Test
    fun `delete delegates to dao`() = runTest {
        coEvery { dao.deleteById(any()) } just Runs

        repository.delete("k-delete")

        coVerify(exactly = 1) { dao.deleteById("k-delete") }
    }
}
