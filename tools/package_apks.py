#!/usr/bin/env python3
"""Package the already-built app APKs; build/test separately before running this."""

from pathlib import Path
import hashlib
import re
import shutil


root = Path(__file__).resolve().parents[1]
gradle = (root / "app/build.gradle.kts").read_text()
match = re.search(r'versionName\s*=\s*"([0-9][A-Za-z0-9._-]*)"', gradle)
if not match:
    raise SystemExit("Cannot determine the app version")
version = match.group(1)
sources = {
    "debug": root / "app/build/outputs/apk/debug/app-debug.apk",
    "release-unsigned": root / "app/build/outputs/apk/release/app-release-unsigned.apk",
}
for source in sources.values():
    if not source.is_file() or source.stat().st_size == 0:
        raise SystemExit(f"Build the APK first: {source}")

destination = root / "artifacts/apk"
destination.mkdir(parents=True, exist_ok=True)
staged = []
for kind, source in sources.items():
    target = destination / f"HiFiPlayer-{version}-{kind}.apk"
    temporary = target.with_suffix(".apk.tmp")
    shutil.copyfile(source, temporary)
    staged.append((temporary, target))

# Only this generated snapshot directory is replaced; old versions remain in Git history.
for previous in destination.glob("HiFiPlayer-*.apk"):
    previous.unlink()
for temporary, target in staged:
    temporary.replace(target)

checksums = []
for _, target in sorted(staged, key=lambda pair: pair[1].name):
    digest = hashlib.sha256(target.read_bytes()).hexdigest()
    checksums.append(f"{digest}  {target.name}\n")
(destination / "SHA256SUMS").write_text("".join(checksums))
print(f"Packaged {len(staged)} APKs for {version}; checksums written to {destination}")
