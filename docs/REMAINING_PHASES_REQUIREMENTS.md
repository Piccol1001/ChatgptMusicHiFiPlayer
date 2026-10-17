# CODEX MASTER DEVELOPMENT PROMPT
# Android Offline Hi-Res Music Player — Phases 2 to 5

## PROJECT CONTEXT

We are continuing development of an existing native Android offline music player.

Phase 1 has already been implemented or is currently undergoing final verification.

The application is designed as a modern, dark-themed, high-quality local music player inspired by foobar2000, particularly its advanced equalizer, audio information display, and spectrum visualizer.

This prompt covers four remaining development phases:

- Phase 2: Advanced Equalizer and Audio DSP
- Phase 3: Six Real-Time Audio Visualizers
- Phase 4: Advanced Audio, AutoEq, Lyrics and USB DAC
- Phase 5: Home-Screen Widgets, UI Refinement, Performance and Final Testing

**IMPORTANT: Preserve the existing application. Do not start a new Android project, replace the existing playback engine unnecessarily, or recreate functionality already implemented in Phase 1.**

The entire application must remain fully offline, privacy-focused, and capable of playing local music without requiring network connectivity.

---

# GLOBAL DEVELOPMENT RULES

## A. Inspect the Existing Codebase

Before modifying anything:

1. Analyze the existing project architecture.
2. Locate the Media3 ExoPlayer instance and MediaSessionService.
3. Understand the playback queue and state management.
4. Identify the existing Room database and DataStore preferences.
5. Inspect music scanning, metadata extraction, and playlist functionality.
6. Identify existing Compose UI components and navigation.
7. Check Gradle dependencies and Android API compatibility.
8. Identify any existing EQ, visualizer, lyric, or widget implementations.

Create a short implementation plan describing how Phases 2–5 will integrate with the existing architecture.

Do not create duplicate playback engines or independent playback sessions for the visualizer, widgets, or equalizer.

## B. Development Principles

- Language: Kotlin.
- UI: Jetpack Compose and Material 3.
- Playback: Existing Media3 ExoPlayer.
- Background service: Existing MediaSessionService.
- Preferences: DataStore.
- Database: Room.
- Architecture: MVVM with clear separation of responsibilities.
- Follow the minimum SDK already established by the project.
- Prefer maintained, compatible Android libraries.
- Avoid unnecessary third-party dependencies.
- Use actual working audio processing, not simulated functionality.
- Keep all user settings and presets locally.
- Do not add INTERNET permission, telemetry, advertising, or online accounts.
- Preserve all Phase 1 functionality.

## C. Execution Requirements

Implement the phases sequentially.

For every phase:

1. Develop the core implementation.
2. Integrate it with the existing player.
3. Create the required UI.
4. Persist relevant settings.
5. Implement tests.
6. Compile the Android project.
7. Fix compilation and integration errors.
8. Report completed features and limitations.
9. Continue to the next phase only when the current phase meets its acceptance criteria.

Do not mark a feature as completed simply because its user interface exists.

If the environment cannot perform hardware-dependent testing, explicitly mark those tests as pending rather than claiming success.

---

# PHASE 2 — ADVANCED EQUALIZER AND AUDIO DSP

## Objective

Create a professional-grade, real-time audio-processing system fully integrated with the existing music playback engine.

The equalizer must support three modes:

1. 10-Band Graphic Equalizer
2. Parametric Equalizer
3. 31-Band Graphic Equalizer

All three must modify the actual audio signal.

## 2.1 Audio DSP Architecture

Implement an audio-processing layer that integrates with Media3 using an appropriate supported PCM processing path.

Investigate Media3 AudioProcessor and AudioSink integration rather than building an unrelated player.

Create logical components such as:

- AudioDspEngine
- EqualizerProcessor
- ParametricFilterProcessor
- ReplayGainProcessor
- PreampProcessor
- DspSettingsRepository
- EqualizerPresetRepository

Names may be adapted to the existing project conventions.

The DSP system must:

- Process decoded PCM audio in real time.
- Support the relevant PCM encodings and sample rates exposed by the playback pipeline.
- Handle sample-rate changes.
- Preserve channel integrity.
- Avoid blocking the playback thread.
- Avoid expensive allocations during active processing.
- Avoid audible clicks when changing parameters.
- Provide complete bypass.
- Support configuration persistence.
- Expose processing state to the UI.
- Provide a suitable downstream PCM tap for visualizers in Phase 3.

