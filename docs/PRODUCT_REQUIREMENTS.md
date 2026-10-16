# CODEX DEVELOPMENT PROMPT
# Project: Offline Hi-Res Music Player for Android

## 1. Project Objective

Develop a complete, native Android music player focused on high-quality local audio playback, advanced equalization, professional audio visualization, and a modern user interface.

The application must be fully functional offline, with all music library information, playlists, settings, presets, and user preferences stored locally.

The attached foobar2000 screenshot is the main functional reference, particularly for its playlist management, multi-band equalizer, spectrum analyzer, and detailed audio information.

However, do not copy its outdated desktop-style UI. Reinterpret the interface using a modern Android design with Material 3, clear navigation, smooth animations, touch-friendly controls, and dark-theme aesthetics.

**Core requirements:**

- Native Android application written in Kotlin.
- Fully offline music playback.
- No account or registration.
- No cloud synchronization.
- No advertising or analytics.
- Read music files from internal storage and supported SD card locations.
- Support MP3, FLAC, WAV, AAC/M4A, OGG Vorbis, Opus, ALAC, and AIFF.
- Advanced 10-band, parametric, and 31-band equalizers.
- Large, categorized equalizer preset library.
- Six real-time audio visualization modes.
- Android home-screen widgets.
- Gapless playback, ReplayGain, synchronized lyrics, and headphone AutoEq profiles.
- Support external audio devices and provide USB DAC information.

Prioritize playback reliability, sound quality, accurate audio processing, low battery consumption, and a consistent user experience.

## 2. Project Setup and Architecture

First inspect the existing repository to determine whether it contains an Android application.

If the repository already contains an Android project, preserve the existing project structure and integrate the music player without damaging unrelated features.

If the repository is empty, create a new native Android project.

Use the following technology stack:

| Component | Technology |
|---|---|
| Programming language | Kotlin |
| Interface | Jetpack Compose |
| Design system | Material 3 |
| Architecture | MVVM with repository/domain separation |
| Playback engine | AndroidX Media3 ExoPlayer |
| Background playback | MediaSessionService |
| Library discovery | MediaStore and Storage Access Framework |
| Local database | Room |
| Preferences | DataStore |
| Dependency injection | Hilt, if appropriate |
| Equalizer | Custom PCM audio-processing engine |
| Visualizations | DSP/FFT with Compose Canvas or accelerated rendering |
| Widgets | Jetpack Glance and Android AppWidget APIs |
| Asynchronous tasks | Kotlin Coroutines and Flow |

Use a currently supported Android SDK configuration with a reasonable minimum SDK, preferably API 26 or newer. Select mutually compatible stable dependencies and document the chosen versions.

Organize the project into clear modules or packages for:

- Audio engine and playback service
- Music library and storage access
- Audio metadata and artwork
- Equalizer and audio DSP
- Visualization engine
- Playlists and playback queue
- Lyrics
- Widgets
- Settings and persistence
- Shared UI components

Maintain a single authoritative playback state that the app UI, notification, lock screen, and widgets all observe.

Keep processing, storage, and rendering code separate. Avoid placing audio-processing logic directly inside Compose screens.

## 3. Audio File and Music Library Support

### 3.1 Supported audio formats

Support the following formats:

**Required:**
- MP3
- FLAC
- WAV/PCM
- AAC/M4A
- OGG Vorbis
- Opus

**Extended:**
- ALAC / Apple Lossless
- AIFF

Use Media3's supported decoder paths wherever possible. For formats that are not supported reliably on Android devices, integrate a suitable compatible decoder or extraction component.

Do not assume that every Android device supports every codec natively.

If a required extended format cannot be implemented immediately, clearly identify the missing decoder and continue implementing the remaining functionality. Do not falsely label an unsupported format as functional.

Support common 16-bit, 24-bit, and higher-resolution audio files where the decoder permits.

Display accurate source-file characteristics without claiming that playback is bit-perfect merely because the source file is high resolution.

### 3.2 Local storage access

Implement automatic music discovery through Android MediaStore with appropriate runtime permissions.

Also provide a manual folder selection option through the Android Storage Access Framework.

Features:
- Scan device music.
- Browse by storage folder.
- Add multiple music directories.
- Detect available SD card media.
- Refresh music library manually.
- Automatically reflect detectable media changes.
- Avoid duplicate entries representing the same file.
- Handle renamed, moved, missing, and deleted files gracefully.
- Persist access to user-selected folders where Android permits.

Use scoped-storage-compliant APIs.

