"""Shared bounded generate -> validate -> seal -> re-enter controller.

A checked cycle is fed back solely as an append-only ledger entry. A failed
cycle does not change verified state. The domain cannot approve itself without
passing its explicit checker. No LLM, network or external process is launched.
"""
from __future__ import annotations
import hashlib
import importlib
import json
import os
from pathlib import Path
import shutil
import tempfile
from datetime import datetime, timezone

SCHEMA = 1
VALID_DOMAINS = {'finite'}


def now():
    return datetime.now(timezone.utc).isoformat()


def canonical(obj) -> bytes:
    return (json.dumps(obj, sort_keys=True, ensure_ascii=False, separators=(',', ':')) + '\n').encode('utf-8')


def sha(b: bytes) -> str:
    return hashlib.sha256(b).hexdigest()


def load_adapter(domain):
    if domain not in VALID_DOMAINS:
        raise ValueError(f'unknown domain {domain!r}; expected {sorted(VALID_DOMAINS)}')
    return importlib.import_module(f'loopcore.adapters.{domain}')


def ledger_path(root: Path):
    return root / 'ledger.json'


def read_ledger(root: Path, domain: str) -> dict:
    p = ledger_path(root)
    if not p.exists():
        return {'schema_version': SCHEMA, 'domain': domain, 'cycles': []}
    doc = json.loads(p.read_text(encoding='utf-8'))
    if doc.get('schema_version') != SCHEMA or doc.get('domain') != domain:
        raise ValueError('ledger schema/domain mismatch: do not reuse a workspace across domains')
    if not isinstance(doc.get('cycles'), list):
        raise ValueError('invalid ledger')
    return doc


def atomic_json(path: Path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.NamedTemporaryFile(mode='wb', dir=path.parent, prefix='.writing-', delete=False) as tmp:
        name = tmp.name
        tmp.write(json.dumps(obj, ensure_ascii=False, sort_keys=True, indent=2).encode() + b'\n')
        tmp.flush()
        os.fsync(tmp.fileno())
    os.replace(name, path)


def record_hash(record: dict) -> str:
    return sha(canonical({k: v for k, v in record.items() if k != 'record_sha256'}))


def safe_filename(name):
    return isinstance(name, str) and bool(name) and name == Path(name).name and name not in ('.', '..') and '\\' not in name


def audit(root: Path, domain: str, rerun_checks: bool = False) -> dict:
    """Re-check ledger links and artifact hashes; optionally recompute each proof."""
    ledger = read_ledger(root, domain)
    adapter = load_adapter(domain)
    previous = None
    models = set()
    for index, record in enumerate(ledger['cycles'], 1):
        if record.get('cycle') != index or record.get('previous_sha256') != previous:
            raise ValueError(f'broken cycle sequence at {index}')
        if record_hash(record) != record.get('record_sha256'):
            raise ValueError(f'record hash mismatch at {index}')
        m = record.get('model')
        if not isinstance(m, dict):
            raise ValueError(f'invalid model at {index}')
        model_key = canonical(m)
        if model_key in models:
            raise ValueError(f'duplicate model at {index}')
        models.add(model_key)
        parent = root / 'cycles' / f'{index:05d}'
        contents = {}
        for filename, expected_hash in record.get('artifacts', {}).items():
            if not safe_filename(filename):
                raise ValueError(f'unsafe artifact name at {index}')
            data = (parent / filename).read_bytes()
            if sha(data) != expected_hash:
                raise ValueError(f'artifact hash mismatch at {index}/{filename}')
            contents[filename] = data
        rep = (parent / 'report.json').read_bytes()
        if sha(rep) != record['report_sha256']:
            raise ValueError(f'verification report hash mismatch at {index}')
        report = json.loads(rep)
        if report.get('passed') is not True:
            raise ValueError(f'non-passing record at {index}')
        if rerun_checks:
            fresh = adapter.check(m, contents, index)
            if fresh != report:
                raise ValueError(f'recomputed verification disagrees at {index}')
        previous = record['record_sha256']
    return {'passed': True, 'domain': domain, 'cycles': len(ledger['cycles']),
            'last_record_sha256': previous,
            'recomputed_domain_checks': bool(rerun_checks),
            'note': 'hash chain detects accidental mutation, not malicious ledger replacement'}


def select(domain: str, history: list[dict]):
    adapter = load_adapter(domain)
    seen = {canonical(r['model']) for r in history}
    for candidate in adapter.candidates():
        if canonical(candidate) not in seen:
            return candidate
    return None


class Lock:
    def __init__(self, path):
        self.path = path
        self.fd = None

    def __enter__(self):
        try:
            self.fd = os.open(self.path, os.O_CREAT | os.O_EXCL | os.O_WRONLY, 0o600)
        except FileExistsError:
            raise RuntimeError('workspace is already locked; inspect/remove a stale .lock manually')
        os.write(self.fd, f'{os.getpid()}\n'.encode())
        return self

    def __exit__(self, exc_type, exc, tb):
        if self.fd is not None:
            os.close(self.fd)
            self.path.unlink(missing_ok=True)


def run_one(root: Path, domain: str) -> dict:
    root = root.resolve()
    adapter = load_adapter(domain)
    root.mkdir(parents=True, exist_ok=True)
    with Lock(root / '.lock'):
        audit(root, domain, rerun_checks=False)  # Never append onto corrupted state.
        ledger = read_ledger(root, domain)
        m = select(domain, ledger['cycles'])
        if m is None:
            return {'status': 'exhausted', 'domain': domain, 'cycles': len(ledger['cycles'])}
        cycle = len(ledger['cycles']) + 1
        destination = root / 'cycles' / f'{cycle:05d}'
        destination.parent.mkdir(parents=True, exist_ok=True)
        if destination.exists():
            raise FileExistsError(f'untracked target already exists: {destination}')
        tmp = Path(tempfile.mkdtemp(prefix='.pending-', dir=destination.parent))
        try:
            files = adapter.build(m, cycle)
            if not isinstance(files, dict) or not files or not all(safe_filename(k) and isinstance(v, bytes) for k, v in files.items()):
                raise ValueError('adapter must output a map of basename to bytes')
            for name, data in files.items():
                (tmp / name).write_bytes(data)
            report = adapter.check(m, files, cycle)
            if not isinstance(report, dict) or report.get('passed') is not True:
                raise ValueError(f'fail closed: domain validator rejected candidate: {report}')
            report_data = json.dumps(report, ensure_ascii=False, sort_keys=True, indent=2).encode() + b'\n'
            (tmp / 'report.json').write_bytes(report_data)
            record = {
                'cycle': cycle,
                'domain': domain,
                'model': m,
                'status': 'verified_by_' + str(report['method']),
                'artifacts': {name: sha(data) for name, data in sorted(files.items())},
                'report_sha256': sha(report_data),
                'previous_sha256': ledger['cycles'][-1]['record_sha256'] if ledger['cycles'] else None,
                'verified_utc': now(),
            }
            record['record_sha256'] = record_hash(record)
            os.replace(tmp, destination)
            ledger['cycles'].append(record)
            atomic_json(ledger_path(root), ledger)
            return {'status': 'verified', 'domain': domain, 'cycle': cycle,
                    'model': m, 'record_sha256': record['record_sha256'],
                    'report': report, 'workspace': str(root)}
        finally:
            if tmp.exists():
                shutil.rmtree(tmp)