Ensure that custom DSP is not silently bypassed by hardware offloading or an incompatible output path.

Where required, disable offload or select an appropriate PCM output path when DSP is enabled.

Do not claim bit-perfect playback when DSP processing is active.

## 2.2 Mode A — 10-Band Equalizer

Implement the following center frequencies:

31 Hz, 62 Hz, 125 Hz, 250 Hz, 500 Hz, 1 kHz, 2 kHz, 4 kHz, 8 kHz, 16 kHz.

Each band must support:

- Gain range: -12 dB to +12 dB
- Fine adjustment
- Numeric gain display
- Frequency label
- Reset individual band
- Reset all bands

### UI requirements

Display ten vertical sliders arranged horizontally.

Use a dark interface with electric-blue slider indicators, inspired by the original foobar2000 screenshot.

Ensure that the design adapts to smaller phone screens.

Provide an overall gain/preamp control and a visible equalizer enable/disable toggle.

## 2.3 Mode B — Parametric Equalizer

Implement at least ten configurable filters.

Each filter must have:

- Enable/disable
- Frequency
- Gain
- Q factor / bandwidth
- Filter type

Support:

- Peaking/Bell
- Low Shelf
- High Shelf
- Low Pass
- High Pass

Use appropriately designed, numerically stable digital filters, such as biquad IIR filters.

### Frequency-response graph

Implement an interactive frequency-response graph.

Requirements:

- Frequency scale from approximately 20 Hz to 20 kHz.
- Logarithmic frequency axis.
- Gain axis in dB.
- Show the calculated combined filter response.
- Display individual filter points.
- Permit dragging filter points to adjust frequency and gain.
- Display editable numeric fields for precise adjustment.
- Update the curve immediately when settings change.

Calculate the response from the actual filter parameters rather than drawing an unrelated decorative curve.

Smooth coefficient transitions to avoid audible artifacts.

## 2.4 Mode C — 31-Band Professional Equalizer

Implement the following one-third-octave band centers:

20, 25, 31.5, 40, 50, 63, 80, 100, 125, 160, 200, 250, 315, 400, 500, 630, 800 Hz, 1 kHz, 1.25 kHz, 1.6 kHz, 2 kHz, 2.5 kHz, 3.15 kHz, 4 kHz, 5 kHz, 6.3 kHz, 8 kHz, 10 kHz, 12.5 kHz, 16 kHz and 20 kHz.

Requirements:

- Adjustable individual bands.
- Gain range of approximately ±12 dB.
- Horizontal scrolling.
- Smooth touch interaction.
- Accurate gain labels.
- Individual band reset.
- Reset all.
- Preset selection.
- Preamp integration.

Handle frequency bands above the available Nyquist frequency appropriately.

Do not build active filters at frequencies that cannot be represented meaningfully for the current sample rate.

## 2.5 Equalizer Presets

Create a searchable, categorized preset library with at least 50 meaningful presets.

### General audio presets

Flat, Reference, Warm, Bright, Vocal Clarity, Bass Boost, Deep Bass, Sub-Bass, Treble Boost, Loudness, Smooth, Punchy, Airy, Balanced, V-Shaped, U-Shaped.

### Music genres

Pop, Rock, Hard Rock, Metal, Alternative, Indie, Classical, Orchestra, Jazz, Blues, Acoustic, Folk, Country, EDM, House, Deep House, Techno, Trance, Dubstep, Drum & Bass, Hip-Hop, Trap, R&B, Funk, Reggae, Latin, K-Pop, J-Pop, Lo-Fi, Chillout, Ambient, Synthwave.

### Listening profiles

Headphones, IEM, Small Speaker, Phone Speaker, Car Audio, Night Listening, Podcast, Gaming, Live Concert, Vintage.

Preset design requirements:

- Use technically plausible and documented EQ curves.
- Do not create meaningless random band gains.
- Flat must produce a neutral response.
- Provide adequate headroom for bass-heavy presets.
- Provide a clear distinction between preset names and their actual response curves.
- Allow preset selection without interrupting playback.
- Support preset favorites and search.
- Support custom preset creation.
- Allow renaming, duplication, and deletion of custom presets.
- Allow import/export of custom presets.

Where a preset is translated between 10-band and 31-band modes, use appropriate interpolation or response fitting.

