# HiFi Player release rules

Use the existing checkout; preserve `com.piccol.hifi`, its established signing certificate, Room v1 and local DataStores. Do not clear app data or uninstall to make an update succeed.

For every future release, read `version.properties`, `releases/history.json`, `RELEASES.md`, `CHANGELOG.md` and `docs/RELEASE_WORKFLOW.md`. Inspect newly supplied/unknown APKs, including their embedded versions and certificates, before choosing the next code. Historical entries and APK bytes are immutable.

Choose MAJOR for significant architectural changes, MINOR for new app functionality, PATCH for fixes/UI refinements. Add structured release notes and use `python3 tools/release.py --bump <major|minor|patch> --notes <notes.json>`. Do not edit counters on every build attempt, derive them from commit counts, or use the old blind-copy packaging behavior. Ordinary builds never bump a release. The workflow promotes only verified successful builds, with codes greater than all known releases.

Keep About's version sourced from the installed PackageManager metadata and keep the main header version-free. Never regenerate or rotate the existing update key implicitly. Report actual embedded versionName/versionCode, artifact location and signing status for every delivered release. Document unverified device/upgrade checks honestly. Run the release-workflow safety tests and relevant Android checks, and preserve the completely offline manifest policy.
