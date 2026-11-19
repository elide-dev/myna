# Copyright the Myna contributors.
# SPDX-License-Identifier: Apache-2.0 OR MIT
"""scripts/trufflecheck.py fails closed: analyzer failures, missing provenance and spaced checkouts.

Each test runs the real script from a fake checkout whose path contains a space, against stub analyzers (javac, java,
cargo, clippy-driver, cargo-deny, gh) and a pre-filled analyzer cache, so no real analyzer or network is used.
"""
import hashlib
import importlib.util
import io
import json
import os
from pathlib import Path
import shutil
import subprocess
import sys
import tarfile
import tempfile
import unittest

ROOT = Path(__file__).resolve().parents[1]
SCRIPT = ROOT / 'scripts/trufflecheck.py'
spec = importlib.util.spec_from_file_location('trufflecheck', SCRIPT)
trufflecheck = importlib.util.module_from_spec(spec)
spec.loader.exec_module(trufflecheck)

# One stub, dispatched on its name. The scenario (STUB, JSON) overrides each tool's output and exit code; every call is
# appended to STUB_LOG. The javac stub parses its @argfile as javac does (quotes, backslash escapes inside quotes) and
# rejects missing sources or a missing policy file with javac's own invalid-flag error and exit code.
STUB = r'''
import json, os, sys
from pathlib import Path

tool = Path(sys.argv[0]).name
args = sys.argv[1:]
scenario = json.loads(os.environ.get('STUB', '{}'))
with open(os.environ['STUB_LOG'], 'a') as log:
    log.write(json.dumps({'tool': tool, 'args': args, 'cwd': os.getcwd()}) + '\n')


def finish(spec, stdout='', stderr='', code=0):
    spec = spec or {}
    sys.stdout.write(spec.get('stdout', stdout))
    sys.stderr.write(spec.get('stderr', stderr))
    sys.exit(spec.get('exit', code))


def tokens(text):
    """javac's argument-file tokenizer (com.sun.tools.javac.main.CommandLine)."""
    out, current, quote, started, i = [], [], None, False, 0
    while i < len(text):
        c = text[i]
        if quote is None and c in ' \t\f\n\r':
            if started:
                out.append(''.join(current))
            current, started = [], False
        elif quote is not None and c in '\n\r':
            raise SystemExit('unterminated quote')
        elif c in '"\'':
            started = True
            if quote is None:
                quote = c
            elif quote == c:
                quote = None
            else:
                current.append(c)
        elif c == '\\' and quote is not None:
            i += 1
            current.append(text[i])
        else:
            started = True
            current.append(c)
        i += 1
    if started:
        out.append(''.join(current))
    return out


if tool == 'javac':
    if args == ['-version']:
        finish(None, 'javac 25-stub\n')
    name = Path(args[args.index('-d') + 1]).name
    for arg in args:
        if arg.startswith('-Xplugin:'):
            for word in arg.split()[1:]:
                if word.startswith('-XepOpt:Trufflecheck:Policy=') and not Path(word.split('=', 1)[1]).is_file():
                    finish(None, '', f'error: invalid flag: {word}\n', 2)
    argfile = next(a[1:] for a in args if a.startswith('@'))
    sources = tokens(Path(argfile).read_text())
    for source in sources:
        if not Path(source).is_file():
            finish(None, '', f'error: invalid flag: {source}\n', 2)
    spec = (scenario.get('javac') or {}).get(name)
    if spec and spec.get('finding'):
        level, rule = spec['finding']
        code = 1 if level == 'error' else 0
        finish(None, '', f'{sources[0]}:3: {level}: [{rule}] stub finding\n1 {level}\n', code)
    finish(spec)
if tool == 'java':
    Path(args[args.index('--out') + 1]).mkdir(parents=True)
    finish(scenario.get('java'))
if tool == 'cargo':
    if args == ['clippy', '--version']:
        finish(None, 'clippy 0.1.0-stub\n')
    finish(scenario.get('cargo'), json.dumps({'reason': 'build-finished', 'success': True}) + '\n')
if tool == 'clippy-driver':
    finish(scenario.get('clippy-driver'))
if tool == 'cargo-deny':
    if args == ['--version']:
        finish(None, 'cargo-deny 0.0.0-stub\n')
    summary = {'type': 'summary', 'fields': {'advisories': {'errors': 0, 'warnings': 0}}}
    finish(scenario.get('cargo-deny'), '', json.dumps(summary) + '\n')
if tool == 'gh':
    verified = [{'verificationResult': {
        'signature': {'certificate': {'buildSignerURI': 'https://github.com/stub/release.yml@refs/tags/v0',
                                      'sourceRepositoryDigest': '0' * 40, 'runInvocationURI': 'https://stub/run'}},
        'statement': {'predicateType': 'https://slsa.dev/provenance/v1'}}}]
    finish(scenario.get('gh'), json.dumps(verified))
if tool == 'git':
    finish(None, '0123456789abcdef\n')
if tool == 'mise':
    finish(None, '', 'not managed by mise\n', 1)
sys.exit(f'unexpected stub tool {tool}')
'''


