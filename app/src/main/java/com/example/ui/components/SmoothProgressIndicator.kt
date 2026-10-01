package com.example.ui.components

import androidx.compose.animation.core.CubicBezierEasing
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A luxurious, ultra-smooth, and solid circular loading indicator.
 * Features a solid background track with rounded caps and dynamic breathing arc expansion/contraction.
 * Engineered for 60/120Hz displays with zero stutter and minimal battery impact.
 */
@Composable
fun SmoothProgressIndicator(
    modifier: Modifier = Modifier.size(32.dp),
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = color.copy(alpha = 0.16f),
    strokeWidth: Dp = 3.dp,
    durationMillis: Int = 1350,
    dotCount: Int = 12 // Kept for signature compatibility
) {
    val transition = rememberInfiniteTransition(label = "luxury_smooth_spinner_transition")

    // Continuous smooth 360-degree orbit rotation
    val baseRotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "luxury_smooth_base_rotation"
    )

    // Breathing arc cycle (expansion then catch-up contraction)
    val cycleProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = durationMillis, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "luxury_smooth_cycle_progress"
    )

    val luxuryEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    Canvas(
        modifier = modifier.progressSemantics()
    ) {
        val minDim = size.minDimension
        if (minDim <= 0f) return@Canvas

        val strokePx = strokeWidth.toPx().coerceIn(1.5f, minDim * 0.25f)
        val diameter = minDim - strokePx
        if (diameter <= 0f) return@Canvas

        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f
        )
        val arcSize = Size(diameter, diameter)

        val stroke = Stroke(
            width = strokePx,
            cap = StrokeCap.Round
        )

        // 1. Subtle, solid underlying guide track with rounded contour
        val effectiveTrackColor = if (trackColor == Color.Transparent) color.copy(alpha = 0.15f) else trackColor
        drawCircle(
            color = effectiveTrackColor,
            radius = diameter / 2f,
            center = center,
            style = stroke
        )

        // 2. Dynamic breathing arc with silky smooth expansion & contraction
        val minSweep = 24f
        val maxSweep = 270f

        val startAngle: Float
        val sweepAngle: Float

        if (cycleProgress < 0.5f) {
            // First phase: Head races forward, expanding the arc
            val t = cycleProgress / 0.5f
            val eased = luxuryEasing.transform(t)
            startAngle = 0f
            sweepAngle = minSweep + (maxSweep - minSweep) * eased
        } else {
            // Second phase: Tail smoothly chases the head, contracting the arc
            val t = (cycleProgress - 0.5f) / 0.5f
            val eased = luxuryEasing.transform(t)
            startAngle = (maxSweep - minSweep) * eased
            sweepAngle = maxSweep - (maxSweep - minSweep) * eased
        }

        // Additional rotational velocity during the expansion phase for fluid organic momentum
        val additionalRotation = cycleProgress * 360f

        drawArc(
            color = color,
            startAngle = baseRotation + additionalRotation + startAngle,
            sweepAngle = sweepAngle,
            useCenter = false,
            topLeft = topLeft,
            size = arcSize,
            style = stroke
        )
    }
}
