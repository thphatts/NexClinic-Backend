#!/usr/bin/env python3
"""Verify the workflow exports the pushed GHCR reference, not a local alias."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

WORKFLOW = Path(__file__).resolve().parents[1] / '.github/workflows/backend-ci.yml'
IMAGE = 'ghcr.io/test/backend@sha256:' + 'a' * 64
LOCAL = 'nexclinic-backend@sha256:' + 'b' * 64


class PublishTests(unittest.TestCase):
    def run_publish(self, digests, inspect_exit=0):
        block = WORKFLOW.read_text().split('      - name: Publish the tested and scanned image\n', 1)[1]
        block = block.split('        run: |\n', 1)[1].split('\n  deploy:', 1)[0]
        script = '\n'.join(line[10:] for line in block.splitlines())
        script = 'repo_digests=' + script.split('repo_digests=', 1)[1]
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            docker = root / 'docker'
            docker.write_text('''#!/bin/sh
case "$1" in
  inspect) printf '%s\\n' "$TEST_DIGESTS"; exit "$TEST_INSPECT_EXIT" ;;
esac
''')
            docker.chmod(0o755)
            output = root / 'output'
            env = dict(os.environ, PATH=tmp + ':' + os.environ['PATH'],
                       image='ghcr.io/test/backend', GITHUB_SHA='test-commit', GITHUB_OUTPUT=str(output),
                       TEST_DIGESTS=digests, TEST_INSPECT_EXIT=str(inspect_exit))
            result = subprocess.run(['bash', '-e', '-o', 'pipefail', '-c', script],
                                    env=env, capture_output=True, text=True)
            return result, output.read_text() if output.exists() else ''

    def test_local_alias_before_ghcr(self):
        result, output = self.run_publish(LOCAL + '\n' + IMAGE)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('image=' + IMAGE + '\n', output)

    def test_ghcr_before_local_alias(self):
        result, output = self.run_publish(IMAGE + '\n' + LOCAL)
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual('image=' + IMAGE + '\n', output)

    def test_missing_or_invalid_ghcr_digest(self):
        for digests in ['', LOCAL, IMAGE.replace('/test/', '/other/'),
                        'ghcr.io/test/backend@sha256:invalid']:
            with self.subTest(digests=digests):
                result, output = self.run_publish(digests)
                self.assertNotEqual(0, result.returncode)
                self.assertEqual('', output)

    def test_inspect_failure(self):
        result, output = self.run_publish(IMAGE, inspect_exit=1)
        self.assertNotEqual(0, result.returncode)
        self.assertEqual('', output)


if __name__ == '__main__':
    unittest.main()
