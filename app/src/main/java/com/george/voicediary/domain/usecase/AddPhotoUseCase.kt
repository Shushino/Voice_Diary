package com.george.voicediary.domain.usecase

import com.george.voicediary.domain.model.Photo
import com.george.voicediary.domain.repository.DiaryRepository
import javax.inject.Inject

class AddPhotoUseCase @Inject constructor(
    private val repository: DiaryRepository
) {
    suspend operator fun invoke(photo: Photo) = repository.addPhoto(photo)
}