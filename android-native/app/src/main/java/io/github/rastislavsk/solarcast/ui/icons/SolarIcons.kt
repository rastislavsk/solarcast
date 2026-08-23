package io.github.rastislavsk.solarcast.ui.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/*
 * Every icon the app uses, drawn from the page's own SVG geometry. Lifting them
 * rather than pulling in a Material icon pack keeps the two versions showing the
 * same glyphs, in one stroke weight, at the cost of nothing.
 */

private fun strokeIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { d ->
            addPath(
                pathData = addPathNodes(d),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.7f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
        }
    }.build()

/** Globe: what the language control is marked with, here and on the page. */
val GlobeIcon: ImageVector by lazy {
    strokeIcon(
        "Globe",
        "M12,2.8 A9.2,9.2 0 1 1 11.99,2.8 Z",
        "M3.1,9.1 h17.8",
        "M3.1,14.9 h17.8",
        "M12,2.8 C14.5,5.5 15.8,8.6 15.8,12 S14.5,18.5 12,21.2 C9.5,18.5 8.2,15.4 8.2,12 S9.5,5.5 12,2.8 Z",
    )
}

/** The sliders mark used for the settings screen, matching the page's header. */
val SlidersIcon: ImageVector by lazy {
    strokeIcon(
        "Sliders",
        "M2.2,5.6 h4.4 M12.4,5.6 h9.4 M2.2,12 h11.2 M19.2,12 h2.6 M2.2,18.4 h2.6 M10.6,18.4 h11.2",
        "M9.5,2.7 A2.9,2.9 0 1 1 9.49,2.7 Z",
        "M16.3,9.1 A2.9,2.9 0 1 1 16.29,9.1 Z",
        "M7.6,15.5 A2.9,2.9 0 1 1 7.59,15.5 Z",
    )
}

/** Refresh, straight from the page's header button. */
val RefreshIcon: ImageVector by lazy {
    strokeIcon(
        "Refresh",
        "M22,12 A10,10 0 1 1 18.9,4.8",
        "M22.2,2.6 v6.2 h-6.2",
    )
}

/** Back arrow for the settings screen. */
val BackIcon: ImageVector by lazy {
    strokeIcon(
        "Back",
        "M20,12 H4.5",
        "M11,4.5 L3.5,12 L11,19.5",
    )
}

/** Remove a string. */
val TrashIcon: ImageVector by lazy {
    strokeIcon(
        "Trash",
        "M4,6.5 h16",
        "M9.5,6.5 V4.2 h5 V6.5",
        "M6.2,6.5 L7.2,20 h9.6 l1,-13.5",
        "M10.2,10 v6.5 M13.8,10 v6.5",
    )
}
