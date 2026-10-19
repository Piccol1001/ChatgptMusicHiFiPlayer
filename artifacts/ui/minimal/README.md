# Three-section redesign captures

These are 27 unedited Android screenshots from the isolated API 30 `hifi-ui-validation-30` AVD, captured on 2026-10-19. They show the existing app with the new Library / Playing / Audio navigation. The capture candidate uses 0.6.0/code 6 until the explicit release workflow promotes the next version; About always reads the installed package metadata.

The opt-in runner uses six generated playable FLAC tones, two playlists and original geometric cover art. It drives the real MediaController, database, DSP and visualization settings; these fixtures are restricted to the disposable validation AVD and removed afterward. During live playback it uses the app's navigation intents to prepare captures; interactive controls and real Android intent delivery are tested separately. No fixture music is bundled in the production APK. Capture 00 comes from an earlier permission-onboarding pass; the complete passing capture run reused the granted music permission.

| Screen | Captures |
| --- | --- |
| Library onboarding / empty | [Permission onboarding](00-library-onboarding.png), [accessible empty library](01-library-empty.png) |
| Library with music | [Songs](05-library-songs.png), [two-column albums](06-library-albums.png), [session mini-player](09-library-mini-player.png) |
| Playlists within Library | [Empty](02-playlists-empty.png), [populated](07-playlists-populated.png) |
| Playing | [Empty](03-playing-empty.png), [portrait](08-now-playing.png), [landscape](17-now-playing-landscape.png) |
| Equalizer | [10 bands](10-eq-ten.png), [31 bands](11-eq-thirty-one.png), [parametric](12-eq-parametric.png), [landscape](18-eq-landscape.png) |
| Visualizer | [Idle](04-visualizer-idle.png), [Spectrum](13-1-visualizer-spectrum.png), [Oscilloscope](13-2-visualizer-oscilloscope.png), [Stereo Meter](13-3-visualizer-stereo-meter.png), [Spectrogram](13-4-visualizer-spectrogram.png), [Circular](13-5-visualizer-circular.png), [Reactive](13-6-visualizer-reactive.png) |
| Adaptive layouts | [360 dp](14-library-360dp.png), [412 dp](14b-library-412dp.png), [larger 609 dp](14c-library-large-screen.png), [360 dp / 1.5× text Library](15-library-large-font.png), [360 dp / 1.5× text EQ](16-eq-large-font.png) |
| Settings | [Grouped settings with the loaded session mini-player](19-settings.png) |

The complete capture run passed (848.248 s); seven affected images were refreshed after the final spacing fixes in a separate passing focused run (590.455 s).

Base portrait images are 1080×1920 pixels; 360 dp captures are 945×1920, 412 dp is 1082×1920, the larger layout is 1600×2560, and landscape is 1920×1080. Density is the emulator's 420 dpi. Horizontally scrollable category/mode/band lists intentionally expose more items by scrolling. Settings and EQ content also scroll vertically. The landscape EQ capture shows its initial viewport; scroll vertically to reach the graph and band controls. These screenshots demonstrate layout and real signal rendering, not frame-rate, acoustic quality or battery acceptance. See the [implementation and validation report](../../../docs/MINIMAL_REDESIGN.md) and [testing record](../../../TESTING.md).
