package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class GetEntryByIdUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(id: Long) = repository.getEntryById(id)
}
