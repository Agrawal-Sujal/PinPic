package com.sunflower.pinpic.presentation.navigation

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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sunflower.pinpic.presentation.gallery.GalleryScreen
import com.sunflower.pinpic.presentation.map.MapScreen
import com.sunflower.pinpic.presentation.profile.ProfileScreen

sealed class MainTab(val icon: ImageVector, val filledIcon: ImageVector, val label: String) {
    data object Map : MainTab(Icons.Outlined.LocationOn, Icons.Filled.LocationOn, "Map")
    data object Gallery : MainTab(Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary, "Gallery")
    data object Chat : MainTab(Icons.Outlined.Chat, Icons.Filled.Chat, "Chat")
    data object Profile : MainTab(Icons.Outlined.Person, Icons.Filled.Person, "Profile")
}

@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit = {},
    currentTab: MutableState<MainTab?>
) {
    val backStack = remember { mutableStateListOf<MainTab>(MainTab.Gallery) }
    currentTab.value = backStack.lastOrNull() ?: MainTab.Gallery
    Box(modifier = Modifier.fillMaxSize()) {
        NavDisplay(
            backStack = backStack,
            onBack = {
                backStack.removeLastOrNull()
                if (currentTab != MainTab.Gallery) backStack.add(MainTab.Gallery)
            },
            entryProvider = entryProvider {
                entry<MainTab.Map> {
                        MapScreen(
                            onBackClick = {
                                backStack.removeLastOrNull()
                                backStack.add(MainTab.Gallery)
                            }
                        )

                }
                entry<MainTab.Gallery> {
                    GalleryScreen(
                        onNavigateToSettings = onNavigateToSettings
                    )
                }
                entry<MainTab.Chat> {
                    DummyScreen("Coming Soon")
                }
                entry<MainTab.Profile> {
                    ProfileScreen()
                }
            }
        )

        if (currentTab.value != MainTab.Map) {
            Box(modifier = Modifier.align(Alignment.BottomCenter)) {
                CustomFloatingBottomNavigation(
                    currentTab = currentTab.value!!,
                    onTabSelected = { tab ->
                        if (currentTab != tab) {
                            if (tab == MainTab.Gallery) backStack.clear()
                            backStack.add(tab)
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CustomFloatingBottomNavigation(
    currentTab: MainTab,
    onTabSelected: (MainTab) -> Unit
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
                .border(
                    width = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = androidx.compose.foundation.shape.CircleShape
                )
                .shadow(elevation = 1.dp, shape = androidx.compose.foundation.shape.CircleShape)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = androidx.compose.foundation.shape.CircleShape
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



@Composable
fun DummyScreen(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            modifier = Modifier,
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
