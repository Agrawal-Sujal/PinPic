package com.sunflower.pinpic.presentation.navigation

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.sunflower.pinpic.presentation.gallery.GalleryScreen
import com.sunflower.pinpic.presentation.map.MapScreen
import com.sunflower.pinpic.presentation.map.OtherUserMapScreen
import com.sunflower.pinpic.presentation.profile.EditProfileScreen
import com.sunflower.pinpic.presentation.profile.OtherUserProfileScreen
import com.sunflower.pinpic.presentation.profile.ProfileScreen
import com.sunflower.pinpic.presentation.search.SearchScreen
import com.sunflower.pinpic.presentation.settings.SettingsScreen

sealed class MainTab(val icon: ImageVector, val filledIcon: ImageVector, val label: String) {
    data object Map : MainTab(Icons.Outlined.LocationOn, Icons.Filled.LocationOn, "Map")
    data object Gallery : MainTab(Icons.Outlined.PhotoLibrary, Icons.Filled.PhotoLibrary, "Gallery")
    data object Search : MainTab(Icons.Outlined.Search, Icons.Filled.Search, "Search")
    data object Chat : MainTab(Icons.Outlined.Chat, Icons.Filled.Chat, "Chat")
    data object Profile : MainTab(Icons.Outlined.Person, Icons.Filled.Person, "Profile")
}

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun MainScreen(
    currentTab: MutableState<MainTab?>
) {
    val backStack = remember { mutableStateListOf<Any>(MainTab.Gallery) }
    var isGallerySelectionMode by remember { mutableStateOf(false) }
    currentTab.value = backStack.lastOrNull { it is MainTab } as? MainTab ?: MainTab.Gallery
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
                        onSelectionModeChange = { isGallerySelectionMode = it }
                    )
                }
                entry<MainTab.Search> {
                    SearchScreen(
                        onNavigateToUserProfile = { user ->
                            backStack.add(
                                Screen.OtherUserProfile(
                                    userId = user.id,
                                    username = user.username,
                                    name = user.name,
                                    pictureUrl = user.pictureUrl,
                                    followerCount = user.followerCount,
                                    followingCount = user.followingCount,
                                    isFollowing = user.isFollowing
                                )
                            )
                        }
                    )
                }
                entry<MainTab.Chat> {
                    DummyScreen("Coming Soon")
                }
                entry<MainTab.Profile> {
                    ProfileScreen(
                        onNavigateToEditProfile = { backStack.add(Screen.EditProfile) },
                        onNavigateToSettings = { backStack.add(Screen.Settings) }
                    )
                }
                entry<Screen.Settings> {
                    SettingsScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Screen.EditProfile> {
                    EditProfileScreen(
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
                entry<Screen.OtherUserProfile> { screen ->
                    OtherUserProfileScreen(
                        initialProfile = screen,
                        onBack = { backStack.removeLastOrNull() },
                        onNavigateToMap = { userId, username ->
                            backStack.add(
                                Screen.OtherUserMap(
                                    userId = userId,
                                    username = username,
                                    name = screen.name
                                )
                            )
                        }
                    )
                }
                entry<Screen.OtherUserMap> { screen ->
                    OtherUserMapScreen(
                        userId = screen.userId,
                        username = screen.username,
                        onBack = { backStack.removeLastOrNull() }
                    )
                }
            }
        )

        val currentEntry = backStack.lastOrNull()
        if (currentEntry is MainTab && currentEntry != MainTab.Map && !(currentEntry == MainTab.Gallery && isGallerySelectionMode)) {
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
                tab = MainTab.Search,
                isSelected = currentTab == MainTab.Search,
                onClick = { onTabSelected(MainTab.Search) }
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
