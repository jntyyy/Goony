package com.example.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.MaterialThemePalette
import com.example.ui.theme.MaterialYouColorPresets

/**
 * Color Palette Horizontal Picker matching the MetroList/Material You UI pattern.
 * Features:
 * - Leading Palette Icon button for Dynamic System Theme.
 * - Circular multi-segment swatches with completely circular selection ring (CircleShape)
 *   matching the circular curvature of the swatches.
 * - Powered by all official themes from mpvRx (Catppuccin, Nord, Tokyo Night, Rose Pine, Gruvbox, Dracula, etc.)
 */
@Composable
fun ColorPalettePicker(
    selectedId: String,
    onSelectPalette: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val presets = MaterialYouColorPresets.Presets
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Color Palette",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Current theme name indicator
            val currentPreset = MaterialYouColorPresets.getPreset(selectedId)
            Text(
                text = currentPreset.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. Leading Palette Icon button (for Dynamic Monet) with rounded squircle / circular design
            val isDynamicSelected = selectedId.equals(MaterialYouColorPresets.SYSTEM_DYNAMIC_ID, ignoreCase = true) || selectedId.isBlank()

            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        if (isDynamicSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                    )
                    .border(
                        width = if (isDynamicSelected) 2.5.dp else 1.dp,
                        color = if (isDynamicSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(18.dp)
                    )
                    .clickable { onSelectPalette(MaterialYouColorPresets.SYSTEM_DYNAMIC_ID) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Palette,
                    contentDescription = "Dynamic Palette",
                    tint = if (isDynamicSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // 2. Presets: Circular swatches with Pure Circular Selection Ring (CircleShape)
            presets.filter { it.id != MaterialYouColorPresets.SYSTEM_DYNAMIC_ID }.forEach { palette ->
                val isSelected = selectedId.equals(palette.id, ignoreCase = true)

                // Outer circular container with circular border selection
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = if (isSelected) 2.5.dp else 0.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                            shape = CircleShape
                        )
                        .clickable { onSelectPalette(palette.id) },
                    contentAlignment = Alignment.Center
                ) {
                    SplitCircleSwatch(
                        topColor = palette.previewTop,
                        bottomLeftColor = palette.previewBottomLeft,
                        bottomRightColor = palette.previewBottomRight,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                    )
                }
            }
        }
    }
}

/**
 * Draws a circle split into:
 * 1. Top half (Dominant theme dark/background color)
 * 2. Bottom-left quadrant (Primary accent)
 * 3. Bottom-right quadrant (Secondary/Tertiary accent)
 */
@Composable
fun SplitCircleSwatch(
    topColor: Color,
    bottomLeftColor: Color,
    bottomRightColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val circlePath = Path().apply {
            addOval(Rect(0f, 0f, width, height))
        }

        clipPath(circlePath) {
            // 1. Top half
            drawRect(
                color = topColor,
                topLeft = Offset(0f, 0f),
                size = Size(width, height / 2f)
            )

            // 2. Bottom-left quadrant
            drawRect(
                color = bottomLeftColor,
                topLeft = Offset(0f, height / 2f),
                size = Size(width / 2f, height / 2f)
            )

            // 3. Bottom-right quadrant
            drawRect(
                color = bottomRightColor,
                topLeft = Offset(width / 2f, height / 2f),
                size = Size(width / 2f, height / 2f)
            )
        }
    }
}
