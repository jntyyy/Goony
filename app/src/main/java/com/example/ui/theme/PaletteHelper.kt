package com.example.ui.theme

import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.palette.graphics.Palette
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracts the vibrant or dominant color from an image URL using AndroidX Palette.
 * Runs asynchronously on IO dispatcher and caches the result for smooth Namida-style UI.
 */
@Composable
fun rememberDominantColor(
    imageUrl: String?,
    fallbackColor: Color = LocalAccentColor.current
): State<Color> {
    val context = LocalContext.current
    val dominantColor = remember(imageUrl) { mutableStateOf(fallbackColor) }

    LaunchedEffect(imageUrl) {
        if (imageUrl.isNullOrBlank()) {
            dominantColor.value = fallbackColor
            return@LaunchedEffect
        }

        withContext(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Required for Palette software bitmap extraction
                    .size(120) // Downscaled for ultra-fast Palette extraction
                    .build()

                val result = (loader.execute(request) as? SuccessResult)?.drawable
                val bitmap = (result as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    val palette = Palette.from(bitmap).generate()
                    val swatch = palette.vibrantSwatch
                        ?: palette.dominantSwatch
                        ?: palette.mutedSwatch
                        ?: palette.lightVibrantSwatch

                    val colorInt = swatch?.rgb
                    if (colorInt != null) {
                        withContext(Dispatchers.Main) {
                            dominantColor.value = Color(colorInt)
                        }
                    }
                }
            } catch (_: Throwable) {
                // Graceful fallback to default accent color
            }
        }
    }

    return dominantColor
}