Avoid misleading one-to-one conversion when the curves cannot be accurately reproduced.

## 2.6 Preamp and Clipping Protection

Implement adjustable preamp gain.

Provide:

- Preamp slider.
- Gain value in dB.
- Reset.
- Suggested headroom.
- Optional automatic gain compensation.
- Clipping-risk indication.

Calculate the cumulative audio response where practical.

Avoid unexpected excessive gain.

If implementing a limiter, make it optional and clearly identify it as signal processing.

## 2.7 ReplayGain

Implement ReplayGain using compatible file metadata.

Support:

- Track Gain
- Album Gain
- Track Peak
- Album Peak
- Adjustable preamp
- Clipping prevention
- Disable/enable

Select gain according to the user's chosen mode.

Do not permanently modify the original audio file.

Avoid double-applying gain when other loudness metadata or gain systems are already active.

## 2.8 Equalizer Persistence

Save locally:

- Active EQ mode
- EQ enabled state
- Band gains
- Parametric filters
- Preamp
- Selected preset
- Custom presets
- ReplayGain settings

Use Room/DataStore according to data complexity.

Settings must survive normal app restart and device reboot.

## PHASE 2 ACCEPTANCE CRITERIA

Phase 2 is complete only when:

- All three equalizer modes work.
- Band adjustments affect the actual audio.
- Parametric filters operate correctly.
- The frequency-response graph reflects actual filter settings.
- Equalizer changes are smooth.
- Preamp works.
- ReplayGain works with supported metadata.
- Custom presets persist.
- EQ bypass works correctly.
- Playback is stable with DSP enabled.
- DSP does not unexpectedly interfere with background playback.
- Unit tests validate filter behavior and preset persistence.
- Gradle builds successfully.

Test EQ response using known audio signals or generated test tones, not only subjective listening.

---

# PHASE 3 — SIX REAL-TIME AUDIO VISUALIZERS

## Objective

Build six visually attractive, accurate, real-time audio visualizations inspired by professional audio players.

All visualizations must respond to actual playback audio.

## 3.1 Visualization Architecture

Create a reusable audio visualization engine.

Suggested components:

- VisualizationEngine
- PcmTapProcessor
- FftAnalyzer
- SpectrumDataProcessor
- WaveformProcessor
- VisualizationSettingsRepository

Integrate the visualization engine with the existing audio-processing pipeline.

Use an appropriate PCM tap to provide audio samples to visualization consumers.

The visualizer must not create another ExoPlayer instance.

Ensure the PCM tap:

- Uses bounded buffers.
- Avoids blocking audio processing.
- Supports sample-rate changes.
- Uses appropriate buffering between audio and rendering threads.
- Safely handles playback stop, pause, and seek.
- Reduces workload when visualizations are not visible.

Do not request microphone permission merely to analyze music already being played by this app.

## 3.2 Visualizer 1 — FFT Spectrum Analyzer

Use the foobar2000 screenshot as the functional reference.

Create a real-time frequency spectrum with vertical bars.

Features:

- Adjustable FFT size.
- Configurable bar count.
- Logarithmic frequency scale.
- Optional linear scale.
- Accurate frequency labels.
- dB amplitude display.
- Peak hold.
- Peak decay.
- Adjustable smoothing.
- Adjustable update rate.
- Full-screen mode.
- Customizable colors.

Default appearance:

- Black background.
- Warm white spectrum bars.
- Subtle grid lines.
- Frequency and dB labels.

Use appropriate windowing, frequency-bin mapping, and amplitude normalization.

Visual output must be based on actual FFT analysis.

## 3.3 Visualizer 2 — Oscilloscope

Display the waveform in real time.

Features:

- Mono view.
- Stereo view.
- Adjustable time window.
- Adjustable amplitude scale.
- Grid lines.
- Waveform color customization.
- Full-screen mode.
- Stable rendering.

Support appropriate waveform triggering or stabilization where practical.

## 3.4 Visualizer 3 — Stereo VU and Peak Meter

Create separate left and right channel displays.

Features:

- Left channel level.
- Right channel level.
- Peak indicators.
- Peak hold.
- Peak decay.
- dB scale.
- Horizontal and vertical layouts.
- Optional clipping indicators.

Clearly distinguish instantaneous peak metering from VU-style average metering.

## 3.5 Visualizer 4 — Spectrogram

Create a scrolling frequency-versus-time display.

