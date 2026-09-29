package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.progressSemantics
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A sleek, soft, and resource-efficient continuous loading spinner.
 * Features a subtle background track and a smooth rotating rounded arc.
 * Consumes minimal CPU/battery via single continuous rotation without layout re-measuring.
 */
@Composable
fun SmoothProgressIndicator(
    modifier: Modifier = Modifier.size(32.dp),
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.16f),
    strokeWidth: Dp = 2.5.dp,
    durationMillis: Int = 900
) {
    val transition = rememberInfiniteTransition(label = "smooth_spinner_transition")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "smooth_spinner_rotation"
    )

    Canvas(
        modifier = modifier.progressSemantics()
    ) {
        val strokePx = strokeWidth.toPx()
        val diameter = size.minDimension - strokePx
        if (diameter <= 0f) return@Canvas

        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round
        )

        val topLeftOffset = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        // 1. Subtle soft circular track
        if (trackColor != Color.Transparent && trackColor.alpha > 0f) {
            drawCircle(
                color = trackColor,
                radius = diameter / 2f,
                center = center,
                style = stroke
            )
        }

        // 2. Smooth rotating rounded arc
        rotate(degrees = rotation, pivot = center) {
            drawArc(
                color = color,
                startAngle = 0f,
                sweepAngle = 110f,
                useCenter = false,
                topLeft = topLeftOffset,
                size = arcSize,
                style = stroke
            )
        }
    }
}
