# Verified Operator Loop — public finite core

An independent, public-safe implementation of the **generate → verify → record → re-enter** cycle, extracted from the earlier finite-operator research architecture.

This repository has 84 bounded finite-state candidate models (`Fin 3` through `Fin 8`), a standard-library Python controller, exhaustive finite checks, a SHA-256-linked ledger, tamper detection, and GitHub Actions tests. Every run checks past ledger integrity before adding **one** new finite witness.

The name of the GitHub repository currently includes a leading hyphen: `Sumuji-zkdwn/-verified-operator-loop`. This does not affect the Python package.

## Boundaries and privacy

**Not included:** the private `kb-ndtjm` quantum source, submission ZIP, KMx/KMB files, Kaggle credentials, private repository history or private CI artifacts. Nothing in this repository can access the private repo. The generated Lean snippets are **uncompiled draft candidates** and are not validated as Lean proofs here.

Local finite checks do **not** verify an original WFS quantum compiler, Kickmix compatibility, full DQI submission or Kaggle score. This is a bounded computational system, not a demonstration of AGI.

## Run locally

Python 3.10+; no third-party packages or secrets.

```bash
python -m unittest discover -s tests -v
python -m loopcore run --domain finite --root work/finite
python -m loopcore run --domain finite --root work/finite
python -m loopcore audit --domain finite --root work/finite --recheck
python -m loopcore status --domain finite --root work/finite
```

The initial tests cover all 84 finite candidates and corruption rejection. After two successful cycles the local work directory contains a persistent, hash-linked record. Once all 84 candidates have been generated, the controller exits without writing another.

## GitHub Actions

- **Public finite operator loop verification** (`.github/workflows/verify-public.yml`): runs automatically on `main` and PRs, plus manual dispatch. Runs the unit tests, creates two fresh finite witnesses and recomputes all checks. Read-only permissions.
- **Manual bounded verified re-entry** (`.github/workflows/manual-reentry.yml`): manual dispatch only; adds **one** checked witness per invocation to `autonomy/verified-finite-state`, an isolated branch. This workflow requires Actions write permission to create/update that branch. It does not write to `main` and has no schedule.

These workflows do not submit to Kaggle, call an AI service or read secrets. In the manual workflow, the controller is checked out from the reviewed `main` source and the isolated state branch is treated as data.

## Provenance

The finite candidate family was adapted from the public [primitive-lean autonomous generator](https://github.com/Sumuji-zkdwn/primitive-lean/blob/main/tools/autonomy/grow.py), specifically source blob `8a1c488ead476cf900662f0f9b6dad3c2fe83ae5`.

This implementation is not the original Lean proof system; it verifies its **own** finite-state models with Python. The hash chain detects accidental changes but is not a cryptographic signature against an attacker with repository-write privileges.

See [deployment and safety notes](docs/DEPLOYMENT.md) for how to use the public/private split. No license has been selected by the owner.
