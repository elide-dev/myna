# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""Report-only trufflecheck run: Error Prone + trufflecheck over the Java tree, clippy and cargo-deny over Rust.

Analyzer JARs and the trufflecheck profiles come from tools/trufflecheck.json, pinned by sha256 and cached in
$TRUFFLECHECK_CACHE (default /build/caches/trufflecheck when /build/caches exists, else $XDG_CACHE_HOME/trufflecheck),
as are the Truffle jars from tools/truffle.json when Elide's cache lacks them. The trufflecheck JAR's build provenance is checked once with `gh attestation verify` and the result is cached beside it.

Areas: the Truffle runtime (src/truffle), generated Truffle cells (tests/truffle/cells.seam in both backends and
examples/truffle.seam) with the binding classes they compile against, the generator/values/Feature Java, the Cargo
workspace, and generated seam.rs files. Findings never fail the run; a missing tool or a checksum or attestation
mismatch does. Reports land in build/trufflecheck: findings.json, summary.md and each tool's raw output.

Needs the JVM jars (`mise run build:jvm`), GRAALVM_HOME, cargo, and gh for the one-time attestation check.
"""
import argparse
from collections import Counter
import hashlib
import json
import os
from pathlib import Path
import platform
import re
import shutil
import subprocess
import sys
import tarfile
import tomllib

ROOT = Path(__file__).resolve().parents[1]
PINS = json.loads((ROOT / 'tools/trufflecheck.json').read_text())
TRUFFLE = json.loads((ROOT / 'tools/truffle.json').read_text())
TARGET = {('Darwin', 'arm64'): 'aarch64-apple-darwin', ('Darwin', 'x86_64'): 'x86_64-apple-darwin',
          ('Linux', 'aarch64'): 'aarch64-unknown-linux-gnu', ('Linux', 'x86_64'): 'x86_64-unknown-linux-gnu'}.get(
    (platform.system(), platform.machine()))
# javac's own module access for Error Prone (as trufflecheck's tests/smoke.sh passes it).
EXPORTS = [f'-J--add-exports=jdk.compiler/com.sun.tools.javac.{part}=ALL-UNNAMED'
           for part in ('api', 'code', 'comp', 'file', 'main', 'model', 'parser', 'processing', 'tree', 'util')]
EXPORTS += ['-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED',
            '-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED']
DIAGNOSTIC = re.compile(r'^(?P<file>[^\s:][^:]*\.java):(?P<line>\d+): (?P<severity>error|warning): \[(?P<rule>[^\]]+)\] (?P<message>.*)$')


def cache_dir():
    if os.environ.get('TRUFFLECHECK_CACHE'):
        return Path(os.environ['TRUFFLECHECK_CACHE'])
    if Path('/build/caches').is_dir():
        return Path('/build/caches/trufflecheck')
    return Path(os.environ.get('XDG_CACHE_HOME', Path.home() / '.cache')) / 'trufflecheck'


def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def fetch(cache, url, digest):
    path = cache / url.rsplit('/', 1)[1]
    if not path.is_file() or sha256(path) != digest:
        cache.mkdir(parents=True, exist_ok=True)
        part = path.with_name(path.name + '.part')
        subprocess.run(['curl', '--http1.1', '--fail', '--location', '--retry', '3', '--silent', '--show-error',
                        '--output', str(part), url], check=True)
        if sha256(part) != digest:
            part.unlink()
            raise SystemExit(f'checksum mismatch: {url}')
        part.replace(path)
    return path


def attest(path):
    """Verify the JAR's release-workflow provenance once per digest; the result is cached beside it."""
    record = path.with_name(path.name + '.attestation.json')
    digest = sha256(path)
    if record.is_file():
        cached = json.loads(record.read_text())
        if cached.get('sha256') == digest:
            return cached
    if shutil.which('gh') is None:
        return {'sha256': digest, 'verified': False, 'reason': 'gh is not installed'}
    result = subprocess.run(['gh', 'attestation', 'verify', str(path), '--repo', PINS['repository'], '--format', 'json'],
                            capture_output=True, text=True)
    if result.returncode != 0:
        raise SystemExit(f'attestation verification failed for {path.name}:\n{result.stderr}')
    verification = json.loads(result.stdout)[0]['verificationResult']
    certificate = verification['signature']['certificate']
    outcome = {'sha256': digest, 'verified': True, 'repository': PINS['repository'],
               'signer': certificate.get('buildSignerURI'), 'source_digest': certificate.get('sourceRepositoryDigest'),
               'run': certificate.get('runInvocationURI'), 'predicate': verification['statement']['predicateType']}
    record.write_text(json.dumps(outcome, indent=2) + '\n')
    return outcome


