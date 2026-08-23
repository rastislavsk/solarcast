package io.github.rastislavsk.solarcast.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rastislavsk.solarcast.model.DayModel
import io.github.rastislavsk.solarcast.model.ForecastModel
import io.github.rastislavsk.solarcast.model.StringCalc
import io.github.rastislavsk.solarcast.ui.theme.JetBrainsMono
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.Palette
import io.github.rastislavsk.solarcast.ui.theme.azimuthColor
import io.github.rastislavsk.solarcast.ui.theme.heatColor
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

private fun axisStyle(color: Color) = TextStyle(
    fontFamily = JetBrainsMono,
    fontSize = 10.sp,
    color = color,
)

private fun DrawScope.label(
    measurer: TextMeasurer,
    text: String,
    x: Float,
    y: Float,
    color: Color,
    centre: Boolean = false,
    right: Boolean = false,
    size: Float = 10f,
) {
    val layout = measurer.measure(text, axisStyle(color).copy(fontSize = size.sp))
    val dx = when {
        centre -> x - layout.size.width / 2f
        right -> x - layout.size.width
        else -> x
    }
    drawText(layout, topLeft = Offset(dx, y - layout.size.height / 2f))
}

/* ------------------------------------------------------------------- dial */

/** The N/E/S/W ticks are drawn as text so they follow the app language. */
@Composable
fun DialWithCompass(
    strings: List<StringCalc>,
    compass: List<String>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(230.dp)) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val radius = min(cx, cy) - 24.dp.toPx()
        if (radius <= 0) return@Canvas

        for (r in listOf(0.33f, 0.66f, 1f)) {
            drawCircle(palette.grid, radius * r, Offset(cx, cy), style = Stroke(1.dp.toPx()))
        }
        drawLine(palette.grid, Offset(cx - radius, cy), Offset(cx + radius, cy), 1.dp.toPx())
        drawLine(palette.grid, Offset(cx, cy - radius), Offset(cx, cy + radius), 1.dp.toPx())

        val maxKwp = strings.maxOfOrNull { it.kwp } ?: 1.0
        strings.forEach { s ->
            val share = if (maxKwp > 0) (s.kwp / maxKwp).toFloat() else 0f
            val len = radius * (0.28f + 0.72f * share)
            val mid = (s.s.az - 90.0) * PI / 180.0
            val half = 13.0 * PI / 180.0
            val path = Path().apply {
                moveTo(cx, cy)
                lineTo(cx + (len * cos(mid - half)).toFloat(), cy + (len * sin(mid - half)).toFloat())
                lineTo(cx + (len * cos(mid + half)).toFloat(), cy + (len * sin(mid + half)).toFloat())
                close()
            }
            drawPath(path, palette.azimuthColor(s.s.az).copy(alpha = 0.9f))
        }
        drawCircle(palette.marker, 3.dp.toPx(), Offset(cx, cy))

        val out = radius + 13.dp.toPx()
        label(measurer, compass.getOrElse(0) { "N" }, cx, cy - out, palette.dim, centre = true)
        label(measurer, compass.getOrElse(2) { "E" }, cx + out, cy, palette.dim, centre = true)
        label(measurer, compass.getOrElse(4) { "S" }, cx, cy + out, palette.dim, centre = true)
        label(measurer, compass.getOrElse(6) { "W" }, cx - out, cy, palette.dim, centre = true)
    }
}

/* ---------------------------------------------------------------- heatmap */

/**
 * Rows are days, columns hours, intensity is AC output. The pale envelope marks
 * the hours between sunrise and sunset, so an empty cell inside it reads as
 * cloud rather than night.
 */
@Composable
fun HeatmapChart(
    model: ForecastModel,
    dayLabels: List<String>,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val rows = model.days.size
    val rowHeight = 26.dp
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(rowHeight * rows + 34.dp),
    ) {
        val leftGutter = 30.dp.toPx()
        val topGutter = 18.dp.toPx()
        val gridW = size.width - leftGutter
        val gridH = size.height - topGutter - 8.dp.toPx()
        if (gridW <= 0 || gridH <= 0 || rows == 0) return@Canvas
        val cellW = gridW / 24f
        val cellH = gridH / rows
        val maxAC = max(model.maxHourAC, 0.0001)

        for (h in 0..24 step 6) {
            val x = leftGutter + h * cellW
            label(measurer, "%02d".format(h % 24), x, topGutter - 9.dp.toPx(), palette.dim2, centre = true)
        }

        model.days.forEachIndexed { r, day ->
            val y = topGutter + r * cellH
            label(
                measurer,
                dayLabels.getOrElse(r) { "" },
                leftGutter - 6.dp.toPx(),
                y + cellH / 2f,
                palette.dim,
                right = true,
            )

            // Daylight envelope behind the cells.
            val rise = day.sunrise
            val set = day.sunset
            if (rise != null && set != null && set > rise) {
                drawRect(
                    color = palette.envelope,
                    topLeft = Offset(leftGutter + (rise * cellW).toFloat(), y + 1f),
                    size = Size(((set - rise) * cellW).toFloat(), cellH - 2f),
                )
            }

            for (h in 0 until 24) {
                val slot = day.hours.getOrNull(h)
                val x = leftGutter + h * cellW
                val colour = if (slot == null || slot.ac <= 0.0) {
                    palette.emptyCell
                } else {
                    palette.heatColor(slot.ac / maxAC)
                }
                drawRect(
                    color = colour,
                    topLeft = Offset(x + 0.5f, y + 1.5f),
                    size = Size(max(0f, cellW - 1f), max(0f, cellH - 3f)),
                )
            }
        }
    }
}

