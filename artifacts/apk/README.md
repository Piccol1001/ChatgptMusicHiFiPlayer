# Prebuilt APKs — 0.1.0

- **HiFiPlayer-0.1.0-debug.apk**: installable debug build for evaluation, signed with the development debug key. Requires Android 8.0 / API 26 or newer.
- **HiFiPlayer-0.1.0-release-unsigned.apk**: unsigned release build. Sign with your own release key before installation or distribution.
- **SHA256SUMS**: SHA-256 checksums for both artifacts. Run `sha256sum -c SHA256SUMS` in this directory to verify them.

These are the Phase 1 foundation, not the completed five-phase application. Debug and release builds, lint, 13 unit cases and 2 API 28 integration tests passed. Generated MP3, FLAC, WAV, AAC/M4A, Vorbis and Opus fixtures played. Cold-start ANRs occurred in software emulation, and startup performance and hardware acceptance remain open. See [testing evidence](../../TESTING.md) and [feature status](../../FEATURE_STATUS.md).

To install the debug build on a connected Android device:

```sh
adb install -r HiFiPlayer-0.1.0-debug.apk
```

These checked-in APKs are a snapshot of version 0.1.0; later source changes require rebuilding and deliberately replacing the artifacts and checksums. Signing keystores and local build caches are not included.