def profiles(cache):
    spec = PINS['profiles']
    archive = fetch(cache, spec['url'], spec['sha256'])
    root = cache / f'profiles-{PINS["version"]}'
    if not (root / 'profiles/jvm/errorprone.args').is_file():
        root.mkdir(parents=True, exist_ok=True)
        with tarfile.open(archive) as tar:
            tar.extractall(root, filter='data')
    return root / 'profiles'


def truffle_jars(cache):
    """The pinned Elide GraalVM Truffle jars (tools/truffle.json), reusing Elide's dependency cache."""
    repository = TRUFFLE['repository'].format(version=TRUFFLE['version'])
    jars = {}
    for name, artifact in TRUFFLE['artifacts'].items():
        local = [ROOT / '.dev/dependencies/m2' / artifact['path'],
                 ROOT / 'build/downloads/truffle' / Path(artifact['path']).name]
        found = next((p for p in local if p.is_file() and sha256(p) == artifact['sha256']), None)
        if found:
            jars[name] = found
        else:
            jars[name] = fetch(cache, f'{repository}/{artifact["path"]}', artifact['sha256'])
    return jars


def errorprone_args(profile):
    flags = []
    for line in profile.read_text().splitlines():
        line = line.split('#', 1)[0].strip()
        if line:
            flags.append(line)
    return flags


def javac(name, out, sources, classpath, analyzers, flags, options=()):
    """One Error Prone compilation; the log is kept and its findings returned.

    A check that crashes stops javac at that class. The crash is recorded as a finding, and the compilation is
    repeated with that check off so the remaining classes are still analyzed; earlier findings are kept.
    """
    home = Path(os.environ['GRAALVM_HOME'])
    argfile = out / f'{name}.sources'
    argfile.write_text('\n'.join(str(s) for s in sources) + '\n')
    findings, disabled, attempt = {}, [], 0
    while True:
        classes = out / 'classes' / name
        shutil.rmtree(classes, ignore_errors=True)
        classes.mkdir(parents=True)
        plugin = ('-Xplugin:ErrorProne ' + ' '.join([*flags, *(f'-Xep:{c}:OFF' for c in disabled)])
                  + f' -XepOpt:Trufflecheck:Policy={ROOT / "trufflecheck.properties"}')
        command = [str(home / 'bin/javac'), *EXPORTS, '-XDcompilePolicy=simple', '--should-stop=ifError=FLOW',
                   '-processorpath', os.pathsep.join(map(str, analyzers)), plugin, *options,
                   '-cp', os.pathsep.join(map(str, classpath)), '-d', str(classes), '-Xmaxerrs', '100000',
                   '-Xmaxwarns', '100000', '-encoding', 'UTF-8', f'@{argfile}']
        result = subprocess.run(command, capture_output=True, text=True, cwd=ROOT)
        text = result.stdout + result.stderr
        log = out / f'errorprone-{name}{"-" + str(attempt) if attempt else ""}.log'
        log.write_text(text)
        if re.search(r'InvalidCommandLineOption|plug-in not found', text):
            raise SystemExit(f'Error Prone did not load for {name}; see {log}')
        lines = text.splitlines()
        for index, line in enumerate(lines):
            match = DIAGNOSTIC.match(line)
            if not match and ': error: An unhandled exception was thrown by the Error Prone' in line:
                crashed = next((l.split('BugPattern:', 1)[1].strip() for l in lines[index:index + 12]
                                if 'BugPattern:' in l), 'unknown')
                where, number = line.split(': error:', 1)[0].rsplit(':', 1)
                finding = {'tool': 'errorprone', 'file': relative(where), 'line': int(number),
                           'rule': f'ErrorProneCrash({crashed})', 'severity': 'error',
                           'message': f'{crashed} threw; javac stopped here and the run was repeated without it'}
                findings[(finding['file'], finding['line'], finding['rule'])] = finding
                if crashed in disabled or crashed == 'unknown':
                    raise SystemExit(f'Error Prone crashed again in {name}; see {log}')
                disabled.append(crashed)
                break
            if match:
                rule = match['rule']
                finding = {'tool': 'errorprone' if rule[:1].isupper() else 'javac', 'file': relative(match['file']),
                           'line': int(match['line']), 'rule': rule, 'severity': match['severity'],
                           'message': match['message']}
                findings[(finding['file'], finding['line'], finding['rule'], finding['message'])] = finding
        else:
            return list(findings.values()), result.returncode, disabled
        attempt += 1


