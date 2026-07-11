package com.sunflower.pinpic.presentation.splash

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.sunflower.pinpic.R
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlin.time.Duration.Companion.milliseconds

/**
 *
 * While the logo is visible, [LaunchedEffect] simulates the backend routing
 * decision. Replace the [delay] with real auth-check logic in future tasks.
 *
 * @param onNavigateToLogin  Called when the routing decision is: user not signed in.
 * @param onNavigateToMain   Called when the routing decision is: user already signed in.
 */
@Composable
fun SplashScreen(
    onNavigateToLogin: () -> Unit,
    onNavigateToMain: () -> Unit,
    onNavigateToOnBoarding:()->Unit,
    viewModel: SplashViewModel = viewModel()
) {
    LaunchedEffect(Unit) {
        viewModel.sideEffect.collectLatest { effect ->
            delay(2500.milliseconds)
            when (effect) {
                SplashSideEffect.NavigateToLogin -> onNavigateToLogin()
                SplashSideEffect.NavigateToMain -> onNavigateToMain()
                SplashSideEffect.NavigationToOnboarding -> onNavigateToOnBoarding()
            }
        }
    }

    var showFinal by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "pinScale"
    )

    LaunchedEffect(Unit) {
        delay(700.milliseconds)
        showFinal = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.app_logo),
                contentDescription = null,
                modifier = Modifier
                    .size(140.dp)
                    .offset(x = 12.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
            )
            AnimatedVisibility(
                visible = showFinal,
                enter = fadeIn(tween(500, delayMillis = 150)) +
                        expandHorizontally(tween(500, delayMillis = 150), expandFrom = Alignment.Start)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column{
                        Row {
                            Text("Pin", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.primary)
                            Text("Pic", style = MaterialTheme.typography.displayLarge, color = MaterialTheme.colorScheme.secondary)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Photo Map & Chat", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }

}