Do not request unrestricted all-files access unless absolutely necessary and justified.

Handle Android 13+ audio permissions correctly and support appropriate earlier Android permission behavior.

Never alter, delete, move, or overwrite original audio files during ordinary library operations.

### 3.3 Library organization

Provide the following navigation categories:

1. All Songs
2. Albums
3. Artists
4. Genres
5. Playlists
6. Folders
7. Favorites
8. Recently Added
9. Recently Played
10. Most Played

Provide sorting by title, artist, album, track number, duration, date added, file format, and file size.

Add instantaneous search and practical filters for file type, codec, and other available metadata.

Library entries should contain title, artist, album, album artist, track/disc number, genre, duration, and album artwork when available.

Use embedded metadata and local extraction. Provide meaningful fallbacks when tags or artwork are missing.

Support large music libraries without loading the entire library into the UI simultaneously.

## 4. Music Playback Engine

Implement reliable playback using Media3 ExoPlayer integrated with a MediaSessionService.

### 4.1 Basic playback

Required controls:

- Play / Pause
- Previous / Next
- Seek and scrubbing
- Repeat Off / All / One
- Shuffle
- Play Next
- Add to Queue
- Remove from Queue
- Reorder Queue
- Save Queue as Playlist

Playback must continue when the user navigates away from the app, locks the screen, or uses another application.

Support notification, lock-screen, Bluetooth headset, and compatible external media controls.

Restore the last queue, current track, playback position, and applicable settings after normal app termination.

### 4.2 Playback quality features

**Gapless playback**

Support continuous playback of adjacent compatible tracks without an unintended pause or inserted silence where the playback pipeline allows accurate gapless transitions.

Test with continuous live albums and tracks containing encoder delay/padding metadata.

**Optional crossfade**

Provide a selectable crossfade duration such as 0–12 seconds. Implement only when the playback pipeline supports reliable overlapping audio.

Do not let crossfade compromise gapless album playback. Disable crossfade by default.

**ReplayGain**

Implement ReplayGain handling for compatible metadata, including:
- Track gain
- Album gain
- Associated peak information where available
- ReplayGain enable/disable
- Track versus album mode
- Adjustable preamp
- Clipping prevention

Do not confuse ReplayGain with dynamic range compression.

**Playback speed**

Provide playback speed control with a default of 1.0x.

**Audio focus**

Handle incoming calls, notifications, other playback applications, headphones disconnecting, and Bluetooth disconnections appropriately.

Prevent unintended playback through the phone speaker after wired or Bluetooth headphones disconnect.

### 4.3 Audio information panel

Provide a detailed information panel for the currently playing track.

Example:

FLAC | 24-bit | 96 kHz | Stereo

Display, when available:
- Audio codec
- Container/file extension
- Sample rate
- Bit depth
- Channel count
- Bitrate
- Duration
- File size
- File location
- Source-file format
- Current playback/output format where reliably measurable
- Active DSP processing

Clearly distinguish decoded/source audio format from actual output-device format.

Do not claim lossless, bit-perfect, native DSD, or high-resolution hardware output without reliable verification.

## 5. Modern UI/UX Requirements

Create a modern, responsive, dark-theme Android interface inspired by the attached foobar2000 screenshot.

The design should feel like an advanced music application, not a technical engineering control panel on every screen.

### 5.1 Visual design

Use:
- Deep black and dark charcoal backgrounds
- High-contrast text
- Clean typography
- Subtle separators
- Consistent spacing
- Rounded cards where appropriate
- Minimal unnecessary borders
- Touch-friendly buttons
- Smooth and restrained animations
- Customizable accent colors
- Material 3 components

Default accent color: electric blue, inspired by the sliders in the reference screenshot.

Allow optional album-art-based accent colors.

Support light/dark themes if practical, but prioritize a polished dark theme.

### 5.2 Navigation

Use five primary destinations:

1. Library
2. Playlists
3. Now Playing
4. Equalizer
5. Visualizer

Provide Settings through a top-level settings action.

A persistent mini-player should appear at the bottom of applicable screens, above the navigation bar.

### 5.3 Now Playing screen

Display:
- Large album artwork
- Song title
- Artist
- Album
- Playback progress
- Elapsed and total duration
- Playback controls
- Shuffle and repeat
- Favorite button
- Queue shortcut
- Lyrics shortcut
- Audio information shortcut
- Equalizer shortcut
- Visualizer shortcut

Allow the visualizer to appear behind or around the artwork when activated.

