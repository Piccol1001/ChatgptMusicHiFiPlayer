# Three-section HiFi Player redesign

The new brief is preserved in [MINIMAL_REDESIGN_REQUIREMENTS.md](MINIMAL_REDESIGN_REQUIREMENTS.md). This iteration refines the existing 0.6.0 app. PlaybackService, ExoPlayer/MediaSession, DSP/filter math, FFT/shaders, library scanning, Room schema v1, playlist storage/rules, AutoEq, lyrics and widgets keep their existing implementations. No network permission or production fixture music is added.

## Navigation and layouts

Library, Playing and Audio are the only three primary destinations. Library contains Songs, Albums, Artists, Playlists and Folders; its overflow retains Genres, Favorites, Recently Added, Recently Played, Most Played and Rescan. Playlists have title search, create/import actions, artwork, export/rename/delete menus and all existing smart collections/rules. Permission onboarding is separate from an accessible but empty library. Song rows retain their menus and also support long press.

Audio contains independent Equalizer and Visualizer tabs. Its overflow exposes AutoEq/output profiles, actual output/USB information and advanced output settings. EQ defaults to the existing 10-band configuration for new users; stored modes and DSP values are retained. Exact gain entry, parametric editing, searchable presets and resets remain accessible. Preamp, headroom, ReplayGain and technical details live under Advanced controls. The visualizer uses existing PCM/FFT and all six modes, with fullscreen, color and analysis options retained. Tapping fullscreen reveals temporary mode/color/settings controls; exit stays available and controls remain visible while a menu or settings sheet is open.

Playing keeps artwork, confirmed source metadata, seeking, shuffle/previous/play/next/repeat, queue, lyrics, EQ, visualizer and audio information. The mini-player observes the existing session, appears above navigation (including Settings), and is omitted on full Playing or fullscreen visualization. Scrollable content receives Scaffold insets rather than drawing under it.

## Components and modified sources

| File | Responsibility |
| --- | --- |
| `ui/DesignSystem.kt` | Shared ScreenHeader, PlayerNavigation, LibraryControls and spacing tokens; neutral compact tabs and consistent typography |
| `ui/AppNavigation.kt` | Three-section route grouping and legacy/new intent/URI resolution |
| `MainActivity.kt`, `AndroidManifest.xml` | Consume navigation intents, retain notification/widget `open_player`, accept offline `hifi://` navigation |
| `ui/HiFiApp.kt` | Integrated library/playlists, Audio tabs/options, Back behavior, persistent session mini-player, long press and confirmed channel metadata |
| `ui/EqualizerScreen.kt` | Compact preset/enable row and collapsible advanced controls |
| `ui/VisualizerScreen.kt` | Meaningful idle state, available-height display sizing and tap-revealed fullscreen controls; rendering math unchanged |
| `ui/LicenseScreen.kt` | Compact local document list with one selectable notice loaded at a time |
| `ui/WidgetSettings.kt` | Reduce-motion description covers screen transitions as well as decorative animation |
| `PlayerUiTest.kt`, `NavigationEntryPointTest.kt`, `UiScreenshotsTest.kt`, `ServiceLifecycleTest.kt`, `RedesignModelTest.kt` | Navigation/entry-point contracts and real isolated emulator capture |

## Entry-point compatibility

Existing notification and widget `open_player=true` intents still open Playing. Equalizer and Visualizer aliases resolve to the corresponding Audio tab: `eq`, `equalizer`, `audio/equalizer`, `visualizer`, `audio/visualizer`. `route`, `screen` and `destination` string extras are accepted, along with explicit `open_equalizer`/`open_visualizer` flags. Examples: `hifi://equalizer`, `hifi://audio/visualizer`, `hifi://library/playlists`. Unknown routes leave normal launch behavior intact. Global queue, lyrics, profile and output overlays dismiss when a requested destination opens. No external content is fetched. Back exits fullscreen/settings/collection layers before returning to the previous relevant primary view; Audio shortcuts from Playing return to Playing.

## Design details and layout fixes

The shared palette uses #080A0E, #171B23 and #222834 surfaces, #F5F7FA / #A4ADBC text, and the existing customizable blue accent (#5AA6FF by default). Headers use 26 sp semibold text. Compact tabs have neutral unselected backgrounds, controls keep Material touch targets, and navigation labels use a separate 12 sp style. Existing AMOLED/dark-gray, artwork accent/gradient/corners and reduced-motion preferences remain available. Short 120 ms destination transitions honor reduced motion.

