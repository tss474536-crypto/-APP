package com.shishi.schedule.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

data class Palette(
    val key: String,
    val primary: Color,
    val soft: Color,
    val accent: Color,
    val paper: Color
)

val palettes = listOf(
    Palette("鼠尾草", Color(0xFF718878), Color(0xFFDCE5DC), Color(0xFFD4A49A), Color(0xFFF6F2EC)),
    Palette("雾霾蓝", Color(0xFF667D91), Color(0xFFDCE4EA), Color(0xFFB58C96), Color(0xFFF3F1EE)),
    Palette("藕粉", Color(0xFF9B747D), Color(0xFFEADDE0), Color(0xFF778B7D), Color(0xFFF7F1F0)),
    Palette("奶咖", Color(0xFF8B7665), Color(0xFFE8DED3), Color(0xFF7D8A78), Color(0xFFF7F2EA)),
    Palette("暮紫", Color(0xFF756E88), Color(0xFFE3DFEA), Color(0xFFC19584), Color(0xFFF4F1F6))
)

fun paletteOf(key: String): Palette = palettes.firstOrNull { it.key == key } ?: palettes.first()

@Composable
fun ShishiTheme(content: @Composable () -> Unit) {
    val p = palettes.first()
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = p.primary,
            secondary = p.accent,
            background = p.paper,
            surface = p.paper,
            onPrimary = Color.White,
            onBackground = Color(0xFF302F2D),
            onSurface = Color(0xFF302F2D)
        ),
        typography = Typography(),
        content = content
    )
}
