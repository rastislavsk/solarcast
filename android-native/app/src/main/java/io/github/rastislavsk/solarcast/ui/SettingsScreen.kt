package io.github.rastislavsk.solarcast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rastislavsk.solarcast.R
import io.github.rastislavsk.solarcast.UiState
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import io.github.rastislavsk.solarcast.data.PvString
import io.github.rastislavsk.solarcast.ui.icons.BackIcon
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.Scheme
import io.github.rastislavsk.solarcast.ui.theme.SolarType

@Composable
fun SettingsScreen(
    state: UiState,
    onBack: () -> Unit,
    onChangeLocation: () -> Unit,
    onAddString: () -> Unit,
    onUpdateString: (Int, PvString) -> Unit,
    onRemoveString: (Int) -> Unit,
    onGlobals: (Double?, Double?, Double?) -> Unit,
    onSuggestOptimal: () -> Unit,
    onScheme: (Scheme) -> Unit,
    onDark: (Boolean) -> Unit,
    onApply: () -> Unit,
    onErase: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            IconButtonBox(onClick = onBack, contentDescription = stringResource(R.string.t_back)) {
                Icon(
                    painter = rememberVectorPainter(BackIcon),
                    contentDescription = null,
                    tint = palette.dim,
                    modifier = Modifier.size(20.dp),
                )
            }
            Text(stringResource(R.string.t_yourArray), style = SolarType.heading, color = palette.text)
        }

        SolarCard(padding = 18) {
            MonoLabel(stringResource(R.string.t_location), modifier = Modifier.padding(bottom = 10.dp))
            Text(
                state.config.place.ifBlank { "–" },
                style = SolarType.subheading,
                color = palette.text,
            )
            if (state.config.placeSub.isNotBlank()) {
                Text(
                    state.config.placeSub,
                    style = SolarType.mono,
                    color = palette.dim,
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            Spacer(Modifier.height(14.dp))
            SecondaryButton(stringResource(R.string.t_change), onClick = onChangeLocation)
        }

        Spacer(Modifier.height(24.dp))

        SolarCard(padding = 18) {
            MonoLabel(stringResource(R.string.t_setupArrT), modifier = Modifier.padding(bottom = 14.dp))
            StringEditor(
                strings = state.config.strings,
                onUpdate = onUpdateString,
                onRemove = onRemoveString,
                onAdd = onAddString,
            )
            Spacer(Modifier.height(16.dp))
            TotalsRow(state.config.totalKwp, state.config.totalPanels)
            Spacer(Modifier.height(16.dp))
            SecondaryButton(stringResource(R.string.t_suggestOpt), onClick = onSuggestOptimal)
            Spacer(Modifier.height(18.dp))
            GlobalsEditor(
                acLimit = state.config.acLimit,
                eff = state.config.eff,
                tempCoef = state.config.tempCoef,
                onChange = onGlobals,
            )
            Spacer(Modifier.height(20.dp))
            PrimaryButton(
                stringResource(R.string.t_applyRecalc),
                onClick = onApply,
                enabled = state.config.ready,
            )
        }

        Spacer(Modifier.height(24.dp))
        AppearanceCard(state, onScheme, onDark)
        Spacer(Modifier.height(24.dp))
        DangerZone(onErase)
        Spacer(Modifier.height(24.dp))
        AboutCard()
    }
}

@Composable
private fun AppearanceCard(state: UiState, onScheme: (Scheme) -> Unit, onDark: (Boolean) -> Unit) {
    SolarCard(padding = 18) {
        MonoLabel(stringResource(R.string.t_appearance), modifier = Modifier.padding(bottom = 14.dp))

        MonoLabel(stringResource(R.string.t_colorScheme), modifier = Modifier.padding(bottom = 8.dp))
        SegmentedRow(
            options = listOf(
                Scheme.DUSK to stringResource(R.string.t_schemeDusk),
                Scheme.SLATE to stringResource(R.string.t_schemeSlate),
                Scheme.TERRA to stringResource(R.string.t_schemeTerra),
            ),
            selected = state.config.scheme,
            onSelect = onScheme,
        )

        Spacer(Modifier.height(16.dp))
        MonoLabel(stringResource(R.string.t_modeLbl), modifier = Modifier.padding(bottom = 8.dp))
        SegmentedRow(
            options = listOf(
                true to stringResource(R.string.t_modeDark),
                false to stringResource(R.string.t_modeLight),
            ),
            selected = state.config.dark,
            onSelect = onDark,
        )
    }
}

@Composable
private fun DangerZone(onErase: () -> Unit) {
    val palette = LocalPalette.current
    // Two steps on purpose: erasing is not undoable, so the warning appears
    // before the button that does it, not after.
    var armed by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(palette.dangerBg, CardShape)
            .border(BorderStroke(1.dp, palette.dangerLine), CardShape)
            .padding(18.dp),
    ) {
        MonoLabel(stringResource(R.string.t_dangerT), color = palette.dangerText)
        Text(
            stringResource(R.string.t_dangerD),
            style = SolarType.bodySmall,
            color = palette.dangerText,
            modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
        )
        if (!armed) {
            SecondaryButton(
                stringResource(R.string.t_resetAll),
                onClick = { armed = true },
                tint = palette.dangerBtn,
            )
        } else {
            Text(
                stringResource(R.string.t_resetWarnT),
                style = SolarType.subheading,
                color = palette.dangerText,
            )
            Text(
                stringResource(R.string.t_resetWarnD),
                style = SolarType.bodySmall,
                color = palette.dangerText,
                modifier = Modifier.padding(top = 8.dp, bottom = 14.dp),
            )
            ButtonRow {
                SecondaryButton(
                    stringResource(R.string.t_resetGo),
                    onClick = onErase,
                    tint = palette.dangerBtn,
                )
                SecondaryButton(stringResource(R.string.t_cancel), onClick = { armed = false })
            }
        }
    }
}

@Composable
private fun AboutCard() {
    val palette = LocalPalette.current
    SolarCard(padding = 18) {
        MonoLabel(stringResource(R.string.t_aboutData), modifier = Modifier.padding(bottom = 12.dp))
        Row(modifier = Modifier.padding(bottom = 8.dp)) {
            Text(
                stringResource(R.string.t_tagModel).uppercase(),
                style = SolarType.label,
                color = palette.accent,
            )
        }
        Text(stringResource(R.string.t_foot1), style = SolarType.bodySmall, color = palette.dim)
        Text(
            stringResource(R.string.t_foot2),
            style = SolarType.bodySmall,
            color = palette.dim,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            "${stringResource(R.string.t_foot3)} Open-Meteo",
            style = SolarType.bodySmall,
            color = palette.dim,
            modifier = Modifier.padding(top = 10.dp),
        )
    }
}
