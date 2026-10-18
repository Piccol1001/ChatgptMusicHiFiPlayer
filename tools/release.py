#!/usr/bin/env python3
"""Build, verify and record a release. Failed builds never consume a version."""
import argparse
import datetime as dt
import fcntl
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import tempfile
import zipfile
from zoneinfo import ZoneInfo

ROOT = Path(__file__).resolve().parents[1]
SEMVER = re.compile(r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\Z")
FORBIDDEN = ("INTERNET", "ACCESS_NETWORK_STATE", "RECORD_AUDIO", "MANAGE_EXTERNAL_STORAGE")


def version_tuple(value):
    if not SEMVER.fullmatch(value):
        raise ValueError("Use a numeric MAJOR.MINOR.PATCH without leading zeros")
    return tuple(map(int, value.split('.')))


def candidate(history, bump):
    entries = history['releases']
    latest = max(entries, key=lambda r: r['versionCode'])
    major, minor, patch = version_tuple(latest['versionName'])
    name = {'major': (major + 1, 0, 0), 'minor': (major, minor + 1, 0),
            'patch': (major, minor, patch + 1)}[bump]
    code = max(r['versionCode'] for r in entries) + 1
    if code > 2100000000:
        raise ValueError('Android versionCode limit reached')
    return '.'.join(map(str, name)), code


def run(args, **kwargs):
    return subprocess.check_output(args, cwd=ROOT, text=True, **kwargs)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def inspect_apk(path, aapt, signer):
    with zipfile.ZipFile(path) as archive:
        if archive.testzip() is not None:
            raise ValueError('Corrupt APK ZIP entries')
        signature_entries = any(re.match(r'META-INF/[^/]+\.(SF|RSA|DSA|EC)$', n, re.I) for n in archive.namelist())
    badging = run([str(aapt), 'dump', 'badging', str(path)])
    match = re.search(r"package: name='([^']+)' versionCode='([0-9]+)' versionName='([^']+)'", badging)
    if not match:
        raise ValueError(f'Cannot read APK metadata: {path}')
    permissions = run([str(aapt), 'dump', 'permissions', str(path)])
    if any('android.permission.' + p in permissions for p in FORBIDDEN):
        raise ValueError('Offline/privacy permission policy changed')
    result = subprocess.run([str(signer), 'verify', '--print-certs', str(path)], cwd=ROOT,
                            text=True, capture_output=True)
    certs = re.findall(r'certificate SHA-256 digest: ([0-9a-fA-F]+)', result.stdout)
    if result.returncode == 0 and len(certs) != 1:
        raise ValueError('Expected exactly one established signing certificate')
    if result.returncode and (certs or signature_entries or b'APK Sig Block 42' in path.read_bytes()):
        raise ValueError('Invalid signed APK')
    return dict(applicationId=match[1], versionCode=int(match[2]), versionName=match[3],
                certificateSha256=certs[0].lower() if result.returncode == 0 else None)


def notes_section(entry, notes):
    text = f"## {entry['versionName']} — {entry['releaseDate']}\n\nAndroid versionCode: `{entry['versionCode']}`.\n"
    for key, title in [('features', 'New features'), ('fixes', 'Bug fixes'), ('ui', 'UI improvements')]:
        text += f'\n### {title}\n\n' + '\n'.join('- ' + x for x in notes[key]) + '\n'
    return text + '\n'


def release_section(entry):
    text = f"\n## {entry['versionName']} (code {entry['versionCode']}) — {entry['releaseDate']}\n\n"
    text += entry['dateEvidence'] + '\n\n'
    text += '| APK | Embedded versionName | Embedded versionCode | Signing | SHA-256 |\n| --- | --- | --- | --- | --- |\n'
    for a in entry['artifacts']:
        location = a['path']
        if a.get('gitCommit'):
            location = f"https://github.com/Piccol1001/ChatgptMusicHiFiPlayer/blob/{a['gitCommit']}/{location}"
        text += f"| [{Path(a['path']).name}]({location}) | {entry['versionName']} | {entry['versionCode']} | {a['signing']} | `{a['sha256']}` |\n"
    return text


def publish(root, history, entry, notes, staged):
    """Rollback ordinary I/O failures; never delete or replace an earlier artifact."""
    if entry['versionCode'] <= max(r['versionCode'] for r in history['releases']):
        raise ValueError('New release must have a strictly greater versionCode')
    if version_tuple(entry['versionName']) <= max(version_tuple(r['versionName']) for r in history['releases']):
        raise ValueError('New release must advance semantic version')
    destination = root / 'artifacts/apk'
    existing = [p for p in destination.glob('*.apk')]
    sums = ''.join(f'{sha(p)}  {p.name}\n' for p in sorted(existing + list(staged.values()), key=lambda p: p.name))
    history_next = dict(history, releases=history['releases'] + [entry])
    changelog = (root / 'CHANGELOG.md').read_text()
    marker = '<!-- releases -->\n'
    if changelog.count(marker) != 1:
        raise ValueError('CHANGELOG.md release marker missing or duplicated')
    prefix, old = changelog.split(marker)
    changes = {
        root / 'version.properties': f'# Updated only after a verified successful release; ordinary builds never increment.\nversionName={entry["versionName"]}\nversionCode={entry["versionCode"]}\n',
        root / 'releases/history.json': json.dumps(history_next, indent=2) + '\n',
        root / 'CHANGELOG.md': prefix + marker + '\n' + notes_section(entry, notes) + old,
        root / 'RELEASES.md': (root / 'RELEASES.md').read_text() + release_section(entry),
        destination / 'SHA256SUMS': sums,
    }
    backups = {p: p.read_bytes() for p in changes}
    created = []
    try:
        for target, source in staged.items():
            if target.exists():
                raise FileExistsError(f'Refusing to overwrite historical APK: {target}')
            # Exclusive create protects history even if another process ignores the release lock.
            with target.open('xb') as output, source.open('rb') as input_file:
                created.append(target)
                shutil.copyfileobj(input_file, output)
        for path, value in changes.items():
            temp = path.with_name(path.name + '.pending')
            temp.write_text(value)
            temp.replace(path)
    except BaseException:
        for p, content in backups.items():
            p.write_bytes(content)
        for p in created:
            p.unlink(missing_ok=True)
        raise
    finally:
        for p in changes:
            p.with_name(p.name + '.pending').unlink(missing_ok=True)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--bump', choices=['major', 'minor', 'patch'], required=True)
    parser.add_argument('--notes', type=Path, required=True, help='JSON with nonempty features/fixes/ui lists')
    args = parser.parse_args()
    notes = json.loads(args.notes.read_text())
    if set(notes) != {'features', 'fixes', 'ui'} or not all(
            isinstance(v, list) and v and all(isinstance(x, str) and x.strip() and '\n' not in x for x in v)
            for v in notes.values()):
        raise ValueError('Provide features, fixes and ui lists; use "None." for an unchanged category')
    lock_path = ROOT / 'build/release.lock'
    lock_path.parent.mkdir(exist_ok=True)
    with lock_path.open('w') as lock:
        fcntl.flock(lock, fcntl.LOCK_EX | fcntl.LOCK_NB)
        history = json.loads((ROOT / 'releases/history.json').read_text())
        name, code = candidate(history, args.bump)
        key = os.environ.get('HIFI_DEBUG_KEYSTORE')
        if not key:
            user_home = os.environ.get('ANDROID_USER_HOME')
            if not user_home:
                raise ValueError('Set ANDROID_USER_HOME or HIFI_DEBUG_KEYSTORE to the original update keystore')
            key = str(Path(user_home) / 'debug.keystore')
        if not Path(key).is_file():
            raise ValueError('Original keystore missing. Restore it; do not generate a replacement key.')
        # Debug signing credentials are Android's standard development credentials, not a new key.
        cert = subprocess.check_output(['keytool', '-exportcert', '-keystore', key,
                                       '-alias', 'androiddebugkey', '-storepass', 'android'], cwd=ROOT)
        if hashlib.sha256(cert).hexdigest() != history['updateCertificateSha256']:
            raise ValueError('Certificate mismatch: this key cannot update existing installed APKs')
        sdk = Path(os.environ['ANDROID_HOME'])
        build_tools = sdk / 'build-tools/35.0.0'
        # Every retained published APK must still match its immutable audit record.
        known = {a['path']: (r, a) for r in history['releases'] for a in r['artifacts']}
        for existing in (ROOT / 'artifacts/apk').glob('*.apk'):
            relative = str(existing.relative_to(ROOT))
            if relative not in known:
                raise ValueError(f'Unrecorded historical APK requires audit before releasing: {relative}')
            record, artifact = known[relative]
            info = inspect_apk(existing, build_tools / 'aapt', build_tools / 'apksigner')
            if sha(existing) != artifact['sha256'] or (info['versionName'], info['versionCode'], info['applicationId'], info['certificateSha256']) != (record['versionName'], record['versionCode'], history['applicationId'], artifact['certificateSha256']):
                raise ValueError(f'Historical APK changed: {relative}')
        print(f'Candidate {name} (code {code}); recorded version remains unchanged until verification succeeds.', flush=True)
        subprocess.run(['./gradlew', '--no-daemon', f'-PreleaseVersionName={name}',
                        f'-PreleaseVersionCode={code}', 'testDebugUnitTest', 'lintDebug',
                        'assembleDebug', 'assembleRelease'], cwd=ROOT, check=True)
        entry = dict(versionName=name, versionCode=code,
                     releaseDate=dt.datetime.now(ZoneInfo('Asia/Kuala_Lumpur')).date().isoformat(),
                     dateEvidence='Verified build/packaging date in Asia/Kuala_Lumpur; GitHub publication occurs separately.',
                     sourceCommit=run(['git', 'rev-parse', 'HEAD']).strip(),
                     sourceDirty=bool(run(['git', 'status', '--porcelain']).strip()),
                     changes=notes, artifacts=[])
        with tempfile.TemporaryDirectory(prefix='hifi-release-', dir=ROOT / 'build') as temp:
            staged = {}
            for variant, source_name in [('debug', 'app-debug.apk'), ('release', 'app-release-unsigned.apk')]:
                source = ROOT / f'app/build/outputs/apk/{variant}/{source_name}'
                copied = Path(temp) / f'{variant}.apk'
                shutil.copyfile(source, copied)
                info = inspect_apk(copied, build_tools / 'aapt', build_tools / 'apksigner')
                if (info['applicationId'], info['versionName'], info['versionCode']) != (history['applicationId'], name, code):
                    raise ValueError(f'Built APK metadata mismatch: {info}')
                cert = info['certificateSha256']
                if cert and cert != history['updateCertificateSha256']:
                    raise ValueError('APK update certificate changed')
                if variant == 'debug' and not cert:
                    raise ValueError('Installable evaluation APK must verify with the existing certificate')
                kind = 'debug' if variant == 'debug' else ('release' if cert else 'release-unsigned')
                filename = f'HiFiPlayer_v{name}_{kind}.apk'
                temporary = Path(temp) / filename
                copied.rename(temporary)
                target = ROOT / 'artifacts/apk' / filename
                staged[target] = temporary
                entry['artifacts'].append(dict(path=str(target.relative_to(ROOT)), sha256=sha(temporary),
                    signing='debug-signed' if variant == 'debug' else ('release-signed' if cert else 'unsigned'),
                    certificateSha256=cert))
            publish(ROOT, history, entry, notes, staged)
        print(json.dumps(entry, indent=2))
        print('Release recorded after successful verification. No Git commit, push or store publication was performed.')


if __name__ == '__main__':
    main()