def relative(path):
    path = Path(path)
    if not path.is_absolute():
        path = ROOT / path
    try:
        return str(path.resolve().relative_to(ROOT))
    except ValueError:
        return str(path)


def generate(out, java, jar):
    """Truffle cell fixtures (both backends) and the ownership fixture's seam.rs, from the built generator JAR."""
    cells = out / 'cells'
    shutil.rmtree(cells, ignore_errors=True)
    cells.mkdir(parents=True)
    ffm = cells / 'cells-ffm.seam'
    ffm.write_text((ROOT / 'tests/truffle/cells.seam').read_text()
                   .replace('truffle_cells=true', 'truffle_cells=true truffle_backend=ffm'))
    seams = {'cells': ROOT / 'tests/truffle/cells.seam', 'cells-ffm': ffm, 'example': ROOT / 'examples/truffle.seam',
             'ownership': ROOT / 'tests/ownership/ownership.seam'}
    for name, seam in seams.items():
        subprocess.run([str(java), '--enable-native-access=ALL-UNNAMED', '-jar', str(jar), 'generate', str(seam),
                        '--out', str(cells / name), '--target', TARGET], check=True, cwd=ROOT, stdout=subprocess.DEVNULL)
    return cells


def lint_flags(profile):
    """trufflecheck's [workspace.lints] as -W flags, so every finding is reported, with the profile's level kept."""
    lints = tomllib.loads((profile / 'rust/lints.toml').read_text())['workspace']['lints']
    levels, flags = {}, []
    for tool, table in lints.items():
        for lint, level in table.items():
            name = lint if tool == 'rust' else f'{tool}::{lint}'
            levels[name] = level
            flags += ['-W', name]
    return levels, flags


def rust_message(message, levels, area, findings):
    code = (message.get('code') or {}).get('code')
    spans = [s for s in message.get('spans', []) if s.get('is_primary')]
    if not code or not spans:
        return
    span = spans[0]
    level = levels.get(code)
    severity = {'deny': 'error', 'forbid': 'error', 'warn': 'warning'}.get(level, message['level'])
    findings.append({'tool': 'clippy', 'area': area, 'file': relative(span['file_name']), 'line': span['line_start'],
                     'rule': code, 'severity': severity, 'profile_level': level or 'default',
                     'message': message['message']})


def clippy(out, profile, cells):
    levels, flags = lint_flags(profile)
    env = dict(os.environ, CLIPPY_CONF_DIR=str(profile / 'rust'), CARGO_TARGET_DIR=str(out / 'cargo-target'))
    findings = []
    result = subprocess.run(['cargo', 'clippy', '--workspace', '--all-targets', '--locked', '--message-format=json', '--',
                             *flags], capture_output=True, text=True, cwd=ROOT, env=env)
    (out / 'clippy-workspace.json').write_text(result.stdout)
    (out / 'clippy-workspace.log').write_text(result.stderr)
    for line in result.stdout.splitlines():
        record = json.loads(line)
        if record.get('reason') == 'compiler-message':
            rust_message(record['message'], levels, 'rust-workspace', findings)
    for name in ('ownership', 'cells'):
        seam = cells / name / 'seam.rs'
        result = subprocess.run(['clippy-driver', '--edition=2024', '--crate-type=lib', '--emit=metadata',
                                 '--error-format=json', '-o', str(out / f'libseam_{name}.rmeta'), str(seam), *flags],
                                capture_output=True, text=True, cwd=ROOT, env=env)
        (out / f'clippy-seam-{name}.json').write_text(result.stderr)
        for line in result.stderr.splitlines():
            if line.startswith('{'):
                rust_message(json.loads(line), levels, 'rust-seam', findings)
    unique = {(f['file'], f['line'], f['rule'], f['message']): f for f in findings}
    return list(unique.values())


