# HiFi Player

A native, offline Android music player built with Kotlin, Compose Material 3, Media3, Room and DataStore. It includes working EQ, six PCM visualizers, local AutoEq profiles, lyrics, output information, smart playlists and three Glance widgets. **Some acceptance remains incomplete**, especially sample-exact MP3 transitions and physical-device audio/performance checks. See [feature status](FEATURE_STATUS.md), [testing](TESTING.md) and [phase reports](docs/PHASE_REPORTS.md).

## APKs and build

Release metadata is managed through [the verified release workflow](docs/RELEASE_WORKFLOW.md), with [changelog](CHANGELOG.md) and [historical APK inventory](RELEASES.md). `version.properties` changes only after successful verified release packaging. Settings → About → App Version reads the installed APK metadata.

Current evaluation artifacts and SHA-256 checksums are in [artifacts/apk](artifacts/apk/README.md). Install the **debug APK**. The release APK is unsigned and needs an owner's release signing configuration. Signing keys and credentials are not committed.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease
./gradlew connectedDebugAndroidTest # disposable Android test target only
```

Connected test runners can uninstall their target APK. Use the separate validation emulator, never an installation containing user data; see [native test instructions](TESTING.md).

Use JDK 17 or 21, Android SDK Platform 36 and Build Tools 35.0.0. Minimum Android is 8.0 / API 26; compile/target SDK is 36. Gradle 8.13, AGP 8.13.2 and Kotlin 2.2.21 are pinned. Set `ANDROID_HOME` or an untracked `local.properties`. Generated APKs are under `app/build/outputs/apk/`.

In this prepared Codex environment, first run `source /workspace/.tooling/env.sh`. Toolchains and caches are outside the checkout. This is an Android application: there is no web server, backend, account or API key to start.

## Redesigned interface

Compact browsing, album/playlist cards, folder hierarchy, an artwork-led player, a persistent mini-player, focused EQ/visualizer controls and grouped Settings preserve the existing playback service. Appearance offers black/dark-gray backgrounds, local artwork accents and artwork shape/gradient controls. See the [redesign report and screenshots](docs/UI_REDESIGN.md).

## Use

1. Allow music access or choose a music folder. Multiple persisted SAF folders are supported; Android can restrict storage-root selection. Browse/search/sort the paged library, mark favorites and build playlists.
2. Open Playing for transport, seek, shuffle, repeat, queue editing, audio information and lyrics. Notification/headset controls share the same media session. The saved queue restores **paused**; abrupt process death can lose up to five seconds of position.
3. In Equalizer, choose 10-band, 31-band or parametric processing; edit numeric values or drag the response controls. There are 58 original presets, custom preset CRUD/favorites/recents and local JSON import/export. ReplayGain reads supported real metadata.
4. Open AutoEq/output profiles to import compatible local text, preview and save a correction. Assign a default or an output category. Editing an active profile preserves separately stored personal EQ. No headphone dataset or network download is bundled.
5. Open Visualizer for measured spectrum, waveform, stereo meters, spectrogram, circular spectrum and three original shader effects. FFT/FPS/quality/color settings, fullscreen and reduced decorative motion are available. Analysis suspends when hidden or paused.
6. Import `.lrc` or plain text lyrics, use compatible embedded lyrics, or authorize a music folder for sibling-file matching. Timed lines highlight/scroll and can seek the existing player.
7. Add Compact, Standard or Expanded widgets from Settings or the launcher's widget picker. Theme/accent settings apply to widgets; artwork/progress opens Now Playing.
8. Create smart playlist rules for metadata, play count, date, favorites and format. M3U8 import/export uses exact indexed content URIs; portable filesystem paths are reported as unresolved.

The play-collection action queues up to 1,000 matching songs and reports that cap. Browsing is paged. Playlist membership contains unique songs, while queues can contain repeated items. Source audio files are never edited, moved or deleted. Missing sources are marked unavailable so playlist/favorite/history relationships are retained.

## Formats and audio limits

| Format | Current evidence |
| --- | --- |
| MP3, FLAC, WAV PCM, AAC/M4A, Vorbis/OGG, Opus | Generated fixtures played through the production service on API 28 |
| AIFF/AIFC signed PCM | Original streaming extractor; 16-bit/48 kHz and 24-bit/96 kHz playback/seeking tested; other supported widths/rates still need device coverage |
| ALAC | Uses an available platform decoder; the API 28 image has none and an explicit unsupported message was tested; no extension decoder bundled |

The DSP/output path converts to **16-bit PCM**. High-resolution source decoding does not establish 24-bit hardware output. The output screen distinguishes source, processing, AudioTrack client format and device-reported capabilities. No exclusive, direct USB or bit-perfect mode is offered.

FLAC transition frame totals and AudioTrack reuse were verified. Sample-exact MP3 gapless remains incomplete on the tested API 28 Google decoder: diagnostics found 22–551 boundary frames and a terminal decoder tail. Its strict regression is explicitly skipped on that known decoder combination and can be forced as documented in TESTING.md. Crossfade and CUE are not implemented.

## Architecture

- `playback/`: one ExoPlayer and MediaSessionService, focus/noisy handling, notifications and saved queue. UI and widgets use MediaController.
- `dsp/`: PCM processor, immutable coefficient plans, stable biquads, smoothed changes, presets and supported-tag ReplayGain. [Design/limits](docs/DSP.md).
- `visualization/`: bounded nonblocking tap analysis, calibrated windowed FFT, Canvas displays and original OpenGL ES shaders. [Details](docs/VISUALIZERS.md).
- `audio/`: validated local AutoEq, output monitoring/profiles, lyrics and AIFF extraction. [Details](docs/ADVANCED_AUDIO.md).
- `data/`, `library/`: Room index/playlists/favorites/history, parameterized Paging queries, SAF/MediaStore reads and local DataStores. Room remains at schema version 1.
- `widgets/`: three responsive Glance layouts with event-driven shared state and controls of the existing session. [Details](docs/WIDGETS.md).
- `ui/`: Compose screens and view-model flows. Queue lists are cached between timeline changes; position ticks stop when playback/UI observation stops.

## Privacy and remaining checks

No Internet, microphone or all-files permission; no accounts, ads, analytics or cloud dependency. Backups/device transfer are excluded. Settings includes selectable offline dependency licenses; the [runtime inventory](docs/DEPENDENCIES.md) and [notices](THIRD_PARTY_NOTICES.md) are included in the repository. Imports and settings stay local. The app uses scoped read access and explicitly selected export destinations.

Physical Bluetooth/USB/headphone removal, incoming calls, launcher variants, battery/thermal behavior and prolonged listening remain device acceptance work. Software-emulator cold-start ANRs/slow frames were observed; physical startup performance is not certified. Local artwork-derived accents are available under Appearance. Portable M3U paths, crossfade and CUE remain unavailable. The original scope is preserved in [requirements](docs/PRODUCT_REQUIREMENTS.md) and the [remaining-phase prompt](docs/REMAINING_PHASES_REQUIREMENTS.md).
