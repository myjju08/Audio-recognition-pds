package com.earbrief.app.data.repository

import com.earbrief.app.data.local.db.dao.CustomKeywordDao
import com.earbrief.app.data.local.db.entity.CustomKeywordEntity
import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.model.KeywordActionType
import com.earbrief.app.domain.repository.CustomKeywordRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RoomCustomKeywordRepository @Inject constructor(
    private val customKeywordDao: CustomKeywordDao
) : CustomKeywordRepository {

    override fun observeEnabledKeywords(): Flow<List<CustomKeyword>> {
        return customKeywordDao.observeEnabled().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override suspend fun getAll(): List<CustomKeyword> {
        return customKeywordDao.getAll().map { it.toDomain() }
    }

    override suspend fun upsert(keyword: CustomKeyword) {
        customKeywordDao.upsert(keyword.toEntity())
    }

    override suspend fun delete(id: String) {
        customKeywordDao.deleteById(id)
    }

    private fun CustomKeyword.toEntity(): CustomKeywordEntity {
        return CustomKeywordEntity(
            id = id,
            keyword = keyword,
            pattern = pattern,
            actionType = actionType.name,
            responseTemplate = responseTemplate,
            isActive = isActive,
            triggerCount = triggerCount,
            lastTriggeredMs = lastTriggeredMs,
            createdAtMs = createdAtMs,
        )
    }

    private fun CustomKeywordEntity.toDomain(): CustomKeyword {
        return CustomKeyword(
            id = id,
            keyword = keyword,
            pattern = pattern,
            actionType = KeywordActionType.valueOf(actionType),
            responseTemplate = responseTemplate,
            isActive = isActive,
            triggerCount = triggerCount,
            lastTriggeredMs = lastTriggeredMs,
            createdAtMs = createdAtMs,
        )
    }
}
