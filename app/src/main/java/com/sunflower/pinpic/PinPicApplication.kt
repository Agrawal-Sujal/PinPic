package com.sunflower.pinpic

import android.app.Application
import com.sunflower.pinpic.domain.map.MapClusteringSystem
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class PinPicApplication : Application() {
    @Inject
    lateinit var mapClusteringSystem: MapClusteringSystem

    override fun onCreate() {
        super.onCreate()
        CoroutineScope(Dispatchers.IO).launch {
            mapClusteringSystem.build()
        }
    }
}