def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def rust_error(rule, level='error'):
    """A rustc/clippy JSON diagnostic with a code and a primary span."""
    return {'level': level, 'message': f'{rule} stub', 'code': {'code': rule},
            'spans': [{'is_primary': True, 'file_name': 'src/lib.rs', 'line_start': 7}]}


class Checkout:
    """A fake Myna checkout under a directory with a space, a pre-filled analyzer cache and stub tools."""

    def __init__(self, base, gh=True):
        self.base = base
        self.root = base / 'My Projects' / 'myna'
        self.cache = base / 'analyzer cache'
        self.home = base / 'graal home'
        self.bin = base / 'bin'
        self.log = base / 'stub.log'
        for directory in (self.root / 'scripts', self.root / 'tools', self.cache, self.home / 'bin', self.bin):
            directory.mkdir(parents=True)
        shutil.copy(SCRIPT, self.root / 'scripts/trufflecheck.py')
        shutil.copy(ROOT / 'trufflecheck.properties', self.root / 'trufflecheck.properties')
        self.write('src/truffle/java/dev/elide/myna/truffle/Runtime.java', 'final class Runtime {}\n')
        self.write('src/main/java/dev/elide/myna/Main.java', 'final class Main {}\n')
        self.write('src/values/java/dev/elide/myna/values/Values.java', 'final class Values {}\n')
        self.write('src/feature/java/dev/elide/myna/feature/Feature.java', 'final class Feature {}\n')
        self.write('tests/truffle/cells.seam', 'library cells truffle_cells=true\n')
        self.write('tests/truffle/Directives.java', 'final class Directives {}\n')
        self.write('tests/ownership/ownership.seam', 'library ownership\n')
        self.write('examples/truffle.seam', 'library example truffle_cells=true\n')
        self.write('.dev/artifacts/jar/myna/myna.jar', 'jar')
        self.write('.dev/artifacts/jar/myna-truffle/myna-truffle.jar', 'jar')
        self.pins()
        for tool in ('javac', 'java'):
            self.stub(self.home / 'bin' / tool)
        for tool in ('cargo', 'clippy-driver', 'cargo-deny', 'git', 'mise', *(('gh',) if gh else ())):
            self.stub(self.bin / tool)

    def write(self, path, text):
        target = self.root / path
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(text)
        return target

    def stub(self, path):
        path.write_text(f'#!{sys.executable}\n{STUB}')
        path.chmod(0o755)

    def pins(self):
        real = json.loads((ROOT / 'tools/trufflecheck.json').read_text())
        for name, entry in real['analyzers'].items():
            jar = self.cache / entry['url'].rsplit('/', 1)[1]
            jar.write_text(f'stub {name}\n')
            entry['sha256'] = digest(jar)
        archive = self.cache / real['profiles']['url'].rsplit('/', 1)[1]
        files = {'profiles/jvm/errorprone.args': '# stub profile\n-Xep:TruffleGenericCatch:ERROR\n',
                 'profiles/rust/lints.toml': '[workspace.lints.clippy]\nunwrap_used = "deny"\n'
                                             '[workspace.lints.rust]\nunsafe_code = "warn"\n',
                 'profiles/rust/deny.toml': '[advisories]\n'}
        with tarfile.open(archive, 'w:gz') as tar:
            for name, text in files.items():
                data = text.encode()
                info = tarfile.TarInfo(name)
                info.size = len(data)
                tar.addfile(info, io.BytesIO(data))
        real['profiles']['sha256'] = digest(archive)
        (self.root / 'tools/trufflecheck.json').write_text(json.dumps(real, indent=2))
        truffle = json.loads((ROOT / 'tools/truffle.json').read_text())
        for name, artifact in truffle['artifacts'].items():
            jar = self.write(f'.dev/dependencies/m2/{artifact["path"]}', f'stub {name}\n')
            artifact['sha256'] = digest(jar)
        (self.root / 'tools/truffle.json').write_text(json.dumps(truffle, indent=2))

    def run(self, *args, scenario=None, **env):
        self.log.write_text('')
        environment = {'PATH': str(self.bin), 'HOME': str(self.base), 'GRAALVM_HOME': str(self.home),
                       'TRUFFLECHECK_CACHE': str(self.cache), 'STUB_LOG': str(self.log),
                       'STUB': json.dumps(scenario or {}), **env}
        return subprocess.run([sys.executable, str(self.root / 'scripts/trufflecheck.py'), *args],
                              capture_output=True, text=True, env=environment, cwd=self.base, timeout=120)

    def report(self):
        return json.loads((self.root / 'build/trufflecheck/findings.json').read_text())

    def calls(self, tool):
        return [c for c in map(json.loads, self.log.read_text().splitlines()) if c['tool'] == tool]


