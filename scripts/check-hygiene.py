"""Check local documentation links, licensing metadata, and release policy files."""
from pathlib import Path
import hashlib
import json
import re
import subprocess
import tomllib
import urllib.parse
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
required = ("LICENSE", "LICENSE-APACHE", "LICENSE-MIT", "SECURITY.md", "CONTRIBUTING.md",
            "THIRD_PARTY.md", ".github/CODEOWNERS", ".github/PULL_REQUEST_TEMPLATE.md")
for name in required:
    assert (ROOT / name).is_file(), f"Missing {name}"
assert 'Apache-2.0 OR MIT' in (ROOT / 'LICENSE').read_text()
workspace = tomllib.loads((ROOT / 'Cargo.toml').read_text())
assert workspace['workspace']['package']['license'] == 'Apache-2.0 OR MIT'
for name in ('LICENSE-APACHE', 'LICENSE-MIT'):
    assert (ROOT / name).read_bytes() == (ROOT / 'crates/myna-values' / name).read_bytes(), name
for name in ('pom.xml', 'benchmarks/pom.xml'):
    root = ET.parse(ROOT / name).getroot()
    assert len(root.findall('{*}licenses/{*}license')) == 2, name

# Local targets only: external link availability should not make PR gates flaky.
files = subprocess.check_output(['git', 'ls-files', '-z', '*.md'], cwd=ROOT).decode().split('\0')
errors = []
for name in filter(None, files):
    if name.startswith('third-party/'):
        continue
    source = ROOT / name
    for target in re.findall(r'\]\(([^)]+)\)', source.read_text()):
        target = target.strip().split(' "', 1)[0].strip('<>')
        url = urllib.parse.urlsplit(target)
        if url.scheme or url.netloc or not url.path:
            continue
        # Documentation snippets may contain placeholders instead of real paths.
        if any(marker in url.path for marker in ('<', '>', '{', '}')):
            continue
        path = (ROOT if url.path.startswith('/') else source.parent) / urllib.parse.unquote(url.path.lstrip('/'))
        if not path.exists():
            errors.append(f'{name}: missing local target {target}')
if errors:
    raise SystemExit('\n'.join(errors))
for name, digest in json.loads((ROOT / 'docs/charts/svm-boundaries.manifest.json').read_text()).items():
    assert hashlib.sha256((ROOT / name).read_bytes()).hexdigest() == digest, f'Stale chart input/output: {name}'
print('Licensing, policy files, and local documentation links verified')
