package io.github.rastislavsk.solarcast.model

import io.github.rastislavsk.solarcast.data.Config
import io.github.rastislavsk.solarcast.data.PvString
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset
import kotlin.math.min

/** One hour of the forecast, already projected onto the array. */
data class HourSlot(
    val h: Int,
    /** AC power after inverter efficiency and the AC limit, in kW. */
    val ac: Double,
    /** The same, split across the strings in config order. */
    val per: List<Double>,
    /** What a cloudless sky would have given at this hour. */
    val clear: Double,
    val temp: Double,
    val cloud: Double?,
)

data class DayModel(
    val date: LocalDate,
    val hours: List<HourSlot?>,
    val kwh: Double,
    val clearKwh: Double,
    val peak: Double,
    val peakHour: Int?,
    val perStr: List<Double>,
    val cloudAvg: Double?,
    val tMax: Double,
    val first: Int?,
    val last: Int?,
    /** Local hour, fractional, from the API's daily block. */
    val sunrise: Double?,
    val sunset: Double?,
)

/** A string with the values the model needs precomputed. */
data class StringCalc(val s: PvString, val kwp: Double, val azSol: Double)

data class ForecastModel(
    val days: List<DayModel>,
    val strs: List<StringCalc>,
    val maxHourAC: Double,
    val tz: String,
)

private fun pos(v: Double): Double = if (!v.isFinite() || v < 0) 0.0 else v

private fun JSONArray.doubleOrNull(i: Int): Double? {
    if (isNull(i)) return null
    val v = optDouble(i, Double.NaN)
    return if (v.isFinite()) v else null
}

/** "2026-08-23T05:41" -> 5.683..., the local hour as a fraction. */
private fun hourOf(iso: String?): Double? {
    if (iso.isNullOrBlank()) return null
    val m = Regex("T(\\d\\d):(\\d\\d)").find(iso) ?: return null
    return m.groupValues[1].toInt() + m.groupValues[2].toInt() / 60.0
}

/**
 * Turns one API response into the seven days the app draws.
 *
 * Two things here are easy to get wrong and are kept exactly as the web app has
 * them. Open-Meteo returns local wall-clock stamps with no offset, and each
 * value is the average over the hour *ending* at its stamp — so the stamp is
 * parsed as if it were UTC, shifted back by the reported offset to get real UTC,
 * and the sun position is taken at the midpoint of the hour before it. The hour
 * a value belongs to is therefore the stamp's hour minus one, which rolls the
 * midnight value back onto the previous day.
 */
