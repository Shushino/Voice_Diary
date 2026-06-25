package com.shushino.voicediary.domain.usecase

import com.shushino.voicediary.domain.model.Photo
import com.shushino.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class AddPhotoUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(photo: Photo) = repository.addPhoto(photo)
}
