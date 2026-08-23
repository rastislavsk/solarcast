package io.github.rastislavsk.solarcast.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import io.github.rastislavsk.solarcast.R
import io.github.rastislavsk.solarcast.model.compassIndex
import java.time.LocalDate
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Fills `{name}` holes, the same shape the page's translations use. Keeping the
 * braces rather than switching to %1$s means the strings stay byte-identical to
 * the ones the generator lifts out of index.html.
 */
fun String.fill(vararg pairs: Pair<String, Any?>): String {
    var out = this
    for ((key, value) in pairs) out = out.replace("{$key}", value?.toString() ?: "")
    return out
}

/** The decimal mark this language writes numbers with — a comma in sk and de. */
@Composable
fun decimalMark(): String = stringResource(R.string.t_dec)

fun formatNumber(value: Double, decimals: Int, mark: String): String {
    if (!value.isFinite()) return "–"
    val s = String.format(Locale.ROOT, "%.${decimals}f", value)
    return if (mark == ".") s else s.replace(".", mark)
}

@Composable
fun num(value: Double, decimals: Int): String = formatNumber(value, decimals, decimalMark())

/** "14:00", the way the page writes an hour. */
fun hhmm(hour: Int): String = (if (hour < 10) "0" else "") + hour + ":00"

/** A fractional local hour as clock time: 5.683 -> "05:41". */
fun clock(hour: Double): String {
    val h = hour.toInt()
    val m = ((hour - h) * 60).roundToInt()
    val hh = if (m == 60) h + 1 else h
    val mm = if (m == 60) 0 else m
    return "%02d:%02d".format(Locale.ROOT, hh, mm)
}

@Composable
fun dayNameLong(date: LocalDate): String {
    val names = stringArrayResource(R.array.day_long)
    // LocalDate's Monday=1..Sunday=7; the arrays are Sunday-first like the page.
    return names[date.dayOfWeek.value % 7]
}

@Composable
fun dayNameShort(date: LocalDate): String {
    val names = stringArrayResource(R.array.day_short)
    return names[date.dayOfWeek.value % 7]
}

/**
 * "Mon 24. Aug" in most languages, "Mon 8/24" in Chinese — the page's own two
 * shapes, kept because a date reads differently in each.
 */
@Composable
fun dayLabel(date: LocalDate, short: Boolean): String {
    val name = if (short) dayNameShort(date) else dayNameLong(date)
    val isChinese = stringResource(R.string.t_dir) == "zh"
    if (isChinese) return "$name ${date.monthValue}/${date.dayOfMonth}"
    val months = stringArrayResource(R.array.month_short)
    return "$name ${date.dayOfMonth}. ${months[date.monthValue - 1]}"
}

@Composable
fun compassName(azimuth: Double): String =
    stringArrayResource(R.array.compass)[compassIndex(azimuth)]

/** Today in the forecast's own time zone, not the phone's. */
@Composable
fun todayIn(zoneId: String?): LocalDate {
    val context = LocalContext.current
    return remember(zoneId, context) {
        val zone = runCatching { java.time.ZoneId.of(zoneId) }
            .getOrElse { java.time.ZoneId.systemDefault() }
        LocalDate.now(zone)
    }
}

/** Signed difference with an arrow, as the summary card writes it. */
fun signed(value: Double, mark: String, decimals: Int = 1): String {
    val arrow = if (value >= 0) "↑ " else "↓ "
    return arrow + formatNumber(abs(value), decimals, mark)
}