Features:

- Time scrolling.
- Logarithmic frequency axis.
- Frequency labels.
- Color intensity representing signal energy.
- Adjustable FFT resolution.
- Adjustable color themes.
- Adjustable history length.
- Full-screen mode.

Use a rolling buffer or efficient texture update method.

Avoid repeatedly allocating entire spectrogram images.

## 3.6 Visualizer 5 — Circular Spectrum

Create a circular audio spectrum visualization.

Requirements:

- Frequency bars around a circle.
- Radial movement based on FFT magnitude.
- Smooth animation.
- Configurable bar count.
- Adjustable sensitivity.
- Album artwork in center.
- Optional rotation effects.
- Color customization.
- Full-screen mode.

Make album artwork optional.

The visualization should work without album artwork.

## 3.7 Visualizer 6 — MilkDrop-Style Visualizer

Implement full-screen music-reactive shader-based animations.

Investigate projectM integration.

ProjectM may be used if technically feasible and licensing requirements are satisfied.

If integrated, provide:

- Preset selection.
- Preset switching.
- Random preset mode.
- Favorite presets.
- Full-screen animation.
- Offline preset storage.

Do not assume projectM can simply be added without native-library, renderer, asset-packaging, or licensing considerations.

If projectM integration is unsuitable, implement an original shader-based visualization engine.

An original implementation should provide several distinctive music-reactive effects.

Do not falsely label the original implementation as projectM.

## 3.8 Visualizer UI

Create a dedicated Visualizer screen.

At the top, show the active visualizer.

Below it, provide mode selection using recognizable icons or compact preview thumbnails.

Do not use a large text-only dropdown as the primary selector.

Controls:

- Change visualizer.
- Full-screen.
- Visualizer settings.
- Color theme.
- FPS/quality settings.

When entering full-screen mode, hide unnecessary navigation controls.

Provide a simple gesture or visible control to exit.

## 3.9 Performance and Battery Optimization

Provide:

- 30 FPS mode.
- 60 FPS mode where supported.
- Low/Medium/High quality.
- Automatic pause when the screen is not showing the visualizer.
- Reduced workload when the app is backgrounded.
- Efficient GPU rendering.
- Bounded buffers.
- Minimal memory allocations during playback.

Visualizers must not introduce regular audio stuttering.

## PHASE 3 ACCEPTANCE CRITERIA

- Six visualization modes are available.
- All six respond to actual music playback.
- FFT frequencies are validated using known test tones.
- Left/right meters respond correctly to stereo test signals.
- Visualizations handle pause, seek, and track changes correctly.
- The interface supports full-screen mode.
- FPS settings work.
- Visualizers do not substantially interfere with normal playback.
- Rendering resources are released appropriately.
- No unnecessary microphone permission is requested.
- Project builds successfully.

---

# PHASE 4 — ADVANCED AUDIO FEATURES

## Objective

Extend the application with audiophile-oriented functionality, AutoEq headphone correction, synchronized lyrics, improved file-format handling, and detailed audio-output information.

## 4.1 Extended Audio Format Support

Verify and implement supported decoding for:

- ALAC / Apple Lossless
- AIFF

Preserve compatibility with existing MP3, FLAC, WAV, AAC, OGG Vorbis, and Opus playback.

Use appropriate decoding and extraction components.

Where external codec libraries are required, investigate maintained libraries and licensing.

Do not label a format as supported unless it has been tested successfully.

Support high-resolution PCM decoding when the device and software pipeline permit it.

## 4.2 AutoEq Headphone Profiles

Implement AutoEq configuration import.

Supported inputs should include:

- Compatible GraphicEQ text files.
- Compatible parametric EQ configuration files.

Parse:

- Frequency.
- Gain.
- Filter type.
- Q factor.
- Preamp values where available.

Show a preview of the imported configuration.

Allow users to:

- Import a profile.
- Save a profile.
- Rename it.
- Delete it.
- Edit individual filters.
- Set it as the active headphone profile.
- Assign profiles to identifiable output devices.

Apply AutoEq using the actual DSP engine developed in Phase 2.

Ensure imported values are validated.

Reject malformed files gracefully without crashing.

### Important

Do not use network access to fetch AutoEq profiles.

Any profile import must work through local files.

Do not bundle large third-party datasets unless the relevant licensing and attribution requirements are satisfied.

