package com.sunflower.pinpic.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.sunflower.pinpic.presentation.gallery.GalleryScreen
import com.sunflower.pinpic.presentation.map.MapScreen
import com.sunflower.pinpic.presentation.profile.ProfileScreen


enum class MainTab(val icon: ImageVector, val filledIcon: ImageVector, val label: String) {
    Map(Icons.Outlined.LocationOn, Icons.Filled.LocationOn, "Map"),
    Gallery(Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary, "Gallery"),
    Chat(Icons.Outlined.Chat, Icons.Filled.Chat, "Chat"),
    Profile(Icons.Outlined.Person, Icons.Filled.Person, "Profile")
}

@Composable
fun MainScreen(
    onNavigateToCamera: () -> Unit
) {
    var currentTab by remember { mutableStateOf(MainTab.Gallery) }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            when (currentTab) {
                MainTab.Map -> MapScreen()
                MainTab.Gallery -> GalleryScreen()
                MainTab.Chat -> DummyScreen("Chat Screen")
                MainTab.Profile -> ProfileScreen()
            }
        }

        Box(modifier = Modifier.align(Alignment.BottomCenter)) {
            CustomFloatingBottomNavigation(
                currentTab = currentTab,
                onTabSelected = { currentTab = it },
                onCameraClick = onNavigateToCamera
            )
        }
    }
}

@Composable
fun CustomFloatingBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
    onCameraClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 8.dp)
            .height(96.dp)
    ) {
        // Curved background bar using your theme colors
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .align(Alignment.BottomCenter)
                .border(width = 0.5.dp, color = MaterialTheme.colorScheme.outline, shape = BottomNavCutoutShape())
                .shadow(elevation = 1.dp, shape = BottomNavCutoutShape())
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = BottomNavCutoutShape()
                )
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavItem(
                tab = MainTab.Map,
                isSelected = currentTab == MainTab.Map,
                onClick = { onTabSelected(MainTab.Map) }
            )
            BottomNavItem(
                tab = MainTab.Gallery,
                isSelected = currentTab == MainTab.Gallery,
                onClick = { onTabSelected(MainTab.Gallery) }
            )

            Spacer(modifier = Modifier.width(64.dp))

            BottomNavItem(
                tab = MainTab.Chat,
                isSelected = currentTab == MainTab.Chat,
                onClick = { onTabSelected(MainTab.Chat) }
            )
            BottomNavItem(
                tab = MainTab.Profile,
                isSelected = currentTab == MainTab.Profile,
                onClick = { onTabSelected(MainTab.Profile) }
            )
        }

        // Center FAB using your theme colors
        FloatingActionButton(
            onClick = onCameraClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary, // Kept your primary theme color
            contentColor = MaterialTheme.colorScheme.onPrimary,  // Kept your on-primary theme color
            modifier = Modifier
                .size(50.dp)
                .align(Alignment.TopCenter)
                .offset(y = 8.dp)
        ) {
            Icon(
                imageVector = Icons.Outlined.CameraAlt,
                contentDescription = "Camera",
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun BottomNavItem(
    tab: MainTab,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(CircleShape)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(8.dp)
            .width(44.dp)
    ) {
        Icon(
            imageVector = if (isSelected) tab.filledIcon else tab.icon,
            contentDescription = tab.label,
            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(
                alpha = 0.6f
            ),
            modifier = Modifier.size(26.dp)
        )
    }
}

class BottomNavCutoutShape : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cornerRadius = size.height / 2

            moveTo(cornerRadius, 0f)

            val cutoutWidth = size.width * 0.25f
            val center = size.width / 2
            val leftCurveStart = center - (cutoutWidth / 2) - 15
            val rightCurveEnd = center + (cutoutWidth / 2) + 15
            val dipDepth = size.height * 0.53f

            lineTo(leftCurveStart, 0f)

            cubicTo(
                x1 = leftCurveStart + (cutoutWidth * 0.2f), y1 = 0f,
                x2 = center - (cutoutWidth * 0.25f), y2 = dipDepth,
                x3 = center, y3 = dipDepth
            )

            cubicTo(
                x1 = center + (cutoutWidth * 0.25f), y1 = dipDepth,
                x2 = rightCurveEnd - (cutoutWidth * 0.2f), y2 = 0f,
                x3 = rightCurveEnd, y3 = 0f
            )

            lineTo(size.width - cornerRadius, 0f)
            arcTo(
                rect = Rect(
                    size.width - (cornerRadius * 2), 0f, size.width, size.height
                ),
                startAngleDegrees = -90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )

            lineTo(cornerRadius, size.height)

            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    0f, 0f, cornerRadius * 2, size.height
                ),
                startAngleDegrees = 90f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            close()
        }
        return Outline.Generic(path)
    }
}

@Composable
fun DummyScreen(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}