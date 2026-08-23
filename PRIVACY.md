# SolarCast privacy policy

Last updated: 23 August 2026

SolarCast is a photovoltaic output forecast. It exists as a web page and as an
Android app; both run the same code and behave the same way.

## What SolarCast stores

Everything you enter — the location of your system, the strings, the panel
count and power, tilt and orientation, the inverter limit, and your choice of
language and colour scheme — is stored **only on your own device**, in the
browser's local storage (in the Android app, in the app's private storage).

None of it is sent to us. There is no SolarCast account, no SolarCast server and
no analytics, advertising or tracking of any kind in the app.

Uninstalling the app, or clearing site data in the browser, erases all of it.
The app's own "Erase data" button does the same thing.

If you have Android backup enabled, your saved system may be copied to your
Google account as part of the standard Android app backup, and restored when you
set up a new device. That backup is governed by Google's privacy policy, not
ours.

## What leaves your device

To produce a forecast, SolarCast makes requests to third parties:

- **Open-Meteo** (`open-meteo.com`) — receives the coordinates of the location
  you searched for, in order to return the solar radiation and cloud cover data.
  It also receives the text you type into the location search, in order to look
  up coordinates and the time zone. See the
  [Open-Meteo terms](https://open-meteo.com/en/terms).
- **Google Fonts** (`fonts.googleapis.com`, `fonts.gstatic.com`) — serves the
  typefaces the interface is drawn in. As with any web request, Google receives
  your device's IP address and user agent. See the
  [Google privacy policy](https://policies.google.com/privacy).

These requests carry no identifier for you and no part of your system
configuration beyond the coordinates needed for the forecast. All of them use
HTTPS; the app blocks unencrypted connections entirely.

## Children

SolarCast is a utility for owners of photovoltaic systems. It is not directed at
children and collects no personal data from anyone.

## Permissions

The Android app requests a single permission, `INTERNET`. It does not ask for
location, storage, contacts, camera or any other permission, and it does not use
an advertising identifier.

## Changes

Any change to this policy will be published in this file in the app's public
repository, with the date above updated.

## Contact

Questions about this policy: open an issue at
<https://github.com/rastislavsk/solarcast/issues>.