fun buildModel(api: JSONObject, config: Config): ForecastModel {
    val lat = config.lat ?: return ForecastModel(emptyList(), emptyList(), 0.0, "")
    val lon = config.lon ?: return ForecastModel(emptyList(), emptyList(), 0.0, "")

    // The API's elevation for the exact coordinates beats the geocoder's.
    val elev = api.optDouble("elevation", Double.NaN).let { if (it.isFinite()) it else config.elev }

    val hourly = api.optJSONObject("hourly") ?: return ForecastModel(emptyList(), emptyList(), 0.0, "")
    val time = hourly.optJSONArray("time") ?: return ForecastModel(emptyList(), emptyList(), 0.0, "")
    val ghiA = hourly.optJSONArray("shortwave_radiation")
    val dniA = hourly.optJSONArray("direct_normal_irradiance")
    val dhiA = hourly.optJSONArray("diffuse_radiation")
    val tempA = hourly.optJSONArray("temperature_2m")
    val cloudA = hourly.optJSONArray("cloud_cover")

    val offsetMs = api.optLong("utc_offset_seconds", 0L) * 1000L

    val strs = config.strings.map { s ->
        StringCalc(s = s, kwp = s.kwp, azSol = azSolar(s.az))
    }

    class Bucket(val date: LocalDate) {
        val hours = arrayOfNulls<HourSlot>(24)
        var kwh = 0.0
        var clearKwh = 0.0
        var peak = 0.0
        var peakHour: Int? = null
        val perStr = DoubleArray(strs.size)
        var cloudSum = 0.0
        var cloudN = 0
        var tMax = -999.0
        var first: Int? = null
        var last: Int? = null
        var sunrise: Double? = null
        var sunset: Double? = null
    }

    val days = LinkedHashMap<LocalDate, Bucket>()

    for (i in 0 until time.length()) {
        val stamp = time.optString(i) ?: continue
        val endUtc = runCatching {
            LocalDateTime.parse(stamp).toInstant(ZoneOffset.UTC).toEpochMilli()
        }.getOrNull() ?: continue

        val trueUtc = endUtc - offsetMs
        val mid = trueUtc - 1_800_000L

        val endLocal = LocalDateTime.ofEpochSecond(endUtc / 1000, 0, ZoneOffset.UTC)
        var hour = endLocal.hour - 1
        var shift = 0L
        if (hour < 0) {
            hour = 23
            shift = -1
        }
        val dayDate = LocalDateTime
            .ofEpochSecond((endUtc + shift * 86_400_000L) / 1000, 0, ZoneOffset.UTC)
            .toLocalDate()

        val bucket = days.getOrPut(dayDate) { Bucket(dayDate) }

        val ghi = pos(ghiA?.optDouble(i, 0.0) ?: 0.0)
        val dni = pos(dniA?.optDouble(i, 0.0) ?: 0.0)
        val dhi = pos(dhiA?.optDouble(i, 0.0) ?: 0.0)
        val ta = tempA?.doubleOrNull(i) ?: 15.0
        val cc = cloudA?.doubleOrNull(i)

        val sp = sunPos(mid, lat, lon)
        val dcs = DoubleArray(strs.size)
        var dcSum = 0.0
        var csSum = 0.0
        for (k in strs.indices) {
            val g = poa(ghi, dni, dhi, sp, strs[k].s.tilt, strs[k].azSol)
            val p = dcPower(g, ta, strs[k].kwp, config.tempCoef)
            dcs[k] = p
            dcSum += p
            csSum += dcPower(
                clearSkyPOA(mid, lat, lon, strs[k].s.tilt, strs[k].azSol, elev),
                ta,
                strs[k].kwp,
                config.tempCoef,
            )
        }
        val ac = min(dcSum * config.eff, config.acLimit)
        val csAC = min(csSum * config.eff, config.acLimit)
        val per = dcs.map { if (dcSum > 0) ac * it / dcSum else 0.0 }

        bucket.hours[hour] = HourSlot(h = hour, ac = ac, per = per, clear = csAC, temp = ta, cloud = cc)
        bucket.kwh += ac
        bucket.clearKwh += csAC
        for (m in per.indices) bucket.perStr[m] += per[m]
        if (ac > bucket.peak) {
            bucket.peak = ac
            bucket.peakHour = hour
        }
        cc?.let {
            bucket.cloudSum += it
            bucket.cloudN++
        }
        if (ta > bucket.tMax) bucket.tMax = ta
        if (ac > 0.02) {
            if (bucket.first == null) bucket.first = hour
            bucket.last = hour
        }
    }

    api.optJSONObject("daily")?.let { daily ->
        val dTime = daily.optJSONArray("time")
        val sunriseA = daily.optJSONArray("sunrise")
        val sunsetA = daily.optJSONArray("sunset")
        if (dTime != null) {
            for (q in 0 until dTime.length()) {
                val date = runCatching { LocalDate.parse(dTime.optString(q)) }.getOrNull() ?: continue
                days[date]?.let { bucket ->
                    bucket.sunrise = hourOf(sunriseA?.optString(q))
                    bucket.sunset = hourOf(sunsetA?.optString(q))
                }
            }
        }
    }

    // A day at either end of the range is usually incomplete; keep only days
    // with most of their hours, then the first seven.
    val list = days.values
        .filter { bucket -> bucket.hours.count { it != null } >= 20 }
        .take(7)
        .map { b ->
            DayModel(
                date = b.date,
                hours = b.hours.toList(),
                kwh = b.kwh,
                clearKwh = b.clearKwh,
                peak = b.peak,
                peakHour = b.peakHour,
                perStr = b.perStr.toList(),
                cloudAvg = if (b.cloudN > 0) b.cloudSum / b.cloudN else null,
                tMax = b.tMax,
                first = b.first,
                last = b.last,
                sunrise = b.sunrise,
                sunset = b.sunset,
            )
        }

    val maxHour = list.flatMap { it.hours }.filterNotNull().maxOfOrNull { it.ac } ?: 0.0

    return ForecastModel(
        days = list,
        strs = strs,
        maxHourAC = maxHour,
        tz = api.optString("timezone").ifBlank { config.tz },
    )
}
