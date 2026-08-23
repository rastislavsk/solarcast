package io.github.rastislavsk.solarcast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rastislavsk.solarcast.R
import io.github.rastislavsk.solarcast.SearchState
import io.github.rastislavsk.solarcast.SetupStep
import io.github.rastislavsk.solarcast.UiState
import io.github.rastislavsk.solarcast.data.Place
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.SolarType

/** "1 · Location  ·  2 · Array", with the live step in full contrast. */
@Composable
private fun StepBar(step: SetupStep) {
    val palette = LocalPalette.current
    Row(modifier = Modifier.padding(bottom = 18.dp)) {
        Text(
            stringResource(R.string.t_stepLoc).uppercase(),
            style = SolarType.label,
            color = if (step == SetupStep.LOCATION) palette.text else palette.dim2,
        )
        Text(
            "  ·  ",
            style = SolarType.label,
            color = palette.dim2,
        )
        Text(
            stringResource(R.string.t_stepArr).uppercase(),
            style = SolarType.label,
            color = if (step == SetupStep.ARRAY) palette.text else palette.dim2,
        )
    }
}

@Composable
fun SetupScreen(
    state: UiState,
    onSearch: (String) -> Unit,
    onPick: (Place) -> Unit,
    onKnowSetup: () -> Unit,
    onSuggest: () -> Unit,
    onAddString: () -> Unit,
    onUpdateString: (Int, io.github.rastislavsk.solarcast.data.PvString) -> Unit,
    onRemoveString: (Int) -> Unit,
    onGlobals: (Double?, Double?, Double?) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SolarCard(modifier = modifier, padding = 20) {
        StepBar(state.step)
        when (state.step) {
            SetupStep.LOCATION -> LocationStep(state, onSearch, onPick)
            SetupStep.ARRAY -> ArrayStep(
                state = state,
                onKnowSetup = onKnowSetup,
                onSuggest = onSuggest,
                onAddString = onAddString,
                onUpdateString = onUpdateString,
                onRemoveString = onRemoveString,
                onGlobals = onGlobals,
                onBack = onBack,
                onFinish = onFinish,
            )
        }
    }
}

@Composable
private fun LocationStep(
    state: UiState,
    onSearch: (String) -> Unit,
    onPick: (Place) -> Unit,
) {
    val palette = LocalPalette.current
    var query by remember { mutableStateOf("") }

    Text(stringResource(R.string.t_setupLocT), style = SolarType.heading, color = palette.text)
    Text(
        stringResource(R.string.t_setupLocD),
        style = SolarType.body,
        color = palette.dim,
        modifier = Modifier.padding(top = 9.dp, bottom = 20.dp),
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = {
                Text(stringResource(R.string.t_searchPh), style = SolarType.body, color = palette.dim2)
            },
            textStyle = SolarType.body.copy(color = palette.text),
            shape = ControlShape,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.cool,
                unfocusedBorderColor = palette.line,
                focusedContainerColor = palette.bg,
                unfocusedContainerColor = palette.bg,
                cursorColor = palette.accent,
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
            ),
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
        )
        PrimaryButton(stringResource(R.string.t_search), onClick = { onSearch(query) })
    }

    SearchResults(state.search, onPick)
}

@Composable
private fun SearchResults(search: SearchState, onPick: (Place) -> Unit) {
    val palette = LocalPalette.current
    when (search) {
        SearchState.Idle -> Unit

        SearchState.Searching -> Row(
            modifier = Modifier.padding(top = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                color = palette.accent,
                strokeWidth = 2.dp,
                modifier = Modifier.height(16.dp).padding(end = 10.dp),
            )
            Text(stringResource(R.string.t_searching), style = SolarType.bodySmall, color = palette.dim)
        }

        SearchState.TooShort -> Hint(stringResource(R.string.t_tooShort))
        SearchState.Failed -> Hint(stringResource(R.string.t_searchFail), bad = true)
        is SearchState.Empty -> Hint(stringResource(R.string.t_noResults), bad = true)

        is SearchState.Results -> Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .background(palette.bg, CardShape)
                .border(BorderStroke(1.dp, palette.line), CardShape),
        ) {
            search.places.forEachIndexed { index, place ->
                if (index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(palette.lineSoft),
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(place) }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                ) {
                    Text(place.name, style = SolarType.body, color = palette.text)
                    if (place.sub.isNotBlank()) {
                        Text(
                            place.sub,
                            style = SolarType.mono.copy(fontSize = 11.5.sp),
                            color = palette.dim,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Hint(text: String, bad: Boolean = false) {
    val palette = LocalPalette.current
    Text(
        text,
        style = SolarType.bodySmall,
        color = if (bad) palette.dangerText else palette.dim,
        modifier = Modifier.padding(top = 14.dp),
    )
}

@Composable
private fun ArrayStep(
    state: UiState,
    onKnowSetup: () -> Unit,
    onSuggest: () -> Unit,
    onAddString: () -> Unit,
    onUpdateString: (Int, io.github.rastislavsk.solarcast.data.PvString) -> Unit,
    onRemoveString: (Int) -> Unit,
    onGlobals: (Double?, Double?, Double?) -> Unit,
    onBack: () -> Unit,
    onFinish: () -> Unit,
) {
    val palette = LocalPalette.current
    Text(stringResource(R.string.t_setupArrT), style = SolarType.heading, color = palette.text)
    Text(
        stringResource(R.string.t_setupArrD),
        style = SolarType.body,
        color = palette.dim,
        modifier = Modifier.padding(top = 9.dp, bottom = 20.dp),
    )

    if (state.config.strings.isEmpty()) {
        ButtonRow {
            PrimaryButton(
                stringResource(R.string.t_iKnow),
                onClick = onKnowSetup,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(
                stringResource(R.string.t_iDontKnow),
                onClick = onSuggest,
                modifier = Modifier.weight(1f),
            )
        }
        if (state.busySuggesting) {
            Row(
                modifier = Modifier.padding(top = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CircularProgressIndicator(
                    color = palette.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.height(16.dp).padding(end = 10.dp),
                )
                Text(stringResource(R.string.t_searching), style = SolarType.bodySmall, color = palette.dim)
            }
        } else {
            Text(
                stringResource(R.string.t_autoHint),
                style = SolarType.bodySmall,
                color = palette.dim2,
                modifier = Modifier.padding(top = 14.dp),
            )
        }
    } else {
        StringEditor(
            strings = state.config.strings,
            onUpdate = onUpdateString,
            onRemove = onRemoveString,
            onAdd = onAddString,
        )
        Spacer(Modifier.height(18.dp))
        TotalsRow(state.config.totalKwp, state.config.totalPanels)
        Spacer(Modifier.height(18.dp))
        GlobalsEditor(
            acLimit = state.config.acLimit,
            eff = state.config.eff,
            tempCoef = state.config.tempCoef,
            onChange = onGlobals,
        )
        Spacer(Modifier.height(20.dp))
        ButtonRow {
            PrimaryButton(
                stringResource(R.string.t_finish),
                onClick = onFinish,
                enabled = state.config.ready,
                modifier = Modifier.weight(1f),
            )
            SecondaryButton(stringResource(R.string.t_back), onClick = onBack)
        }
        if (!state.config.ready) {
            Text(
                stringResource(R.string.t_needStr),
                style = SolarType.bodySmall,
                color = palette.dangerText,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
    }
}