def cargo_deny(out, profile):
    tool = shutil.which('cargo-deny')
    where = subprocess.run(['mise', 'where', 'aqua:EmbarkStudios/cargo-deny'], capture_output=True, text=True)
    if where.returncode == 0:
        pinned = sorted(Path(where.stdout.strip()).glob('**/cargo-deny'))
        tool = str(pinned[0]) if pinned else tool
    if tool is None:
        return [], {'version': 'not installed (pinned in mise.toml)'}
    result = subprocess.run([tool, '--manifest-path', str(ROOT / 'Cargo.toml'), '--format', 'json', '--config',
                             str(profile / 'rust/deny.toml'), 'check'], capture_output=True, text=True, cwd=ROOT)
    (out / 'cargo-deny.json').write_text(result.stderr)
    findings, totals = [], None
    for line in result.stderr.splitlines():
        if not line.startswith('{'):
            continue
        record = json.loads(line)
        if record.get('type') == 'summary':
            totals = record['fields']
        if record.get('type') != 'diagnostic':
            continue
        fields = record['fields']
        if fields['severity'] not in ('error', 'warning'):
            continue
        label = (fields.get('labels') or [{}])[0]
        findings.append({'tool': 'cargo-deny', 'area': 'rust-workspace', 'file': 'Cargo.lock',
                         'line': label.get('line', 0), 'rule': f'deny:{fields.get("code", "unknown")}',
                         'severity': fields['severity'], 'message': fields['message'] + (
                             f' ({label["span"]})' if label.get('span') else '')})
    if totals is None:
        raise SystemExit(f'cargo-deny produced no summary; see {out / "cargo-deny.json"}')
    version = subprocess.run([tool, '--version'], capture_output=True, text=True).stdout.strip()
    return findings, {'version': version, 'summary': totals}


def area(finding):
    name = finding['file']
    if name.startswith('src/truffle/'):
        return 'runtime'
    if name.startswith('tests/'):
        return 'fixture'
    if name.startswith('build/trufflecheck/cells/'):
        return 'cells' if name.endswith('Truffle.java') else 'cell-bindings'
    return 'java'


