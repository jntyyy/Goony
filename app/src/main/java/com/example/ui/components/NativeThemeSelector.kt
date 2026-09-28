package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class ThemeOptionItem(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val previewBgColor: Color
)

@Composable
fun NativeThemeSelector(
    selectedTheme: String,
    onSelectTheme: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        ThemeOptionItem(
            id = "Dark",
            title = "Dark",
            icon = Icons.Outlined.DarkMode,
            previewBgColor = Color(0xFF1E1E22)
        ),
        ThemeOptionItem(
            id = "Amoled",
            title = "Amoled",
            icon = Icons.Outlined.Brightness2,
            previewBgColor = Color(0xFF000000)
        ),
        ThemeOptionItem(
            id = "Light",
            title = "Light",
            icon = Icons.Outlined.LightMode,
            previewBgColor = Color(0xFFF1F5F9)
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedTheme.equals(option.id, ignoreCase = true)
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                animationSpec = tween(220),
                label = "theme_card_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                animationSpec = tween(220),
                label = "theme_card_border"
            )

            Surface(
                onClick = { onSelectTheme(option.id) },
                shape = CircleShape,
                color = containerColor,
                border = androidx.compose.foundation.BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 7.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview Icon Swatch (Slimmer & Fully Circular)
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(option.previewBgColor)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = null,
                            tint = if (option.id == "Light") MaterialTheme.colorScheme.primary else Color(0xFFE2E8F0),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Native Radio Button Selection
                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectTheme(option.id) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    )
                }
            }
        }
    }
}
