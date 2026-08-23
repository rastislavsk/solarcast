# SolarCast for Android

A native shell around the standalone `index.html` at the repository root, packaged
as an Android App Bundle for Google Play.

The page is **not** forked into this module. `index.html` stays the single source
of truth; the `bundleWebApp` Gradle task copies it into `assets/www/` on every
build, so the Android app can never drift from the web version.

## How it works

The page is served out of the APK by `WebViewAssetLoader` over
`https://appassets.androidplatform.net/assets/www/index.html` rather than
`file://`. That matters:

- it is a normal secure origin, so `localStorage` (where the location, strings
  and inverter settings live) behaves exactly as it does in a browser and
  survives app updates;
- the Open-Meteo requests are ordinary CORS requests instead of opaque
  `file://` ones;
- no `allowFileAccess` is needed, so the WebView cannot read the filesystem.

The shell adds what a browser tab gives the page for free and an activity does not:

| Concern | Handling |
| --- | --- |
| Theme | The page reports its resolved background and light/dark mode over a `WebMessageListener` bound to our own origin; the shell paints the window behind the page and flips the status/navigation bar icons to match. |
| Edge to edge | Mandatory from API 35. The shell applies system-bar, cutout and IME insets as padding, since the page knows nothing about them. |
| Back | An `OnBackPressedCallback` that is enabled only while the WebView has history, so predictive back keeps showing the correct preview. |
| External links | The Open-Meteo docs link opens in a Custom Tab, not inside the app. |
| Renderer crash | `onRenderProcessGone` recreates the activity instead of letting the renderer take the process down. |

There is no `@JavascriptInterface` bridge anywhere — `WebMessageListener` is the
supported replacement and exposes no reflection surface to the page.

## Requirements

- JDK 17 or newer (Android Studio's bundled JBR works: `…/Android Studio/jbr`)
- Android SDK Platform 37 (the build downloads it if it is missing)
- Gradle wrapper included; no separate Gradle install needed

## Build

```bash
cd android && ./gradlew :app:assembleDebug
```

```bash
cd android && ./gradlew :app:bundleRelease
```

The release bundle lands in `app/build/outputs/bundle/release/app-release.aab`
(~1.6 MB) with R8 and resource shrinking on. `lintVitalRelease` runs as part of
it and fails the build on fatal issues.

Without `keystore.properties` the release build still succeeds and produces an
**unsigned** bundle, so a fresh clone and CI are never blocked on secrets.

## Signing for Play

Create the upload key once and keep it forever — Play ties the app to it:

```bash
keytool -genkeypair -v -keystore upload-key.jks -alias solarcast -keyalg RSA -keysize 4096 -validity 10000
```

Then copy `keystore.properties.example` to `android/keystore.properties` and fill
it in. Both the keystore and that file are git-ignored; back them up somewhere
that is not this repository. Enrol in Play App Signing so a lost upload key can
be reset.

## Play requirements this module already satisfies

- **Target API level** — `targetSdk = 36`, the level Play requires for new apps
  and updates from 31 August 2026. Compiled against 37.
- **App Bundle** — `bundleRelease` produces an `.aab`; Play has not accepted APKs
  for new apps since 2021.
- **Permissions** — `INTERNET` only. No location, no storage, no ad ID, so no
  sensitive-permission declaration form.
- **64-bit / 16 KB page size** — no native code at all, so both are trivially met.
- **Edge-to-edge** — enforced from API 35; handled in `MainActivity`.
- **Predictive back** — `enableOnBackInvokedCallback="true"` plus a correctly
  scoped `OnBackPressedCallback`.
- **Adaptive icon with a monochrome layer** — themed icons on Android 13+.
- **Per-app language** — `generateLocaleConfig` emits the locale config from the
  `values-sk/de/zh` folders, so SolarCast appears under Settings → Languages.
- **Backup and device transfer rules** — declared for both the Android 11 and the
  Android 12+ formats; the saved system is carried over, caches are not.
- **Cleartext blocked** — network security config denies HTTP on every API level.
- **Deobfuscated crash reports** — line numbers kept for Play Console.
- **Data safety and privacy policy** — see [`store/listing.md`](store/listing.md)
  for the answers to give in Play Console and [`../PRIVACY.md`](../PRIVACY.md)
  for the policy to host and link.

## Store listing assets

`store/icon-512.png` and `store/feature-graphic-1024x500.png` are generated from
the app's own glyph and palette. Play additionally requires **at least two phone
screenshots**; take them from a release build on a device or emulator — the
in-app forecast screen and the day-profile chart make the strongest pair.

## Lint

`./gradlew :app:lintRelease` reports **0 errors**. Four warnings survive, all of
them deliberate — do not "fix" them blind:

- `MissingOnRenderProcessGone` (twice) is a **false positive**. The override is
  right there in `MainActivity`; the check just does not see it through the
  anonymous `WebViewClientCompat` subclass. Adding a second implementation would
  not compile.
- `UnusedAttribute` on `enableOnBackInvokedCallback` is expected: the attribute
  only does anything from API 33, and `minSdk` is 26.
- `ObsoleteSdkInt` on `mipmap-anydpi-v26` suggests dropping the `-v26`
  qualifier. The AGP resource merger does not pick the folder up without it, and
  the build fails with `resource mipmap/ic_launcher_round not found`.

## Things worth knowing

- The page pulls its webfonts from `fonts.googleapis.com`. That is a request to
  Google carrying the device IP, and it is why the privacy policy mentions
  Google Fonts. The layout degrades gracefully to system fonts offline. Bundling
  the fonts locally would remove the third party at the cost of roughly a
  megabyte, mostly Noto Sans SC.
- The forecast itself always needs the network; there is no offline cache.
- Rendering depends on the device's Android System WebView, which updates
  independently of this app.