@unittest.skipIf(os.name == 'nt' or ' ' in sys.executable, 'stub executables need a POSIX shebang without spaces')
class TrufflecheckRunTest(unittest.TestCase):
    def checkout(self, **options):
        base = Path(tempfile.mkdtemp(prefix='trufflecheck-test-'))
        self.addCleanup(shutil.rmtree, base)
        return Checkout(base, **options)

    def assertFailed(self, result, analysis, checkout):
        self.assertEqual(result.returncode, 1, result.stdout + result.stderr)
        self.assertIn(f'ERROR: {analysis} failed', result.stderr)
        self.assertIn('trufflecheck FAILED', result.stderr)
        failures = checkout.report()['meta']['failures']
        self.assertIn(analysis, [f['analysis'] for f in failures])
        summary = (checkout.root / 'build/trufflecheck/summary.md').read_text()
        self.assertIn('FAILED', summary)
        self.assertIn(analysis, summary)

    # Verified path.

    def test_clean_verified_run_succeeds(self):
        checkout = self.checkout()
        result = checkout.run()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        meta = checkout.report()['meta']
        self.assertEqual(meta['failures'], [])
        self.assertFalse(meta['attestation_skipped'])
        attested = {p['file']: p['verified'] for p in meta['provenance']}
        self.assertTrue(attested['trufflecheck-errorprone-0.1.1.jar'])
        self.assertTrue(attested['trufflecheck-profiles-0.1.1.tar.gz'])
        self.assertEqual(len(checkout.calls('gh')), 2)
        self.assertEqual(len(checkout.calls('javac')), 6)  # five compilations and -version
        self.assertEqual(checkout.report()['findings'], [])

    def test_findings_are_reported_without_failing(self):
        checkout = self.checkout()
        scenario = {'javac': {'runtime': {'finding': ['error', 'TruffleGenericCatch']},
                              'java': {'finding': ['warning', 'DefaultCharset']}},
                    'cargo': {'exit': 101, 'stdout': json.dumps(
                        {'reason': 'compiler-message', 'message': rust_error('clippy::unwrap_used')}) + '\n'},
                    'clippy-driver': {'exit': 1, 'stderr': json.dumps(rust_error('unsafe_code')) + '\n'}}
        result = checkout.run(scenario=scenario)
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        rules = sorted(f['rule'] for f in checkout.report()['findings'])
        self.assertEqual(rules, ['DefaultCharset', 'TruffleGenericCatch', 'clippy::unwrap_used', 'unsafe_code'])
        self.assertEqual(checkout.report()['meta']['failures'], [])

    # Fail-closed path.

    def test_javac_rejecting_its_arguments_fails_the_run(self):
        checkout = self.checkout()
        result = checkout.run(scenario={'javac': {'runtime': {'exit': 2, 'stderr': 'error: invalid flag: --bogus\n'}}})
        self.assertFailed(result, 'javac runtime', checkout)
        # The remaining analyses still ran and were reported.
        self.assertEqual(len(checkout.calls('clippy-driver')), 2)

    def test_javac_error_that_is_not_a_finding_fails_the_run(self):
        checkout = self.checkout()
        error = 'Directives.java:4: error: cannot find symbol\n1 error\n'
        result = checkout.run(scenario={'javac': {'cells': {'exit': 1, 'stderr': error}}})
        self.assertFailed(result, 'javac cells', checkout)

    def test_javac_abnormal_exit_fails_the_run(self):
        checkout = self.checkout()
        result = checkout.run(scenario={'javac': {'example': {'exit': 4}}})
        self.assertFailed(result, 'javac example', checkout)

    def test_cargo_failure_without_diagnostics_fails_the_run(self):
        checkout = self.checkout()
        stderr = 'error: failed to download `serde v1.0.0`\n'
        result = checkout.run(scenario={'cargo': {'exit': 101, 'stdout': '', 'stderr': stderr}})
        self.assertFailed(result, 'clippy workspace', checkout)

    def test_clippy_driver_crash_fails_the_run(self):
        checkout = self.checkout()
        crash = 'error: internal compiler error: stub\nthread panicked\n'
        result = checkout.run(scenario={'clippy-driver': {'exit': 101, 'stderr': crash}})
        self.assertFailed(result, 'clippy seam ownership', checkout)

    def test_cargo_deny_failure_without_summary_fails_the_run(self):
        checkout = self.checkout()
        result = checkout.run(scenario={'cargo-deny': {'exit': 2, 'stderr': 'error: failed to fetch advisory db\n'}})
        self.assertFailed(result, 'cargo-deny', checkout)

    # Opt-outs.

    def test_skip_rust_does_not_run_or_fail_on_rust_analyzers(self):
        checkout = self.checkout()
        result = checkout.run('--skip-rust', scenario={'cargo': {'exit': 101, 'stdout': '', 'stderr': 'boom\n'}})
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        self.assertEqual([c['args'] for c in checkout.calls('cargo')], [['clippy', '--version']])
        self.assertEqual(checkout.calls('clippy-driver'), [])
        self.assertEqual(checkout.report()['meta']['cargo_deny'], 'skipped')

    # Provenance (fails closed without gh; review thread PRRT_kwDOU-wyt864KXRe).

    def test_missing_gh_fails_closed(self):
        checkout = self.checkout(gh=False)
        result = checkout.run()
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('gh is required to verify', result.stderr)
        self.assertEqual(checkout.calls('javac'), [])
        self.assertFalse((checkout.root / 'build/trufflecheck/findings.json').exists())

    def test_failed_attestation_fails_closed(self):
        checkout = self.checkout()
        result = checkout.run(scenario={'gh': {'exit': 1, 'stdout': '', 'stderr': 'no matching attestation\n'}})
        self.assertNotEqual(result.returncode, 0)
        self.assertIn('attestation verification failed', result.stderr)
        self.assertEqual(checkout.calls('javac'), [])

    def test_skip_attestation_opts_out_and_is_recorded(self):
        for args, env in ((('--skip-attestation',), {}), ((), {'TRUFFLECHECK_SKIP_ATTESTATION': '1'})):
            with self.subTest(args=args, env=env):
                checkout = self.checkout(gh=False)
                result = checkout.run(*args, **env)
                self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
                self.assertIn('NOT verified (--skip-attestation)', result.stderr)
                meta = checkout.report()['meta']
                self.assertTrue(meta['attestation_skipped'])
                self.assertFalse(any(p['verified'] for p in meta['provenance']))
                self.assertIn('--skip-attestation', (checkout.root / 'build/trufflecheck/summary.md').read_text())

    # A checkout path with a space (review thread PRRT_kwDOU-wyt864KXKk).

    def test_spaced_checkout_paths_reach_javac_intact(self):
        checkout = self.checkout()
        self.assertIn(' ', str(checkout.root))
        result = checkout.run()
        self.assertEqual(result.returncode, 0, result.stdout + result.stderr)
        compilations = [c for c in checkout.calls('javac') if c['args'] != ['-version']]
        self.assertEqual(len(compilations), 5)
        for call in compilations:
            args = call['args']
            self.assertEqual(Path(call['cwd']), checkout.root)
            plugin = next(a for a in args if a.startswith('-Xplugin:'))
            self.assertIn('-XepOpt:Trufflecheck:Policy=trufflecheck.properties', plugin.split())
            self.assertNotIn(str(checkout.base), plugin)
            self.assertIn(str(checkout.root), args[args.index('-cp') + 1])
            self.assertTrue(args[args.index('-d') + 1].startswith(str(checkout.root)))


