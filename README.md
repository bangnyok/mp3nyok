# Wamp3 — YouTube / YT Music to MP3 (Winamp-style Player)

Native Android app (Kotlin + Jetpack Compose) untuk download muzik dari
YouTube / YouTube Music, convert ke **MP3**, dan play dengan interface
klasik **Winamp**.

## Ciri-ciri (ikut requirement anda)

| Kehendak | Cara dalam app |
|---|---|
| Download via link atau playlist | Butang `ADD` → paste link (video atau playlist). Playlist di-fetch dengan `--flat-playlist` dan setiap video jadi satu listing |
| Convert ke MP3 | yt-dlp `-x --audio-format mp3 --audio-quality 0` (best audio, ffmpeg bundled) |
| Simpan dalam folder spesifik | `FOLDER` → default `/storage/emulated/0/Music/Wamp3`, boleh tukar ikut suka. Path dipaparkan di status bar bawah |
| Interface Winamp | Panel gelap klasik, LED time merah, playlist hijau monospace, transport buttons |
| Play dulu untuk test | Tap mana-mana listing: kalau MP3 belum ada → **test stream** terus dari web (label `TEST STREAM` / `WEB`); kalau sudah download → main fail MP3 (`MP3`) |
| Recreate playlist / buang listing | Pilih listing → `REM` untuk buang; `MANAGE` untuk playlist baru / delete playlist |
| Merge playlist | `MANAGE` → pilih playlist sumber → `MERGE -> CURRENT` (semua listing disalin ke hujung playlist semasa) |
| Rename listing dulu | Dalam dialog `ADD`, setiap listing ada text field nama fail — edit sebelum add. Lepas tu boleh `RENAME` mana-mana listing; **fail MP3 di disk juga ikut direname** |

## Cara build

1. Buka **Android Studio** (Ladybug 2024.2.1+ / AGP 8.7).
2. `File > Open` → pilih folder projek ini.
3. Kalau diminta Gradle wrapper, biarkan Android Studio jana
   (Gradle 8.10.2, JDK 17).
4. `Run` pada device/emulator (minSdk 24).

> Nota: APK agak besar (~150MB) sebab library `youtubedl-android` bundle
> Python + yt-dlp + ffmpeg untuk semua ABI. Untuk kecilkan, uncomment
> `abiFilters` dalam `app/build.gradle.kts` (arm64-v8a untuk kebanyakan
> telefon moden).

## Nota penting

- **Kali pertama buka app**, yt-dlp ambil beberapa saat untuk init
  (status bar bawah akan tulis `yt-dlp: initialising...`).
- Untuk simpan ke folder awam (`Music/Wamp3`) di Android 11+,
  tekan `FOLDER` → **GRANT ALL FILES ACCESS**. Tanpa itu, boleh guna
  folder app-specific (`Android/data/com.mrras.wamp3/files/Music`).
- `UPDATE yt-dlp` dalam Settings untuk kemaskini extractor bila YouTube
  berubah.
- Merge playlist menyalin listing (bukan pindah), jadi playlist asal
  kekal.
- Fail MP3 download dulu dengan nama sementara (yt video id), kemudian
  direname ikut nama listing — jika nama sudah wujud, jadi
  `Nama (2).mp3`.
- Sila guna untuk kandungan yang anda ada hak / kebenaran. Download dari
  YouTube menyalahi ToS YouTube melainkan kandungan itu dibenarkan.

## Struktur kod

```
app/src/main/java/com/mrras/wamp3/
  Wamp3App.kt          — init yt-dlp + ffmpeg di background
  MainActivity.kt      — permission + setContent
  data/Models.kt       — Track / Playlist / NewTrack
  data/Store.kt        — library.json (playlists, tracks, folder)
  player/PlayerHolder.kt — ExoPlayer + test-stream dari yt-dlp
  download/DLEngine.kt — queue download, listEntries, update yt-dlp
  ui/WaTheme.kt        — warna Winamp + komponen kongsi
  ui/PlayerWindow.kt   — panel player utama
  ui/PlaylistWindow.kt — playlist editor (ADD/REM/RENAME/DL/MANAGE)
  ui/Dialogs.kt        — Add URL, Playlist Manager, Settings, Rename
  ui/MainScreen.kt     — komposisi skrin + status bar
```
