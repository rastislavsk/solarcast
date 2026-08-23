package io.github.rastislavsk.solarcast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rastislavsk.solarcast.R
import io.github.rastislavsk.solarcast.model.DayModel
import io.github.rastislavsk.solarcast.model.ForecastModel
import io.github.rastislavsk.solarcast.ui.charts.BarsChart
import io.github.rastislavsk.solarcast.ui.charts.CurveChart
import io.github.rastislavsk.solarcast.ui.charts.DialWithCompass
import io.github.rastislavsk.solarcast.ui.charts.HeatmapChart
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.SolarType
import io.github.rastislavsk.solarcast.ui.theme.azimuthColor
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun ForecastScreen(
    model: ForecastModel,
    acLimit: Double,
    selectedDay: Int,
    onSelectDay: (Int) -> Unit,
    suggested: Boolean,
    suggestedNote: String?,
    modifier: Modifier = Modifier,
) {
    val today = todayIn(model.tz)
    val todayIndex = model.days.indexOfFirst { it.date == today }
    val mark = decimalMark()

    Column(modifier = modifier.fillMaxWidth()) {
        if (suggested && suggestedNote != null) {
            Notice(stringResource(R.string.t_suggT), suggestedNote)
            Spacer(Modifier.height(24.dp))
        }

        SummaryCard(model, todayIndex)
        Spacer(Modifier.height(32.dp))

        SectionHeader(stringResource(R.string.t_heatT), description = stringResource(R.string.t_heatD))
        SolarCard(padding = 12) {
            HeatmapChart(
                model = model,
                dayLabels = model.days.map { dayNameShortOf(it.date) },
            )
        }
        Spacer(Modifier.height(32.dp))

        SectionHeader(stringResource(R.string.t_barsT), description = stringResource(R.string.t_barsD))
        BarsLegend()
        SolarCard(padding = 12) {
            BarsChart(
                model = model,
                dayLabels = model.days.map { dayNameShortOf(it.date) },
                dateLabels = model.days.map { shortDate(it.date) },
                todayIndex = todayIndex,
                formatValue = { formatNumber(it, 0, mark) },
            )
        }
        Spacer(Modifier.height(32.dp))

        SectionHeader(stringResource(R.string.t_curveT), description = stringResource(R.string.t_curveD))
        DayTabs(model, selectedDay, todayIndex, onSelectDay)
        Spacer(Modifier.height(12.dp))
        model.days.getOrNull(selectedDay)?.let { day ->
            var pickedHour by remember(selectedDay) { mutableStateOf<Int?>(null) }
            SolarCard(padding = 12) {
                CurveChart(
                    day = day,
                    strings = model.strs,
                    acLimit = acLimit,
                    selectedHour = pickedHour,
                    onSelectHour = { pickedHour = it },
                )
            }
            HourReadout(day, pickedHour)
            Spacer(Modifier.height(12.dp))
            CurveLegend(model)
        }
        Spacer(Modifier.height(32.dp))

        SectionHeader(stringResource(R.string.t_tableT))
        DayTable(model, todayIndex)
    }
}

/* ---------------------------------------------------------------- summary */

@Composable
private fun SummaryCard(model: ForecastModel, todayIndex: Int) {
    val palette = LocalPalette.current
    val mark = decimalMark()
    val todayDay = model.days.getOrNull(if (todayIndex >= 0) todayIndex else 0)
    val tomorrow = model.days.getOrNull((if (todayIndex >= 0) todayIndex else 0) + 1)
    val weekTotal = model.days.sumOf { it.kwh }

    SolarCard(padding = 0) {
        Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
            DialWithCompass(
                strings = model.strs,
                compass = stringArrayResource(R.array.compass).toList(),
            )
        }
        Text(
            stringResource(R.string.t_dialCap).uppercase(),
            style = SolarType.label,
            color = palette.dim2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
        )
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.line))

        Column(modifier = Modifier.padding(18.dp)) {
            todayDay?.let { day ->
                Figure(
                    label = stringResource(R.string.t_today),
                    value = num(day.kwh, 1),
                    unit = "kWh",
                    note = day.peakHour?.let {
                        stringResource(R.string.t_peakAt)
                            .fill("p" to num(day.peak, 2), "h" to hhmm(it))
                    } ?: stringResource(R.string.t_noProd),
                )
            }
            tomorrow?.let { day ->
                Divider()
                val delta = day.kwh - (todayDay?.kwh ?: 0.0)
                val pct = if ((todayDay?.kwh ?: 0.0) > 0) {
                    (delta / (todayDay?.kwh ?: 1.0) * 100).roundToInt()
                } else {
                    null
                }
                Figure(
                    label = stringResource(R.string.t_tomorrow),
                    value = num(day.kwh, 1),
                    unit = "kWh",
                    note = buildString {
                        append(signed(delta, mark))
                        append(" kWh ")
                        append(stringResource(R.string.t_vsToday))
                        if (pct != null) append(" · ${if (pct >= 0) "+" else "−"}${abs(pct)} %")
                    },
                )
            }
            Divider()
            val best = model.days.maxByOrNull { it.kwh }
            val worst = model.days.minByOrNull { it.kwh }
            Figure(
                label = stringResource(R.string.t_sevenDays),
                value = num(weekTotal, 0),
                unit = "kWh",
                note = if (best != null && worst != null) {
                    "${stringResource(R.string.t_best)} ${shortDate(best.date)} ${num(best.kwh, 1)} kWh · " +
                        "${stringResource(R.string.t_worst)} ${shortDate(worst.date)} ${num(worst.kwh, 1)} kWh"
                } else {
                    ""
                },
            )
        }
    }
}

