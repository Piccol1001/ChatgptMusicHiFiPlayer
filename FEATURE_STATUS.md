# Feature status

This is the Phase 1 foundation. **The full requested product is not complete.** All 13 unit cases and both API 28 integration tests pass. Device-specific acceptance, cold-start performance and later phases remain open. Cold-start ANRs were observed in the software emulator; Phase 1 acceptance is not complete.

| Area | Current implementation | Remaining work |
| --- | --- | --- |
| Native app | Kotlin, Compose, Material 3, min 26 / target 36; dark blue/mint/violet accents | Device accessibility audit, album-derived accents, light theme |
| Library | MediaStore including removable volumes on API 29+, multiple persisted SAF trees, IO scan, paging, search, sorting and MIME filters | Native Room index verified with 6 MediaStore files; standalone Opus indexing varies by OS. Real SD/provider matrix; cross-provider URI dedup; incremental background indexing; codec-specific filters |
| Metadata/art | MediaStore tags and artwork; SAF metadata and cached downsampled embedded artwork | Broader tag support, advanced technical metadata; cache pruning |
| Categories | Songs, albums, artists, genres, folders, favorites, recent additions/history and play counts; separate playlist tab | Group lists themselves are not paged; benchmark very large group counts |
| Playback | One Media3 service/player, session controls, play/pause/seek/next/previous, focus/noisy handling | Bluetooth/headphone and locked-screen acceptance on hardware |
| Queue | Play next, append/remove, up/down reorder, save playlist; repeat/shuffle through Media3 | Drag gestures, huge queue optimization; playlist membership intentionally excludes duplicates |
| Restoration | Queue/index/position/shuffle/repeat saved in DataStore, restores paused | Abrupt process death can lose up to five seconds; device process-recreation check |
| Playlists | Create, rename, delete, add/remove songs, ordered membership, count/duration | Drag/sort/search within playlist UI, covers, smart rules, M3U/M3U8 |
| Settings | Speed, information strip, accent, folders, scan, history clearing | Import/export/reset and advanced audio settings |
| Core formats | Standard Media3/Android decoder paths; all six generated format fixtures played on API 28 | See TESTING.md for actual executed coverage; hardware codec variation |
| ALAC / AIFF | Discovery recognizes files | Decoder/extractor integration pending; not claimed supported |
| Gapless | Standard Media3 pipeline used | Continuous live-album and encoder-delay/padding validation pending |
| EQ / ReplayGain | No custom processing; EQ tab states unavailable | Entire Phase 2 including 50+ presets and persistent custom profiles |
| Visualizers | Tab states unavailable | Entire Phase 3; no fake/random audio animation |
| Lyrics / AutoEq / DAC | Not implemented | Phase 4 |
| Widgets | Not implemented | Phase 5 |
| Privacy | No INTERNET, RECORD_AUDIO, MANAGE_EXTERNAL_STORAGE, accounts, cloud, ads or analytics | Airplane-mode device scenario and final dependency audit |

Notification access is optional on API 33+; media-session notifications are handled by Media3. Music permission denial still permits separately authorized SAF access. Deleted/missing music remains indexed as unavailable so playlist relationships are not silently lost.
