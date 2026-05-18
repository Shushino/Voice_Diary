package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class SearchEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(query: String) = repository.searchEntries(query)
}