#!/usr/bin/env python3
"""Reject edits/deletions of Flyway migrations already present on a base ref."""
import re
import subprocess
import sys
from pathlib import Path

base = sys.argv[1]
root = 'src/main/resources/db/migration'
tracked = subprocess.check_output(['git', 'ls-tree', '-r', '--name-only', base, root], text=True).splitlines()
for name in tracked:
    if name.endswith('.sql'):
        old = subprocess.check_output(['git', 'show', f'{base}:{name}'])
        assert Path(name).exists() and Path(name).read_bytes() == old, f'Applied migration changed: {name}. Add a new migration.'
versions = set()
for migration in Path(root).glob('*.sql'):
    match = re.fullmatch(r'V(\d+)__[a-zA-Z0-9_]+\.sql', migration.name)
    assert match, f'Invalid migration name: {migration.name}'
    version = int(match.group(1))
    assert version not in versions, f'Duplicate migration version: {version}'
    versions.add(version)
print(f'PASS {len(versions)} migration names and immutable migrations from {base}')
