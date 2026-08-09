package com.sunflower.pinpic.di

import android.content.Context
import androidx.room.Room
import com.sunflower.pinpic.data.local.AppDatabase
import com.sunflower.pinpic.data.local.dao.LocationInfoDao
import com.sunflower.pinpic.data.local.dao.PhotoDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides

    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pinpic_db"
        )
            .fallbackToDestructiveMigration(true)
            .build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: AppDatabase): PhotoDao {
        return database.photoDao()
    }

    @Provides
    @Singleton
    fun provideLocationInfoDao(database: AppDatabase): LocationInfoDao {
        return database.locationInfoDao()
    }
}
