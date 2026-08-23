package io.github.rastislavsk.solarcast.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** One hit from the geocoder. */
data class Place(
    val name: String,
    val sub: String,
    val lat: Double,
    val lon: Double,
    val tz: String,
    val elev: Double,
)

/** Raised when the caller should show the rate-limit message rather than a generic failure. */
class RateLimitedException : Exception()

/**
 * Open-Meteo, over plain HttpURLConnection.
 *
 * The forecast is one request no matter how many strings there are: the app
 * downloads the radiation components and projects them onto each plane itself.
 */
object OpenMeteo {

    private const val TIMEOUT_MS = 15_000

    suspend fun geocode(query: String, language: String): List<Place> {
        val q = query.trim()
        if (q.length < 2) return emptyList()
        val url = "https://geocoding-api.open-meteo.com/v1/search" +
            "?name=" + URLEncoder.encode(q, "UTF-8") +
            "&count=10&language=" + URLEncoder.encode(language, "UTF-8") +
            "&format=json"

        val json = JSONObject(get(url))
        val results = json.optJSONArray("results") ?: return emptyList()
        return (0 until results.length()).mapNotNull { i ->
            val r = results.optJSONObject(i) ?: return@mapNotNull null
            val sub = listOfNotNull(
                r.optString("admin1").takeIf { it.isNotBlank() },
                r.optString("country").takeIf { it.isNotBlank() },
            ).joinToString(" · ")
            Place(
                name = r.optString("name"),
                sub = sub,
                lat = r.optDouble("latitude"),
                lon = r.optDouble("longitude"),
                tz = r.optString("timezone").ifBlank { "auto" },
                elev = r.optDouble("elevation", 0.0).let { if (it.isFinite()) it else 0.0 },
            )
        }
    }

    suspend fun forecast(config: Config): JSONObject {
        val url = "https://api.open-meteo.com/v1/forecast" +
            "?latitude=" + config.lat +
            "&longitude=" + config.lon +
            "&hourly=shortwave_radiation,direct_normal_irradiance,diffuse_radiation," +
            "temperature_2m,cloud_cover" +
            "&daily=sunrise,sunset" +
            "&forecast_days=8&timezone=" + URLEncoder.encode(config.tz.ifBlank { "auto" }, "UTF-8")
        return JSONObject(get(url))
    }

    /**
     * A GET with the page's retry behaviour: three attempts, backing off, and a
     * distinct signal for 429 so the UI can say "too many requests" rather than
     * "could not load".
     */
    private suspend fun get(url: String): String = withContext(Dispatchers.IO) {
        var lastFailure: Exception? = null
        for (attempt in 1..3) {
            try {
                val connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/json")
                }
                try {
                    val code = connection.responseCode
                    if (code == 429) {
                        if (attempt < 3) {
                            delay(1500L * attempt)
                            continue
                        }
                        throw RateLimitedException()
                    }
                    if (code !in 200..299) {
                        val body = connection.errorStream?.bufferedReader()?.use(BufferedReader::readText)
                        val reason = body?.let {
                            runCatching { JSONObject(it).optString("reason") }.getOrNull()
                        }?.takeIf { it.isNotBlank() }
                        throw Exception(reason ?: "HTTP $code")
                    }
                    return@withContext connection.inputStream.bufferedReader().use(BufferedReader::readText)
                } finally {
                    connection.disconnect()
                }
            } catch (e: RateLimitedException) {
                throw e
            } catch (e: Exception) {
                lastFailure = e
                if (attempt < 3) delay(1200L * attempt) else throw e
            }
        }
        throw lastFailure ?: Exception("request failed")
    }
}
