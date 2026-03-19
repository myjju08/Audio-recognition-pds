package com.earbrief.app.domain.repository

import com.earbrief.app.domain.model.CustomKeyword
import kotlinx.coroutines.flow.Flow

interface CustomKeywordRepository {
    fun observeEnabledKeywords(): Flow<List<CustomKeyword>>
    suspend fun getAll(): List<CustomKeyword>
    suspend fun upsert(keyword: CustomKeyword)
    suspend fun delete(id: String)
}