Provide an option to show a compact technical playback information strip.

### 5.4 Equalizer screen

The equalizer should resemble a professional audio interface with clearly separated frequency sliders, but use a responsive touch-friendly layout.

Features:
- Frequency labels
- Gain labels
- Preamp
- Enable/disable switch
- Preset selector
- Reset controls
- Save Custom Preset
- Import/Export
- Clipping indicator where technically supported
- Scrollable advanced EQ bands on smaller devices

Avoid tiny sliders that are difficult to adjust.

### 5.5 Playlist interface

Support manual playlists and smart playlists.

Provide:
- Create/rename/delete playlist
- Add or remove tracks
- Drag to reorder
- Sort playlist
- Playlist search
- Import/export M3U and M3U8 where feasible
- Playlist cover images
- Playlist track count and total duration

Do not delete original audio files when deleting a playlist.

## 6. Advanced Equalizer System

Implement three independently selectable equalizer modes.

### Mode A: 10-Band Graphic Equalizer

Use the following nominal center frequencies:

31 Hz, 62 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz.

Provide configurable gain in a safe practical range, such as -12 dB to +12 dB.

Allow exact adjustments using sliders and numeric entry.

### Mode B: Parametric Equalizer

Provide a minimum of 10 configurable parametric filters, subject to reasonable performance limits.

For each filter, expose:
- Center/cutoff frequency
- Gain
- Q / bandwidth
- Filter type
- Enabled state

Support:
- Peaking/bell
- Low shelf
- High shelf
- Low pass
- High pass

Provide an interactive frequency-response graph.

Where practical, allow dragging control points to change gain and frequency.

### Mode C: 31-Band Advanced Graphic Equalizer

Implement a standard one-third-octave layout using approximately:

20, 25, 31.5, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800 Hz, 1, 1.25, 1.6, 2, 2.5, 3.15, 4, 5, 6.3, 8, 10, 12.5, 16, and 20 kHz.

The interface must remain practical on a phone screen through horizontal scrolling, zooming, grouping, or another suitable interaction design.

### DSP implementation requirements

Do not implement purely decorative equalizer sliders.

All EQ modifications must actually affect the decoded audio.

Use proper digital filters with stable coefficients, suitable sample-rate handling, and real-time-safe processing.

Avoid unnecessary allocations in the real-time audio path.

Apply preamp gain correctly and provide appropriate headroom to reduce clipping.

Smooth parameter changes to avoid audible clicks.

Implement a reliable DSP bypass.

Ensure EQ settings persist between sessions.

Document Android device limitations, audio offload limitations, and any conditions in which custom PCM processing cannot be applied.

Do not falsely claim system-wide equalization. This equalizer should primarily process music played through this application.

## 7. Equalizer Preset Library

Build a large built-in preset system with a target of at least 50 useful presets.

Organize them into categories.

### Core

Flat, Reference, Warm, Bright, Vocal Clarity, Bass Boost, Sub-Bass, Treble Boost, Loudness, Smooth, Punchy, Airy, Balanced, V-Shaped, U-Shaped.

### Genres

Pop, Rock, Hard Rock, Metal, Alternative, Indie, Classical, Orchestra, Jazz, Blues, Acoustic, Folk, Country, EDM, House, Deep House, Techno, Trance, Dubstep, Drum & Bass, Hip-Hop, Trap, R&B, Funk, Reggae, Latin, K-Pop, J-Pop, Lo-Fi, Chillout, Ambient, Synthwave.

### Listening profiles

Headphones, In-Ear Monitors, Phone Speaker, Small Speaker, Car Audio, Night Listening, Podcast, Gaming, Live Concert, Vintage.

Use documented, technically plausible gain curves for these presets.

Do not assign random EQ values simply to populate the menu. Flat and Reference should have a genuinely neutral response.

Implement:
- Preset search
- Favorites
- Recent presets
- Preset preview
- Save custom preset
- Rename
- Duplicate
- Delete custom preset
- Restore defaults
- Import/export custom presets as local files

Apply presets across different EQ modes using an appropriately designed mapping or interpolation strategy. Do not simply discard frequency detail without warning.

## 8. AutoEq Headphone Correction

Support importing headphone correction profiles from the open-source AutoEq ecosystem.

Required:
- Import locally stored AutoEq text configuration files.
- Parse supported GraphicEQ frequency/gain pairs.
- Parse compatible parametric filter definitions when available.
- Display parsed filters before applying.
- Apply correction through the app's DSP engine.
- Allow editing imported profiles.
- Save multiple headphone profiles locally.
- Assign preferred profiles to identified audio outputs where reliable device identification is available.

