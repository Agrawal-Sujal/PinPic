package com.sunflower.pinpic

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.sunflower.pinpic.domain.map.MapClusteringSystem
import com.sunflower.pinpic.worker.SyncScheduler
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PinPicApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var mapClusteringSystem: MapClusteringSystem

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var syncScheduler: SyncScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            mapClusteringSystem.build()
        }
        syncScheduler.scheduleSync()
    }
}