package com.sunflower.pinpic.presentation.gallery.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sunflower.pinpic.data.local.entity.Visibility

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeVisibilityBottomSheet(
    selectedCount: Int,
    initialVisibility: Visibility? = null,
    onDismissRequest: () -> Unit,
    onApply: (Visibility) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedVisibility by remember { mutableStateOf<Visibility?>(initialVisibility) }
    val context = LocalContext.current

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 38.dp, height = 4.dp)
                    .background(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                        shape = CircleShape
                    )
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
        ) {
            Spacer(modifier = Modifier.height(4.dp))

            // Title
            Text(
                text = "Change Visibility",
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = if (selectedCount == 1) "Apply to 1 selected photo" else "Apply to $selectedCount selected photos",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Option 1: Make Private
            VisibilityOptionRow(
                title = "Make Private",
                subtitle = "Only you can see these photos",
                icon = Icons.Filled.Lock,
                iconTint = Color(0xFFF05A28),
                iconContainerColor = Color(0xFFFFECE8),
                isSelected = selectedVisibility == Visibility.PRIVATE,
                onClick = { selectedVisibility = Visibility.PRIVATE }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 2: Share with Friends
            VisibilityOptionRow(
                title = "Share with Friends",
                subtitle = "Visible to your friends",
                icon = Icons.Filled.People,
                iconTint = Color(0xFF2E7D32),
                iconContainerColor = Color(0xFFE8F5E9),
                isSelected = selectedVisibility == Visibility.FRIENDS,
                onClick = { selectedVisibility = Visibility.FRIENDS }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Option 3: Make Public
            VisibilityOptionRow(
                title = "Make Public",
                subtitle = "Visible to everyone",
                icon = Icons.Filled.Public,
                iconTint = Color(0xFF1E88E5),
                iconContainerColor = Color(0xFFE3F2FD),
                isSelected = selectedVisibility == Visibility.PUBLIC,
                onClick = { selectedVisibility = Visibility.PUBLIC }
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Apply Button
            val buttonText = if (selectedCount == 1) "Apply to 1 Photo" else "Apply to $selectedCount Photos"
            Button(
                onClick = {
                    val chosen = selectedVisibility
                    if (chosen == null) {
                        Toast.makeText(context, "Please select a visibility option", Toast.LENGTH_SHORT).show()
                    } else {
                        onApply(chosen)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = buttonText,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VisibilityOptionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    iconContainerColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Leading Icon in circular colored container
        Box(
            modifier = Modifier
                .size(46.dp)
                .background(color = iconContainerColor, shape = CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(22.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // Title and Subtitle
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Trailing Radio Button
        RadioButton(
            selected = isSelected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
            )
        )
    }
}