The app must not require internet access to use AutoEq.

Do not include remote headphone-profile fetching.

If profiles are bundled with the application, check licenses, data provenance, attribution requirements, and package size.

Warn about unusually high positive gain or clipping risk.

## 9. Six Real-Time Audio Visualizers

Implement six visualization modes.

**All visualizations must use actual audio data, not randomly generated animations unrelated to playback.**

### Visualizer 1: FFT Spectrum Analyzer

This is the highest-priority visualizer and must resemble the bottom spectrum display in the foobar2000 screenshot.

Features:
- Adjustable FFT resolution
- Linear/logarithmic frequency axis
- Configurable bar count
- dB scale
- Peak hold
- Peak decay
- Adjustable smoothing
- Dynamic frequency labels
- Multiple color themes
- Full-screen mode

### Visualizer 2: Oscilloscope

Show the audio waveform over time.

Provide:
- Stereo/mono views
- Adjustable time scale
- Adjustable amplitude scale
- Line color options
- Full-screen mode

### Visualizer 3: Stereo VU/Peak Meter

Display independent left/right channel levels.

Provide:
- Peak hold
- Peak decay
- Level scales
- Optional clipping indication when the monitored signal permits it

### Visualizer 4: Spectrogram

Display a scrolling time-frequency visualization.

Provide:
- Time-based scrolling
- Frequency axis
- Color intensity mapped to signal energy
- Adjustable resolution
- Log/linear frequency options

### Visualizer 5: Circular Spectrum

Render FFT frequency bands radially around a circle.

Options:
- Album artwork at center
- Bar count
- Rotation effects
- Smoothing
- Color themes

### Visualizer 6: MilkDrop-Style Animated Visualizer

Provide advanced music-reactive full-screen animation.

Investigate projectM as an optional visualization engine.

If projectM is integrated:
- Review its license and Android integration requirements.
- Include only properly licensed presets and dependencies.
- Support preset switching.
- Support randomization and favorites.
- Keep presets available offline.

If direct projectM integration is not viable in the first implementation, build an original music-reactive shader visualizer and explicitly document that it is not projectM.

Do not claim thousands of visualization presets unless they are actually included and functional.

### Visualization performance

Use the decoded PCM stream or a suitable internal audio tap whenever practical.

Avoid requesting microphone permissions just to visualize the app's own audio.

If a fallback Android visualization API requires additional permissions, explain and handle the requirement separately.

Support:
- 30 FPS and 60 FPS options
- Quality settings
- Automatic suspension when the visualization is not visible
- Efficient FFT calculations
- Low allocation overhead
- Orientation changes
- Full-screen mode

Audio processing and visualization rendering must not block playback.

## 10. Android Home-Screen Widgets

Provide three responsive home-screen widgets.

### Widget A: Compact

Suggested size: 2×2 grid cells.

Display:
- Album artwork
- Play/Pause
- Next track

### Widget B: Standard

Suggested size: 4×2 grid cells.

Display:
- Album artwork
- Song title
- Artist
- Previous
- Play/Pause
- Next

### Widget C: Expanded

Suggested size: 4×4 grid cells.

Display:
- Larger artwork
- Title and artist
- Playback progress
- Previous/Play/Next
- Shuffle
- Repeat
- Shortcut to open the full player

Implement widgets using supported Android widget APIs and Jetpack Glance where appropriate.

Widgets must:
- Reflect playback state.
- Control the existing media session.
- Avoid starting duplicate playback engines.
- Handle process recreation.
- Adapt to different widget sizes.
- Support transparent and dark backgrounds.
- Avoid unnecessary background polling and battery consumption.

Use event-driven widget updates whenever possible.

## 11. Offline Lyrics

Implement fully offline lyric support.

Support:
- Embedded lyrics where available
- External .lrc files
- Plain text lyrics
- Time-synchronized highlighting
- Auto-scroll
- Manual scrolling
- Font-size customization

Allow users to link a local lyrics file to a song.

Do not use an online lyrics service.

Lyrics must remain available without an internet connection.

## 12. USB DAC and External Audio Output

Detect available audio-output devices using Android-supported APIs.

Provide an Audio Output information screen.

Show available information such as:
- Current output route
- Wired / Bluetooth / Speaker / USB
- Audio device name where exposed
- Supported sample rates where exposed
- Supported channel counts and encodings where exposed
- Source audio characteristics
- Active audio processing

