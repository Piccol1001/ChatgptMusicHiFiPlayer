#!/usr/bin/env bash
set -euo pipefail
# Tooling lives outside the checkout; existing application files are never rewritten.
project_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
tooling_dir=/workspace/.penguin-tools
mkdir -p "$tooling_dir"/{bin,data,config,cache}
export XDG_DATA_HOME="$tooling_dir/data"
export XDG_CONFIG_HOME="$tooling_dir/config"
export XDG_CACHE_HOME="$tooling_dir/cache"
version=4.6.3
release="${version}-stable"
base_url="https://github.com/godotengine/godot-builds/releases/download/$release"
fetch_verified() {
  local filename="$1" output="$2"
  curl --fail --location --retry 2 "$base_url/$filename" -o "$output"
  python3 - "$tooling_dir/SHA512-SUMS.txt" "$filename" "$output" <<'PY'
import hashlib, pathlib, sys
manifest, name, artifact = sys.argv[1:]
line = next(l for l in pathlib.Path(manifest).read_text().splitlines() if l.split()[-1].lstrip('*') == name)
assert hashlib.file_digest(open(artifact, 'rb'), 'sha512').hexdigest() == line.split()[0], 'Official SHA512 verification failed'
PY
}
engine="$(command -v godot || true)"
if [[ -z "$engine" ]] || [[ "$("$engine" --version)" != 4.6.3.stable.* ]]; then
  curl --fail --location --retry 2 "$base_url/SHA512-SUMS.txt" -o "$tooling_dir/SHA512-SUMS.txt"
  fetch_verified "Godot_v${release}_linux.x86_64.zip" "$tooling_dir/engine.zip"
  python3 - "$tooling_dir" "$release" <<'PY'
import pathlib, sys, zipfile
root, release = pathlib.Path(sys.argv[1]), sys.argv[2]
with zipfile.ZipFile(root/'engine.zip') as archive:
    name = f'Godot_v{release}_linux.x86_64'
    (root/'bin/godot').write_bytes(archive.read(name))
(root/'bin/godot').chmod(0o755)
PY
  engine="$tooling_dir/bin/godot"
fi
templates="$XDG_DATA_HOME/godot/export_templates/$version.stable"
if [[ ! -s "$templates/windows_release_x86_64.exe" || ! -s "$templates/linux_release.x86_64" ]]; then
  curl --fail --location --retry 2 "$base_url/SHA512-SUMS.txt" -o "$tooling_dir/SHA512-SUMS.txt"
  fetch_verified "Godot_v${release}_export_templates.tpz" "$tooling_dir/templates.tpz"
  python3 - "$tooling_dir/templates.tpz" "$templates" <<'PY'
import pathlib,sys,zipfile
root=pathlib.Path(sys.argv[2]); root.mkdir(parents=True,exist_ok=True)
with zipfile.ZipFile(sys.argv[1]) as archive:
 for name in ['windows_debug_x86_64.exe','windows_release_x86_64.exe','linux_debug.x86_64','linux_release.x86_64','version.txt']:
  (root/name).write_bytes(archive.read('templates/'+name))
PY
  rm "$tooling_dir/templates.tpz"
fi
cat > "$tooling_dir/env.sh" <<ENV
export XDG_DATA_HOME='$tooling_dir/data'
export XDG_CONFIG_HOME='$tooling_dir/config'
export XDG_CACHE_HOME='$tooling_dir/cache'
export PATH='$tooling_dir/bin':\$PATH
ENV
cd "$project_dir"
"$engine" --headless --editor --import --quit
"$engine" --headless --script tests/gameplay_test.gd
