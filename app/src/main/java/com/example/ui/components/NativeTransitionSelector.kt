package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class TransitionOptionItem(
    val id: Int,
    val title: String,
    val subtitle: String,
    val icon: ImageVector
)

@Composable
fun NativeTransitionSelector(
    selectedStyle: Int,
    onSelectStyle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf(
        TransitionOptionItem(
            id = 0,
            title = "Default Motion",
            subtitle = "Smooth vertical slide & fade",
            icon = Icons.Outlined.SwapVert
        ),
        TransitionOptionItem(
            id = 1,
            title = "Lateral Slide",
            subtitle = "Clean horizontal side navigation",
            icon = Icons.Outlined.SwapHoriz
        ),
        TransitionOptionItem(
            id = 2,
            title = "Smooth Fade & Scale",
            subtitle = "Ultra lightweight & fluid transition",
            icon = Icons.Outlined.AutoAwesome
        ),
        TransitionOptionItem(
            id = 3,
            title = "Link Transition",
            subtitle = "Static top header with seamless in-place transitions",
            icon = Icons.Outlined.Link
        )
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            val isSelected = selectedStyle == option.id
            val containerColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.30f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                animationSpec = tween(220),
                label = "transition_card_bg"
            )
            val borderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                animationSpec = tween(220),
                label = "transition_card_border"
            )

            Surface(
                onClick = { onSelectStyle(option.id) },
                shape = CircleShape,
                color = containerColor,
                border = BorderStroke(if (isSelected) 1.5.dp else 1.dp, borderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                        .fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview Icon Swatch (Slimmer & Fully Circular)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                                else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = option.icon,
                            contentDescription = option.title,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = option.title,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 14.sp
                            ),
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = option.subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 11.sp
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    RadioButton(
                        selected = isSelected,
                        onClick = { onSelectStyle(option.id) },
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.primary,
                            unselectedColor = MaterialTheme.colorScheme.outlineVariant
                        ),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
