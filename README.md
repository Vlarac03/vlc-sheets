# VLC Sheets

**English** · [Català](README.ca.md) · [Español](README.es.md)

> Your music, always ready.

**VLC Sheets** is a sheet music reader for Android made for playing live: fast, dark
when needed, and fully offline. Import PDFs or images, organise your library, write
on the page, build setlists and turn pages with Bluetooth pedals or the volume keys.

This is a new app with its own identity and original code. It contains no
trademarks, texts or assets from MobileSheets: it only takes the idea of a
professional sheet music reader and rebuilds it with Kotlin, Jetpack Compose and
Material 3.

---

## Install

Download the APK from [Releases](../../releases) and open it on your tablet. The
first time, Android asks for permission to install apps from your browser.

---

## Features

### Library
- Import many **PDFs** and **images** (JPG, PNG, WebP…) at once from any file manager.
- **Text and ChordPro** (`.txt`, `.cho`, `.chopro`…) become a PDF with the chords above
  the lyrics, so they read and annotate like any score.
- `.msf` files exported from MobileSheets (document, title, annotations and audio).
- Its own **`.vlcs`** file: one score or a whole setlist with documents, annotations
  and audio, to move it to another tablet.
- Automatic cover (thumbnail of the first page).
- Metadata: title, composer, genre, key, BPM, time signature, year, duration, tags,
  notes and favourite.
- Instant search by title, composer, tag, collection or notes, ignoring accents and case.
- Sort by title, composer, year, last opened or date added.
- Grid or list view, favourites and quick filters.
- Collections by style, instrument or gig.

### Reading
- Full-screen viewer with horizontal paging and a page indicator.
- **Pinch to zoom**, with free panning.
- Turn pages by tapping the sides (can be turned off), with the buttons, a keyboard or a
  **Bluetooth pedal** (volume keys, arrows, Page Up/Down, space and two keys you pick in
  Settings).
- **Two pages** side by side in landscape, **half-page turns** (the top half changes
  first, then the bottom) and **white margin cropping**.
- **Audio per score**: link one or more tracks and play them while you read, slower
  (50–90 %) without changing the pitch, with a loop and with marks that turn the page.
- **Audio buttons** on the page and **jump marks** to another page.
- **Concert mode** (pages can only be turned), **warm paper** and **cover areas**.
- Actions when holding a side (first or last page, previous or next score, audio,
  metronome).
- **Export to PDF** with the annotations drawn in.
- Immersive mode with auto-hiding bars, and the screen stays on.
- **Inverted colours** for reading in low light.
- Page slider and bookmarks.

### Annotations
- Pen and highlighter with six colours and three widths.
- Only the stylus (S Pen or similar) draws: fingers turn pages and zoom.
- Eraser that works by dragging; holding the stylus button turns the pen into an eraser.
- Undo, clear the page, hide annotations.
- **Handwriting → text**: handwritten numbers and dynamics become text, with an own
  recogniser trained on your handwriting, no internet needed.
- Annotations are stored per page and relative to it, so they look the same on any
  screen size.

### Setlists
- Create setlists, add scores and reorder them, with a note per piece and the total
  duration.
- Playing a setlist: on the last page, the "Next score" button opens the next one.
  Turning pages never jumps to another score by itself.
- Counts of scores, pages and annotated scores.

### Metronome
- Built-in metronome on AudioTrack (precise clicks, no drift).
- 30–260 BPM, 2/3/4/6 beats per bar, accent on the first beat.
- Starts with the tempo and time signature saved in the score.
- Can turn the page every N bars, with a count-in.
- Keeps playing while you read and can be stopped from the viewer.

### Tuner
- Chromatic tuner with the microphone: note, cents and Hz; reference A from 415 to 466 Hz.

### Data
- Full backup to a **ZIP** (metadata, files and annotations).
- Complete restore of the library.
- Full wipe, with confirmation.

### Details
- Light, dark or automatic theme; optional dynamic colours (Android 12+).
- Catalan, Spanish and English.
- Own icon and brand.
- No storage permissions: everything goes through the **Storage Access Framework**.
- No network, no accounts, no telemetry.

---

## Screenshots

*(Coming soon.)*

---

## Architecture

```
app/src/main/java/com/vlcsheets/app
├── MainActivity.kt          Edge-to-edge, pedal keys, VIEW intents
├── MainViewModel.kt         Library state, filters, actions, metronome
├── VlcSheetsApp.kt          Manual dependency container
├── data
│   ├── Models.kt            Song, Setlist, Collection, Stroke, Bookmark + JSON
│   ├── LibraryRepository.kt Atomic JSON index, import, backup, restore, audio
│   ├── ChordPro.kt          Text and ChordPro parsing
│   ├── Handwriting.kt       Handwriting recogniser (point cloud, $P)
│   ├── MsfReader.kt         Reads .msf files exported from MobileSheets
│   └── SettingsRepository.kt Reactive SharedPreferences
├── media
│   ├── Media.kt             PdfRenderer, BitmapFactory, EXIF, margin cropping, thumbnails
│   ├── AudioPlayer.kt       Plays each score's audio (MediaPlayer)
│   ├── PdfExporter.kt       PDF with annotations, written page by page
│   ├── ChordProPdf.kt       Text and ChordPro turned into PDF
│   ├── Practice.kt          Audio marks, loop and metronome page turns
│   ├── Tuner.kt             Pitch detection (YIN)
│   └── MetronomeEngine.kt   Click generation on AudioTrack
└── ui
    ├── App.kt               Navigation, snackbars, bottom bar
    ├── theme/Theme.kt       Palette, typography, shapes (Material 3)
    ├── components/Common.kt Brand mark, thumbnails, empty states, dialogs
    └── screens
        ├── LibraryScreen.kt      Grid/list, filters, import
        ├── SongEditorSheet.kt    Metadata and collections
        ├── SetlistsScreen.kt     Setlists, detail and score picker
        ├── CollectionsScreen.kt  Collections
        ├── SettingsScreen.kt     Settings, backup, about
        ├── HandwritingTrainer.kt Teaching your handwriting
        ├── TunerSheet.kt         Tuner
        └── ViewerScreen.kt       Viewer, zoom, annotations, bookmarks, metronome, audio
```

**No external database or ORM.** The library is a small, atomic `index.json`
(write to a temp file + `fsync` + rename) inside `filesDir/library`, next to the
PDFs. Each score's annotations live in their own file (`songs/<id>/annotations.json`),
so a new stroke does not rewrite the whole index. A backup is just a zipped folder,
and the app starts instantly.

**No third-party PDF libraries.** Rendering uses `android.graphics.pdf.PdfRenderer`.
Pages are rendered at about 1.4× the screen width in `ARGB_8888`, the only format
`PdfRenderer` accepts; images are decoded in `RGB_565` to save memory.

---

## Build and test

Requirements: JDK 17+ and the Android SDK with platform 36.

```bash
# 1. Point to the SDK
echo "sdk.dir=C\:\\Android\\sdk" > local.properties

# 2. Build
./gradlew :app:assembleDebug

# 3. Tests and lint
./gradlew :app:testDebugUnitTest
./gradlew :app:lintDebug

# 4. Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

The APK is `app/build/outputs/apk/debug/app-debug.apk`.

---

## Privacy

VLC Sheets makes no network connections (it has no internet permission). Its only
permission is the microphone, for the tuner: it is asked the first time you open
it, and the sound is analysed on the tablet, never stored or sent. Imported files
are copied into the app's private storage and only leave the device when you
export a PDF, a `.vlcs` file or a backup.

---

## Status

Version 1.0.0. See [Releases](../../releases) for each version.
