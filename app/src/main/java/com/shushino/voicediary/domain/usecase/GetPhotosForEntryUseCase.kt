package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.repository.DiaryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPhotosForEntryUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    operator fun invoke(entryId: Long): Flow<List<Photo>> = repository.getPhotosForEntry(entryId)
}
