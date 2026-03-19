package com.earbrief.app.domain.usecase

import com.earbrief.app.domain.model.CustomKeyword
import com.earbrief.app.domain.repository.CustomKeywordRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveEnabledKeywordsUseCase @Inject constructor(
    private val customKeywordRepository: CustomKeywordRepository
) {
    operator fun invoke(): Flow<List<CustomKeyword>> {
        return customKeywordRepository.observeEnabledKeywords()
    }
}
