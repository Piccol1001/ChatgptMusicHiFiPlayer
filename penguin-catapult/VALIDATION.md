# Validation — v0.2.0 reference reconstruction

Checked on **2026-10-20**, using Godot **4.6.3.stable.official.7d41c59c4** in the Linux cloud environment.

## Checks actually performed

- Inspected the uploaded 357.75-second video using full-size frames and motion contact sheets. Independently inspected the supplied APK manifest, certificate metadata, local resource containers and decompiled structural identifiers. See [REFERENCE.md](REFERENCE.md) for confirmed observations and remaining assumptions. Neither source file is packaged or published.
- Godot imports and starts the rebuilt main scene without script or resource errors.
- **55 gameplay checks passed, zero failed.** Production scene logic was exercised at the configured 120 Hz fixed timestep: early/on-time/late/unpressed swings; pendulum rope length; launch and dive pause; zero-success defeat; team transfer; Continue and transition input gates; timed dive victory; unused-flight and off-target defeat; swept high-speed collision and misses; breakable ice; unsupported bear fall; duplicate-hit prevention; quota/active-dive result gating; eight repeated retries; keyboard/mouse and held-key behavior; save/reload; audio stream generation.
- The timed-input win simulation hit **10 bears with six launched penguins**, demonstrating that the six-bear mission quota is achievable with the actual movement/collision logic, including rebounds and ice support loss. This was automated timing, not human playtesting.
- **11 presentation/input checks passed, zero failed**, using real Godot viewport mouse events and Compatibility OpenGL rendering: map/help navigation, Start, playfield launch, pause icon, Resume, briefing Continue, result Continue, visible completion button bounds, and a 1152×648 laptop window preserving the logical aspect ratio with centered side margins. Captured all major screens; inspected map, catapult, attack, briefing and results for visual layout.
- Windows x86_64 release export and Linux x86_64 validation export completed. The Linux executable was started headlessly for 120 frames to check packaged script/resource and level-JSON startup. File inspection identifies the Windows executable as PE32+ x86-64.
- The cloud setup script was rerun successfully. Official export templates were previously verified against the release SHA512 manifest; no signature/checksum/TLS bypass was used.
- Distribution ZIP integrity and SHA-256 checksums were checked. Only `penguin-catapult/` changed; the Android app is untouched.

Rendering checks used Xorg's dummy display and Mesa llvmpipe. Its unavailable-V-sync warning is expected. Audio was generated using a dummy driver; audible output was not tested. Test scenes are excluded from exports. Captures show deliberate fixture states and are not evidence of human gameplay. Persistence tests preserve and restore an existing save file.

## Still needs the Windows laptop

- Extract the ZIP and run `Snowbound.exe` on Windows x86_64. The executable is unsigned.
- Complete a win/loss using keyboard and mouse; judge swing timing, dive/impact feel and rebound pacing.
- Check sound quality, volume/mute behavior, window resizing/letterboxing, display scaling and laptop GPU performance.
- Compare the rebuilt screens against the supplied video. The art is original and the layout/physics are reconstructed; pixel-perfect art, original camera easing, miss accounting, special powers and a complete campaign are not claimed.

## Reproduce

```sh
source /workspace/.penguin-tools/env.sh
cd /workspace/ChatgptMusicHiFiPlayer/penguin-catapult
bash tools/setup-cloud.sh
# Requires an existing X11 display, e.g. DISPLAY=:99:
godot --audio-driver Dummy --resolution 960x704 --script tests/presentation_capture.gd
```

`tests/gameplay_test.gd` simulates the production physics update with controlled inputs and returns nonzero on a failed check. No service, network access or secret is needed to run the game. Uploaded local references resolved the YouTube-access blocker. Prepared cloud configuration is saved separately from publication; fresh-task restoration has not been independently verified.
