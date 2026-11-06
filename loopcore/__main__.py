"""Run the public-only finite operator loop: python -m loopcore ..."""
from __future__ import annotations
import argparse
import json
from pathlib import Path
import sys
from .engine import audit, read_ledger, run_one, load_adapter, canonical


def main(argv=None):
    parser = argparse.ArgumentParser(description="Finite operator loop (no private adapters)")
    subs = parser.add_subparsers(dest="command", required=True)
    for command in ("run", "audit", "status"):
        sub = subs.add_parser(command)
        sub.add_argument("--root", type=Path, required=True)
        sub.add_argument("--domain", choices=("finite",), default="finite")
        if command == "audit":
            sub.add_argument("--recheck", action="store_true")
    args = parser.parse_args(argv)
    try:
        if args.command == "run":
            result = run_one(args.root, args.domain)
        elif args.command == "audit":
            result = audit(args.root, args.domain, rerun_checks=args.recheck)
        else:
            ledger = read_ledger(args.root, args.domain)
            seen = {canonical(x["model"]) for x in ledger["cycles"]}
            remaining = sum(canonical(model) not in seen for model in load_adapter(args.domain).candidates())
            result = {"domain": args.domain, "cycles": len(ledger["cycles"]), "remaining": remaining}
    except Exception as exc:
        print(json.dumps({"passed": False, "error": f"{type(exc).__name__}: {exc}"}), file=sys.stderr)
        return 1
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
