package com.earbrief.app.domain.usecase

import com.earbrief.app.domain.repository.CustomKeywordRepository
import javax.inject.Inject

class DeleteCustomKeywordUseCase @Inject constructor(
    private val customKeywordRepository: CustomKeywordRepository
) {
    suspend operator fun invoke(id: String) {
        customKeywordRepository.delete(id)
    }
}