def summary(out, findings, meta):
    lines = ['# trufflecheck run', '', f'Commit `{meta["commit"]}`; trufflecheck {meta["version"]}.', '',
             '| Area | Tool | Rule | Severity | Count |', '| :--- | :--- | :--- | :--- | ---: |']
    counts = Counter((f['area'], f['tool'], f['rule'], f['severity']) for f in findings)
    for (where, tool, rule, severity), count in sorted(counts.items()):
        lines.append(f'| {where} | {tool} | `{rule}` | {severity} | {count} |')
    lines += ['', '## Findings', '']
    for f in sorted(findings, key=lambda f: (f['area'], f['severity'], f['rule'], f['file'], f['line'])):
        lines.append(f'- {f["area"]} {f["severity"]} `{f["rule"]}` {f["file"]}:{f["line"]}: {f["message"]}')
    (out / 'summary.md').write_text('\n'.join(lines) + '\n')
    totals = Counter((f['area'], f['severity']) for f in findings)
    for (where, severity), count in sorted(totals.items()):
        print(f'{where:16} {severity:8} {count}')


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--skip-rust', action='store_true', help='Run only the Java checks')
    args = parser.parse_args()
    out = ROOT / 'build/trufflecheck'
    out.mkdir(parents=True, exist_ok=True)
    cache = cache_dir()
    analyzers = {name: fetch(cache, spec['url'], spec['sha256']) for name, spec in PINS['analyzers'].items()}
    attestation = attest(analyzers['trufflecheck-errorprone'])
    profile = profiles(cache)
    flags = errorprone_args(profile / 'jvm/errorprone.args')
    truffle = truffle_jars(cache)
    home = Path(os.environ['GRAALVM_HOME'])
    jar = ROOT / '.dev/artifacts/jar/myna/myna.jar'
    truffle_jar = ROOT / '.dev/artifacts/jar/myna-truffle/myna-truffle.jar'
    for built in (jar, truffle_jar):
        if not built.is_file():
            raise SystemExit(f'{built.relative_to(ROOT)} is missing; run `mise run build:jvm`')
    cells = generate(out, home / 'bin/java', jar)
    checkers = [analyzers[n] for n in ('error_prone_core', 'dataflow-errorprone', 'nullaway', 'dataflow-nullaway',
                                       'trufflecheck-errorprone')]
    sdk = [home / 'lib/svm' / name for name in ('svm-shared.jar', 'svm-guest-staging.jar', 'svm-guest.jar')]
    api = [truffle[n] for n in ('truffle-api', 'polyglot', 'collections', 'nativeimage', 'word')]
    findings, exits, crashed = [], {}, {}

    def tree(path):
        return sorted((ROOT / path).rglob('*.java'))

    # (a) The runtime, with the Truffle DSL processor as Elide runs it; DSL output lands under an excluded path.
    found, exits['runtime'], crashed['runtime'] = javac('runtime', out, tree('src/truffle/java'), [*api, analyzers['jspecify']],
                                    [*checkers, truffle['truffle-dsl-processor']], flags,
                                    ('-proc:full', '-s', str(out / 'dsl/generated')))
    findings += found
    # (b) Generated cells, compiled with the bindings they call, as tests/truffle_integration.py compiles them.
    # The SVM backend's Directives name the fixture's own @CContext directives; FFM cells need no C context.
    svm = ('CellApi', 'CellApiConstants', 'CellApiTruffle', 'CellApiOwnership', 'CellApiDirectives')
    ffm = ('CellApiConstants', 'CellApiFFM', 'CellApiTruffle', 'CellApiForeignFeature')
    for name in ('cells', 'cells-ffm', 'example'):
        sources = [cells / name / f'{n}.java' for n in (ffm if name == 'cells-ffm' else svm)]
        sources = [s for s in sources if s.is_file()]
        if name == 'cells':
            sources.append(ROOT / 'tests/truffle/Directives.java')
        options = ('-proc:none',) if name == 'cells-ffm' else ('-proc:none', '--module-path', os.pathsep.join(map(str, api)))
        found, exits[name], crashed[name] = javac(name, out, sources, [*sdk, truffle_jar, *truffle.values(), analyzers['jspecify']],
                                   checkers, flags, options)
        findings += found
    # (c) Generator, value reader and hosted Feature: the non-Truffle profile checks.
    found, exits['java'], crashed['java'] = javac('java', out, tree('src/main/java') + tree('src/values/java') + tree('src/feature/java'),
                                 [*sdk, truffle['nativeimage'], truffle['word'], analyzers['jspecify']], checkers, flags,
                                 ('-proc:none',))
    findings += found
    for finding in findings:
        finding['area'] = area(finding)
    deny_version = 'skipped'
    if not args.skip_rust:
        findings += clippy(out, profile, cells)
        deny, deny_version = cargo_deny(out, profile)
        findings += deny
    commit = subprocess.run(['git', 'rev-parse', 'HEAD'], capture_output=True, text=True, cwd=ROOT).stdout.strip()
    meta = {'commit': commit, 'version': PINS['version'], 'attestation': attestation, 'javac_exit': exits, 'crashed_checks': crashed,
            'cargo_deny': deny_version, 'analyzers': {n: PINS['analyzers'][n]['sha256'] for n in PINS['analyzers']},
            'profiles_sha256': PINS['profiles']['sha256'],
            'clippy': subprocess.run(['cargo', 'clippy', '--version'], capture_output=True, text=True).stdout.strip(),
            'javac': subprocess.run([str(home / 'bin/javac'), '-version'], capture_output=True, text=True).stdout.strip()}
    (out / 'findings.json').write_text(json.dumps({'meta': meta, 'findings': findings}, indent=1) + '\n')
    summary(out, findings, meta)
    print(f'trufflecheck reports: {(out / "summary.md").relative_to(ROOT)}, {(out / "findings.json").relative_to(ROOT)}')


if __name__ == '__main__':
    sys.exit(main())
