package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetAllEntriesUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke() = repository.getAllEntries()
}