## 4.3 Output-Specific Equalizer Profiles

Allow users to associate EQ settings with available audio-output types.

Examples:

- Phone Speaker
- Wired Headphones
- Bluetooth Headphones
- USB DAC

Where Android exposes a reliable device identity, support device-specific profile assignment.

Otherwise, use a generic output-category profile.

Avoid assuming that all Bluetooth devices or USB DACs have persistent, unique IDs available to the app.

Apply profiles carefully when the output route changes.

Do not unexpectedly overwrite the user's custom settings.

## 4.4 Offline Synchronized Lyrics

Implement support for:

- Embedded lyrics where compatible.
- External .lrc files.
- Plain text lyrics.
- Time-synchronized highlighting.

Features:

- Automatic local lyric-file matching.
- Manual lyric-file selection.
- Adjustable font size.
- Auto-scroll.
- Manual scroll.
- Current-line highlighting.
- Lyric timing offset.
- Missing-lyrics message.

Support common LRC timestamp formats and multiple lines sharing the same timestamp.

Lyrics must remain entirely offline.

Do not add an online lyrics provider.

## 4.5 Gapless Playback Verification

Review Phase 1's gapless handling.

Ensure transitions between compatible tracks do not introduce unintended delays.

Test:

- Consecutive FLAC files.
- Gapless MP3 files with valid delay/padding metadata.
- Continuous live recordings.
- Tracks with different metadata and artwork.

Do not introduce audio gaps due to EQ preset changes, visualization processing, or UI updates.

## 4.6 Optional Crossfade

If technically feasible, implement adjustable crossfade.

Suggested options:

- Off
- 2 seconds
- 4 seconds
- 6 seconds
- 8 seconds
- 10 seconds
- 12 seconds

Implement actual overlap between tracks rather than reducing the first track to silence and then starting the next.

Crossfade must work correctly with the audio-processing architecture.

If robust crossfade requires a second synchronized playback pipeline, implement it carefully and without duplicating the app's authoritative media session.

Keep crossfade disabled by default.

Clearly distinguish gapless playback from crossfade.

## 4.7 USB DAC and External Output

Create an Audio Output Information screen.

Detect relevant outputs using supported Android APIs.

Display available information, such as:

- Current audio route.
- Output type.
- Device name where available.
- Supported sample rates where reported.
- Supported channel counts.
- Available audio encodings.
- Source sample rate.
- Source bit depth.
- Active DSP processing.

Do not claim bit-perfect or exclusive mode solely because a USB DAC is connected.

If supported by the Android version and hardware, investigate direct/high-resolution output capabilities.

Only provide a user-selectable bit-perfect or direct-output mode if the implementation can genuinely satisfy and report its requirements.

Where this is not possible, display an explanation of the limitation.

DSP, crossfade, software volume processing, and ReplayGain may be incompatible with bit-perfect output.

Provide the appropriate settings and warnings.

## 4.8 Detailed Audio Information

Enhance the Now Playing information panel.

Show:

- Song title.
- Artist.
- Album.
- Genre.
- Track/disc number.
- Duration.
- Codec.
- Container.
- File size.
- Source sample rate.
- Bit depth where available.
- Bitrate.
- Channel count.
- Current playback processing.
- Audio route.

Example:

FLAC • 24-bit • 96 kHz • Stereo

Clearly distinguish source-file data from output-route data.

## 4.9 Advanced Library Features

Implement or refine:

- Smart playlists.
- Favorites.
- Most played.
- Recently played.
- Recently added.
- Playback statistics.
- Search filtering.
- Playlist import/export.
- CUE sheet support if practical.

For smart playlists, allow criteria such as:

- Artist.
- Album.
- Genre.
- Play count.
- Date added.
- Favorites.
- Audio format.

Avoid excessively expensive database queries during playback.

## PHASE 4 ACCEPTANCE CRITERIA

- ALAC/AIFF behavior is verified and unsupported cases are documented.
- AutoEq files import correctly.
- AutoEq filters actually affect audio.
- Malformed AutoEq files do not crash the app.
- Offline lyrics display and synchronize correctly.
- Output-specific EQ settings behave predictably.
- Gapless playback is validated.
- Crossfade works if implemented, or is explicitly listed as incomplete.
- Audio-output information is accurate.
- USB DAC disconnection does not crash playback.
- Smart playlists update correctly.
- All data remains local.
- No network permission is introduced.
- Project builds successfully.

