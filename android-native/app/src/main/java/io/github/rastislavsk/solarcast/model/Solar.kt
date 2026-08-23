package io.github.rastislavsk.solarcast.model

import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/*
 * The solar model, ported line for line from index.html. The numbers this
 * produces must match the web app's, so the formulas, the constants and the
 * order of operations are kept exactly as they are there — including the
 * calibrated clear-sky constants, which are not textbook values.
 */

const val RAD = Math.PI / 180.0

fun norm360(a: Double): Double {
    val x = a % 360.0
    return if (x < 0) x + 360.0 else x
}

fun clamp(v: Double, a: Double, b: Double): Double = if (v < a) a else if (v > b) b else v

fun lerp(a: Double, b: Double, t: Double): Double = a + (b - a) * t

/**
 * The user enters compass azimuth (0 = north, 180 = south). The solar formulas
 * below work in the convention where 0 = south and east is negative.
 */
fun azSolar(azCompass: Double): Double {
    var a = azCompass - 180.0
    if (a > 180) a -= 360.0
    if (a < -180) a += 360.0
    return a
}

/** Where the sun is, by the simplified NOAA algorithm. */
data class SunPos(val zen: Double, val cosZ: Double, val az: Double, val elev: Double)

fun sunPos(utcMs: Long, lat: Double, lon: Double): SunPos {
    val jd = utcMs / 86400000.0 + 2440587.5
    val t2 = (jd - 2451545.0) / 36525.0
    val l0 = (280.46646 + 36000.76983 * t2 + 0.0003032 * t2 * t2) % 360.0
    val m = 357.52911 + 35999.05029 * t2 - 0.0001537 * t2 * t2
    val e = 0.016708634 - t2 * (0.000042037 + 0.0000001267 * t2)
    val c = sin(m * RAD) * (1.914602 - t2 * (0.004817 + 0.000014 * t2)) +
        sin(2 * m * RAD) * (0.019993 - 0.000101 * t2) +
        sin(3 * m * RAD) * 0.000289
    val trueLong = l0 + c
    val omega = 125.04 - 1934.136 * t2
    val lambda = trueLong - 0.00569 - 0.00478 * sin(omega * RAD)
    val eps0 = 23 + (26 + (21.448 - t2 * (46.815 + t2 * (0.00059 - t2 * 0.001813))) / 60) / 60
    val eps = eps0 + 0.00256 * cos(omega * RAD)
    val decl = asin(sin(eps * RAD) * sin(lambda * RAD)) / RAD

    val y = tan(eps / 2 * RAD) * tan(eps / 2 * RAD)
    val eqTime = 4 * (
        y * sin(2 * l0 * RAD) - 2 * e * sin(m * RAD) +
            4 * e * y * sin(m * RAD) * cos(2 * l0 * RAD) -
            0.5 * y * y * sin(4 * l0 * RAD) - 1.25 * e * e * sin(2 * m * RAD)
        ) / RAD

    // Minutes since midnight UTC, from the epoch millis directly: the web
    // version reads them off a Date, and this avoids dragging a calendar in.
    val msIntoDay = Math.floorMod(utcMs, 86400000L)
    val utMin = msIntoDay / 60000.0

    var tst = (utMin + eqTime + 4 * lon) % 1440.0
    if (tst < 0) tst += 1440.0
    val ha = tst / 4 - 180.0

    val cosZ = clamp(
        sin(lat * RAD) * sin(decl * RAD) + cos(lat * RAD) * cos(decl * RAD) * cos(ha * RAD),
        -1.0,
        1.0,
    )
    val zen = acos(cosZ) / RAD
    val az = atan2(
        sin(ha * RAD),
        cos(ha * RAD) * sin(lat * RAD) - tan(decl * RAD) * cos(lat * RAD),
    ) / RAD
    return SunPos(zen = zen, cosZ = cosZ, az = az, elev = 90 - zen)
}

/**
 * Irradiance on a tilted plane. Isotropic sky, ground albedo 0.2 — the same
 * assumptions Open-Meteo makes for its own GTI variable, which is what lets the
 * two agree to within 0.1% per hour.
 */
