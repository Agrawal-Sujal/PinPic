package com.sunflower.pinpic.presentation.onboarding.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sunflower.pinpic.R

@Composable
fun CameraIllustration() {

    val illustration = if (isSystemInDarkTheme()) {
        R.drawable.onboarding_camera_dark
    } else {
        R.drawable.onboarding_camera_light
    }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {

        Image(
            painter = painterResource(illustration),
            contentDescription = null,
            modifier = Modifier.size(330.dp)
        )

    }

}