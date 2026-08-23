# Play Console listing and declarations

Everything below is ready to paste into Play Console. Nothing here is submitted
automatically — publishing stays a deliberate act.

## App details

- **App name:** SolarCast
- **Default language:** English (United Kingdom)
- **App or game:** App
- **Free or paid:** Free
- **Category:** Weather (alternative: Tools)
- **Contact email:** the address on your Play developer account
- **Privacy policy URL:** must be a public URL. The file lives at `PRIVACY.md` in
  this repository; publish it with GitHub Pages and link the rendered page, or
  link the raw file view.

## Short description (max 80 characters)

**en** — Seven-day photovoltaic output forecast for your own array.

**sk** — Sedemdňová predpoveď výroby pre tvoju vlastnú fotovoltiku.

**de** — Siebentägige Ertragsprognose für deine eigene PV-Anlage.

**zh** — 为你自己的光伏阵列提供未来 7 天发电量预测。

## Full description (max 4000 characters)

> SolarCast turns a public weather forecast into the number you actually care
> about: how many kilowatt-hours your own array will make over the next seven
> days.
>
> Tell it where your system is and how it is built — how many strings, how many
> panels each, the power of a single panel, the tilt and the direction each one
> faces — and it projects the forecast solar radiation onto those exact planes.
> It accounts for the split between direct and diffuse radiation, for the
> temperature coefficient of your panels, for system losses and for the AC limit
> of your inverter.
>
> What you get:
>
> • Seven daily totals, each against the clear-sky ceiling for that day, so you
>   can see how much cloud is costing you.
> • An hourly power curve for any day, broken down by string, with cloud cover
>   drawn over it.
> • A day-by-day table with peak power, share of ceiling and average cloud.
> • Sunrise, sunset and the position of the sun through the day.
>
> Do not know your numbers? SolarCast can work out the optimal tilt for your
> latitude and start you off with a sensible 10 kWp system that you can adjust
> later.
>
> Four languages: English, Slovak, German and Chinese. Three colour schemes, in
> light and dark.
>
> No account, no advertising, no tracking. Your system's configuration never
> leaves your phone — it is stored on the device and nowhere else. The only thing
> sent anywhere is the location you search for, to Open-Meteo, which returns the
> weather data.
>
> Forecast data comes from Open-Meteo. A forecast is a forecast: treat the
> numbers as a good estimate, not a guarantee.

## Data safety form

- **Does your app collect or share any of the required user data types?** — No.

  The location the user enters is sent to Open-Meteo solely to fulfil the
  request the user just made, and it is not collected by the developer. Under
  Play's definitions this is neither "collection" (nothing is transmitted off
  the device *to the developer*) nor "sharing" for the purposes of the form.
  The configuration is stored on the device only.

- **Is all of the user data encrypted in transit?** — Yes. Cleartext is blocked
  by the app's network security config.
- **Do you provide a way for users to request that their data is deleted?** —
  The app's own "Erase data" button clears everything; uninstalling does the
  same. No server-side data exists.

## Content rating questionnaire

Answer "no" to every content question. The expected outcome is the
lowest rating in each region (PEGI 3, ESRB Everyone, USK 0).

## Ads

**No**, the app contains no ads.

## Target audience

13 and over. Not designed for children, so no Families policy requirements apply.

## Government apps / financial / health declarations

None apply.

## Graphic assets in this folder

| File | Play slot | Requirement |
| --- | --- | --- |
| `icon-512.png` | App icon | 512 × 512 PNG |
| `feature-graphic-1024x500.png` | Feature graphic | 1024 × 500 PNG |

Still needed, and only capturable from a running build: **at least two phone
screenshots**, 16:9 or 9:16, each between 320 px and 3840 px on its longest side.
Take them from a release build — the forecast overview and the day-profile chart
are the two screens that show what the app is for.
