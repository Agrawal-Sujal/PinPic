package com.sunflower.pinpic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.sunflower.pinpic.presentation.navigation.AppNavDisplay
import com.sunflower.pinpic.ui.theme.PinPicTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PinPicTheme {
                AppNavDisplay()
            }
        }
    }
}