/* ------------------------------------------------------------------- bars */

/**
 * Daily totals. The filled bar is the forecast, the dashed outline behind it the
 * modelled clear-sky ceiling — the gap is what the weather is costing.
 */
@Composable
fun BarsChart(
    model: ForecastModel,
    dayLabels: List<String>,
    dateLabels: List<String>,
    todayIndex: Int,
    formatValue: (Double) -> String,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    Canvas(modifier = modifier.fillMaxWidth().height(240.dp)) {
        val bottom = size.height - 34.dp.toPx()
        val top = 18.dp.toPx()
        val left = 34.dp.toPx()
        val plotW = size.width - left - 4.dp.toPx()
        val plotH = bottom - top
        if (plotW <= 0 || plotH <= 0 || model.days.isEmpty()) return@Canvas

        val peak = max(
            model.days.maxOfOrNull { max(it.kwh, it.clearKwh) } ?: 1.0,
            0.0001,
        )
        // Round the axis up to something a person would choose.
        val step = niceStep(peak / 4)
        val axisMax = (Math.ceil(peak / step) * step).toFloat()

        var g = 0.0
        while (g <= axisMax) {
            val y = bottom - (g / axisMax * plotH).toFloat()
            drawLine(palette.grid, Offset(left, y), Offset(left + plotW, y), 1f)
            label(measurer, formatValue(g), left - 6.dp.toPx(), y, palette.dim2, right = true)
            g += step
        }
        drawLine(palette.gridBase, Offset(left, bottom), Offset(left + plotW, bottom), 1.dp.toPx())

        val slot = plotW / model.days.size
        val barW = slot * 0.56f
        model.days.forEachIndexed { i, day ->
            val cx = left + slot * i + slot / 2f
            val ceilH = (day.clearKwh / axisMax * plotH).toFloat()
            val valH = (day.kwh / axisMax * plotH).toFloat()

            drawRect(
                color = palette.ceiling,
                topLeft = Offset(cx - barW / 2f, bottom - ceilH),
                size = Size(barW, ceilH),
                style = Stroke(
                    width = 1.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f)),
                ),
            )
            drawRect(
                color = if (i == todayIndex) palette.barToday else palette.barFill,
                topLeft = Offset(cx - barW / 2f, bottom - valH),
                size = Size(barW, valH),
            )
            label(
                measurer,
                formatValue(day.kwh),
                cx,
                bottom - valH - 9.dp.toPx(),
                palette.text,
                centre = true,
                size = 11f,
            )
            label(measurer, dayLabels.getOrElse(i) { "" }, cx, bottom + 12.dp.toPx(), palette.dim, centre = true)
            label(measurer, dateLabels.getOrElse(i) { "" }, cx, bottom + 25.dp.toPx(), palette.dim2, centre = true, size = 9f)
        }
    }
}

private fun niceStep(raw: Double): Double {
    if (raw <= 0) return 1.0
    val magnitude = Math.pow(10.0, Math.floor(Math.log10(raw)))
    val normalised = raw / magnitude
    val stepped = when {
        normalised <= 1 -> 1.0
        normalised <= 2 -> 2.0
        normalised <= 5 -> 5.0
        else -> 10.0
    }
    return stepped * magnitude
}

/* ------------------------------------------------------------------ curve */

/**
 * One day's power through the hours, stacked by string, with cloud cover on the
 * right axis. Tapping picks an hour and reports it back for the read-out.
 */