---

# PHASE 5 — WIDGETS, UI REFINEMENT AND FINAL OPTIMIZATION

## Objective

Complete the user experience with Android home-screen widgets, modern interface refinements, accessibility, testing, and performance optimization.

## 5.1 Home-Screen Widget Architecture

Use Jetpack Glance and appropriate Android AppWidget APIs.

Widgets must communicate with the existing MediaSessionService.

Do not create an independent playback engine for widgets.

Share authoritative playback state through an appropriate application-level state source.

Use efficient widget updates instead of continuous polling.

## 5.2 Widget A — Compact (2×2)

Display:

- Album artwork.
- Play/Pause.
- Next track.

Tapping artwork should open Now Playing.

The widget should adapt to launcher size constraints.

## 5.3 Widget B — Standard (4×2)

Display:

- Album artwork.
- Song title.
- Artist.
- Previous track.
- Play/Pause.
- Next track.

Use compact, readable typography.

Truncate long titles appropriately.

## 5.4 Widget C — Expanded (4×4)

Display:

- Large album artwork.
- Song title.
- Artist.
- Album, if space permits.
- Playback progress.
- Previous.
- Play/Pause.
- Next.
- Shuffle.
- Repeat.

Provide an entry point to the full player.

If interactive seeking cannot be implemented reliably with the selected widget APIs, use a non-interactive progress indicator and open Now Playing for seeking.

Do not create fake interactive controls.

## 5.5 Widget Appearance

Provide:

- Dark theme.
- Transparent theme.
- Accent color customization.
- Rounded corners.
- Responsive sizing.
- Fallback artwork.
- Readable contrast.

Widgets must handle:

- No music loaded.
- Player stopped.
- Playback paused.
- Missing artwork.
- Background playback.
- App process recreation.
- Device restart.
- Multiple widgets.

Avoid unnecessary periodic updates merely to animate a progress bar.

## 5.6 Final UI Refinement

Review all screens for consistent visual design.

### Library

- Clean list/card layouts.
- Proper album artwork.
- Compact and detailed list options.
- Consistent text sizing.
- Smooth scrolling.
- Accurate empty states.
- Search and sort controls.

### Now Playing

- Large album artwork.
- Clean playback controls.
- Progress bar.
- Codec information.
- Queue.
- Lyrics shortcut.
- Equalizer shortcut.
- Visualizer shortcut.

### Equalizer

- Consistent slider sizes.
- Clear frequency labels.
- Visible preset selector.
- Smooth mode switching.
- Easy reset.
- Responsive layout.

### Visualizer

- Clean visualization selector.
- Full-screen display.
- Theme customization.
- No unnecessary overlays.

### Settings

Group related options logically.

Avoid deeply nested settings for frequently used audio controls.

## 5.7 Theme System

Default theme:

- Dark background.
- Electric-blue accent.
- Bright readable text.
- Subtle borders.
- Minimal visual clutter.

Provide optional accent choices.

If practical, support dynamic colors derived from album artwork.

Avoid distracting animations during navigation.

Provide an option to reduce or disable decorative animation.

## 5.8 Performance Optimization

Review:

- Playback CPU usage.
- DSP processing overhead.
- FFT computation overhead.
- GPU usage.
- UI recomposition frequency.
- Battery consumption.
- Database queries.
- Artwork loading.
- Music-library scanning.
- Memory allocations.
- Resource leaks.

Ensure that visualizers do not continue expensive rendering when not displayed.

Avoid unnecessary polling.

Keep playback stable during heavy UI interaction.

## 5.9 Accessibility

Implement:

- Content descriptions for icon buttons.
- Adequate touch-target sizes.
- Scalable text.
- Sufficient contrast.
- Accessible playback controls.
- Screen-reader-compatible labels.
- Logical focus ordering.
- Reduced-motion support where practical.

## 5.10 Privacy and Security Audit

Verify:

- No INTERNET permission.
- No advertising SDK.
- No analytics.
- No cloud storage dependency.
- No mandatory account.
- No hidden data collection.
- All playlists stored locally.
- All custom presets stored locally.
- All playback history stored locally.
- No unnecessary microphone permission.
- Scoped storage practices are followed.
- Original audio files are never modified unintentionally.

## 5.11 Reliability Testing

Test the application under:

