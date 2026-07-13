package com.sunflower.pinpic.di

import com.sunflower.pinpic.data.repository.PhotosRepositoryImpl
import com.sunflower.pinpic.domain.repository.PhotosRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindPhotosRepository(
        photosRepositoryImpl: PhotosRepositoryImpl
    ): PhotosRepository
}
