package io.github.rastislavsk.solarcast.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import io.github.rastislavsk.solarcast.model.clamp
import io.github.rastislavsk.solarcast.model.norm360
import kotlin.math.floor
import kotlin.math.min

/**
 * A string's colour carries information: cool for morning and east, warm for
 * evening and west. The hue is interpolated round the compass rather than
 * picked from a list, so two strings 20° apart look 20° apart.
 */
fun Palette.azimuthColor(azCompass: Double): Color {
    val a = norm360(azCompass)
    val stops = listOf(
        0.0 to az.n,
        90.0 to az.e,
        180.0 to az.s,
        270.0 to az.w,
        360.0 to az.n,
    )
    for (i in 1 until stops.size) {
        if (a <= stops[i].first) {
            val (fromAt, fromColor) = stops[i - 1]
            val (toAt, toColor) = stops[i]
            val f = ((a - fromAt) / (toAt - fromAt)).toFloat()
            return lerp(fromColor, toColor, f)
        }
    }
    return az.s
}

/** Position on the heat ramp, 0 to 1. */
fun Palette.heatColor(t: Double): Color {
    val x = clamp(t, 0.0, 1.0) * (heat.size - 1)
    val i = min(heat.size - 2, floor(x).toInt())
    return lerp(heat[i], heat[i + 1], (x - i).toFloat())
}