1. Normal playback.
2. Background playback.
3. Locked screen.
4. Incoming call.
5. Bluetooth disconnection.
6. Wired headphone disconnection.
7. USB DAC removal.
8. Screen rotation.
9. App process recreation.
10. App restart.
11. Device restart.
12. Music file deletion.
13. Storage permission revocation.
14. Corrupted audio files.
15. Large music library.
16. EQ changes during playback.
17. Visualizer switching.
18. Widget interactions.
19. Airplane mode.
20. Extended continuous playback.

Ensure errors are handled gracefully.

Do not silently clear playlists, history, or custom presets during recovery.

## 5.12 Final Integration Testing

Verify that all features work together.

Examples:

- FLAC playback with 31-band EQ and FFT spectrum.
- MP3 playback with ReplayGain and parametric EQ.
- AutoEq profile with headphone output.
- Synchronized lyrics during background playback.
- Widget control while the main app is closed.
- Bluetooth playback with output-specific EQ.
- Audio route changes while visualizer is running.

Pay special attention to interactions between DSP, visualization, output routing, and Media3 playback.

## PHASE 5 ACCEPTANCE CRITERIA

- All three widget layouts work.
- Widgets control the existing playback session.
- Widget state updates correctly.
- UI is consistent across screens.
- Dark theme is polished.
- Layouts adapt to common phone sizes.
- Accessibility improvements are implemented.
- Visualizers suspend unnecessary background rendering.
- The app remains stable during prolonged playback.
- All existing playback functionality remains operational.
- Automated tests pass where available.
- Release build succeeds.
- Application functions without internet.
- No critical known crash or data-loss problem remains.

---

# FINAL PROJECT QUALITY REQUIREMENTS

## 1. No Fake Features

Do not implement decorative controls that pretend to function.

Examples:

- EQ sliders must change audio.
- Visualizers must use real audio signals.
- ReplayGain must apply actual gain metadata.
- AutoEq profiles must create actual filters.
- Widgets must control playback.
- Format-support claims must match functioning decoder paths.
- DAC capabilities must reflect the actual device.

If a feature is incomplete, identify it explicitly.

## 2. Do Not Break Phase 1

Preserve:

- Local music scanning.
- Existing playlists.
- Music playback.
- Background service.
- Media session.
- Bluetooth controls.
- Queue management.
- Search.
- Metadata.
- Existing settings.

Retest these features after every development phase.

## 3. Build and Validation

After each phase:

- Run the relevant Gradle build.
- Run automated tests.
- Run lint where available.
- Fix compilation problems.
- Check for crashes.
- Verify database migrations.
- Verify compatibility with the existing Android SDK settings.

Do not claim that emulator/device tests were completed unless they were actually executed.

## 4. Documentation

Maintain these files:

README.md

DEVELOPMENT_PLAN.md

FEATURE_STATUS.md

TESTING.md

Document:

- Completed features.
- Partially implemented features.
- Missing features.
- Dependencies.
- Licenses.
- Android limitations.
- Hardware-dependent behavior.
- Known problems.
- Test results.

Update the documents after each phase.

## 5. Required Phase Report

At the end of every phase, provide a concise report containing:

**Completed Features**
List the features implemented.

**Modified Files**
Identify the main files created or changed.

**Architecture**
Explain any significant integrations.

**Build Status**
State whether the app compiled successfully.

**Testing**
List actual tests performed and their results.

**Known Limitations**
Explain unsupported features and hardware restrictions.

**Next Phase**
Identify the remaining work.

---

# FINAL EXECUTION INSTRUCTION

Begin by examining the existing project.

Verify Phase 1 functionality before modifying the playback pipeline.

Then implement the phases in the following order:

1. Phase 2 — Advanced Equalizer and Audio DSP.
2. Phase 3 — Six Real-Time Visualizers.
3. Phase 4 — Advanced Audio Features.
4. Phase 5 — Home-Screen Widgets and Final Optimization.

Proceed autonomously through the phases when the implementation environment permits, while maintaining a clear checkpoint after each phase.

If a phase cannot be fully verified, record the exact limitation. Do not silently treat incomplete requirements as completed.

Prioritize correctness, reliability, sound quality, offline privacy, and maintainable code over rushing through features.

**The final target is a fully functional Android offline music player with professional equalization, real-time audio visualizations, advanced file-format support, offline lyrics, AutoEq profiles, USB DAC information, and responsive home-screen widgets.**
