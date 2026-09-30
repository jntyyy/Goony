package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Fargos Web-style Cubic Bezier Easing curve (0.16, 1, 0.3, 1).
 */
val FargosEasing = CubicBezierEasing(0.16f, 1.0f, 0.30f, 1.0f)

/**
 * Fargos tween animation spec helper.
 */
fun <T> fargosTween(durationMillis: Int = 280, delayMillis: Int = 0) = tween<T>(
    durationMillis = durationMillis,
    delayMillis = delayMillis,
    easing = FargosEasing
)

/**
 * Namida-inspired staggered entrance animation.
 * Triggers only on first appearance of the item (using rememberSaveable so scrolling does not replay).
 */
@Composable
fun StaggeredEntranceItem(
    index: Int,
    modifier: Modifier = Modifier,
    delayPerItemMs: Long = 40L,
    content: @Composable () -> Unit
) {
    var isVisible by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!isVisible) {
            val staggerDelay = (index.coerceAtMost(12) * delayPerItemMs)
            delay(staggerDelay)
            isVisible = true
        }
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(durationMillis = 350)) +
                slideInVertically(
                    animationSpec = tween(durationMillis = 350),
                    initialOffsetY = { fullHeight -> (fullHeight / 4).coerceAtLeast(30) }
                ),
        modifier = modifier
    ) {
        content()
    }
}
