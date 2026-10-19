# Feature status

Phases 1–5 have working implementations, with acceptance limits listed below. **The full requested product is not complete.** Automated checks cover DSP, visualizers, advanced local audio/library features and widgets; physical-device acceptance and sample-exact MP3 gapless remain open. See [testing evidence](TESTING.md). Software-emulator cold-start ANRs and slow frames were observed.

| Area | Current implementation | Remaining work |
| --- | --- | --- |
| Native app | Kotlin, Compose, Material 3, min 26 / target 36; black/dark-gray themes, blue/mint/violet or local artwork accents, grouped Settings/About; Library/Playing/Audio primary navigation | Device accessibility audit, light theme |
| Library | MediaStore including removable volumes on API 29+, multiple persisted SAF trees, IO scan, paging, search, sorting and MIME filters | Native Room index verified with 6 MediaStore files; standalone Opus indexing varies by OS. Real SD/provider matrix; cross-provider URI dedup; incremental background indexing; codec-specific filters |
| Metadata/art | MediaStore/SAF tags, cached downsampled embedded artwork, source/processing/output technical information | Broader tag corpus, uncommon metadata and cache pruning |
| Categories | Songs, albums, artists, genres, folders, favorites, recent additions/history and play counts; Playlists category inside Library | Group lists themselves are not paged; benchmark very large group counts |
| Playback | One Media3 service/player, session controls, play/pause/seek/next/previous, focus/noisy handling | Bluetooth/headphone and locked-screen acceptance on hardware |
| Queue | Play next, append/remove, up/down reorder, save playlist; repeat/shuffle through Media3 | Drag gestures, huge queue optimization; playlist membership intentionally excludes duplicates |
| Restoration | Queue/index/position/shuffle/repeat saved in DataStore, restores paused | Service destruction/recreation tested; full process kill and force-stop acceptance pending; abrupt death can lose up to five seconds |
| Playlists | Create, rename, delete, add/remove songs, ordered membership, artwork covers, count/duration and playlist-title search | Drag/sort within playlists; portable M3U path resolution (URI M3U8 and smart rules now implemented) |
| Settings | Speed, information strip, accent, folders/scan, history clearing, audio profiles/output, widgets, reduced motion offline licenses, installed About version and artwork appearance | App-wide backup/export/reset and light theme |
| Core formats | Standard Media3/Android decoder paths; all six generated format fixtures played on API 28 | See TESTING.md for actual executed coverage; hardware codec variation |
| ALAC / AIFF | Streaming AIFF/AIFC signed PCM extractor; ALAC platform decoder detection and explicit unsupported message | API 28 verified AIFF 16/24-bit and seeking; ALAC unsupported message verified; no bundled ALAC decoder; compressed/float AIFC unsupported |
| Gapless | Media3 1.11.1; exact FLAC PCM and AudioTrack reuse verified; API 30 strict MP3 fixture transitions pass; API 28 boundary/terminal-tail diagnostic retained | MP3 sample-exact acceptance incomplete: API 28 transition residual 22–551 samples (~0.5–12.5 ms), terminal tail 551 samples; physical listening pending |
| EQ / ReplayGain | Real PCM DSP, 10/31-band and parametric EQ, response graph, headroom, 58 presets, custom JSON import/export, supported-tag ReplayGain; settings persist; compact Audio tab with collapsible advanced controls | 16-bit processing path; hardware listening/CPU/battery and route testing pending; see docs/DSP.md |
| Visualizers | Six real PCM modes, 30/60 FPS targets, FFT/quality/colors, fullscreen, lifecycle suspension; three original ES 2.0 effects | Physical GPU, battery, thermal and audible stutter checks; projectM not bundled; see docs/VISUALIZERS.md |
| Lyrics / AutoEq / DAC | Local AutoEq preview/profiles and route-category DSP, LRC/plain/USLT/Vorbis lyrics, actual AudioTrack route/capabilities and source information | Physical route/disconnect checks; persistent per-device identities unavailable; automatic lyrics require authorized SAF tree; see docs/ADVANCED_AUDIO.md |
| Widgets | Three responsive Glance widgets, artwork, themes, event-driven snapshots, real shared-session controls | Real AppWidgetHost/RemoteViews and button actions tested on API 28; physical launchers, reboot and Android 12+ restrictions pending |
| Privacy | No INTERNET, RECORD_AUDIO, MANAGE_EXTERNAL_STORAGE, accounts, cloud, ads or analytics | Native local tests run with guest Wi-Fi/mobile data disabled; prolonged airplane-mode hardware scenario and full dependency license audit pending |

Notification access is optional on API 33+; media-session notifications are handled by Media3. Music permission denial still permits separately authorized SAF access. Deleted/missing music remains indexed as unavailable so playlist relationships are not silently lost.
