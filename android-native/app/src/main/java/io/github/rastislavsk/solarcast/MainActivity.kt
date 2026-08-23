package io.github.rastislavsk.solarcast

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rastislavsk.solarcast.ui.AppHeader
import io.github.rastislavsk.solarcast.ui.ForecastScreen
import io.github.rastislavsk.solarcast.ui.PrimaryButton
import io.github.rastislavsk.solarcast.ui.SettingsScreen
import io.github.rastislavsk.solarcast.ui.SetupScreen
import io.github.rastislavsk.solarcast.ui.SolarCard
import io.github.rastislavsk.solarcast.ui.currentLanguageTag
import io.github.rastislavsk.solarcast.ui.fill
import io.github.rastislavsk.solarcast.ui.num
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.SolarCastTheme
import io.github.rastislavsk.solarcast.ui.theme.SolarType
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity() {

    private val viewModel: SolarCastViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Hold the splash until the saved system has been read, so the app never
        // flashes the setup screen at someone who already finished setup.
        splash.setKeepOnScreenCondition { !viewModel.state.value.loaded }

        setContent {
            val state by viewModel.state.collectAsStateWithLifecycle()

            SolarCastTheme(scheme = state.config.scheme, dark = state.config.dark) {
                val palette = LocalPalette.current

                // The page owns its palette, so the system bars follow the chosen
                // scheme rather than the system's idea of light and dark.
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !state.config.dark
                    isAppearanceLightNavigationBars = !state.config.dark
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(palette.bg),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .systemBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp)
                            .padding(top = 20.dp, bottom = 48.dp),
                    ) {
                        AppScreens(state, viewModel)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScreens(state: UiState, viewModel: SolarCastViewModel) {
    val palette = LocalPalette.current
    val language = currentLanguageTag()

    val showPlace = state.screen == Screen.FORECAST || state.screen == Screen.LOADING
    AppHeader(
        place = if (showPlace) state.config.place else null,
        placeSub = if (showPlace) state.config.placeSub else null,
        showTools = state.screen == Screen.FORECAST,
        onRefresh = viewModel::refresh,
        onSettings = viewModel::openSettings,
    )

    when (state.screen) {
        Screen.SETUP -> SetupScreen(
            state = state,
            onSearch = { viewModel.search(it, language) },
            onPick = { viewModel.pickPlace(it, thenGoToArray = !state.config.ready) },
            onKnowSetup = viewModel::startManualArray,
            onSuggest = viewModel::suggestArray,
            onAddString = viewModel::addString,
            onUpdateString = viewModel::updateString,
            onRemoveString = viewModel::removeString,
            onGlobals = viewModel::updateGlobals,
            onBack = { viewModel.goToStep(SetupStep.LOCATION) },
            onFinish = viewModel::finishSetup,
        )

        Screen.LOADING -> SolarCard(padding = 26) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                CircularProgressIndicator(color = palette.accent, strokeWidth = 2.dp)
                Spacer(Modifier.height(18.dp))
                Text(
                    stringResource(R.string.t_loadingT),
                    style = SolarType.heading,
                    color = palette.text,
                )
                Text(
                    stringResource(R.string.t_loadingD),
                    style = SolarType.bodySmall,
                    color = palette.dim,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }

        Screen.ERROR -> SolarCard(padding = 22) {
            Text(stringResource(R.string.t_errT), style = SolarType.heading, color = palette.text)
            Text(
                stringResource(
                    if (state.errorKind == ErrorKind.RATE_LIMITED) R.string.t_rateLim
                    else R.string.t_searchFail,
                ),
                style = SolarType.bodySmall,
                color = palette.dim,
                modifier = Modifier.padding(top = 10.dp, bottom = 18.dp),
            )
            PrimaryButton(stringResource(R.string.t_retry), onClick = viewModel::refresh)
        }

        Screen.FORECAST -> state.forecast?.let { model ->
            ForecastScreen(
                model = model,
                acLimit = state.config.acLimit,
                selectedDay = state.selectedDay,
                onSelectDay = viewModel::selectDay,
                suggested = state.config.suggested,
                suggestedNote = suggestedNote(state),
            )
        }

        Screen.SETTINGS -> SettingsScreen(
            state = state,
            onBack = viewModel::closeSettings,
            onChangeLocation = { viewModel.goToStep(SetupStep.LOCATION) },
            onAddString = viewModel::addString,
            onUpdateString = viewModel::updateString,
            onRemoveString = viewModel::removeString,
            onGlobals = viewModel::updateGlobals,
            onSuggestOptimal = viewModel::applyOptimalTilt,
            onScheme = viewModel::setScheme,
            onDark = viewModel::setDark,
            onApply = viewModel::applyAndRecalculate,
            onErase = viewModel::eraseEverything,
        )
    }
}

/** Spells out exactly what the app made up, so a placeholder never reads as data. */
@Composable
private fun suggestedNote(state: UiState): String? {
    val first = state.config.strings.firstOrNull() ?: return null
    return stringResource(R.string.t_suggD).fill(
        "n" to state.config.totalPanels,
        "w" to first.wp.roundToInt(),
        "t" to first.tilt.roundToInt(),
        "c" to io.github.rastislavsk.solarcast.ui.compassName(first.az),
        "a" to num(state.config.acLimit, 0),
    )
}
