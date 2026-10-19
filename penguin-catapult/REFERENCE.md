# Reference inspection and reconstruction

## Supplied sources, actually inspected

- Video: https://youtu.be/QHKjysU1jrw, supplied as `crazy-penguin-catapult-review-gameplay-by-appyshka-480-ytshorts.savetube.me.mp4`. Local copy inspected with extracted frames and motion contact sheets: **652×480, 357.75 seconds**, about 27 frames/s. This uploaded copy resolved the earlier YouTube network blocker.
- APK: `Crazy+Penguin_1.1.25_APKPure.apk`, **1,331,277 bytes**. SHA-256: `ccafe22a10a4003b46856a6659601a41343b3117ed2d1ea31946b69d2e673aa5`.
- APK manifest: package `com.digitalchocolate.androidtuxedo`, versionName **1.1.25**, versionCode **1125**, landscape activity `.DCTuxedo`, application label `Demo Catapulta Loca del Pinguino`.
- APK certificate metadata: subject contains Digital Chocolate, Inc.; certificate SHA-256 `98:47:85:AF:F5:1C:E8:A6:33:31:58:09:91:45:94:F3:74:5F:96:28:59:DC:91:C3:3F:7E:B1:93:02:1C:04:A1`. Certificate inspection is not a guarantee of APK authenticity or safety.
- The APK was inspected as an archive, its resource containers were inspected locally, and classes were decompiled for structural analysis. It was **not installed or run**. Its demo label and 800×480 splash asset indicate version/platform differences from the supplied video; APK behavior is not assumed identical to the video.

## Confirmed visual and behavioral observations

| Video time (approx.) | Observed detail | Reconstruction |
| --- | --- | --- |
| 00:00–01:07; later intermissions | Snow islands on a blue sea, winding mission route, small top-center controls | An original single-mission island map; no invented playable campaign |
| 01:08–01:25 | Overhead bridge queue, helmeted penguins swinging on a rope, catapult below with an operator | Rope swing, queue, catapult operator and timed FIRE feedback replace the flat walking line |
| 01:09–01:13 | Launched penguins leave upward/right; counter decreases | Launch departure animation and compact leading-zero team counter |
| 01:32 | Bright icy panel on an orange/yellow background; a promotional screen | Panel style informed the UI. Advertising and power-ups were not added |
| 01:36–01:40 | Mission briefing with character art and a bear-hit quota | Dedicated briefing and Continue input gate |
| 01:41–02:14 | One penguin flight/dive opportunity at a time; left-to-right travel, following camera | Sequential penguins with camera tracking across a wider level |
| 01:42–01:48 | Upright bears, horizontal ice platforms and vertical supports, ice fragments, bears falling | Destructible ice, upright bear artwork, falling bear handling and impact particles |
| 01:42–01:48 | A penguin can rebound and hit several bears | Limited multi-hit rebounds; exact speeds remain configurable |
| 01:49–02:14 | Hit count can exceed quota while penguins remain | Reaching the quota does not terminate pending opportunities/dives |
| Around 02:16; 03:13; 05:16 onward | Pale blue double-border results panel, bear-hit fraction, Continue and celebration art | Ice-framed results, leading-zero quota counter, original penguin-on-bear celebration |

The APK independently contains rope-launch, flight, start-dive, dive, hit, slide and die animation identifiers; upright bear imagery; catapult and ice tile artwork; and HUD animation identifiers. Structural constants include a 700 ms queue insertion interval and 800 ms catapult transition, but those are not asserted to be the video's launch window or total stage duration. No original Java code, extracted sprites, audio, promotional screens or APK are included in the Godot project or Windows distribution.

## Remaining assumptions and deliberate limits

- This is a reconstruction of **one representative mission**, not a complete port of every menu, level, campaign, strategy mode or power-up in the APK.
- Six launching attempts, a six-bear quota, ten placed bears, and a two-part ice layout make this mission complete and winnable. The exact mission-7/8/9 layouts in the video are not reproduced tile-for-tile.
- Each failed attempt consumes one penguin here. The exact original miss/retry accounting is not established across the video and demo APK.
- The success window is 63–83% of a 1.55 s swing, giving a 310 ms window. Flight is 340 logical px/s; dive starts at 350 px/s downward with forward speed 220 px/s and gravity 1060 px/s². These are reconstruction tuning values, not verified conversions of the original engine's fixed-point units.
- The swing is a configurable one-way pendulum arc. Original rope motion, randomized timing and catapult-angle collision are more involved; this build uses a timing window rather than a full simulation of the original catapult/snowball collision.
- Bear bounds are an expanded rectangle tested against the full movement segment. Ice is removed on segment contact. Rebounds, maximum bounce count, falling-bear knockout height and ground resolution are configurable approximations.
- New level art is drawn in SVG from scratch, guided by the observed palette, proportions and composition. Text uses OFL fonts, with licenses included. This is **not a pixel-identical art extraction**.
- Pause/help/start-screen details adapt the requested Windows controls; the video's full pause/start UI is not continuously visible. “Continue” after the mission leads to a completion screen, not another implemented mission.
- Point scores and persistent best score are local convenience values; the original per-object score formula and campaign “Total” are not reproduced. The HUD TOTAL in this build shows current-mission hits.

All gameplay dimensions, layout, speeds, timing windows, collision sizes and scoring are in `levels/level_01.json`. The logical view is **960×704**, preserving the uploaded video's near-4:3 framing with letterboxing on wider displays.