The audit found five competing primary destinations, duplicated audio headings, advanced EQ controls mixed into the first view, and old sheets remaining above a newly requested route. The refactor consolidates navigation and headers, collapses advanced controls and dismisses global overlays when navigating. Portrait player artwork now reserves space for the secondary action labels; EQ removes a redundant instruction line and gives numeric gain buttons descriptive accessibility labels. Large-text search hints stay on one line with ellipsis. The license list loads one real bundled notice at a time, avoiding layout of the complete notice corpus in one view.

## Validation

The host checks passed 46 unit cases and six release-workflow safety cases; lint reported 0 errors and 22 warnings. The isolated API 30 production regression passed 12/12 (750.497 s), covering MP3/FLAC and the other generated formats, strict MP3/FLAC transition frames, DSP/ReplayGain, AutoEq/lyrics, real widget actions, corrupt-file recovery, queue persistence and service recreation. The known API 28 MP3 decoder residual remains documented separately. The complete screenshot runner passed 1/1 (848.248 s). See the [27-image gallery](../artifacts/ui/minimal/README.md) and [testing record](../TESTING.md). Screenshots use only the disposable `hifi-ui-validation-30` AVD and opt-in `uiValidation=true` fixture runner. Connected test runners can uninstall their target: never use them on a user's existing installation. Real devices are still required for audio-route fidelity, TalkBack, touch ergonomics, battery and performance acceptance.

The final fullscreen hide/tap/reveal/exit assertions passed in `PlayerUiTest`. One following combined-run native widget-selection check timed out; its unchanged real-intent test passed in a fresh instrumentation process (76.001 s), including Android manifest resolution and reuse of the existing activity. This remains an unresolved emulator-test flake, rather than a claimed all-green combined run. Cold System/System UI ANRs were also observed. Physical-device startup, touch, TalkBack, route fidelity, battery and frame-rate acceptance remain unverified.

Screenshot preparation requires an empty disposable fixture installation. A focused attempt correctly rejected a restored service-test queue; only the isolated screenshot installation was recreated. Empty-state assertions remain enabled, and capture navigation now checks its selected primary section before advancing through the screen transition.

The final fresh focused capture passed (1/1, 590.455 s); seven affected images were refreshed and all 27 selected PNGs verified. Portrait secondary labels and standard EQ frequency labels now fit above the bottom controls. At 360 dp / 1.5× text, EQ requires vertical scrolling, with horizontal scrolling for category/mode/band lists. No screenshot pixels were edited.

The strengthened service-lifecycle case passed separately (58.273 s), explicitly verifying replay with no foreground MainActivity, the production service alive, queue restoration paused and subsequent playback progression. Short landscape EQ viewports require vertical scrolling to reach the graph/bands; no bottom bar overlaps the scrollable content. Hardware ergonomics remain an acceptance limit.

## Released artifacts

The verified UI-refinement release is **0.6.1 / versionCode 7**, built from clean source `634ff20176894e805b130a05317d8048b05efb34`. Embedded APK metadata, package, offline permission policy, certificate and all eight retained artifact checksums passed inspection. The release gates passed 46 unit cases and lint (0 errors, 22 warnings); the six workflow safety cases also passed. [Installable evaluation APK](../artifacts/apk/HiFiPlayer_v0.6.1_debug.apk) uses the unchanged debug/update certificate. The [unsigned release variant](../artifacts/apk/HiFiPlayer_v0.6.1_release-unsigned.apk) requires owner signing and is not installable. See [release inventory](../RELEASES.md) and [changelog](../CHANGELOG.md). Historical APKs and ledger entries were retained unchanged.

The retained API 28 installation updated from 0.6.0/code 6 to 0.6.1/code 7 using `adb install -r`. All seven regular private database/data files had identical hashes before and after the update, checked before relaunch. No uninstall, data clear or fixture seeding was performed on the retained installation. Physical-device upgrades remain unverified.

The updated installation also displayed **0.6.1 (code 7)** in Settings → About, verified through native accessibility navigation. Its cold software-emulator launch first exceeded the 10.91-second activity wait and showed a System UI ANR over the rendered Library. System UI recovered after Wait; the app then responded to Settings/About navigation. This does not certify startup performance or System UI stability.
