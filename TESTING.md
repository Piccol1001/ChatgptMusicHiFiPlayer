# Validation and test procedures

Evidence is from the prepared Codex Linux machine and an API 28 software emulator. Build/JVM/emulator results do not certify physical audio hardware, battery use or startup performance. Historical phase checkpoints are in [docs/PHASE_REPORTS.md](docs/PHASE_REPORTS.md).

## Redesign verification — 2026-10-18

The updated host run passed **45 unit cases, 0 failures/errors/skips**, and lint passed with **0 errors / 22 warnings**. Six release-workflow safety cases passed. The new model tests cover installed PackageManager version metadata, real Room album/playlist cover/count projections and folder-prefix/count behavior.

Native results were obtained across two isolated software AVDs, not a single uninterrupted all-green API 28 run:

- API 28: all nine non-skipped playback/library/DSP/widget cases and the shader case passed (**10 passed**). The strict MP3 case was explicitly skipped on the known decoder combination. The UI case was aborted when Android's system process crashed with `SurfaceControl.finalize() timed out after 10 seconds`; the service case did not execute in that aborted run.
- API 30: service destruction/restoration passed. The first UI attempt encountered a cold-boot System UI ANR overlay and Back-key injection was denied because another application owned the window. After dismissing that system alert, the **unchanged UI assertions passed** in a targeted warm rerun, including all EQ/visualizer selectors, fullscreen, About's actual installed name/code, bundled licenses and output information. Test cleanup explicitly finishes its own activity before ActivityScenario's native idle wait.

Across the 13 distinct production cases, **12 passed and 1 known MP3 case was skipped**. The aborted API 28 UI attempt remains an emulator limitation, not an API 28 UI pass. These results do not establish physical-device performance or complete accessibility acceptance.

## Historical 0.5.0 verification

Version 0.5.0 uses JDK 21, Gradle 8.13, AGP 8.13.2, Kotlin 2.2.21, SDK 36 and Media3 1.11.1. The standard build command is:

```sh
source /workspace/.tooling/env.sh # prepared cloud machine only
./gradlew --no-daemon testDebugUnitTest lintDebug assembleDebug assembleRelease
./gradlew --no-daemon connectedDebugAndroidTest
```

The 0.5.0 unit run executed **42 cases: 42 passed, 0 failed, 0 skipped**. Coverage includes real Room/SQLite queries (including a synthetic 10,000-entry index), literal search and missing-source preservation, queue snapshots, DSP signal/headroom/bypass/channel behavior, preset persistence, pending-format activation, FFT accuracy/ring cancellation, AutoEq/LRC/ReplayGain parsing, smart rules, URI M3U8, AIFF extraction/seeking and widget state/clock persistence. The synthetic index test is not a real-device scan or latency benchmark.

Full API 28 regression completed at 2026-10-16 21:50 UTC: **13 cases, 12 passed, 1 skipped, 0 failed/errors**. The skipped case is the known MP3 decoder combination described below. The runner progress counter displayed 14/13 because it counted the ignored case twice; the JUnit XML contains the actual 13 cases. The full native result was saved before the subsequent targeted license-screen run.

Final debug and unsigned release builds passed. Lint passed with **0 errors and 19 warnings** (dependency recommendations, deliberate service export and API/style suggestions); no lint baseline was added. The targeted native UI rerun passed **1/1, 0 failed/skipped**, including scrolling to Settings, loading bundled license texts and returning to output information. Both APKs report version 0.5.0/code 5, contain the notices, and were checked to exclude INTERNET, ACCESS_NETWORK_STATE, RECORD_AUDIO and MANAGE_EXTERNAL_STORAGE permissions.

## Redesign and release-workflow checks

The release safety suite is `python3 -m unittest discover -s tools/tests -v` (six cases). It checks failed-build retries without a consumed version, immutable historical artifacts/text, wrong-key rejection, monotonic version selection and promotion rollback. New Android model cases exercise installed PackageManager metadata and real Room group/playlist projections, plus folder-prefix/count behavior.

Run the 13 production native cases on the isolated validation AVD. Connected test runners can uninstall their target APK during cleanup; do not point this command at a user's existing installation when checking data-preserving upgrades.

```sh
ANDROID_SERIAL=emulator-5558 ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.piccol.hifi.PlaybackIntegrationTest,com.piccol.hifi.ReactiveShaderTest,com.piccol.hifi.PlayerUiTest,com.piccol.hifi.ServiceLifecycleTest
```

The optional `UiScreenshotsTest` is a separate fixture-capture test, skipped unless explicitly enabled on `hifi-ui-validation` or `hifi-ui-validation-30`. See [the redesign report](docs/UI_REDESIGN.md) for reproduction, screenshots, emulator rendering limitations and executed outcomes. Small-screen, 1.5× text and landscape captures do not certify TalkBack, touch ergonomics or hardware performance.

## Native tests

The suite contains 13 cases across `PlaybackIntegrationTest`, `ReactiveShaderTest`, `PlayerUiTest` and `ServiceLifecycleTest`:

- Six original generated format fixtures (MP3, FLAC, WAV, AAC/M4A, Vorbis and Opus) play through the production MediaSessionService with advancing position and no decoder error.
- Actual PCM gain in all three EQ modes and tagged ReplayGain; imported AutoEq profile gain, embedded lyrics and actual AudioTrack route reporting.
- Queue transport/editing and persisted state; service destruction/recreation restores a paused queue and can resume. This does not simulate complete process death or force-stop.
- AIFF 16-bit/48 kHz and 24-bit/96 kHz playback/seeking; explicit ALAC unsupported feedback when the platform lacks its decoder.
- FLAC transitions preserve exact frame totals and reuse a single AudioTrack. A separate strict MP3 diagnostic measures boundary frames against an isolated decode.
- All three original ES 2.0 effects compile/render on Android EGL, with black silence and nonzero signal output.
- Three real AppWidgetHost sizes produce RemoteViews; widget PendingIntents and shared-controller actions change the existing session.
- Compose navigation through EQ/profile sheets, all six visualizer modes, fullscreen and output settings; corrupt-source error and recovery.

The API 28 emulator guest has Wi-Fi and mobile data disabled during final regression. No Internet permission is present. This is not prolonged physical airplane-mode acceptance. Tests use a debug-only, non-exported FileProvider restricted to private fixture storage. Fixtures are original generated sine tones, not real-world music/codec coverage; they are packaged only in the test APK.

### Known MP3 gapless limitation

On **API 28 with `OMX.google.mp3.decoder`**, sample-exact MP3 acceptance failed. Media3 1.11.1 improves the earlier 1.8.0 result, but diagnostics still observed a 551-frame terminal tail and a 22–551-frame transition discrepancy (~0.5–12.5 ms at 44.1 kHz). The strict assertion remains in the suite, with a JUnit assumption skip limited to that exact OS/decoder combination. Other combinations execute it normally. Force the known failure with:

```sh
ANDROID_SERIAL=emulator-5556 ./gradlew connectedDebugAndroidTest \
  '-Pandroid.testInstrumentationRunnerArguments.class=com.piccol.hifi.PlaybackIntegrationTest#mp3TransitionsPreserveExactBoundaryFramesOnSupportedDecoders' \
  -Pandroid.testInstrumentationRunnerArguments.verifyKnownMp3Gapless=true
```

Expected on that known decoder: assertion failure, not sample-exact certification. FLAC's passing frame test does not establish MP3 acceptance. Physical listening and other decoders remain unverified.

## Defects found during implementation

Native checks exposed and led to fixes for empty-buffer self-copy, pending mono/stereo processor configuration affecting undrained PCM, optimistic requested DSP state being mistaken for an audible plan, and ReceiverRestrictedContext preventing real widget PendingIntents from connecting to the session. The EQ response graph now calculates automatic headroom once per response curve rather than once for every plotted sample. Tests retain their signal assertions. UI automation was corrected to use real Back key delivery for a dialog rather than bypassing its window. The license-screen check initially queried an uncomposed off-screen item; scrolling the Settings list before querying it fixed the automation, and the rerun passed.

Software emulation has no KVM acceleration. Cold-start app/system ANRs, underruns and slow frames were observed, including during initial Phase 1 setup. They remain unresolved performance observations. Passing functional checks do not establish smooth startup, 60 FPS, low battery drain or uninterrupted hardware playback.

## Remaining physical/manual acceptance

| Area | Still required |
| --- | --- |
| Audio hardware | Bluetooth/wired/USB attach, route changes and unplug, headset buttons, incoming calls/focus, real DAC format and prolonged listening |
| Storage | Real removable SD loss/reinsert, varied SAF providers, grant/revoke, multiple volumes, unusual tags and large real scans |
| Background | Home/lock-screen/notification controls under restrictions; complete process kill, force-stop, reboot and restoration |
| Widgets | Physical launcher sizes/resizing, reboot, stale-host state after force-stop, Android 12+ background-start rules |
| Accessibility/UI | TalkBack, large text, rotations, light/album-derived themes and graph touch usability on hardware |
| Performance | Physical startup, sustained 30/60 FPS targets, thermal/battery/CPU measurements and real-library latency |
| Formats | Broad real-world codec corpus, ALAC on a supporting decoder, uncommon AIFF widths/rates, MP3 gapless on other decoders |
| Offline | Prolonged airplane-mode playback after relaunch on hardware |

Crossfade, CUE, portable filesystem M3U resolution remain unimplemented. Processing is 16-bit PCM; high-resolution source decoding is not 24-bit, exclusive or bit-perfect output.

## Reports and APKs

Runner reports are under `app/build/reports/tests/testDebugUnitTest/`, `app/build/reports/lint-results-debug.html` and `app/build/reports/androidTests/connected/debug/`; XML results are under `app/build/test-results/` and `app/build/outputs/androidTest-results/`. Check their timestamps and selected-device outcomes when reproducing. Reports/logs are generated and not committed.

`tools/release.py` explicitly selects a semantic bump, builds and verifies candidate APKs, then appends artifacts/history/checksums and records the successful version. The old `tools/package_apks.py` entry point now requires the same explicit options and forwards to this workflow; it no longer deletes or relabels historical APKs. See docs/RELEASE_WORKFLOW.md. The debug APK is installable for evaluation; the unsigned release needs owner signing. Toolchains, emulator images, credentials, caches and keystores stay outside version control. TLS/checksum verification was preserved during setup. Reusable environment setup instructions were saved separately for review; saving them does not demonstrate a fresh-machine run.

Final API 30 screenshot fixture run passed (1 test, 18 selected captures), including small-screen, large-text and landscape layouts.