fun poa(ghi: Double, dni: Double, dhi: Double, sp: SunPos, tilt: Double, azSol: Double): Double {
    if (sp.elev <= 0) return dhi * (1 + cos(tilt * RAD)) / 2
    val cosAOI = sp.cosZ * cos(tilt * RAD) +
        sin(sp.zen * RAD) * sin(tilt * RAD) * cos((sp.az - azSol) * RAD)
    return dni * max(0.0, cosAOI) +
        dhi * (1 + cos(tilt * RAD)) / 2 +
        ghi * 0.2 * (1 - cos(tilt * RAD)) / 2
}

/*
 * Clear-sky model: Meinel attenuation with Laue's altitude correction (1970),
 * transmittance 0.80 and a diffuse fraction of 0.12.
 *
 * These constants are not from a handbook — they were calibrated so the modelled
 * value really is an upper bound. With the textbook setup (0.70, no altitude
 * correction) the actual forecast irradiance overshot the ceiling by up to 42%,
 * worst at altitude in clean air. Checked against eight places from Singapore to
 * Reykjavík and from sea level to La Paz (3,767 m): with these constants the
 * actual/ceiling ratio stays below 1 everywhere.
 */
const val CS_TAU = 0.80
const val CS_DHI = 0.12

fun clearSkyPOA(
    utcMs: Long,
    lat: Double,
    lon: Double,
    tilt: Double,
    azSol: Double,
    elevM: Double,
): Double {
    val sp = sunPos(utcMs, lat, lon)
    if (sp.elev <= 0.5) return 0.0
    val am = 1 / (sp.cosZ + 0.50572 * (96.07995 - sp.zen).pow(-1.6364))
    val hkm = max(0.0, elevM) / 1000.0
    val dni = 1353 * ((1 - 0.14 * hkm) * CS_TAU.pow(am.pow(0.678)) + 0.14 * hkm)
    val dhi = CS_DHI * dni * sp.cosZ
    val ghi = dni * sp.cosZ + dhi
    return poa(ghi, dni, dhi, sp, tilt, azSol)
}

data class OptimalSetup(val tilt: Int, val az: Int)

/**
 * The tilt with the highest annual clear-sky total for this latitude — computed,
 * not a rule of thumb. Panels face the equator: south in the northern
 * hemisphere, north in the southern.
 */
fun optimalSetup(lat: Double, lon: Double): OptimalSetup {
    val azComp = if (lat >= 0) 180 else 0
    val azS = azSolar(azComp.toDouble())

    // 2025-01-01T00:00Z, matching the reference year the web app sweeps.
    val yearStart = 1735689600000L
    val sun = ArrayList<SunPos>(2000)
    var d = 0
    while (d < 365) {
        for (h in 0 until 24) {
            val ms = yearStart + h * 3600000L + 1800000L + d * 86400000L
            val sp = sunPos(ms, lat, lon)
            if (sp.elev > 0.5) sun.add(sp)
        }
        d += 5
    }

    var best = 0
    var bestSum = -1.0
    for (t in 0..75) {
        var sum = 0.0
        for (sp in sun) {
            val am = 1 / (sp.cosZ + 0.50572 * (96.07995 - sp.zen).pow(-1.6364))
            val dni = 1353 * CS_TAU.pow(am.pow(0.678))
            val dhi = CS_DHI * dni * sp.cosZ
            val ghi = dni * sp.cosZ + dhi
            sum += poa(ghi, dni, dhi, sp, t.toDouble(), azS)
        }
        if (sum > bestSum) {
            bestSum = sum
            best = t
        }
    }
    return OptimalSetup(tilt = best, az = azComp)
}

/** DC power from plane-of-array irradiance, by the NOCT cell-temperature approach. */
fun dcPower(gti: Double, tAir: Double, kwp: Double, tempCoef: Double): Double {
    if (gti <= 0) return 0.0
    val tCell = tAir + gti * (45 - 20) / 800
    val f = 1 + (tempCoef / 100) * (tCell - 25)
    return max(0.0, (gti / 1000) * kwp * f)
}

/** Which of the eight compass points an azimuth rounds to. */
fun compassIndex(azComp: Double): Int = (Math.round(norm360(azComp) / 45).toInt()) % 8
