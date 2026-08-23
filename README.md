# SolarCast
[by Claude Code]

A seven-day photovoltaic output forecast for any location. Tell it where your
system is and how it is built, and it projects Open-Meteo's radiation forecast
onto your actual panel planes.

## Layout

| Path | What it is |
| --- | --- |
| `index.html` | The whole web app. One standalone file, no build step, no backend. Open it in a browser and it works. |
| `android/` | Native Android shell that packages `index.html` as an app for Google Play. See [android/README.md](android/README.md). |
| `PRIVACY.md` | Privacy policy. Play requires a public URL for it. |

`index.html` is the single source of truth. The Android build copies it into the
app's assets at build time rather than keeping a second copy, so the two can
never drift apart.

## Running the web version

Open `index.html` in any modern browser, or serve the directory over HTTP. There
is nothing to install and nothing to configure.

Everything you enter is stored in that browser's local storage and nowhere else.

## Data

Forecast and geocoding come from [Open-Meteo](https://open-meteo.com/). A
forecast is an estimate, not a guarantee.
