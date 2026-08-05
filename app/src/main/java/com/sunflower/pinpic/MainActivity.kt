package com.sunflower.pinpic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.sunflower.pinpic.data.local.datastore.DataStoreManager
import com.sunflower.pinpic.presentation.navigation.AppNavDisplay
import com.sunflower.pinpic.ui.theme.PinPicTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStoreManager: DataStoreManager

    companion object{
        var darkTheme = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val themeMode by dataStoreManager.themeModeFlow.collectAsState(initial = "LIGHT")
            darkTheme = when (themeMode) {
                "DARK" -> true
                "SYSTEM" -> isSystemInDarkTheme()
                else -> false
            }

            PinPicTheme(darkTheme = darkTheme) {
                AppNavDisplay()
            }
        }
    }
}
