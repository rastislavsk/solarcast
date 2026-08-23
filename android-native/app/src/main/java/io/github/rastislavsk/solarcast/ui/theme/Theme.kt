package io.github.rastislavsk.solarcast.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import io.github.rastislavsk.solarcast.R

/*
 * Both faces are bundled rather than fetched. The page pulls them from Google
 * Fonts, which costs a request and shows fallback metrics until it lands; here
 * they ship with the app and are correct from the first frame. Chinese has no
 * bundled face on purpose — Android's system fallback already covers CJK, and a
 * Noto Sans SC would add megabytes to do what the platform does.
 */
val Archivo = FontFamily(
    Font(R.font.archivo_400, FontWeight.Normal),
    Font(R.font.archivo_600, FontWeight.SemiBold),
    Font(R.font.archivo_700, FontWeight.Bold),
)

val JetBrainsMono = FontFamily(
    Font(R.font.jetbrainsmono_400, FontWeight.Normal),
    Font(R.font.jetbrainsmono_500, FontWeight.Medium),
)

/**
 * The mono face carries every number and label in this app, the way it does on
 * the page: figures line up in columns and small-caps labels read as labels.
 */
object SolarType {
    val display = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = (-0.6).sp,
    )
    val heading = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Bold,
        fontSize = 19.sp,
        letterSpacing = (-0.3).sp,
    )
    val subheading = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
    )
    val body = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 14.5.sp,
        lineHeight = 21.sp,
    )
    val bodySmall = TextStyle(
        fontFamily = Archivo,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 19.sp,
    )
    /** Small-caps-ish section label: mono, wide tracking, upper case at the call site. */
    val label = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 1.4.sp,
    )
    val mono = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Normal,
        fontSize = 12.5.sp,
    )
    val monoStrong = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
    )
    /** The one big number on the summary card. */
    val figure = TextStyle(
        fontFamily = JetBrainsMono,
        fontWeight = FontWeight.Medium,
        fontSize = 38.sp,
        letterSpacing = (-1).sp,
    )
}

val LocalPalette = staticCompositionLocalOf { Palettes.of(Scheme.TERRA, dark = true) }

@Composable
fun SolarCastTheme(
    scheme: Scheme,
    dark: Boolean,
    content: @Composable () -> Unit,
) {
    val palette = Palettes.of(scheme, dark)

    // Material3 is used for a handful of controls (ripples, text fields, the
    // switch), so its scheme is mapped onto the app's palette rather than left
    // to the purple default.
    val colors = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.insideLabel,
            secondary = palette.cool,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.dim,
            outline = palette.line,
            error = palette.warn,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.insideLabel,
            secondary = palette.cool,
            background = palette.bg,
            onBackground = palette.text,
            surface = palette.surface,
            onSurface = palette.text,
            surfaceVariant = palette.surface2,
            onSurfaceVariant = palette.dim,
            outline = palette.line,
            error = palette.warn,
        )
    }

    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = colors,
            typography = Typography(
                bodyLarge = SolarType.body,
                bodyMedium = SolarType.bodySmall,
                labelLarge = SolarType.monoStrong,
            ),
            content = content,
        )
    }
}
