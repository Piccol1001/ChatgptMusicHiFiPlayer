# UI comparison captures

These are Android emulator screenshots, not design mockups. `before/` comes from the original 0.5.0 application. `current/` uses a separate API 30 validation AVD with generated playable FLAC tones and original geometric artwork; the shipped application contains no fixture library. See [the redesign report](../../docs/UI_REDESIGN.md) for capture conditions, rendering limitations and validation results. The final capture test passed; these are functional visual checks, not performance measurements.

| Screen | Before | Redesigned empty/idle | Redesigned populated/playing |
| --- | --- | --- | --- |
| Library | [Before](before/library.png) | [Empty](current/01-library-empty.png) | [Songs](current/05-library-songs.png), [albums](current/06-library-albums.png) |
| Playlists | [Before](before/playlists.png) | [Empty](current/02-playlists-empty.png) | [Populated](current/07-playlists-populated.png) |
| Now Playing | [Before](before/playing.png) | [Empty](current/03-playing-empty.png) | [Playing](current/08-now-playing.png), [mini-player](current/09-library-mini-player.png) |
| Equalizer | [Before](before/equalizer.png) | — | [10 bands](current/10-eq-ten.png), [31 bands](current/11-eq-thirty-one.png), [parametric](current/12-eq-parametric.png) |
| Visualizer | [Before](before/visualizer.png) | [Idle](current/04-visualizer-idle.png) | [Real signal](current/13-visualizer-playing.png) |

Adaptive captures: [320 dp library](current/14-library-small.png), [1.5× text library](current/15-library-large-font.png), [1.5× text EQ](current/16-eq-large-font.png), [landscape player](current/17-now-playing-landscape.png), [landscape EQ](current/18-eq-landscape.png).
