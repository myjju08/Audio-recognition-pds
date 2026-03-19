package com.earbrief.app.domain.usecase

import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.repository.CustomKeywordRepository
import javax.inject.Inject

class UpsertCustomKeywordUseCase @Inject constructor(
    private val customKeywordRepository: CustomKeywordRepository
) {
    suspend operator fun invoke(keyword: CustomKeyword) {
        customKeywordRepository.upsert(keyword)
    }
}
