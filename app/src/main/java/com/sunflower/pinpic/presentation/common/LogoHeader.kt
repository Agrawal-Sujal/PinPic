package com.sunflower.pinpic.presentation.common

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.sunflower.pinpic.R

@Composable
fun LogoHeader(textStyle: TextStyle,imageHeight: Dp) {

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {

        Image(
            painter = painterResource(R.drawable.app_logo),
            contentDescription = null,
            modifier = Modifier.height(imageHeight)
        )
        Text(
            text = "Pin",
            fontWeight = FontWeight(800),
            style = textStyle,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Pic",
            fontWeight = FontWeight(800),
            style = textStyle,
            color = MaterialTheme.colorScheme.secondary
        )

    }

}