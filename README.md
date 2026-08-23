# SolarCast
[HTML + Android Native app - test RR & Claude Code]

A seven-day photovoltaic output forecast for any location. Tell it where your
system is and how it is built, and it projects Open-Meteo's radiation forecast
onto your actual panel planes.

## Layout

| Path | What it is |
| --- | --- |
| `index.html` | The whole web app. One standalone file, no build step, no backend. Open it in a browser and it works. |
| `android-native/` | Native Android app: Kotlin and Jetpack Compose, no WebView, the solar model ported to Kotlin. See [android-native/README.md](android-native/README.md). |
| `PRIVACY.md` | Privacy policy. Play requires a public URL for it. |

`index.html` is the single source of truth for the Android app: a generator
lifts its translations and colour schemes into Android resources and Kotlin
rather than keeping a second, editable copy.

## Running the web version

Open `index.html` in any modern browser, or serve the directory over HTTP. There
is nothing to install and nothing to configure.

Everything you enter is stored in that browser's local storage and nowhere else.

## Data

Forecast and geocoding come from [Open-Meteo](https://open-meteo.com/). A
forecast is an estimate, not a guarantee.