Provide selectable output modes such as:
- Normal Playback
- DSP Playback
- Experimental High-Fidelity / Direct Output, only if technically supported

Do not promise bit-perfect output unless the actual Android audio route supports it and the implementation can verify the relevant conditions.

When processing such as EQ, ReplayGain, or crossfade changes the audio samples, indicate that bit-perfect playback is not active.

Do not create a fake exclusive DAC mode.

Handle DAC disconnection and output-route changes safely.

## 13. Settings

Organize settings into:

### Audio
- Playback behavior
- Gapless/crossfade
- ReplayGain
- Playback speed
- Audio output
- DSP behavior

### Equalizer
- Default EQ mode
- Presets
- Preamp
- AutoEq profile
- Output-specific profiles

### Visualizer
- Default mode
- FPS
- Quality
- FFT settings
- Colors
- Peak hold/smoothing

### Library
- Music directories
- Scan library
- Metadata display
- Missing-file handling
- Playlist management

### Appearance
- Dark theme
- Accent color
- Album-art colors
- Animation intensity
- Compact/detailed views

### Privacy & Data
- Export/import settings
- Reset app preferences
- Clear playback history
- Clear locally cached metadata
- Rebuild database

### About
- Version
- Open-source licenses
- Audio-engine information

## 14. Privacy, Security, and Offline Requirements

The application must be privacy-first and fully offline.

Mandatory requirements:
- Do not declare INTERNET permission for the final application.
- Do not use advertising SDKs.
- Do not use analytics SDKs.
- Do not add Firebase or cloud synchronization.
- Do not require login.
- Do not transmit music metadata or personal information.
- Store playlists, settings, history, and EQ presets locally.
- Use scoped storage.
- Request only necessary permissions.
- Handle permissions being denied or revoked.
- Avoid exposing private file paths in unnecessary logs.
- Avoid copying large music files into app storage unless explicitly needed.
- Do not request microphone access unless an optional feature genuinely requires it and the user explicitly enables that feature.

The application must work in airplane mode after installation.

External dependencies may be downloaded by the developer's build system during development, but runtime use must not depend on internet connectivity.

## 15. Reliability, Accessibility, and Performance

Implement:
- Proper lifecycle management
- Audio focus management
- Safe foreground playback service behavior
- Media session integration
- Battery-efficient background playback
- Robust error handling
- Recovery from corrupt/unreadable music files
- Graceful recovery from storage removal
- Responsive UI during library scans
- Large-library performance
- Proper resource cleanup
- Accessible content descriptions
- Scalable text
- Adequate touch targets
- Screen reader support where practical
- Support for portrait and landscape layouts

Avoid blocking the main thread for file scans, metadata parsing, FFT processing, waveform computation, or database operations.

## 16. Development Phases and Acceptance Criteria

Implement the application in manageable phases.

Do not attempt all complex systems simultaneously before the basic player works.

### PHASE 1 — Core Application and Music Playback

Implement:
- Android project and architecture
- Dark Material 3 interface
- Local file permissions
- MediaStore library scanning
- Folder selection
- Music library
- Metadata and album artwork
- Playback engine
- Background playback
- Queue management
- Basic playlists
- Notification and lock-screen controls
- Basic settings

**Acceptance criteria:**
- App builds and launches.
- User can authorize music access.
- User can browse music files.
- MP3 and FLAC files can play successfully.
- Core supported formats are tested.
- Play, pause, next, previous, seek, shuffle, and repeat work.
- Background playback works with the screen locked.
- Bluetooth media controls function.
- App operates without internet.
- Playback does not create duplicate services.
- Playback queue and state restore correctly.
- Existing unrelated repository functionality remains intact.

### PHASE 2 — Equalizer and Audio DSP

Implement:
- 10-band EQ
- Parametric EQ
- 31-band EQ
- Functional DSP
- Preset library
- Custom preset persistence
- Import/export
- Preamp
- Frequency-response graph
- ReplayGain
- Basic clipping/headroom protection

**Acceptance criteria:**
- Every band genuinely changes the corresponding audio frequency region.
- Bypass produces no unintended EQ changes.
- Changing EQ does not cause obvious clicks or playback interruptions.
- EQ presets load correctly.
- User-created presets survive app restart.
- ReplayGain values are interpreted correctly from supported metadata.
- Real-time audio processing remains stable under normal use.
- The app accurately indicates active processing.

### PHASE 3 — Visualizer Engine