@Composable
private fun Divider() {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .height(1.dp)
            .background(palette.lineSoft),
    )
}

@Composable
private fun Figure(label: String, value: String, unit: String, note: String) {
    val palette = LocalPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        MonoLabel(label, modifier = Modifier.padding(bottom = 8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = SolarType.figure, color = palette.text)
            Text(
                " $unit",
                style = SolarType.monoStrong,
                color = palette.dim,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        if (note.isNotBlank()) {
            Text(
                note,
                style = SolarType.bodySmall,
                color = palette.dim,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

/* ------------------------------------------------------------------ parts */

@Composable
private fun BarsLegend() {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        LegendSwatch(palette.barToday, stringResource(R.string.t_legForecast))
        LegendSwatch(palette.ceiling, stringResource(R.string.t_legCeiling))
    }
}

@Composable
private fun CurveLegend(model: ForecastModel) {
    val palette = LocalPalette.current
    Column(modifier = Modifier.fillMaxWidth()) {
        model.strs.forEach { s ->
            LegendSwatch(
                palette.azimuthColor(s.s.az),
                "${compassName(s.s.az)} ${s.s.az.roundToInt()}° / ${s.s.tilt.roundToInt()}° · " +
                    "${num(s.kwp, 2)} ${stringResource(R.string.t_kwp)}",
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        LegendSwatch(palette.cloud, stringResource(R.string.t_legCloud))
    }
}

@Composable
private fun DayTabs(
    model: ForecastModel,
    selected: Int,
    todayIndex: Int,
    onSelect: (Int) -> Unit,
) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        model.days.forEachIndexed { index, day ->
            val active = index == selected
            Text(
                text = if (index == todayIndex) {
                    stringResource(R.string.t_today)
                } else {
                    dayLabel(day.date, short = false)
                },
                style = SolarType.mono,
                color = if (active) palette.bg else palette.dim,
                modifier = Modifier
                    .background(if (active) palette.text else palette.surface, ControlShape)
                    .border(BorderStroke(1.dp, if (active) palette.text else palette.line), ControlShape)
                    .clickable { onSelect(index) }
                    .padding(horizontal = 15.dp, vertical = 12.dp),
            )
        }
    }
}

/** Reads out the hour the user tapped on the curve; silent until they tap one. */
@Composable
private fun HourReadout(day: DayModel, hour: Int?) {
    val palette = LocalPalette.current
    val slot = hour?.let { day.hours.getOrNull(it) } ?: return
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(hhmm(slot.h), style = SolarType.monoStrong, color = palette.text)
        Text("${num(slot.ac, 2)} kW", style = SolarType.mono, color = palette.dim)
        slot.cloud?.let {
            Text(
                "${stringResource(R.string.t_cloudiness)} ${it.roundToInt()} %",
                style = SolarType.mono,
                color = palette.dim,
            )
        }
        Text("${num(slot.temp, 1)} °C", style = SolarType.mono, color = palette.dim)
    }
}

/* ------------------------------------------------------------------ table */

@Composable
private fun DayTable(model: ForecastModel, todayIndex: Int) {
    val palette = LocalPalette.current
    SolarCard(padding = 0) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            MonoLabel(stringResource(R.string.t_thDay), modifier = Modifier.weight(2.2f))
            MonoLabel(stringResource(R.string.t_thKwh), modifier = Modifier.weight(1.2f))
            MonoLabel(stringResource(R.string.t_thUse), modifier = Modifier.weight(1.2f))
            MonoLabel(stringResource(R.string.t_thPeak), modifier = Modifier.weight(1.2f))
            MonoLabel(stringResource(R.string.t_thCloud), modifier = Modifier.weight(1f))
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.line))
        model.days.forEachIndexed { index, day ->
            val share = if (day.clearKwh > 0) (day.kwh / day.clearKwh * 100).roundToInt() else null
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (index == todayIndex) palette.surface2 else palette.surface)
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(2.2f)) {
                    Text(
                        if (index == todayIndex) {
                            stringResource(R.string.t_today)
                        } else {
                            dayNameLongOf(day.date)
                        },
                        style = SolarType.body,
                        color = palette.text,
                    )
                    Text(
                        shortDate(day.date),
                        style = SolarType.mono.copy(fontSize = 10.5.sp),
                        color = palette.dim2,
                    )
                }
                Text(num(day.kwh, 1), style = SolarType.monoStrong, color = palette.text, modifier = Modifier.weight(1.2f))
                Text(
                    share?.let { "$it %" } ?: "–",
                    style = SolarType.mono,
                    color = palette.dim,
                    modifier = Modifier.weight(1.2f),
                )
                Text(num(day.peak, 2), style = SolarType.mono, color = palette.dim, modifier = Modifier.weight(1.2f))
                Text(
                    day.cloudAvg?.let { "${it.roundToInt()} %" } ?: "–",
                    style = SolarType.mono,
                    color = palette.dim,
                    modifier = Modifier.weight(1f),
                )
            }
            if (index < model.days.lastIndex) {
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(palette.lineSoft))
            }
        }
    }
}

/* --------------------------------------------------------------- helpers */

@Composable
private fun dayNameShortOf(date: LocalDate): String = dayNameShort(date)

@Composable
private fun dayNameLongOf(date: LocalDate): String = dayNameLong(date)

/** "8/24" in Chinese, "24. Aug" elsewhere. */
@Composable
private fun shortDate(date: LocalDate): String {
    val isChinese = stringResource(R.string.t_dir) == "zh"
    if (isChinese) return "${date.monthValue}/${date.dayOfMonth}"
    val months = stringArrayResource(R.array.month_short)
    return "${date.dayOfMonth}. ${months[date.monthValue - 1]}"
}
