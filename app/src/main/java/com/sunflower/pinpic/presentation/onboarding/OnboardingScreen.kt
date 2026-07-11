package com.sunflower.pinpic.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import com.sunflower.pinpic.presentation.onboarding.components.ContinueButton
import com.sunflower.pinpic.presentation.onboarding.components.OnboardingPage
import com.sunflower.pinpic.presentation.onboarding.components.PagerIndicator

@Composable
fun OnboardingScreen(
    onFinish: () -> Unit
) {

    val pages = remember { onboardingPages() }

    val pagerState = rememberPagerState(
        initialPage = 0,
        pageCount = { pages.size }
    )

    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->

            OnboardingPage(
                page = pages[page]
            )

        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(
                    horizontal = 24.dp,
                    vertical = 32.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            PagerIndicator(
                pageCount = pages.size,
                currentPage = pagerState.currentPage
            )

            Spacer(Modifier.height(28.dp))

            ContinueButton {

                if (pagerState.currentPage == pages.lastIndex) {

                    onFinish()

                } else {

                    scope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1
                        )
                    }

                }

            }

        }

    }

}