# Advanced audio and local library

## AutoEq and output profiles

Import local Equalizer APO/AutoEq text through Android's document picker. The bounded parser accepts one `GraphicEQ:` line of increasing frequency/gain pairs, or numbered `Filter` lines with ON/OFF, PK/LS/HS/LP/HP, Fc, optional pass-filter gain and explicit Q. `Preamp:` and `#` comments are supported. Includes, convolution, arbitrary APO commands, unsupported compression/filter forms and out-of-range values are rejected. Frequency 10–24,000 Hz, gain ±12 dB, Q 0.1–18, preamp −24 to +12 dB and at most 32 parametric filters match the actual engine. A GraphicEQ curve is approximated by interpolation onto the 31-band controls, with that limitation shown in the preview. It is not an exact arbitrary-frequency FIR response.

Preview before saving; no network or headphone dataset is used. Profiles have rename/delete, manual activation and output-category assignments. Edit an active profile with the normal Equalizer controls, including numeric filter edits. Personal EQ remains separately stored. Route-specific profiles take precedence over the default profile; ReplayGain remains a shared setting. Deleting a profile clears its assignments. Returning to personal EQ clears the current category's override and the manual default.

Actual route changes come from the service's Android AudioTrack routing listener. Output inventory comes from AudioManager and is displayed separately. Device IDs, USB card addresses and Bluetooth names are not treated as stable unique identities; assignments in this version are by category. Reliable persistent per-device identity is not available uniformly without additional permissions or platform support.

## Offline lyrics

The lyric panel supports imported plain text and common LRC timestamps, hours, one/two/three-digit fractions, multiple timestamps, equal-time lines, file offsets, per-song user offsets, adjustable text size, current-line highlighting and auto-scroll. Dragging turns auto-scroll off. Tapping a timed line seeks the existing player. Positive offsets delay highlighting.

Manual imports copy at most 256 KiB into private storage, keyed by the song URI. Up to 1,000 copied lyric files are retained. Audio files are never edited. Automatic matching searches same-basename `.lrc` then `.txt` siblings only in explicitly authorized SAF trees, with a bounded 10,000-document search. MediaStore audio permission alone does not grant access to arbitrary lyric text files; select their music folder or import manually. Embedded ID3 USLT and compatible Vorbis LYRICS/UNSYNCEDLYRICS metadata are used when available. Manual lyrics take precedence over matching and embedded text. Unsupported tags are not claimed supported.

## Source, processing and output

The information panels distinguish selected source format/metadata, app PCM processing and the AudioTrack client format. FLAC source bit depth is read from a valid STREAMINFO header when available; missing values are reported as unknown. AudioTrack's sample rate is not the DAC hardware rate. Android may mix/resample, Bluetooth may encode, and output device capability lists can be empty. No exclusive, direct or bit-perfect mode is offered.

The processing path remains signed 16-bit PCM after Media3 conversion. 24-bit/96 kHz AIFF extraction does not imply preserved 24-bit hardware output. See DSP.md for the limitation and why offload/passthrough are disabled.

## Formats and transitions

The original streaming AIFF extractor accepts AIFF/AIFC signed 16/24/32-bit PCM with 1–8 channels and integer rates from 8–384 kHz, big-endian NONE/twos or little-endian sowt. It validates FORM/chunk lengths, COMM, SSND offsets, padding and data size, streams bounded aligned packets, and publishes a frame-aligned seek map. Compressed AIFC, floating point and unusual sample widths/rates are rejected. No native codec dependency or third-party AIFF implementation is bundled.

ALAC uses Media3 MP4 extraction and an installed platform decoder if one exists. No FFmpeg extension is bundled. API 28 had no ALAC decoder; its unsupported message was verified. The API 28 test explicitly checks the decoder inventory and verifies either playback or an explicit unsupported-decoder error. Device behavior is documented in the phase report after execution.

Gapless tests use generated original continuous tones, consecutive FLAC and MP3 files with LAME delay/padding, and changed metadata. They check exact FLAC frame totals, AudioTrack reuse, and MP3 boundary increments relative to an isolated final track. On API 28, MP3 has a measured terminal tail of 551 frames and a transition discrepancy of 22–551 frames (about 0.5–12.5 ms). The strict regression is explicitly skipped for the known API 28 Google MP3 decoder combination; it can be forced with the documented instrumentation argument. Sample-exact MP3 gapless acceptance is incomplete. These checks do not establish the absence of audible hardware underruns; listening and route-dependent timing remain device acceptance checks. Crossfade is not implemented: it needs a dependable overlapping pipeline, and a fade-out/fade-in substitute is not offered. CUE sheet support is also deferred.

## Smart playlists and M3U8

Smart playlists persist rules separately from Room and query the existing indexed library with bound SQL parameters. Artist/album/genre exact matches ignore case; minimum play count, added-since date, favorites and MIME text can be combined. Room paging observes tracks, history and favorites so results update when those change. Existing Room schema version 1 and playlists remain intact.

Playlist export/import supports M3U8 files containing exact indexed `content://` URIs. Import validates before a Room transaction creates membership; unknown, unavailable, relative-file and network entries are reported as unresolved. URI exports are useful within the same indexed library; portable filesystem path resolution across devices/providers is not implemented. Imports are bounded to 2 MiB and 10,000 entries. Playlist membership continues to contain unique songs rather than duplicate occurrences.
