package com.sunflower.pinpic.domain.usecase

import com.sunflower.pinpic.domain.model.Photo
import com.sunflower.pinpic.domain.repository.PhotosRepository
import com.sunflower.pinpic.common.Result
import javax.inject.Inject

class SavePhotoUseCase @Inject constructor(
    private val repository: PhotosRepository
) {
    suspend operator fun invoke(photo: Photo): Result<Unit> {
        return repository.savePhoto(photo)
    }
}
