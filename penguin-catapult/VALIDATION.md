# Validation record

Validated on 2026-10-19 (client date), in the Linux cloud environment with Godot **4.6.3.stable.official.7d41c59c4**. This records actual checks, not a claim of reference-game accuracy.

## Passed here

- Godot editor import completed without script/import errors after configuring writable cloud XDG directories.
- `bash tools/setup-cloud.sh` completed, then completed again using the existing tooling and templates. This imports the project and runs the gameplay checks without changing the Android application or tracked files that existed before this task.
- **46 gameplay checks, zero failures**, executing the production main scene and its fixed-step gameplay functions with deterministic inputs: early/on-time/late/unpressed launches; zero-team defeat; eight-launch attack; timed dive hits and complete victory; off-target dives and unused-flight misses; defeat; high-speed swept collision and clear misses; active-dive result gating; launch and attack pause; transition input suppression; twelve retries; mouse/Space input; held-key suppression; next-level completion navigation; synthesized audio stream generation; local save and reload.
- **Four mouse-routing checks, zero failures**, through real Godot viewport input with the Compatibility OpenGL renderer: Start does not also launch, playfield click launches, Resume does not also act, Next opens the completion screen.
- Captured six actual rendered screens: start, launch, pause, attack, result and completion. Inspected the start, launch and attack captures for layout/readability; captured screens are available under `builds/` in the prepared workspace.
- Windows x86_64 release export completed without export errors. File inspection identifies a PE32+ Windows GUI x86-64 executable with embedded game data.
- A Linux x86_64 release export also completed. The exported executable started headlessly for 120 frames without script/resource errors, checking packaged startup and level JSON availability.
- Official export template archive was verified against the release's SHA512 manifest before use. No checksum or TLS bypasses were used.

The deterministic tests call the same physics update used by the game, but do not claim manual timing/playtesting or physical input-latency measurements. Rendering checks used Xorg's dummy display and Mesa llvmpipe software OpenGL. A V-sync-unavailable warning is expected there. Cloud audio used a dummy driver; generation was verified, audible quality was not.

## Still requires the Windows laptop

- Extract the supplied Windows ZIP and launch `Snowbound.exe` on Windows 10/11 x86_64.
- Play one full win and one loss with each control method; judge timing and impact feel.
- Listen to sound at a comfortable volume; check sound toggle and persistence across app restarts.
- Check Esc/pause, retry, menu clicks, window resize/letterboxing and display scaling at the laptop's resolution.
- Check Compatibility/OpenGL driver support and performance on the laptop GPU. Windows execution, antivirus reputation, signing and hardware audio were not validated in this Linux machine. The build is unsigned.
- Compare against authoritative Crazy Penguin Catapult screenshots/video once provided. Flight order, timing, camera, collision/scoring and artwork fidelity remain explicitly unverified in `REFERENCE.md`.

## Reproduce

```sh
source /workspace/.penguin-tools/env.sh
cd /workspace/ChatgptMusicHiFiPlayer/penguin-catapult
godot --headless --editor --import --quit
godot --headless --script tests/gameplay_test.gd
# With an existing X11 display, e.g. DISPLAY=:99:
godot --audio-driver Dummy --resolution 1280x720 --script tests/presentation_capture.gd
```

`tests/gameplay_test.gd` isolates gameplay from automatic physics scheduling, preserves/restores any existing save file around its persistence test, and exits nonzero on failed assertions. Presentation captures use synthetic fixture positions to show each screen; they are not proof of human gameplay. Tests are excluded from exported builds.

## Scope and environment configuration

The existing Android music-player project remains untouched. All new game work is in `penguin-catapult/`. Necessary `install_script` and `start_skill` instructions were saved to the cloud environment draft. This does not publish the snapshot; review and save them in environment settings, then publish the environment to retain the prepared setup. Fresh-task restoration was not independently tested. No credentials, network additions or runtime servers are needed for the game.
