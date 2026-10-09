package com.sunflower.pinpic.di


import com.sunflower.pinpic.data.repository.PhotosRepositoryImpl
import com.sunflower.pinpic.data.repository.UserRepositoryImpl
import com.sunflower.pinpic.domain.repository.PhotosRepository
import com.sunflower.pinpic.domain.repository.UserRepository
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

    @Binds
    @Singleton
    abstract fun bindUserRepository(
        userRepositoryImpl: UserRepositoryImpl
    ): UserRepository


}
