package com.shishi.schedule.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.shishi.schedule.data.SceneSettings
import java.time.LocalDate
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun DynamicBackdrop(settings: SceneSettings, palette: Palette, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize()) {
        SeasonalCanvas(palette)
        Box(Modifier.fillMaxSize().background(palette.paper.copy(alpha = 0.58f)))
    }
}

@Composable
private fun SeasonalCanvas(palette: Palette) {
    val transition = rememberInfiniteTransition(label = "season")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000), RepeatMode.Restart),
        label = "phase"
    )
    val month = LocalDate.now().monthValue
    Canvas(Modifier.fillMaxSize().background(palette.soft)) {
        when (month) {
            in 3..5 -> {
                repeat(22) { i ->
                    val x = ((i * 83f + phase * size.width * 0.6f) % (size.width + 80f)) - 40f
                    val y = ((i * 131f + phase * size.height) % (size.height + 80f)) - 40f
                    drawOval(palette.accent.copy(alpha = .28f), Offset(x, y), Size(18f, 10f))
                }
            }
            in 6..8 -> {
                repeat(6) { row ->
                    val path = Path()
                    val base = size.height * (.45f + row * .085f)
                    path.moveTo(0f, base)
                    var x = 0f
                    while (x <= size.width) {
                        val y = base + sin((x / 80f) + phase * 6.28f + row) * 18f
                        path.lineTo(x, y)
                        x += 16f
                    }
                    drawPath(path, palette.primary.copy(alpha = .12f + row * .02f), style = Stroke(7f))
                }
            }
            in 9..11 -> {
                repeat(24) { i ->
                    val random = Random(i)
                    val x = (random.nextFloat() * size.width + phase * 180f) % size.width
                    val y = (random.nextFloat() * size.height + phase * size.height) % size.height
                    drawOval(palette.accent.copy(alpha = .25f), Offset(x, y), Size(15f, 25f))
                }
            }
            else -> {
                repeat(42) { i ->
                    val x = ((i * 67f + phase * 50f) % size.width)
                    val y = ((i * 109f + phase * size.height) % size.height)
                    drawCircle(Color.White.copy(alpha = .55f), 3f + (i % 4), Offset(x, y))
                }
            }
        }
    }
}
