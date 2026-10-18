# UI redesign integration

The supplied redesign brief is preserved in REDESIGN_REQUIREMENTS.md. The five referenced screenshots were not attached, so the running app supplies the before-state. The official Poweramp, Oto Music, Symfonium and Musicolet URLs were requested, but the environment proxy rejected all four with HTTP CONNECT 403. External screenshot research is therefore unverified; no proprietary graphics/layout assets are copied. The implementation follows the concrete principles in the brief: focused artwork/transport, compact browsing, deliberate empty states and contextual audio controls.

## Plan and architecture

1. Shared typography, compact headers/tabs/navigation, neutral surfaces and reusable artwork/empty/settings components. Replace repeating subtitles and global refresh actions; retain one MediaController and service.
2. Put library controls inside its scrolling layout, add real album cards/artist counts and a presentation hierarchy over indexed folders. Use playlist covers and built-in smart collections. Reorganize Now Playing around artwork, metadata, one transport row and one secondary action row; integrate the mini-player above navigation.
3. Put EQ graph and bands together, move explanations into expandable panels, fix slider bounds and add axis labels. Make visualizer display primary, selectors horizontal and technical settings secondary. Group Settings and keep installed version under About.
4. Verify compilation/lint, meaningful unit and native regressions, original/generated-fixture screenshots at several sizes/font scales, and a data-preserving signed APK upgrade. Record exactly what was inspected and what remains unverified.

Audio engine, DSP math, FFT/shaders, Room schema and scoped storage remain the existing implementation. New appearance preferences are additive DataStore keys. Album artwork color is sampled locally; no online color/artwork service is introduced. New group/cover projections use existing Room tables without a migration. Missing art and empty libraries use original vector placeholders, never fake live music entries.

## Implemented screens and shared components

| Source | Change |
| --- | --- |
| `ui/DesignSystem.kt` | Shared typography, compact tabs, section headings, intentional empty states/settings rows and local artwork accent sampling |
| `ui/HiFiApp.kt` | Contextual headers, five compact destinations, real-progress mini-player, scrolling library controls, album/artist cards, folder hierarchy, playlist covers/smart collections, artwork-led player and grouped Settings |
| `ui/EqualizerScreen.kt` | Adjacent response graph/bands, readable logarithmic frequency and dB axes, scrollable 10/31-band controls, compact parametric editors, preset sheet and collapsible headroom/technical information |
| `ui/VisualizerScreen.kt` | Primary real-signal display, horizontal mode selection, compact footer, fullscreen overlay and secondary settings sheet |
| `ui/AboutSettings.kt` | Installed PackageManager version name/code under Settings → About |
| `ui/FolderNavigation.kt` | Presentation hierarchy over actual indexed folders, retaining access to songs in parent folders |
| `data/Database.kt`, `data/Preferences.kt`, `ui/PlayerViewModel.kt` | Additive cover/group projections, appearance preferences and lifecycle-aware group observation; Room schema remains v1 |

Existing lyrics, queue operations, favorite/playlist actions, AutoEq profiles, ReplayGain, source/output information, widgets and all six visualization modes retain their production implementations. Parametric numeric editing includes a zero-gain action. No playback, DSP, decoder or FFT algorithm was replaced.

## Screenshot reproduction

Use the separate `hifi-ui-validation-30` API 30 AVD, never an installed user's library. The capture test requires both its AVD name and `uiValidation=true`. Its generated FLAC tones and original geometric artwork live only in isolated test storage; inserted rows/playlists are removed in cleanup. They are not packaged as a demonstration library in the application.

```sh
source /workspace/.tooling/env.sh
ANDROID_SERIAL=emulator-5558 ./gradlew connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.piccol.hifi.UiScreenshotsTest \
  -Pandroid.testInstrumentationRunnerArguments.uiValidation=true
adb -s emulator-5558 pull /sdcard/Download/hifi-ui-validation/v3/. artifacts/ui/current/
```

Captures use the actual Android screen surface. During continuous playback, the test uses Android accessibility bounds and touch events with explicit Compose-clock frames rather than requiring animation quiescence. The validation AVD uses a software GLES backend; the application retains its production rendering and image loader. Cold System UI ANRs are tracked separately from application assertions. No screenshot is generated, composited or retouched to imitate a running UI.

## Visual review and iteration

The actual renders exposed two refinements beyond the first implementation: the EQ graph/band heights now derive from available content height so the mini-player does not consume the band panel's first viewport; landscape Now Playing places source details after the transport to keep primary controls prominent. Large-text navigation reserves a gutter around each label. Long EQ/control content remains vertically scrollable, and band/mode rows scroll independently horizontally.

The captured gallery is in [artifacts/ui](../artifacts/ui/README.md). The final API 30 capture test passed (one test, 18 selected screenshots), including 320 dp, 1.5× text and landscape. Earlier API 28 runs encountered System UI ANRs, a SurfaceControl system-server failure during resize/rotation stress and an ActivityScenario idle wait during capture cleanup. The capture helper now finishes its own activity after cleanup. These observations preclude a claim of smooth startup or complete hardware visual/accessibility acceptance.