class ArgumentTest(unittest.TestCase):
    def test_plugin_option_rejects_flags_javac_would_split(self):
        with self.assertRaises(SystemExit):
            trufflecheck.plugin_option(['-XepExcludedPaths:/My Projects/.*'], [])
        option = trufflecheck.plugin_option(['-Xep:A:ERROR'], ['B'])
        self.assertEqual(option.split(), ['-Xplugin:ErrorProne', '-Xep:A:ERROR', '-Xep:B:OFF',
                                          '-XepOpt:Trufflecheck:Policy=trufflecheck.properties'])

    def test_argfile_entries_escape_quotes_and_backslashes(self):
        self.assertEqual(trufflecheck.argfile_entry('/a b/c"d\\e.java'), '"/a b/c\\"d\\\\e.java"')
        with self.assertRaises(SystemExit):
            trufflecheck.argfile_entry('/a\nb.java')

    @unittest.skipUnless(shutil.which('javac'), 'needs a JDK javac to parse the argument file')
    def test_real_javac_reads_spaced_argfile_entries(self):
        base = Path(tempfile.mkdtemp(prefix='trufflecheck argfile '))
        self.addCleanup(shutil.rmtree, base)
        source = base / 'dir with "quotes" and spaces' / 'A.java'
        source.parent.mkdir()
        source.write_text('class A {}\n')
        argfile = base / 'sources'
        argfile.write_text(trufflecheck.argfile_entry(source) + '\n')
        result = subprocess.run(['javac', '-d', str(base / 'classes'), f'@{argfile}'], capture_output=True, text=True)
        self.assertEqual(result.returncode, 0, result.stderr)
        self.assertTrue((base / 'classes/A.class').is_file())


if __name__ == '__main__':
    unittest.main()
