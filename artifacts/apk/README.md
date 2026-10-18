# Prebuilt APKs

The 0.5.0 files below are retained historical evaluation artifacts. Future verified releases are appended with `HiFiPlayer_v<version>_<variant>.apk` names; see [RELEASES.md](../../RELEASES.md), [CHANGELOG.md](../../CHANGELOG.md) and the [release workflow](../../docs/RELEASE_WORKFLOW.md). Do not overwrite or relabel old files.

## Historical snapshot — 0.5.0

- **HiFiPlayer-0.5.0-debug.apk**: installable evaluation build, signed with the development debug key. Android 8.0 / API 26 or newer.
- **HiFiPlayer-0.5.0-release-unsigned.apk**: unsigned release build. Not installable. Do not sign with a different key expecting an in-place update; see the established-certificate policy in the release workflow.
- **SHA256SUMS**: verify both artifacts with `sha256sum -c SHA256SUMS` in this directory.

This snapshot includes EQ/ReplayGain, six PCM visualizers, local AutoEq/output profiles, offline lyrics, AIFF, smart playlists/URI M3U8, output information and three session widgets. See [testing evidence](../../TESTING.md), [feature status](../../FEATURE_STATUS.md) and [phase reports](../../docs/PHASE_REPORTS.md).

Debug/release builds, lint and 42 unit cases passed. Full API 28 regression: 12 passed, 1 explicitly skipped for the known Google MP3 gapless decoder limitation. A subsequent UI check also passed with the bundled offline license screen. Processing is 16-bit PCM; sample-exact MP3 gapless, physical routes/launchers, startup performance and battery acceptance remain incomplete. The release APK is not a signed production release.

```sh
adb install -r HiFiPlayer-0.5.0-debug.apk
```

Source revision: `3db0061aafd242d63a5e32e0c802f5d998434f85`. The release APK's embedded VCS metadata identifies this revision. Use the verified release workflow to append a new release after source changes. The original 0.1.0 snapshots are also restored here byte-for-byte and listed in RELEASES.md. Signing keys, credentials, SDKs and caches are excluded.
