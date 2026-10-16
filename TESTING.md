# Validation and test procedures

Results are for the current Codex Linux machine on 2026-10-16. Physical-device behavior is not implied by build or JVM results.

## Executed build and unit checks

- `assembleDebug`: passed; installable debug APK produced.
- `assembleRelease`: passed; unsigned release APK produced, not a signed/distributed release.
- `assembleDebugAndroidTest`: passed; integration test APK and original audio fixtures compiled.
- `testDebugUnitTest`: **13 executed, 13 passed, 0 failed, 0 skipped**. Library tests run with Robolectric API 28 and 35 (10 cases total); queue serialization tests run with API 28 (3 cases).
- `lintDebug`: passed with 0 errors and 8 warnings. Warnings cover dependency update suggestions, intentionally exported MediaSessionService, backup guidance, an empty resource configuration and URI KTX suggestions. No lint baseline or disabled assertions were used.
- Final release APK permissions inspected with `aapt2 dump permissions`: no INTERNET, ACCESS_NETWORK_STATE, RECORD_AUDIO or MANAGE_EXTERNAL_STORAGE. A dependency's network-state permission is explicitly removed during manifest merge.

Unit checks execute real Room/SQLite queries for playlist membership ordering and deduplication, cascade deletion without deleting tracks, refresh/missing-file preservation, literal search with wildcard/injection characters, independent history order/count, metadata fallbacks, and queue snapshot round trips including duplicate tracks and corrupt snapshots. Search tests exposed and led to correction of an SQL ESCAPE bug.

## Runtime validation status

The host has no `/dev/kvm`. A software API 35 emulator reached adb and system_server but did not complete boot during the initial attempt. The lighter **API 28 software emulator completed boot**, installed the app and rendered the Compose library/permission screen. Initial cold-launch waiting timed out before rendering. Android System UI displayed an ANR dialog under software emulation. An early cold start also produced a HiFi Player service-execution ANR. Its captured main thread was Runnable in Compose layout/frame-rate voting; the VM was heavily CPU constrained and still finishing boot. This is recorded as an unresolved startup/performance observation, not dismissed as proof of normal device behavior. Do not infer smooth performance from this environment.

The six-format playback instrumentation method passed: generated MP3, FLAC, WAV, AAC/M4A, Vorbis and Opus each reached playing state with advancing position, expected duration and no player error. The first queue test reached its final snapshot assertion too early: it used a fixed 700 ms wait after optimistic controller commands. It now waits for the service's asynchronous disk state. The subsequent complete `connectedDebugAndroidTest` run **passed: 2 executed, 2 passed, 0 failed, 0 skipped** on API 28, including queue persistence and all six formats. The Gradle runner reported BUILD SUCCESSFUL. Neither unit nor integration assertions were removed.

A separate native MediaStore check copied seven generated files to the emulator's Music folder. Android indexed six (including the 60-second background-test tone); a read-only query of the app's Room database confirmed **6 tracks, all 6 available**. The standalone `.opus` fixture did not appear in this API 28 MediaStore audio index, despite playing through the app's content-URI path in instrumentation. SAF discovery recognizes the extension, but native SAF selection is not acceptance-tested. UI automation could not reliably obtain a root node after cold launch, so screen interactions and manual background/lock-screen acceptance remain unverified.

Two instrumentation methods exist in `PlaybackIntegrationTest`:

1. Play generated 8-second MP3, FLAC, WAV/PCM, AAC/M4A, Vorbis/OGG and Opus files through the actual MediaSessionService; require playing state, advancing position, meaningful duration and no decoder error.
2. Exercise seek, pause, shuffle/repeat settings, next/previous, queue movement/removal and the service's persisted snapshot.

Fixtures are original stereo 440 Hz tones at 48 kHz. They are not real-world codec coverage, high-resolution output measurements, gapless samples or hardware tests. A non-exported FileProvider exists **only in the debug variant**, restricted to the private integration-test directory, so the suite works without requesting write access to personal media. Fixtures are deleted after tests.

To reproduce on a ready device:

```sh
source /workspace/.tooling/env.sh # prepared cloud machine only
adb shell getprop sys.boot_completed # must return 1
./gradlew connectedDebugAndroidTest
```

Reports: `app/build/reports/tests/testDebugUnitTest/`, `app/build/reports/lint-results-debug.html`, and, when executed, `app/build/reports/androidTests/connected/debug/`. Ensure report timestamps and selected-device outcomes belong to the current run.

## Manual device acceptance checklist (unrun unless specifically recorded above)

| Scenario | Procedure and expected behavior |
| --- | --- |
| Permissions | Deny audio permission, select a SAF music folder, grant audio later, revoke it in Android settings. Folder access works independently; refresh and errors remain usable. |
| Library | Scan internal and SD music, search literal `%`/`_`, browse categories, change all sorts and MIME filters; refresh after renaming/deleting a test file. Missing tracks become unavailable without changing original audio. |
| Formats | Play real MP3, FLAC (16/24 bit), PCM WAV, AAC/M4A, Vorbis and Opus samples on each API/device. Test corrupt/unsupported files; errors must remain actionable and Next must work. ALAC/AIFF are not acceptance-passed. |
| Core transport | Play/pause, scrub, skip forward/back, repeat one/all/off, toggle shuffle; edit a queue containing duplicates and verify the selected occurrence is retained. |
| Playlists | Create/rename/delete, add duplicate membership, remove songs, save queue, relaunch and verify data. Confirm deleting a playlist does not remove source files. |
| Background | Play, press Home, lock screen for several minutes, use notification controls and return. Check a single MediaSession/PlaybackService and no unintended restarts. |
| Focus / headphones | Receive a call, start another audio app, unplug wired headphones and disconnect Bluetooth. No unintended speaker playback. Verify headset buttons and lock-screen controls on hardware. |
| Restoration | Pause and swipe away; relaunch. Repeat with normal process termination and force-stop. Queue/index/modes/position restore paused; abrupt kill may lose up to five seconds. Force-stop itself is expected to stop Android services. |
| SD / provider loss | Remove SD card during playback/scan; revoke SAF grant; reinsert/regrant and refresh. UI remains responsive and no original files are altered. |
| Rotation / access | Rotate library, now-playing and queue screens; use large fonts and TalkBack; verify all controls and text remain usable. |
| Offline | Install, add music, enable airplane mode, disable Wi-Fi, relaunch and play. No account or network fallback is required. |
| Large library / battery | Use 10k+ real entries and many groups; measure scan time, paging memory, search latency, playback stability and idle battery. No performance claim has been measured yet. |
| Gapless / DAC | Test continuous live-album and encoder-padding fixtures, source/output routes and USB attach/detach on hardware. Currently unverified. |
| Later phases | EQ response/bypass/smoothing/clipping, ReplayGain, FFT accuracy, AutoEq, lyrics, widgets and crossfade tests are pending implementation, not skipped passing tests. |

## Environment

Verified JDK 21, Gradle 8.13, SDK Platform 36 and Build Tools 35/36 are retained under `/workspace/.tooling`. Tools, downloaded archives, Gradle caches, emulator images, debug signing material and ordinary generated outputs are outside version control. At the owner's request, copies of the tested debug APK and unsigned release APK are checked in under `artifacts/apk` with SHA-256 checksums. Proxy settings and the machine's Java trust store are used; TLS and artifact checksums were not disabled. No application credentials are needed.
