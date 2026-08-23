# SolarCast for Android — native

SolarCast rebuilt as a real Android app: Kotlin, Jetpack Compose, no WebView.
Every screen, every chart and the whole solar model are native code.

This is a **second, independent Android app** in this repository. The other one,
[`../android`](../android), packages `index.html` in a WebView. They install side
by side (different application ids) so the two can be compared; if this one
replaces it, delete `android/`.

## What is native here

| Part | How |
| --- | --- |
| Solar model | `model/Solar.kt` — NOAA sun position, isotropic-sky POA transposition, the calibrated Meinel clear-sky ceiling, the annual tilt sweep, NOCT cell temperature. Ported line for line from the page. |
| Forecast assembly | `model/ForecastModel.kt` — one Open-Meteo request, projected onto every string, bucketed into days. |
| Charts | `ui/charts/Charts.kt` — compass dial, week heat map, daily bars with the clear-sky ceiling, and the tappable day-profile curve, all drawn on a Compose `Canvas`. |
| Storage | `data/Config.kt` — DataStore, on this device only. Replaces the page's `localStorage`. |
| Networking | `data/OpenMeteo.kt` — `HttpURLConnection` and `org.json`, both already in the platform. No HTTP or JSON library is pulled in. |
| Language | The in-app picker calls `AppCompatDelegate.setApplicationLocales`, so it is the *same switch* as Settings → Apps → SolarCast → Language rather than a private setting beside it. |

## The numbers have to match

The point of a rewrite is that it produces the same answers. The physics
constants, the order of operations, and the two subtleties that are easy to get
wrong are kept exactly as the page has them:

- Open-Meteo returns local wall-clock stamps with no offset, and each value is
  the average over the hour *ending* at its stamp. So the stamp is parsed as if
  it were UTC, shifted back by the reported offset, and the sun position is taken
  at the midpoint of the hour before it.
- The clear-sky constants (τ = 0.80, diffuse fraction 0.12, Laue altitude
  correction) are calibrated, not textbook. With the textbook 0.70 the "ceiling"
  is not a ceiling.

Checked against the web app on the same location and array: Nitra, 20 × 500 W at
43° facing south, 10 kW inverter — both give a computed optimal tilt of 43° and
the same daily totals.

## Shared with the page, not copied from it

`tools/generate-from-web.mjs` lifts the four translations (112 strings each), the
calendar words and the three colour schemes (6 palettes × 30 colours) straight
out of `../index.html` and writes them as Android resources and Kotlin:

```bash
cd android-native && node tools/generate-from-web.mjs
```

`app/src/main/res/values*/strings_i18n.xml` and `ui/theme/Palettes.kt` are
generated — edit the page and re-run, never edit them directly. Three strings are
deliberately reworded for a phone (data lives "on this device", not "in this
browser"); that list is at the top of the generator and is the complete set of
divergences.

## Requirements

- JDK 17+ (Android Studio's bundled JBR works)
- Android SDK Platform 37 (the build downloads it if missing)

## Build

```bash
cd android-native && ./gradlew :app:assembleDebug
```

```bash
cd android-native && ./gradlew :app:bundleRelease
```

The release bundle is ~4.2 MB, most of it the two bundled typefaces. Without
`keystore.properties` it is unsigned but still builds; see
[`../android/README.md`](../android/README.md) for the signing setup, which is
identical.

## Typography

Archivo and JetBrains Mono ship with the app as static TTFs rather than being
fetched from Google Fonts, so they are correct on the first frame and offline.
Chinese has no bundled face on purpose: Android's system fallback already covers
CJK, and a Noto Sans SC would add megabytes to do what the platform does.

## Play requirements this module satisfies

`targetSdk 36` compiled against 37 · App Bundle with R8 and resource shrinking ·
`INTERNET` as the only permission · edge-to-edge · predictive back · adaptive
icon with a monochrome layer · generated locale config for the four languages ·
backup and device-transfer rules covering the DataStore · cleartext blocked ·
line numbers kept for deobfuscated crash reports.

`./gradlew :app:lintRelease` reports **0 errors**. Three warnings survive, all
deliberate:

- `UnusedAttribute` on `enableOnBackInvokedCallback` — the attribute only does
  anything from API 33, and `minSdk` is 26.
- `NewerVersionAvailable` on the Compose compiler plugin — it must track the
  Kotlin version AGP is built on, not the newest release.
- `ObsoleteSdkInt` on `mipmap-anydpi-v26` — the AGP resource merger does not pick
  the folder up without the qualifier, and the build fails without it.

## Not carried over

The page's hover tooltips became a tap-to-read-out row under the day curve.
Everything else in the web app is here.
