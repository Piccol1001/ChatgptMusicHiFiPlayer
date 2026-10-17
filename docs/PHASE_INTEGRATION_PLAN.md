# Integration plan for Phases 2–5

Preserve the existing single ExoPlayer / MediaSessionService, MediaController UI, Room v1 library and playlists, and player DataStore. The complete continuation requirements are in REMAINING_PHASES_REQUIREMENTS.md.

1. Re-run Phase 1 build, unit tests, lint and native service tests before changing the player pipeline. Existing cold-start ANRs on the software emulator remain an acceptance limitation requiring startup profiling; do not infer physical-device performance.
2. Add a Media3 PCM AudioProcessor to the existing DefaultAudioSink. Disable audio offload and floating-point sink bypass so custom processing cannot be silently skipped. Implement real EQ, headroom, ReplayGain and a bounded post-DSP analysis tap. Audio settings and custom presets use a separate DataStore; the existing Room database remains intact.
3. Consume the tap from an analysis worker and render six visualizer modes. Only an actively visible visualization subscribes; audio callbacks never wait for FFT or Compose. Validate frequency/amplitude and stereo behavior with deterministic signals.
4. Extend the same DSP/profile repository with validated local AutoEq import. Add offline lyrics, metadata/output reporting and smart playlist queries without destructive Room migration. Treat optional crossfade and extended decoders as separate capabilities requiring actual validation.
5. Add responsive widgets that control the existing media session and receive event-driven updates. Refine accessibility, startup work and rendering; run integration and privacy checks and refresh versioned APKs with checksums.

Each phase has a build/test/documentation checkpoint. Hardware-only checks are explicitly pending. Unsupported capabilities never receive decorative controls or successful status labels.