@Composable
fun CurveChart(
    day: DayModel,
    strings: List<StringCalc>,
    acLimit: Double,
    selectedHour: Int?,
    onSelectHour: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    Box(modifier = modifier.fillMaxWidth().height(250.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .pointerInput(day, strings.size) {
                    detectTapGestures { offset ->
                        val left = with(density) { 34.dp.toPx() }
                        val right = with(density) { 34.dp.toPx() }
                        val plotW = size.width - left - right
                        if (plotW <= 0) return@detectTapGestures
                        val fraction = ((offset.x - left) / plotW).coerceIn(0f, 1f)
                        val hour = (fraction * 23f).toInt().coerceIn(0, 23)
                        onSelectHour(if (hour == selectedHour) null else hour)
                    }
                },
        ) {
            val left = 34.dp.toPx()
            val right = 34.dp.toPx()
            val top = 16.dp.toPx()
            val bottom = size.height - 26.dp.toPx()
            val plotW = size.width - left - right
            val plotH = bottom - top
            if (plotW <= 0 || plotH <= 0) return@Canvas

            val peak = max(
                max(day.hours.filterNotNull().maxOfOrNull { max(it.ac, it.clear) } ?: 0.0, acLimit * 0.2),
                0.0001,
            )
            val step = niceStep(peak / 4)
            val axisMax = (Math.ceil(peak / step) * step).toFloat()

            var g = 0.0
            while (g <= axisMax) {
                val y = bottom - (g / axisMax * plotH).toFloat()
                drawLine(palette.grid, Offset(left, y), Offset(left + plotW, y), 1f)
                label(measurer, trimNumber(g), left - 6.dp.toPx(), y, palette.dim2, right = true)
                g += step
            }

            // Right axis: cloud cover, always 0-100.
            for (pct in listOf(0, 50, 100)) {
                val y = bottom - (pct / 100f) * plotH
                label(measurer, "$pct%", left + plotW + 6.dp.toPx(), y, palette.dim2)
            }

            fun xAt(h: Int) = left + plotW * (h / 23f)
            fun yAt(v: Double) = bottom - (v / axisMax * plotH).toFloat()

            // Clear-sky ceiling, dashed.
            val ceilingPath = Path()
            var started = false
            for (h in 0 until 24) {
                val slot = day.hours.getOrNull(h) ?: continue
                val p = Offset(xAt(h), yAt(slot.clear))
                if (!started) {
                    ceilingPath.moveTo(p.x, p.y); started = true
                } else {
                    ceilingPath.lineTo(p.x, p.y)
                }
            }
            if (started) {
                drawPath(
                    ceilingPath,
                    palette.ceiling,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 5f)),
                    ),
                )
            }

            // Strings, back to front, each filled to the axis.
            strings.indices.reversed().forEach { k ->
                val colour = palette.azimuthColor(strings[k].s.az)
                val area = Path()
                var open = false
                for (h in 0 until 24) {
                    val slot = day.hours.getOrNull(h) ?: continue
                    val stacked = slot.per.take(k + 1).sum()
                    val p = Offset(xAt(h), yAt(stacked))
                    if (!open) {
                        area.moveTo(p.x, bottom); area.lineTo(p.x, p.y); open = true
                    } else {
                        area.lineTo(p.x, p.y)
                    }
                }
                if (open) {
                    area.lineTo(xAt(23), bottom)
                    area.close()
                    drawPath(area, colour.copy(alpha = 0.35f))
                    val line = Path()
                    var first = true
                    for (h in 0 until 24) {
                        val slot = day.hours.getOrNull(h) ?: continue
                        val stacked = slot.per.take(k + 1).sum()
                        val p = Offset(xAt(h), yAt(stacked))
                        if (first) { line.moveTo(p.x, p.y); first = false } else line.lineTo(p.x, p.y)
                    }
                    drawPath(line, colour, style = Stroke(width = 2.dp.toPx()))
                }
            }

            // Cloud cover, dotted, on the right axis.
            val cloudPath = Path()
            var cloudStarted = false
            for (h in 0 until 24) {
                val cc = day.hours.getOrNull(h)?.cloud ?: continue
                val y = bottom - (cc / 100.0 * plotH).toFloat()
                val p = Offset(xAt(h), y)
                if (!cloudStarted) { cloudPath.moveTo(p.x, p.y); cloudStarted = true } else cloudPath.lineTo(p.x, p.y)
            }
            if (cloudStarted) {
                drawPath(
                    cloudPath,
                    palette.cloud,
                    style = Stroke(
                        width = 1.5.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(2f, 6f)),
                    ),
                )
            }

            for (h in listOf(0, 6, 12, 18)) {
                label(measurer, h.toString(), xAt(h), bottom + 12.dp.toPx(), palette.dim2, centre = true)
            }

            selectedHour?.let { h ->
                val x = xAt(h)
                drawLine(palette.marker.copy(alpha = 0.5f), Offset(x, top), Offset(x, bottom), 1.dp.toPx())
                day.hours.getOrNull(h)?.let { slot ->
                    drawCircle(palette.marker, 3.5.dp.toPx(), Offset(x, yAt(slot.ac)))
                }
            }
        }
    }
}

private fun trimNumber(v: Double): String =
    if (v == Math.floor(v)) v.toInt().toString() else "%.1f".format(v)
