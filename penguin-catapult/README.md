# Penguin Catapult: Snowbound

One complete, offline, two-stage level built in **Godot 4.6.3 stable**, GDScript, Compatibility renderer. Original procedural cartoon art and synthesized sound. This is a reference-inspired prototype; exact Crazy Penguin Catapult fidelity has **not** been verified without reference footage. See [REFERENCE.md](REFERENCE.md).

## Play on Windows (no Godot installation needed)

[Download the Windows game ZIP](https://github.com/Piccol1001/ChatgptMusicHiFiPlayer/raw/refs/heads/main/penguin-catapult/downloads/Snowbound-Windows-x86_64.zip). On GitHub you can also open the ZIP in `downloads/` and choose **Download raw file**.

1. Extract `Snowbound-Windows-x86_64.zip` to a folder on your laptop.
2. Open the extracted folder and double-click **Snowbound.exe**. The executable contains the game data and needs no Internet connection.
3. Click **Start Level 01**.
4. **Catapult:** wait until each walking penguin enters the gold cradle, then press **Space** or click the playfield. Press once per penguin. Early, late, and unpressed launches miss. You get eight attempts.
5. **Attack:** your successful penguins fly across in order. A red arrow marks the next penguin you control. Press **Space** or click to make it dive. Dives carry forward, so press a little before the penguin is directly over a bear. Each penguin can hit one bear.
6. Knock out all three bears to win. **Esc** or **Pause** freezes the game; **Esc** resumes. Retry resets the level. Next leads to the completion screen because this deliverable has one level.

Close the window to quit. Toggle sound on the start screen. Best score, completion, and the sound setting are stored only on your computer in Godot's `user://progress.cfg` (normally `%APPDATA%\Godot\app_userdata\Penguin Catapult — Snowbound\progress.cfg`). There are no accounts, downloads, analytics, or network calls.

The Windows build is unsigned. It was exported successfully here but has not been run on Windows. If Windows shows a security prompt, verify the source and checksums before deciding whether to run it. See [VALIDATION.md](VALIDATION.md) for laptop checks still needed.

## Open and edit the project

1. Download the standard **Godot 4.6.3 stable** Windows x86_64 editor (the .NET version is unnecessary).
2. Extract Godot and run its editor executable.
3. In Project Manager click **Import**, choose this folder's `project.godot`, then **Import & Edit**.
4. Wait for the asset import. Press **F6** to run the open main scene, or **F5** to run the project. F5 is the simplest option.
5. Close the game window to return to the editor.

Keep the entire source folder together. No plugins or packages are required. The logical view is 1280×720, initially displayed at 1152×648. Resizing preserves the scene aspect ratio with letterboxing. Physics runs at 120 fixed steps per second, independently of rendering.

## Where things live

| File | Purpose |
| --- | --- |
| `scenes/main.tscn` | Main scene; attaches gameplay and creates UI/camera/audio |
| `scripts/game.gd` | Explicit states, stage timing, movement, swept collisions, results |
| `levels/level_01.json` | Team, bear layout, timing, speeds, gravity, collision sizes and score |
| `scripts/hud.gd` | HUD and start/pause/result/completion menus |
| `scripts/art.gd` | Replaceable penguin and bear drawing adapter |
| `scripts/sound.gd` | Replaceable audio cue interface; generates original tones |
| `scripts/local_save.gd` | Local settings and best-score file |
| `assets/icon.svg` | Original penguin icon |
| `tests/` | Executable gameplay regression tests and rendering/mouse checks |

To tune the game, edit the JSON numbers and rerun. Coordinates are in logical pixels. Bear positions are local to the attack screen. The launch half-window is a distance; its total time is `2 × launch_half_window / approach_speed` (currently 0.272 seconds). Dives preserve a configured forward speed and accelerate downward. Those numbers are assumptions, not measurements from the original game. Art and audio can be replaced at their adapters without changing the rules.

## Export again from Godot

Use **Editor → Manage Export Templates → Download and Install** for 4.6.3. Then **Project → Export → Windows Desktop → Export Project**. Choose a new `Snowbound.exe` location; the preset targets x86_64 and embeds the game data. Do not check “Export With Debug” for distribution. Templates are only needed to export, not to play the supplied build.

## Cloud / Linux development

Run `bash tools/setup-cloud.sh` once, then `source /workspace/.penguin-tools/env.sh`. Setup uses official checksum-verified downloads if tools/templates are missing; the already prepared machine reuses them. It leaves the unrelated Android app unchanged.

```sh
cd /workspace/ChatgptMusicHiFiPlayer/penguin-catapult
godot --headless --editor --import --quit
godot --headless --script tests/gameplay_test.gd
godot --path .  # requires a graphical display
mkdir -p builds/windows
godot --headless --export-release 'Windows Desktop' builds/windows/Snowbound.exe
```

No server or credentials are required. `tests/presentation_capture.gd` requires an X11 display and tests real mouse routing while capturing menu/gameplay screens. Set `DISPLAY` for your display; run `godot --audio-driver Dummy --resolution 1280x720 --script tests/presentation_capture.gd`. Cloud audio hardware is unavailable, so audible quality needs a laptop check.
