#!/usr/bin/env python3
"""Exercise deploy success, health failure, rollback, and pull failure without EC2."""
import os
from pathlib import Path
import subprocess
import tempfile
import unittest

SCRIPT = Path(__file__).resolve().parents[1] / 'deploy/ec2-deploy.sh'
NEW = 'ghcr.io/test/backend@sha256:' + 'a' * 64
OLD = 'ghcr.io/test/backend@sha256:' + 'b' * 64

class DeployTests(unittest.TestCase):
    def run_deploy(self, mode):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            (root / '.env').write_text('')
            (root / 'release.env').write_text(f'IMAGE={OLD}\n')
            bin_path = root / 'bin'
            bin_path.mkdir()
            for name, body in {
                'flock': '#!/bin/sh\nexit 0\n',
                'sleep': '#!/bin/sh\nexit 0\n',
                'docker': '''#!/bin/sh
printf '%s\\n' "$*" >> "$DEPLOY_DIR/commands"
case "$*" in
  *"ps -q app"*) echo existing ;;
  *".Config.Image"*) echo "$OLD" ;;
  *".State.Health"*)
    if [ "$MODE" = health_failure ] && grep -q aaaaa "$DEPLOY_DIR/release.env"; then echo unhealthy; else echo healthy; fi ;;
  *"pull app"*) [ "$MODE" != pull_failure ] ;;
esac
''',
            }.items():
                file = bin_path / name
                file.write_text(body)
                file.chmod(0o755)
            env = dict(os.environ, DEPLOY_DIR=str(root), PATH=str(bin_path) + ':' + os.environ['PATH'], MODE=mode, OLD=OLD)
            result = subprocess.run(['bash', str(SCRIPT), NEW], env=env, capture_output=True, text=True)
            return result, (root / 'release.env').read_text(), (root / 'commands').read_text()

    def test_success(self):
        result, release, _ = self.run_deploy('success')
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertIn(NEW, release)

    def test_health_failure_rolls_back(self):
        result, release, commands = self.run_deploy('health_failure')
        self.assertNotEqual(0, result.returncode)
        self.assertIn(OLD, release)
        self.assertIn('up -d --no-deps app', commands)
        self.assertIn('restored and healthy', result.stderr)

    def test_pull_failure_rolls_back(self):
        result, release, _ = self.run_deploy('pull_failure')
        self.assertNotEqual(0, result.returncode)
        self.assertIn(OLD, release)

if __name__ == '__main__':
    unittest.main()
