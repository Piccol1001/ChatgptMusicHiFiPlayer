# Changelog

Semantic Versioning: MAJOR = significant architectural changes; MINOR = new functionality; PATCH = fixes/UI refinements. Dates below use the release evidence in RELEASES.md. Historical development phases were not separate releases. New verified release entries are inserted after the marker; older text is preserved.

<!-- releases -->

## 0.6.0 — 2026-10-18

Android versionCode: `6`.

### New features

- Verified release workflow with explicit semantic-version selection, monotonic Android version codes and append-only artifact history.
- Local artwork-derived accent, black/dark-gray themes, artwork gradient/corner controls and indexed folder-tree browsing.

### Bug fixes

- Failed build attempts no longer advance the recorded release version.
- Release packaging verifies embedded APK metadata, checksums and the existing update certificate without deleting older APKs.

### UI improvements

- Complete original redesign of navigation, library/album and playlist cards, artwork-led Now Playing, EQ controls/axes, visualizer layout and grouped Settings.
- Persistent compact mini-player with real progress and a single main transport row.
- Settings → About → App Version reads installed package metadata; the main header contains no version.
- Available-height EQ sizing, compact landscape transport and spaced large-text navigation labels.


## 0.5.0 — 2026-10-17

Android versionCode: `5`.

### New features

- Actual EQ/ReplayGain, six PCM visualizers, offline AutoEq and lyrics, AIFF, smart playlists/URI M3U8, output information and three widgets.

### Bug fixes

- PCM empty-buffer and pending-format handling, actual DSP plan observation, widget service binding and repeated EQ graph headroom computation.

### UI improvements

- Equalizer/visualizer/profile/output controls, widget appearance, reduced motion and offline license screen.

## 0.1.0 — 2026-10-16

Android versionCode: `1`.

### New features

- Offline library, playlists/favorites/history, single Media3 service/session, local scanning and core playback.

### Bug fixes

- Literal search escaping and asynchronous queue persistence verification.

### UI improvements

- Initial Compose library and player screens.

