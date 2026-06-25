package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class SearchEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(query: String) = repository.searchEntries(query)
}
