# NEXUS Media

Native Android video & music player (Kotlin + Jetpack Compose + Media3 + Room).
Dark AMOLED, Midnight Blue + Electric Blue. Reads the real device storage (MediaStore + Storage Access Framework). No fake/hardcoded library.

## Fitur yang sudah ada
- Scan MediaStore otomatis + rescan manual + auto-refresh saat ada file baru
- Folder custom via SAF (pilih folder), sembunyikan folder
- Musik: lagu / album / artis, antrian, shuffle, repeat, sleep timer, equalizer (band, bass, surround, preset, profil kustom)
- Mode player: Cover, Vinyl (berputar), Lirik (.lrc/.txt pilihan user, sinkron), Compact
- Mini player + full player, background playback (MediaSessionService + notifikasi), restore antrian terakhir
- Video: fullscreen landscape, gesture (kecerahan, volume, geser seek, double tap +/-10 dtk), kecepatan, aspect ratio, subtitle eksternal (.srt/.vtt/.ass), Picture-in-Picture, resume posisi terakhir
- Playlist, riwayat, lanjut menonton, deteksi duplikat, pencarian (Room)

## Build
Android Studio: buka folder ini -> Run.  
CLI: `gradle assembleDebug` (Gradle 8.9, JDK 17).  
GitHub Actions: lihat `.github/workflows/build.yml`, APK ada di tab Actions -> Artifacts.

## Catatan arsitektur
Paket: `core`, `data`, `media`, `player`, `features`, `ui`, `di`.
DI memakai `AppContainer` manual (Hilt belum dipasang supaya build awal stabil); mudah dimigrasi ke Hilt.

## Belum ada
Thumbnail preview di timeline video, audio latar belakang untuk video, unit test.
