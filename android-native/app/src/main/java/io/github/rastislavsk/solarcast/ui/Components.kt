package io.github.rastislavsk.solarcast.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.rastislavsk.solarcast.ui.theme.LocalPalette
import io.github.rastislavsk.solarcast.ui.theme.SolarType

val CardShape = RoundedCornerShape(14.dp)
val ControlShape = RoundedCornerShape(9.dp)

@Composable
fun SolarCard(
    modifier: Modifier = Modifier,
    padding: Int = 16,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.surface, CardShape)
            .border(BorderStroke(1.dp, palette.line), CardShape)
            .padding(padding.dp),
        content = content,
    )
}

/** The wide-tracked mono label the page puts above every section and figure. */
@Composable
fun MonoLabel(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    val palette = LocalPalette.current
    Text(
        text = text.uppercase(),
        style = SolarType.label,
        color = color ?: palette.dim,
        modifier = modifier,
    )
}

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier, description: String? = null) {
    val palette = LocalPalette.current
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 14.dp)) {
        Text(title, style = SolarType.heading, color = palette.text)
        if (description != null) {
            Text(
                description,
                style = SolarType.bodySmall,
                color = palette.dim,
                modifier = Modifier.padding(top = 6.dp),
            )
        }
    }
}

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier
            .background(if (enabled) palette.text else palette.surface2, ControlShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SolarType.monoStrong, color = if (enabled) palette.bg else palette.dim2)
    }
}

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier
            .background(palette.surface, ControlShape)
            .border(BorderStroke(1.dp, tint ?: palette.line), ControlShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = SolarType.monoStrong, color = tint ?: palette.dim)
    }
}

/** A 40dp square icon target, matching the header controls on the page. */
@Composable
fun IconButtonBox(
    onClick: () -> Unit,
    contentDescription: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalPalette.current
    Box(
        modifier = modifier
            .size(40.dp)
            .background(palette.surface, ControlShape)
            .border(BorderStroke(1.dp, palette.line), ControlShape)
            .clickable(onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
        content = { content() },
    )
}

/** A labelled numeric field with the hint line the page shows underneath. */
@Composable
fun NumberField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    hint: String? = null,
    decimal: Boolean = false,
) {
    val palette = LocalPalette.current
    Column(modifier = modifier) {
        MonoLabel(label, modifier = Modifier.padding(bottom = 6.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = SolarType.mono.copy(color = palette.text),
            keyboardOptions = KeyboardOptions(
                keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number,
            ),
            shape = ControlShape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = palette.cool,
                unfocusedBorderColor = palette.line,
                focusedContainerColor = palette.bg,
                unfocusedContainerColor = palette.bg,
                cursorColor = palette.accent,
                focusedTextColor = palette.text,
                unfocusedTextColor = palette.text,
            ),
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        )
        if (hint != null) {
            Text(
                hint,
                style = SolarType.bodySmall.copy(fontSize = 11.5.sp),
                color = palette.dim2,
                modifier = Modifier.padding(top = 5.dp),
            )
        }
    }
}

/** A row of mutually exclusive chips — the page's segmented control. */
@Composable
fun <T> SegmentedRow(
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (value, label) ->
            val active = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(if (active) palette.text else palette.surface, ControlShape)
                    .border(BorderStroke(1.dp, if (active) palette.text else palette.line), ControlShape)
                    .clickable { onSelect(value) }
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = SolarType.mono, color = if (active) palette.bg else palette.dim)
            }
        }
    }
}

/** The amber "this is only a placeholder" panel. */
@Composable
fun Notice(title: String, body: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(palette.noticeBg, CardShape)
            .border(BorderStroke(1.dp, palette.noticeLine), CardShape)
            .padding(15.dp),
    ) {
        Text(title, style = SolarType.subheading, color = palette.noticeText)
        Text(
            body,
            style = SolarType.bodySmall,
            color = palette.noticeText,
            modifier = Modifier.padding(top = 7.dp),
        )
    }
}

/** Key/value line used across the summary and the legends. */
@Composable
fun LegendSwatch(color: Color, label: String, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(11.dp)
                .background(color, RoundedCornerShape(2.dp)),
        )
        Text(
            label,
            style = SolarType.mono.copy(fontSize = 11.5.sp),
            color = palette.dim,
            modifier = Modifier.padding(start = 7.dp),
        )
    }
}

/** Spacer-free helper so rows of buttons read the same everywhere. */
@Composable
fun ButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}
