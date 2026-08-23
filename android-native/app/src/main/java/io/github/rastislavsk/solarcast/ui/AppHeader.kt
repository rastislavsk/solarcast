package io.github.rastislavsk.solarcast.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.LocaleListCompat
import io.github.rastislavsk.solarcast.R
import io.github.rastislavsk.solarcast.ui.icons.GlobeIcon
import io.github.rastislavsk.solarcast.ui.icons.RefreshIcon
import io.github.rastislavsk.solarcast.ui.icons.SlidersIcon
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.SolarType

/** The four languages the app is written in, with the label each uses for itself. */
val SUPPORTED_LANGUAGES = listOf(
    "sk" to "SK",
    "en" to "EN",
    "de" to "DE",
    "zh" to "中文",
)

/**
 * Reads the language the app is currently running in.
 *
 * This is the per-app locale, not a private setting: the same value the system
 * shows under Settings > Apps > SolarCast > Language, so the in-app picker and
 * the system one are the same switch.
 */
@Composable
fun currentLanguageTag(): String {
    val applied = AppCompatDelegate.getApplicationLocales()
    val fromSystem = stringResource(R.string.t_dir)
    return if (applied.isEmpty) fromSystem else applied[0]?.language ?: fromSystem
}

fun setLanguage(tag: String) {
    AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(tag))
}

/** Globe plus the language code — the same control the page grew. */
@Composable
fun LanguagePicker(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    var open by remember { mutableStateOf(false) }
    val current = currentLanguageTag()
    val label = SUPPORTED_LANGUAGES.firstOrNull { it.first == current }?.second ?: "EN"

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .height(40.dp)
                .background(palette.surface, ControlShape)
                .border(BorderStroke(1.dp, palette.line), ControlShape)
                .clickable { open = true }
                .padding(start = 9.dp, end = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = rememberVectorPainter(GlobeIcon),
                contentDescription = stringResource(R.string.t_langSel),
                tint = palette.dim,
                modifier = Modifier.size(18.dp),
            )
            Text(label, style = SolarType.mono, color = palette.dim)
        }
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            SUPPORTED_LANGUAGES.forEach { (tag, name) ->
                DropdownMenuItem(
                    text = { Text(name, style = SolarType.mono) },
                    onClick = {
                        open = false
                        setLanguage(tag)
                    },
                )
            }
        }
    }
}

/**
 * The header. The refresh and settings controls only make sense once there is a
 * forecast to refresh, so they appear with it rather than sitting dead on the
 * setup screen.
 */
@Composable
fun AppHeader(
    place: String?,
    placeSub: String?,
    showTools: Boolean,
    onRefresh: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 18.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SolarMark(modifier = Modifier.size(26.dp))
                    Text(
                        "SolarCast",
                        style = SolarType.display,
                        color = palette.text,
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
                if (!place.isNullOrBlank()) {
                    Text(
                        place,
                        style = SolarType.subheading,
                        color = palette.text,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                Text(
                    if (!placeSub.isNullOrBlank()) placeSub else stringResource(R.string.t_tagline),
                    style = SolarType.mono.copy(fontSize = 11.5.sp),
                    color = palette.dim,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LanguagePicker()
            if (showTools) {
                IconButtonBox(
                    onClick = onSettings,
                    contentDescription = stringResource(R.string.t_yourArray),
                ) {
                    Icon(
                        painter = rememberVectorPainter(SlidersIcon),
                        contentDescription = null,
                        tint = palette.dim,
                        modifier = Modifier.size(21.dp),
                    )
                }
                IconButtonBox(
                    onClick = onRefresh,
                    contentDescription = stringResource(R.string.t_refresh),
                ) {
                    Icon(
                        painter = rememberVectorPainter(RefreshIcon),
                        contentDescription = null,
                        tint = palette.dim,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}
