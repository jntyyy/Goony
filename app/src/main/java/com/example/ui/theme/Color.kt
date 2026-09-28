package com.example.ui.theme

import androidx.compose.ui.graphics.Color

open class VaultThemePalette(
    open val name: String,
    open val bg: Color,
    open val surface: Color,
    open val cardBg: Color,
    open val textPrimary: Color,
    open val textSecondary: Color,
    open val textMuted: Color,
    open val border: Color,
    open val skeletonBg: Color
) {
    class Dynamic(
        override val name: String,
        override val bg: Color,
        override val surface: Color,
        override val cardBg: Color,
        override val textPrimary: Color,
        override val textSecondary: Color,
        override val textMuted: Color,
        override val border: Color,
        override val skeletonBg: Color
    ) : VaultThemePalette(name, bg, surface, cardBg, textPrimary, textSecondary, textMuted, border, skeletonBg)

    object Dark : VaultThemePalette(
        name = "Dark",
        bg = Color(0xFF28282D),
        surface = Color(0xFF28282D),
        cardBg = Color(0xFF34343B),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFD1D5DB),
        textMuted = Color(0xFF9CA3AF),
        border = Color(0x2EFFFFFF),
        skeletonBg = Color(0xFF404048)
    )

    object Amoled : VaultThemePalette(
        name = "Amoled",
        bg = Color(0xFF000000),
        surface = Color(0xFF000000),
        cardBg = Color(0xFF121216),
        textPrimary = Color(0xFFFFFFFF),
        textSecondary = Color(0xFFCCCCCC),
        textMuted = Color(0xFF888888),
        border = Color(0x33FFFFFF),
        skeletonBg = Color(0xFF1B1B20)
    )

    object Light : VaultThemePalette(
        name = "Light",
        bg = Color(0xFFF8FAFC),
        surface = Color(0xFFF8FAFC),
        cardBg = Color(0xFFFFFFFF),
        textPrimary = Color(0xFF0F172A),
        textSecondary = Color(0xFF475569),
        textMuted = Color(0xFF64748B),
        border = Color(0x1F0F172A),
        skeletonBg = Color(0xFFE2E8F0)
    )

    companion object {
        fun fromName(name: String): VaultThemePalette {
            return when (name.lowercase()) {
                "amoled" -> Amoled
                "light" -> Light
                else -> Dark
            }
        }
    }
}
