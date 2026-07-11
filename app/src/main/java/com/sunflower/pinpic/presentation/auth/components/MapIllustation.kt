package com.sunflower.pinpic.presentation.auth.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sunflower.pinpic.R

@Composable
fun MapIllustration(){
    val illustration = if (isSystemInDarkTheme()) {
        R.drawable.onboarding_map_dark
    } else {
        R.drawable.onboarding_map_light
    }
    Image(
        painter = painterResource(id = illustration),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp) // Allocates space for top map graphic area
            .clip(BottomCurveShape())
    )
}