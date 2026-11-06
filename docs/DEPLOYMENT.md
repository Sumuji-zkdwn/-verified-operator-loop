# Public/private split — deployment notes

## Public repository

Repository: https://github.com/Sumuji-zkdwn/-verified-operator-loop

This repository contains the bounded finite-operator controller only. The standard `main` Actions workflow performs a clean two-cycle smoke test and full finite recheck. The second workflow is manual-only and persists one new verified candidate per invocation to its own branch, `autonomy/verified-finite-state`.

Verify the first Actions run before manually triggering state growth. No schedule is active.

If the manual job cannot write its dedicated branch, inspect repository Settings → Actions → General → Workflow permissions and any branch rules. Only grant the minimal necessary permissions. Do not put a private token or repository secret into public workflow inputs.

## Private quantum boundary

The original `Sumuji-zkdwn/kb-ndtjm` remains private, including its original `quantum/wfs_mobile_ci.py`, `dqi.kmb` and `submission.zip`. The public project is **not** a free hosted runner for the private source. The generic public tests establish only finite semantics. For original quantum verification, the private repo's own runner or a trusted local environment must execute its actual quantum compiler and official validators.

No cross-repository `checkout`, private token, `gh release download`, uploaded artifact, or action step in this public repository accesses `kb-ndtjm`.

## Verification scope

- Local `python -m unittest discover -s tests -v`: unit tests, exhaustive finite witness checks and tamper rejection.
- `python -m loopcore audit --domain finite --root ... --recheck`: independently recomputes finite checks for the stored candidate files.
- `witness.lean` files are **only drafts**; no Lean compiler is installed by these workflows, and no Lean certificate is claimed.
- The hash chain does not provide adversarial authenticity when an attacker can rewrite the whole history.

Before sharing any artifacts publicly, independently review the contents and ensure no private circuit, benchmark output or authentication material has been copied.
