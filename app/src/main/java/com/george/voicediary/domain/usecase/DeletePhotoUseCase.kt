package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class DeletePhotoUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(id: Long) = repository.deletePhoto(id)
}