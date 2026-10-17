# Development plan

The starting repository had no commits or files. The original requested scope is preserved in `docs/PRODUCT_REQUIREMENTS.md`. No reference screenshot was supplied in this task. Implementation proceeds in acceptance-gated phases; controls are connected to real behavior or clearly unavailable.

## Phase 1 — Core player (implemented; acceptance partially verified)

Single application module with separate data, library, playback and UI packages. Use manual application-scoped dependencies for the initial scope rather than introduce Hilt prematurely.

- Kotlin, Compose Material 3 dark interface, five navigation destinations and a persistent mini-player.
- MediaStore and multiple SAF folders, appropriate permissions, paged library, sorting, search and metadata/artwork.
- MediaSessionService, queue editing, transport, focus/noisy handling, saved paused restoration.
- Room playlists/favorites/history; DataStore settings and queue persistence.
- Build, lint, unit tests, generated format fixtures and service integration tests.
- Gate: compile and launch, actual MP3/FLAC and core format playback, queue controls and restoration, no Internet permission. Hardware Bluetooth/lock-screen checks remain explicitly unverified until performed.

## Phase 2 — DSP and equalizer (implemented; automated checkpoint passed)

Implement a sample-rate-aware, allocation-bounded Media3 PCM AudioProcessor with stable biquads and coefficient smoothing; test impulse/frequency response, bypass, clipping and channel independence before exposing sliders. Add the 10-band, >=10-filter parametric and 31-band modes, a response graph and exact numeric editing. Define 50+ documented preset curves and log-frequency interpolation with disclosure when converting modes. Add custom preset import/export and persistence. Parse real ReplayGain metadata and test gain/peak/preamp arithmetic. Disable incompatible offload routes when PCM DSP is active. Validate clicks, headroom and battery behavior on devices.

## Phase 3 — Visualizers (implemented; automated checkpoint passed)

Add an internal PCM tap and nonblocking analysis handoff. Test FFT bin accuracy using deterministic tones; render actual spectrum, waveform, stereo levels, spectrogram, circular spectrum and an original reactive shader. Suspend analysis when hidden. Add FPS, FFT, quality, decay and color controls. Investigate projectM separately with a license audit; never label the original renderer as projectM.

## Phase 4 — Advanced library/audio (implemented; MP3 acceptance partial)

Investigate and license ALAC/AIFF extraction and decoding. Implement offline AutoEq import with preview, LRC/embedded lyrics, smart playlists, M3U/M3U8 import/export, detailed source/output metadata and Android-exposed audio route information. Test real gapless album boundaries and encoder padding. Crossfade stays off and unavailable until a dependable overlapping pipeline exists. Never fabricate exclusive USB or bit-perfect capabilities.

## Phase 5 — Widgets and polish (implemented; device acceptance partial)

Three responsive widgets control the existing session and update on events, with artwork and theme options. Real AppWidgetHost/RemoteViews and PendingIntent actions, service recreation, corrupt-file recovery and a synthetic 10k-entry Room query are covered. Reduced decorative motion, cached queue state and visibility-limited position updates reduce avoidable work. Physical accessibility, battery, real-library scanning, launcher/reboot behavior, hardware routes, SD removal and prolonged airplane-mode playback remain unverified. An offline license screen and bundled notices are included. Splash refinements and owner release signing remain open.

## Definition of done

Keep `FEATURE_STATUS.md` and `TESTING.md` synchronized with actual test evidence. A compiled feature is not a passed device acceptance test. Record failed, unrun and hardware-dependent checks separately. Do not advance a phase's acceptance status based on a zero-test result or a UI placeholder.
