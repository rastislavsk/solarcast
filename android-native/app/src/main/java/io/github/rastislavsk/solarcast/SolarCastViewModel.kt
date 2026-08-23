package io.github.rastislavsk.solarcast

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.rastislavsk.solarcast.data.Config
import io.github.rastislavsk.solarcast.data.ConfigRepository
import io.github.rastislavsk.solarcast.data.OpenMeteo
import io.github.rastislavsk.solarcast.data.Place
import io.github.rastislavsk.solarcast.data.PvString
import io.github.rastislavsk.solarcast.data.RateLimitedException
import io.github.rastislavsk.solarcast.model.ForecastModel
import io.github.rastislavsk.solarcast.model.buildModel
import io.github.rastislavsk.solarcast.model.optimalSetup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import kotlin.math.roundToInt

/** Which screen the app is on. */
enum class Screen { SETUP, LOADING, ERROR, FORECAST, SETTINGS }

/** Which step of first-run setup. */
enum class SetupStep { LOCATION, ARRAY }

/** What the location search is doing right now. */
sealed interface SearchState {
    data object Idle : SearchState
    data object Searching : SearchState
    data class Results(val places: List<Place>) : SearchState
    data class Empty(val query: String) : SearchState
    data object Failed : SearchState
    data object TooShort : SearchState
}

data class UiState(
    val loaded: Boolean = false,
    val config: Config = Config(),
    val screen: Screen = Screen.SETUP,
    val step: SetupStep = SetupStep.LOCATION,
    val forecast: ForecastModel? = null,
    val selectedDay: Int = 0,
    val fetchedAt: Instant? = null,
    /** Set when the forecast failed; already translated by the caller. */
    val errorKind: ErrorKind? = null,
    val search: SearchState = SearchState.Idle,
    val busySuggesting: Boolean = false,
)

enum class ErrorKind { RATE_LIMITED, GENERIC }

class SolarCastViewModel(app: Application) : AndroidViewModel(app) {

    private val repo = ConfigRepository(app.applicationContext)

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var fetchJob: Job? = null

    init {
        viewModelScope.launch {
            val stored = repo.config.first()
            _state.value = _state.value.copy(loaded = true, config = stored)
            boot(stored)
        }
    }

    /** Decides where a launch lands: straight to the forecast, or into setup. */
    private fun boot(config: Config) {
        if (!config.ready) {
            _state.value = _state.value.copy(
                screen = Screen.SETUP,
                step = if (config.hasPlace) SetupStep.ARRAY else SetupStep.LOCATION,
            )
            return
        }
        refresh()
    }

    private fun persist(config: Config) {
        _state.value = _state.value.copy(config = config)
        viewModelScope.launch { repo.save(config) }
    }

    /* ------------------------------------------------------------ search */

    fun search(query: String, language: String) {
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _state.value = _state.value.copy(search = SearchState.TooShort)
            return
        }
        _state.value = _state.value.copy(search = SearchState.Searching)
        searchJob = viewModelScope.launch {
            try {
                val results = OpenMeteo.geocode(query, language)
                _state.value = _state.value.copy(
                    search = if (results.isEmpty()) SearchState.Empty(query.trim())
                    else SearchState.Results(results),
                )
            } catch (_: Exception) {
                _state.value = _state.value.copy(search = SearchState.Failed)
            }
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        _state.value = _state.value.copy(search = SearchState.Idle)
    }

    fun pickPlace(place: Place, thenGoToArray: Boolean) {
        val rounded = { v: Double -> (v * 100).roundToInt() / 100.0 }
        val next = _state.value.config.copy(
            lat = rounded(place.lat),
            lon = rounded(place.lon),
            place = place.name,
            placeSub = place.sub,
            tz = place.tz.ifBlank { "auto" },
            elev = if (place.elev.isFinite()) place.elev else 0.0,
        )
        persist(next)
        _state.value = _state.value.copy(search = SearchState.Idle)
        if (thenGoToArray) {
            _state.value = _state.value.copy(screen = Screen.SETUP, step = SetupStep.ARRAY)
        } else {
            refresh()
        }
    }

    /* ------------------------------------------------------------- setup */

    fun goToStep(step: SetupStep) {
        _state.value = _state.value.copy(screen = Screen.SETUP, step = step)
    }

    /**
     * Fills in a plausible system for someone who does not know their numbers:
     * the optimal tilt for this latitude and a 10 kWp array. Flagged as
     * suggested so the app keeps saying it is a placeholder until it is edited.
     */
    fun suggestArray() {
        val config = _state.value.config
        val lat = config.lat ?: return
        val lon = config.lon ?: return
        _state.value = _state.value.copy(busySuggesting = true)
        viewModelScope.launch {
            // The tilt sweep is a few hundred thousand sun positions; off the main thread.
            val optimal = withContext(Dispatchers.Default) { optimalSetup(lat, lon) }
            val next = config.copy(
                strings = listOf(
                    PvString(
                        panels = 20,
                        wp = 500.0,
                        tilt = optimal.tilt.toDouble(),
                        az = optimal.az.toDouble(),
                    ),
                ),
                acLimit = 10.0,
                suggested = true,
            )
            _state.value = _state.value.copy(busySuggesting = false)
            persist(next)
        }
    }

