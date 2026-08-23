package io.github.rastislavsk.solarcast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rastislavsk.solarcast.R
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import io.github.rastislavsk.solarcast.data.PvString
import io.github.rastislavsk.solarcast.ui.icons.TrashIcon
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.SolarType
import io.github.rastislavsk.solarcast.ui.theme.azimuthColor
import kotlin.math.roundToInt

/**
 * Parses what the user typed without fighting them: a half-typed number leaves
 * the stored value alone rather than snapping to zero.
 */
private fun String.toDoubleOrKeep(current: Double): Double =
    replace(',', '.').trim().toDoubleOrNull() ?: current

@Composable
fun StringEditor(
    strings: List<PvString>,
    onUpdate: (Int, PvString) -> Unit,
    onRemove: (Int) -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(modifier = modifier.fillMaxWidth()) {
        strings.forEachIndexed { index, string ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .background(palette.bg, CardShape)
                    .border(BorderStroke(1.dp, palette.line), CardShape)
                    .padding(14.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // The same hue this string will carry in the day chart.
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .background(palette.azimuthColor(string.az), ControlShape),
                        )
                        MonoLabel(
                            "${stringResource(R.string.t_stringN)} ${index + 1}",
                            modifier = Modifier.padding(start = 8.dp),
                            color = palette.text,
                        )
                        Text(
                            "  ${compassName(string.az)} · ${num(string.kwp, 2)} ${stringResource(R.string.t_kwp)}",
                            style = SolarType.mono,
                            color = palette.dim,
                        )
                    }
                    if (strings.size > 1) {
                        IconButtonBox(
                            onClick = { onRemove(index) },
                            contentDescription = stringResource(R.string.t_removeStr),
                        ) {
                            Icon(
                                painter = rememberVectorPainter(TrashIcon),
                                contentDescription = null,
                                tint = palette.dim2,
                                modifier = Modifier.size(19.dp),
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NumberField(
                        label = stringResource(R.string.t_panels),
                        value = string.panels.toString(),
                        onValueChange = { raw ->
                            val n = raw.trim().toIntOrNull() ?: return@NumberField
                            onUpdate(index, string.copy(panels = n))
                        },
                        modifier = Modifier.weight(1f),
                    )
                    NumberField(
                        label = stringResource(R.string.t_wp),
                        value = string.wp.trimZeros(),
                        onValueChange = { raw ->
                            onUpdate(index, string.copy(wp = raw.toDoubleOrKeep(string.wp)))
                        },
                        modifier = Modifier.weight(1f),
                        decimal = true,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    NumberField(
                        label = stringResource(R.string.t_tilt),
                        value = string.tilt.trimZeros(),
                        onValueChange = { raw ->
                            onUpdate(index, string.copy(tilt = raw.toDoubleOrKeep(string.tilt)))
                        },
                        modifier = Modifier.weight(1f),
                        decimal = true,
                    )
                    NumberField(
                        label = stringResource(R.string.t_azimuth),
                        value = string.az.trimZeros(),
                        onValueChange = { raw ->
                            onUpdate(index, string.copy(az = raw.toDoubleOrKeep(string.az)))
                        },
                        modifier = Modifier.weight(1f),
                        hint = stringResource(R.string.t_azHint),
                        decimal = true,
                    )
                }
            }
        }
        SecondaryButton(stringResource(R.string.t_addString), onClick = onAdd)
    }
}

/** 450.0 reads as "450"; 22.5 stays "22.5". */
private fun Double.trimZeros(): String =
    if (this == this.roundToInt().toDouble()) this.roundToInt().toString() else this.toString()

@Composable
fun TotalsRow(totalKwp: Double, totalPanels: Int, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    // Label above value in two equal halves: the labels are long in German and
    // Slovak, and side by side on one line they collide.
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface2, CardShape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            MonoLabel(stringResource(R.string.t_totalKwp))
            Text(
                num(totalKwp, 2),
                style = SolarType.monoStrong,
                color = palette.text,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            MonoLabel(stringResource(R.string.t_totalPanels))
            Text(
                totalPanels.toString(),
                style = SolarType.monoStrong,
                color = palette.text,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
fun GlobalsEditor(
    acLimit: Double,
    eff: Double,
    tempCoef: Double,
    onChange: (Double?, Double?, Double?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        NumberField(
            label = stringResource(R.string.t_inverter),
            value = acLimit.toString(),
            onValueChange = { onChange(it.replace(',', '.').toDoubleOrNull(), null, null) },
            hint = stringResource(R.string.t_inverterH),
            decimal = true,
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            NumberField(
                label = stringResource(R.string.t_sysEff),
                value = eff.toString(),
                onValueChange = { onChange(null, it.replace(',', '.').toDoubleOrNull(), null) },
                hint = stringResource(R.string.t_sysEffH),
                decimal = true,
                modifier = Modifier.weight(1f),
            )
            NumberField(
                label = stringResource(R.string.t_tempCoef),
                value = tempCoef.toString(),
                onValueChange = { onChange(null, null, it.replace(',', '.').toDoubleOrNull()) },
                hint = stringResource(R.string.t_tempCoefH),
                decimal = true,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
