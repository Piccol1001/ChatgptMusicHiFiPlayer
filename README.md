# HiFi Player

A native, local-only Android music player built with Kotlin, Compose Material 3, and Media3. This repository starts with **Phase 1** of the [product requirements](docs/PRODUCT_REQUIREMENTS.md). It is not the completed five-phase product. See [FEATURE_STATUS.md](FEATURE_STATUS.md) for the exact scope.

## Build

Use Android Studio with JDK 17 or 21, Android SDK Platform 36 and Build Tools 35.0.0. Android 8.0 / API 26 is the minimum; target SDK is 36. Set `ANDROID_HOME` or create an untracked `local.properties` with `sdk.dir`.

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug assembleRelease
./gradlew connectedDebugAndroidTest # requires an Android device or emulator
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`. The release APK is unsigned; supply your own signing configuration for distribution. Never commit signing credentials.

Prebuilt version 0.1.0 APKs and SHA-256 checksums are included in [artifacts/apk](artifacts/apk/README.md). Use the debug APK for installation; the release artifact is unsigned. Review the known limitations before evaluating either build.

In the prepared Codex machine, first run `source /workspace/.tooling/env.sh`. Build caches and tools live outside the checkout. There is no application backend to start, no account, and no runtime API key. Android devices and emulators run the application; this is not a web application.

## Use

1. Allow music access, or choose a folder using Android's folder picker. Add more folders in Settings. Android may restrict selecting storage roots; choose a music subfolder instead.
2. Browse songs, albums, artists, genres or folders. Search title, artist and album; change sorting or filter the reported MIME type. Favorites and listening history are local.
3. Tap a song to play it. Use its menu to play next, enqueue, favorite, or add to a playlist. The play-collection action queues up to 1,000 matching songs; a message reports the cap. Library browsing itself is paged.
4. Open Playing for seek, shuffle, repeat, queue editing and queue-to-playlist saving. Queue movement uses accessible up/down buttons in this phase. Notification and compatible headset controls use the same media session.
5. The queue and position are saved periodically while playing and after changes. Relaunch restores the queue **paused**, avoiding unexpected speaker playback. An abrupt kill may lose up to five seconds of position. Android force-stop stops playback until you reopen the app.

Audio files are never edited, moved or deleted. Removing a playlist removes its membership only. Removing a folder relinquishes its persisted access; cached entries are marked unavailable.

## Formats and audio honesty

| Format | Decoder path | Validation |
| --- | --- | --- |
| MP3, FLAC, WAV PCM, AAC/M4A, Vorbis/OGG, Opus | Media3 extraction and Android decoders | See TESTING.md for actual execution results; availability can vary by device, container and encoding |
| ALAC | No bundled extension decoder | Unverified; not advertised as supported |
| AIFF | No compatible extractor/decoder integrated | Pending Phase 4 |

The app uses the normal Android audio route. Custom EQ, ReplayGain, crossfade, direct USB output, lyrics and widgets are pending. There is no system-wide equalizer or bit-perfect claim. EQ and Visualizer destinations are explicitly unavailable rather than simulated.

## Architecture

- `library/`: MediaStore volume discovery, persistable SAF tree access and metadata extraction on IO workers. Refresh is serialized; MediaStore changes trigger a debounced refresh while the UI model lives. Manually refresh SAF folders after external edits.
- `data/`: Room track index, playlist membership, favorites and history; parameterized SQL with Paging. DataStore owns preferences, selected trees and queue snapshots.
- `playback/`: one ExoPlayer inside one MediaSessionService. It owns focus, noisy-route handling, session metadata, notifications and the playback timeline. The UI connects with MediaController; it never creates a second player.
- `ui/`: an AndroidViewModel exposes repository flows and session state. Position polling occurs only while its playback state has subscribers. Compose renders state and sends commands.

Unavailable files are retained in the database so playlist references and favorites survive storage removal. Where a document provider supports MediaStore mapping, the scanner reuses that identity. Providers without stable mappings can expose the same file under multiple URIs; cross-provider deduplication remains limited. Before API 29 the folder view for MediaStore items uses a broad device-music group; selected SAF directories retain folder names.

## Pinned dependencies

Gradle 8.13 (wrapper SHA-256 verified), Android Gradle Plugin 8.13.2, Kotlin/Compose compiler 2.2.21, KSP 2.2.21-2.0.4, Compose BOM 2025.12.01, Media3 1.8.0, Room 2.8.4, DataStore 1.2.0, Lifecycle 2.10.0, Paging 3.3.6, Coil 2.7.0, Coroutines 1.10.2. Exact declarations are in the Gradle files. Room schemas are exported for later migration work.

## Privacy

The manifest contains no `INTERNET`, microphone, all-files, advertising or analytics permissions. Artwork and tags come from local providers. No remote artwork, lyrics, profile or metadata fetching is implemented. Backups are disabled to keep the database and preferences local. Development dependency downloads do require network access.

See [DEVELOPMENT_PLAN.md](DEVELOPMENT_PLAN.md), [TESTING.md](TESTING.md), and [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md).
