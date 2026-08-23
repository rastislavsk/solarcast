#!/usr/bin/env node
/*
 * Lifts the parts of ../../index.html that must stay identical between the web
 * app and the native app — the four translations, the calendar words and the
 * three colour schemes — and writes them out as Android resources and Kotlin.
 *
 * Hand-copying ~90 strings across four languages, and 3 x 2 palettes of 30
 * colours each, is how the two versions quietly drift apart. This keeps the
 * page the single source of truth for all of it.
 *
 *   node tools/generate-from-web.mjs
 */
import { readFileSync, writeFileSync, mkdirSync } from 'node:fs';
import { dirname, join, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const module_ = resolve(here, '..');
const source = resolve(module_, '..', 'index.html');
const html = readFileSync(source, 'utf8');

/** Pulls `var <name> = { ... };` out of the page and evaluates the literal. */
function literal(name) {
  const start = html.indexOf(`var ${name} = {`);
  if (start < 0) throw new Error(`${name} not found in ${source}`);
  const open = html.indexOf('{', start);
  let depth = 0;
  for (let i = open; i < html.length; i++) {
    const c = html[i];
    if (c === '{') depth++;
    else if (c === '}') {
      depth--;
      if (depth === 0) return (0, eval)('(' + html.slice(open, i + 1) + ')');
    }
  }
  throw new Error(`${name} is not balanced`);
}

const I18N = literal('I18N');
const DAYS_L = literal('DAYS_L');
const DAYS_S = literal('DAYS_S');
const MONS = literal('MONS');
const COMPASS = literal('COMPASS');
const PALETTES = literal('PALETTES');

/*
 * Three strings describe the web app's world — data kept "in this browser",
 * dismissing a list by "clicking" beside it. On a phone both are wrong, so the
 * native app says device and tap instead. This is the complete list of
 * divergences from the page's copy; everything else is used verbatim. Add to it
 * only for wording that is genuinely false on Android, never to reword.
 */
const NATIVE_WORDING = {
  editLocH: {
    sk: 'Vyber z ponuky, alebo ťukni mimo poľa a nechaj pôvodnú lokalitu.',
    en: 'Pick one from the list, or tap outside the field to keep the current location.',
    de: 'Wähle einen Eintrag aus der Liste, oder tippe neben das Feld, um den bisherigen Standort zu behalten.',
    zh: '从列表中选择，或点击输入框外保留当前位置。',
  },
  setupLocD: {
    sk: 'Zadaj mesto alebo obec, kdekoľvek na svete. Časové pásmo a súradnice si SolarCast dohľadá sám a uloží ich len do tohto zariadenia.',
    en: 'Enter a city or town, anywhere in the world. SolarCast looks up the coordinates and time zone itself and keeps them on this device only.',
    de: 'Gib eine Stadt oder Gemeinde ein, weltweit. SolarCast ermittelt Koordinaten und Zeitzone selbst und speichert sie nur auf diesem Gerät.',
    zh: '输入世界上任意城市或乡镇。SolarCast 会自动查出坐标和时区，并且只保存在这台设备上。',
  },
  dangerD: {
    sk: 'Údaje sú uložené len v tomto zariadení. Vymazaním sa vrátiš na úvodné nastavenie.',
    en: 'Your data is stored on this device only. Erasing it takes you back to first-run setup.',
    de: 'Deine Daten liegen nur auf diesem Gerät. Beim Löschen landest du wieder bei der Ersteinrichtung.',
    zh: '数据仅保存在此设备中。清除后将回到首次设置。',
  },
};

// Catch the page renaming or dropping a key we override: a silent miss would
// ship the browser wording back into the app.
for (const key of Object.keys(NATIVE_WORDING)) {
  if (!(key in I18N.en)) throw new Error(`NATIVE_WORDING overrides '${key}', which no longer exists in I18N`);
}

const LANGS = ['sk', 'en', 'de', 'zh'];
// values/ is English; the rest get a locale qualifier. This matches
// res/resources.properties, which declares the unqualified folder as en-US.
const FOLDER = { en: 'values', sk: 'values-sk', de: 'values-de', zh: 'values-zh' };

/* ---------------------------------------------------------------- strings */

/**
 * Android string resources are not XML text nodes with XML rules alone: the
 * resource compiler also reads a backslash escape layer, and treats a leading
 * @ or ? as a reference. Apostrophes and quotes must be backslash-escaped or
 * aapt2 fails the build outright.
 */
function androidEscape(value) {
  let s = String(value)
    .replace(/\\/g, '\\\\')
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/'/g, "\\'")
    .replace(/"/g, '\\"')
    .replace(/\n/g, '\\n');
  if (/^[@?]/.test(s)) s = '\\' + s;
  return s;
}

function stringsXml(lang) {
  const table = I18N[lang];
  const rows = [];
  for (const key of Object.keys(table)) {
    const value = NATIVE_WORDING[key]?.[lang] ?? table[key];
    if (typeof value !== 'string') continue;
    // A literal % would be read as a format specifier by any later
    // getString(id, args) call; say up front that these are not format strings.
    const formatted = value.includes('%') ? ' formatted="false"' : '';
    rows.push(`    <string name="t_${key}"${formatted}>${androidEscape(value)}</string>`);
  }

  const arrays = [
    ['day_long', DAYS_L[lang]],
    ['day_short', DAYS_S[lang]],
    ['month_short', MONS[lang]],
    ['compass', COMPASS[lang]],
  ].map(([name, items]) => {
    const entries = items.map((it) => `        <item>${androidEscape(it)}</item>`).join('\n');
    return `    <string-array name="${name}">\n${entries}\n    </string-array>`;
  });

  return [
    '<?xml version="1.0" encoding="utf-8"?>',
    '<!--',
    '    GENERATED by tools/generate-from-web.mjs from ../index.html.',
    '    Edit the page, then re-run the generator. Do not edit this file.',
    '-->',
    '<resources xmlns:tools="http://schemas.android.com/tools" tools:ignore="UnusedResources">',
    rows.join('\n'),
    '',
    arrays.join('\n'),
    '</resources>',
    '',
  ].join('\n');
}

for (const lang of LANGS) {
  const dir = join(module_, 'app', 'src', 'main', 'res', FOLDER[lang]);
  mkdirSync(dir, { recursive: true });
  writeFileSync(join(dir, 'strings_i18n.xml'), stringsXml(lang), 'utf8');
}

/* --------------------------------------------------------------- palettes */

/** '#RRGGBB' or 'rgba(r,g,b,a)' -> a Kotlin 0xAARRGGBB literal. */
function color(value) {
  const hex = /^#([0-9a-f]{6})$/i.exec(value);
  if (hex) return `0xFF${hex[1].toUpperCase()}`;
  const rgba = /^rgba?\(\s*([\d.]+)\s*,\s*([\d.]+)\s*,\s*([\d.]+)\s*(?:,\s*([\d.]+)\s*)?\)$/i.exec(value);
  if (rgba) {
    const [, r, g, b, a] = rgba;
    const alpha = Math.round((a === undefined ? 1 : Number(a)) * 255);
    const byte = (n) => Number(n).toString(16).padStart(2, '0').toUpperCase();
    return `0x${byte(alpha)}${byte(r)}${byte(g)}${byte(b)}`;
  }
  throw new Error(`unrecognised colour: ${value}`);
}

const SWATCHES = [
  'bg', 'surface', 'surface2', 'line', 'lineSoft', 'text', 'dim', 'dim2',
  'accent', 'cool', 'warn', 'tipBg', 'shadow', 'noticeBg', 'noticeLine',
  'noticeText', 'dangerBg', 'dangerLine', 'dangerText', 'dangerBtn', 'barFill',
  'barToday', 'insideLabel', 'grid', 'gridBase', 'ceiling', 'cloud',
  'emptyCell', 'marker', 'envelope',
];

function paletteKt(scheme, mode) {
  const p = PALETTES[scheme][mode];
  const swatches = SWATCHES.map((k) => `        ${k} = Color(${color(p[k])}),`);
  const heat = p.heat.map(([r, g, b]) => `Color(0xFF${[r, g, b].map((n) => n.toString(16).padStart(2, '0').toUpperCase()).join('')})`);
  const az = ['n', 'e', 's', 'w'].map((k) => {
    const [r, g, b] = p.az[k];
    return `${k} = Color(0xFF${[r, g, b].map((n) => n.toString(16).padStart(2, '0').toUpperCase()).join('')})`;
  });
  return [
    `    private val ${scheme}${mode[0].toUpperCase()}${mode.slice(1)} = Palette(`,
    ...swatches,
    `        heat = listOf(\n            ${heat.join(',\n            ')},\n        ),`,
    `        az = AzimuthColors(${az.join(', ')}),`,
    '    )',
  ].join('\n');
}

const fields = SWATCHES.map((k) => `    val ${k}: Color,`).join('\n');
const bodies = [];
for (const scheme of ['dusk', 'slate', 'terra']) {
  for (const mode of ['dark', 'light']) bodies.push(paletteKt(scheme, mode));
}
const lookup = ['dusk', 'slate', 'terra'].map((scheme) =>
  `            Scheme.${scheme.toUpperCase()} -> if (dark) ${scheme}Dark else ${scheme}Light`
).join('\n');

const palettesKt = `package io.github.rastislavsk.solarcast.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/*
 * GENERATED by tools/generate-from-web.mjs from ../index.html.
 * Edit the page, then re-run the generator. Do not edit this file.
 *
 * The charts draw their own colours rather than inheriting them, so the whole
 * palette lives in one place and every drawing function reads from it.
 */

/** The four cardinal hues a string's colour is interpolated between. */
@Immutable
data class AzimuthColors(val n: Color, val e: Color, val s: Color, val w: Color)

@Immutable
data class Palette(
${fields}
    /** Heat-map ramp, dark to bright in dark mode and pale to saturated in light. */
    val heat: List<Color>,
    val az: AzimuthColors,
)

enum class Scheme { DUSK, SLATE, TERRA }

object Palettes {
${bodies.join('\n\n')}

    fun of(scheme: Scheme, dark: Boolean): Palette =
        when (scheme) {
${lookup}
        }
}
`;

const ktDir = join(module_, 'app', 'src', 'main', 'java', 'io', 'github', 'rastislavsk', 'solarcast', 'ui', 'theme');
mkdirSync(ktDir, { recursive: true });
writeFileSync(join(ktDir, 'Palettes.kt'), palettesKt, 'utf8');

const counts = LANGS.map((l) => `${l}:${Object.values(I18N[l]).filter((v) => typeof v === 'string').length}`).join(' ');
console.log(`generated strings_i18n.xml for ${LANGS.length} languages (${counts}) and Palettes.kt (6 palettes)`);
