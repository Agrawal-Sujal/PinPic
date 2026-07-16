package com.sunflower.pinpic.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.sunflower.pinpic.data.local.AppDatabase
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


    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE photos ADD COLUMN thumbnailUri TEXT")
        }
    }


    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("CREATE TABLE photos_new (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, lat REAL, lng REAL, date TEXT, time TEXT, locationName TEXT, uri TEXT, url TEXT, clusterId INTEGER)")
            db.execSQL("INSERT INTO photos_new (id, lat, lng, date, time, locationName, uri, url, clusterId) SELECT id, lat, lng, date, time, locationName, uri, url, clusterId FROM photos")
            db.execSQL("DROP TABLE photos")
            db.execSQL("ALTER TABLE photos_new RENAME TO photos")
            db.execSQL("ALTER TABLE cluster_points ADD COLUMN thumbnailUri TEXT")
        }
    }

    @Provides

    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "pinpic_db"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration(false)
            .build()
    }

    @Provides
    @Singleton
    fun providePhotoDao(database: AppDatabase): PhotoDao {
        return database.photoDao()
    }
}