Implement:
- Spectrum Analyzer
- Oscilloscope
- Stereo VU/Peak Meter
- Spectrogram
- Circular Spectrum
- MilkDrop-style visualization
- Full-screen visualizer
- Performance settings

**Acceptance criteria:**
- All six modes are selectable.
- Visualizations respond to actual playback audio.
- Spectrum analyzer frequency positions are accurate within expected FFT resolution.
- Animations stop or reduce work when not visible.
- Visualizers do not cause regular playback stutter.
- Settings persist.
- Visualizer quality can be adjusted to reduce battery use.
- No unnecessary microphone permission is requested.

### PHASE 4 — Advanced Library and Audio Features

Implement:
- ALAC and AIFF handling
- AutoEq import
- Synchronized lyrics
- Smart playlists
- Advanced metadata
- Gapless validation
- Optional crossfade
- Detailed playback information
- USB DAC and audio route information

**Acceptance criteria:**
- Required extended audio formats are supported through tested decoder paths, or unresolved implementation gaps are explicitly documented.
- AutoEq profiles import and apply correctly.
- External and embedded lyrics display correctly.
- Gapless-compatible files can play continuously.
- Output information accurately reflects what Android exposes.
- Unsupported DAC capabilities are not fabricated.
- Playlist import/export works as specified.

### PHASE 5 — Widgets and Final Polish

Implement:
- Compact widget
- Standard widget
- Expanded widget
- Widget themes
- UI refinements
- Accessibility improvements
- Performance optimization
- Error handling
- App icon and splash screen
- Final integration tests

**Acceptance criteria:**
- All widgets can be placed on the launcher.
- Widget controls operate the existing player.
- Widgets reflect playback changes.
- Widgets recover after normal process recreation.
- Music playback remains stable during extended use.
- Library navigation remains responsive.
- App functions in airplane mode.
- No Internet permission or unnecessary data collection exists.
- Release build succeeds.
- No critical crashes or data-loss issues remain.

## 17. Testing Requirements

Write automated tests where practical for:
- Queue behavior
- Shuffle and repeat
- Playlist persistence
- Metadata parsing
- AutoEq parsing
- EQ coefficient calculations
- Preset serialization
- ReplayGain calculations
- Lyrics timestamp parsing
- Storage indexing logic

Test on multiple Android API levels and device configurations where available.

Provide manual testing procedures for:
- MP3/FLAC/extended format playback
- Headphones and Bluetooth
- USB DAC connection/disconnection
- Background playback
- Permission denial
- SD card removal
- Corrupt files
- App force-stop/relaunch
- Widgets
- Screen rotation
- Airplane mode
- Large music libraries

Do not state that a hardware or emulator test passed unless it was actually performed.

## 18. Instructions for Codex

Follow these development rules:

1. Inspect the repository before modifying it.
2. Establish the project architecture before implementing features.
3. Use compatible, maintained dependencies.
4. Implement actual working functionality; do not use fake data or decorative controls as substitutes.
5. Keep components reusable and clearly separated.
6. Preserve unrelated existing files and features.
7. Do not silently remove or simplify requested functionality.
8. Document any feature blocked by platform restrictions or unavailable decoders.
9. Implement graceful fallbacks where appropriate.
10. Keep advanced features optional when they affect performance.
11. Avoid unnecessary runtime permissions.
12. Provide proper error states and loading indicators.
13. Add relevant tests for implemented logic.
14. Run Gradle build, lint, and tests where supported by the environment.
15. Fix discovered compilation and integration errors before declaring a phase complete.
16. Do not claim that untested features are verified.
17. Maintain a clear record of completed and pending functionality.

Create and maintain:
- README.md with setup, supported formats, and usage
- DEVELOPMENT_PLAN.md listing development phases
- FEATURE_STATUS.md showing implemented, partially implemented, and pending features
- TESTING.md with validation results and outstanding testing needs

If available, use the supplied foobar2000 screenshot to guide the equalizer and spectrum analyzer layouts while following the modern UI requirements defined above.

**Execution instruction:**

Start by inspecting the current repository and creating an implementation plan. Then implement Phase 1, rather than stopping after generating a plan. Proceed through later phases when feasible, verifying each phase before moving forward.

At the end of each phase, report:
- Features implemented
- Files created or changed
- Build and test results
- Known limitations
- Features remaining
- Next implementation phase

The final goal is a polished, fully offline, high-quality Android music player whose music library, playback engine, equalizer, visualizers, widgets, and advanced audio functionality are real, integrated, and reliable.
