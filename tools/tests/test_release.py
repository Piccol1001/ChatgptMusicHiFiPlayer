import importlib.util
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch
import hashlib

spec = importlib.util.spec_from_file_location('hifi_release', Path(__file__).parents[1] / 'release.py')
r = importlib.util.module_from_spec(spec)
spec.loader.exec_module(r)


class ReleaseSafetyTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        (self.root / 'artifacts/apk').mkdir(parents=True)
        (self.root / 'releases').mkdir()
        self.history = dict(applicationId='com.piccol.hifi', updateCertificateSha256=hashlib.sha256(b'cert').hexdigest(),
                            releases=[dict(versionName='0.5.0', versionCode=5, releaseDate='2026-10-17', artifacts=[])])
        self.notes = dict(features=['None.'], fixes=['Release verification.'], ui=['Installed version.'])
        self.entry = dict(versionName='0.5.1', versionCode=6, releaseDate='2026-10-17', dateEvidence='Test', artifacts=[])
        for name, value in [('version.properties', 'versionName=0.5.0\nversionCode=5\n'),
                            ('releases/history.json', json.dumps(self.history)),
                            ('CHANGELOG.md', '# Changelog\n<!-- releases -->\n\nKEEP HISTORICAL TEXT\n'),
                            ('RELEASES.md', '# Releases\nKEEP HISTORY\n'), ('artifacts/apk/SHA256SUMS', '')]:
            (self.root / name).write_text(value)

    def snapshot(self):
        return {str(p.relative_to(self.root)): p.read_bytes() for p in self.root.rglob('*') if p.is_file() and 'build' not in p.parts}

    def test_failed_build_twice_does_not_consume_version_or_change_records(self):
        (self.root / 'debug.keystore').write_bytes(b'key')
        notes = self.root / 'notes.json'
        notes.write_text(json.dumps(self.notes))
        before = self.snapshot()
        with patch.object(r, 'ROOT', self.root), patch.dict(r.os.environ, {
                'HIFI_DEBUG_KEYSTORE': str(self.root / 'debug.keystore'), 'ANDROID_HOME': str(self.root)}), \
                patch.object(r.subprocess, 'check_output', return_value=b'cert'), \
                patch.object(r.subprocess, 'run', side_effect=subprocess.CalledProcessError(1, 'gradlew')) as build, \
                patch('sys.argv', ['release.py', '--bump', 'patch', '--notes', str(notes)]):
            for _ in range(2):
                with self.assertRaises(subprocess.CalledProcessError):
                    r.main()
                self.assertEqual(before, self.snapshot())
            for call in build.call_args_list:
                self.assertIn('-PreleaseVersionCode=6', call.args[0])
                self.assertIn('-PreleaseVersionName=0.5.1', call.args[0])

    def test_mismatched_key_is_rejected_before_build(self):
        (self.root / 'debug.keystore').write_bytes(b'key')
        notes = self.root / 'notes.json'; notes.write_text(json.dumps(self.notes))
        with patch.object(r, 'ROOT', self.root), patch.dict(r.os.environ, {'HIFI_DEBUG_KEYSTORE': str(self.root / 'debug.keystore')}), \
                patch.object(r.subprocess, 'check_output', return_value=b'wrong cert'), \
                patch.object(r.subprocess, 'run') as build, \
                patch('sys.argv', ['release.py', '--bump', 'patch', '--notes', str(notes)]):
            with self.assertRaisesRegex(ValueError, 'Certificate mismatch'):
                r.main()
            build.assert_not_called()

    def test_existing_artifact_cannot_be_overwritten(self):
        old = self.root / 'artifacts/apk/HiFiPlayer_v0.5.1_debug.apk'
        old.write_bytes(b'historical')
        incoming = self.root / 'candidate.apk'; incoming.write_bytes(b'replacement')
        before = self.snapshot()
        with self.assertRaises(FileExistsError):
            r.publish(self.root, self.history, self.entry, self.notes, {old: incoming})
        self.assertEqual(before, self.snapshot())

    def test_io_failure_rolls_back_metadata_and_artifacts(self):
        incoming = self.root / 'candidate.apk'; incoming.write_bytes(b'candidate')
        target = self.root / 'artifacts/apk/HiFiPlayer_v0.5.1_debug.apk'
        before = self.snapshot()
        original = Path.replace
        def fail_history(path, destination):
            if path.name == 'history.json.pending':
                raise OSError('disk failure')
            return original(path, destination)
        with patch.object(Path, 'replace', fail_history):
            with self.assertRaises(OSError):
                r.publish(self.root, self.history, self.entry, self.notes, {target: incoming})
        self.assertEqual(before, self.snapshot())

    def test_success_keeps_old_artifact_and_historical_text(self):
        old = self.root / 'artifacts/apk/old.apk'; old.write_bytes(b'old')
        incoming = self.root / 'new.apk'; incoming.write_bytes(b'new')
        target = self.root / 'artifacts/apk/HiFiPlayer_v0.5.1_debug.apk'
        r.publish(self.root, self.history, self.entry, self.notes, {target: incoming})
        new_history = json.loads((self.root / 'releases/history.json').read_text())
        self.assertEqual(self.history['releases'], new_history['releases'][:-1])
        self.assertEqual(b'old', old.read_bytes())
        self.assertIn('KEEP HISTORICAL TEXT', (self.root / 'CHANGELOG.md').read_text())
        self.assertIn('KEEP HISTORY', (self.root / 'RELEASES.md').read_text())
        self.assertIn('versionCode=6', (self.root / 'version.properties').read_text())
        self.assertIn(hashlib.sha256(b'old').hexdigest(), (self.root / 'artifacts/apk/SHA256SUMS').read_text())

    def test_semver_classification_and_monotonic_code(self):
        for bump, name in [('major', '1.0.0'), ('minor', '0.6.0'), ('patch', '0.5.1')]:
            self.assertEqual((name, 6), r.candidate(self.history, bump))
        with self.assertRaises(ValueError):
            r.version_tuple('00.5.1')
        self.entry['versionCode'] = 5
        with self.assertRaises(ValueError):
            r.publish(self.root, self.history, self.entry, self.notes, {})


if __name__ == '__main__':
    unittest.main()
