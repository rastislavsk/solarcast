package io.github.rastislavsk.solarcast.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import kotlin.math.min

/**
 * The app's glyph: a compass rose with a cool wedge over noon and a warm one
 * over three o'clock. Same geometry as the page's header mark and the launcher
 * icon, drawn here so it takes the current scheme's colours.
 */
@Composable
fun SolarMark(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Canvas(modifier = modifier) {
        val d = min(size.width, size.height)
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = d / 2f * 0.92f
        val wedgeR = r * 0.93f
        val box = Rect(Offset(cx - wedgeR, cy - wedgeR), Size(wedgeR * 2, wedgeR * 2))

        // Cool wedge straddles 12 o'clock, warm straddles 3 — sweeping 90° each.
        drawArc(
            color = palette.cool.copy(alpha = 0.85f),
            startAngle = -135f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = box.topLeft,
            size = box.size,
        )
        drawArc(
            color = palette.accent.copy(alpha = 0.85f),
            startAngle = -45f,
            sweepAngle = 90f,
            useCenter = true,
            topLeft = box.topLeft,
            size = box.size,
        )

        val hair = d * 0.042f
        drawLine(palette.line, Offset(cx, cy - r), Offset(cx, cy + r), hair)
        drawLine(palette.line, Offset(cx - r, cy), Offset(cx + r, cy), hair)
        drawCircle(palette.line, r, Offset(cx, cy), style = Stroke(width = d * 0.058f))
        drawCircle(palette.text, d * 0.071f, Offset(cx, cy))
    }
}