    fun startManualArray() {
        val config = _state.value.config
        if (config.strings.isEmpty()) {
            val lat = config.lat ?: 0.0
            persist(
                config.copy(
                    strings = listOf(PvString(panels = 20, wp = 450.0, tilt = 35.0, az = if (lat >= 0) 180.0 else 0.0)),
                    suggested = false,
                ),
            )
        }
    }

    fun addString() {
        val config = _state.value.config
        val last = config.strings.lastOrNull()
        val fresh = PvString(
            panels = last?.panels ?: 20,
            wp = last?.wp ?: 450.0,
            tilt = last?.tilt ?: 35.0,
            az = last?.az ?: 180.0,
        )
        persist(config.copy(strings = config.strings + fresh, suggested = false))
    }

    fun updateString(index: Int, string: PvString) {
        val config = _state.value.config
        if (index !in config.strings.indices) return
        val next = config.strings.toMutableList().also { it[index] = string }
        persist(config.copy(strings = next, suggested = false))
    }

    fun removeString(index: Int) {
        val config = _state.value.config
        if (index !in config.strings.indices) return
        val next = config.strings.toMutableList().also { it.removeAt(index) }
        persist(config.copy(strings = next, suggested = false))
    }

    fun updateGlobals(acLimit: Double?, eff: Double?, tempCoef: Double?) {
        var config = _state.value.config
        var touched = false
        acLimit?.takeIf { it.isFinite() && it > 0 && it != config.acLimit }?.let {
            config = config.copy(acLimit = it); touched = true
        }
        eff?.takeIf { it.isFinite() && it > 0 && it <= 1 && it != config.eff }?.let {
            config = config.copy(eff = it); touched = true
        }
        tempCoef?.takeIf { it.isFinite() && it != config.tempCoef }?.let {
            config = config.copy(tempCoef = it); touched = true
        }
        if (touched) persist(config.copy(suggested = false))
    }

    /** Applies the computed optimal tilt and bearing to every string. */
    fun applyOptimalTilt() {
        val config = _state.value.config
        val lat = config.lat ?: return
        val lon = config.lon ?: return
        viewModelScope.launch {
            val optimal = withContext(Dispatchers.Default) { optimalSetup(lat, lon) }
            persist(
                config.copy(
                    strings = config.strings.map {
                        it.copy(tilt = optimal.tilt.toDouble(), az = optimal.az.toDouble())
                    },
                    suggested = false,
                ),
            )
        }
    }

    fun setScheme(scheme: io.github.rastislavsk.solarcast.ui.theme.Scheme) =
        persist(_state.value.config.copy(scheme = scheme))

    fun setDark(dark: Boolean) = persist(_state.value.config.copy(dark = dark))

    /* --------------------------------------------------------- forecast */

    fun finishSetup() {
        if (!_state.value.config.ready) return
        refresh()
    }

    fun refresh() {
        val config = _state.value.config
        if (!config.ready) {
            _state.value = _state.value.copy(screen = Screen.SETUP)
            return
        }
        fetchJob?.cancel()
        _state.value = _state.value.copy(screen = Screen.LOADING, errorKind = null)
        fetchJob = viewModelScope.launch {
            try {
                val api = OpenMeteo.forecast(config)
                val model = withContext(Dispatchers.Default) { buildModel(api, config) }
                if (model.days.isEmpty()) throw IllegalStateException("no usable hourly data")

                // The API knows the elevation of these exact coordinates better
                // than the geocoder did; keep it for the clear-sky ceiling.
                val apiElev = api.optDouble("elevation", Double.NaN)
                if (apiElev.isFinite() && apiElev != config.elev) {
                    persist(_state.value.config.copy(elev = apiElev))
                }

                _state.value = _state.value.copy(
                    screen = Screen.FORECAST,
                    forecast = model,
                    selectedDay = 0,
                    fetchedAt = Instant.now(),
                    errorKind = null,
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    screen = Screen.ERROR,
                    errorKind = if (e is RateLimitedException) ErrorKind.RATE_LIMITED else ErrorKind.GENERIC,
                )
            }
        }
    }

    fun selectDay(index: Int) {
        _state.value = _state.value.copy(selectedDay = index)
    }

    fun openSettings() {
        _state.value = _state.value.copy(screen = Screen.SETTINGS)
    }

    fun closeSettings() {
        _state.value = _state.value.copy(
            screen = if (_state.value.forecast != null) Screen.FORECAST else Screen.SETUP,
        )
    }

    /** Saves the edited system and recomputes without a new download if possible. */
    fun applyAndRecalculate() {
        if (_state.value.config.ready) refresh() else _state.value = _state.value.copy(screen = Screen.SETUP)
    }

    fun eraseEverything() {
        fetchJob?.cancel()
        searchJob?.cancel()
        viewModelScope.launch {
            repo.eraseAll()
            // Give DataStore a beat to flush before the UI reads it back.
            delay(50)
            val fresh = Config()
            _state.value = UiState(loaded = true, config = fresh, screen = Screen.SETUP, step = SetupStep.LOCATION)
        }
    }
}
