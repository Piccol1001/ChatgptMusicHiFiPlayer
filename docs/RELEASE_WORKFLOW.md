# Verified Android releases

The application is **HiFi Player**, package `com.piccol.hifi`. Keep its identity and existing Room/DataStore records. The financial-record wording in the additional requirement is treated as a requirement to preserve this app's local user data; this repository does not contain a financial-record application.

`version.properties` is the version of the last verified release. Normal Gradle builds/retries never modify it. `releases/history.json` records actual inspected APK versions, SHA-256 digests, signing status and verifiable dates. Do not change earlier ledger entries, rename an old APK to claim a new version, or infer releases from development phases. `RELEASES.md` documents historical APKs; `CHANGELOG.md` preserves their feature/fix/UI history.

## Release procedure

1. Read the current version and all known release records. Audit any additional APKs before releasing; the command rejects unrecorded files in `artifacts/apk`. Include APKs supplied outside this directory in the audit and ledger before invoking the workflow. Build outputs are transient candidates, not releases.
2. Choose the change type deliberately: **major** for significant architectural changes, **minor** for new app functionality, **patch** for bug fixes/UI refinements. The combined redesign adds appearance options and folder browsing, so its release uses a minor increment; the playback architecture is unchanged. Build count, Git commit count and wall-clock time do not determine versions.
3. Add a release-notes JSON file, e.g. `releases/0.6.0.json`, with `features`, `fixes`, and `ui` arrays. Use `"None."` for an unchanged category. Commit reviewed source and notes first when possible. If the source is dirty, that fact is recorded; its HEAD alone does not reproduce uncommitted changes.
4. Activate the Android toolchain (`source /workspace/.tooling/env.sh` in the prepared cloud), restore the **existing** private debug/update keystore securely if needed, and run:

   ```sh
   python3 -m unittest discover -s tools/tests -v
   python3 tools/release.py --bump minor --notes releases/0.6.0.json
   ```

   Choose `--bump minor` or `--bump major` for those change types. The command calculates the next semantic version and `max(recorded versionCode) + 1` under an exclusive release lock, then builds with temporary Gradle overrides. It runs Android unit tests, lint, debug and unsigned release builds. Until all checks and APK inspections succeed, the recorded version is unchanged. A failed build/retry does not consume a version. There is intentionally no implicit bump on `assembleRelease`.
5. The command inspects **staged APK bytes** with `aapt` and `apksigner`: package, semantic name, code, ZIP integrity, signing certificate and offline permission policy. It checks the retained historical APKs against their immutable hashes and metadata. It refuses missing/replaced update keys, historical file replacement and unsupported signing changes. New filenames are `HiFiPlayer_v<version>_debug.apk` and `HiFiPlayer_v<version>_release-unsigned.apk`.
6. On success it appends artifacts, extends checksums/ledger/release history/changelog and updates `version.properties`. Ordinary I/O failures roll back these publication edits. Keep the release process uninterrupted during the short promotion step; after a host crash inspect the ledger, properties and checksums before retrying. It prints the **actual** name, code, APK paths, hashes and signing status. It never commits, pushes or publishes to an app store by itself.
7. Verify upgrade compatibility on a test device with the prior install: compare application ID/certificate, back up important data, stop the app, record private database/preferences hashes, and use `adb install -r <new-debug-apk>`. Do not uninstall or clear storage. Compare data before relaunch, then check Settings → About → App Version. These APKs preserve Room v1 and DataStore; no destructive migration or app-data reset is used. Android still enforces signer, package, OS and version compatibility. An unsigned APK is not installable.
8. Review and commit release outputs, verify `sha256sum -c artifacts/apk/SHA256SUMS` from the APK directory (or `cd artifacts/apk && sha256sum -c SHA256SUMS`), and push only when authorized. If a Git push fails, retry publishing the already verified artifacts; do not run another semantic bump just to retry the push. Include actual version, code, APK paths and signing status in the delivery report.

## Signing and privacy

The existing evaluation certificate SHA-256 is recorded in the ledger. Its key currently resides outside Git at `$ANDROID_USER_HOME/debug.keystore` in the prepared cloud. `HIFI_DEBUG_KEYSTORE` can point to a secure restored copy; Gradle and the release script must use the same path. Standard Android debug credentials apply to this existing development key. Never commit the key, replace it, or silently generate a new one. Gradle's debug signing validation also refuses missing/mismatched keys. Loss of the key prevents ordinary compatible updates; a differently signed APK typically requires uninstalling (and losing app-private data), which this workflow does not do.

No owner production key has been supplied. The installable artifact remains **debug-signed for evaluation**, and the release build remains **unsigned**. A future production signing strategy must explicitly address migration from this development certificate; do not silently sign with a different certificate. Android key-rotation support is outside this workflow.

Release tooling may download build dependencies on the host. The APK remains completely offline: no INTERNET, network-state, microphone or all-files permission is added. About reads Android PackageManager metadata locally; the main header contains no version number.
