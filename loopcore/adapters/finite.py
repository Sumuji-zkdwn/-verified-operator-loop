"""Finite operator-observation witness generator.

Adapted from primitive-lean/tools/autonomy/grow.py at main (blob
8a1c488ead476cf900662f0f9b6dad3c2fe83ae5).

This adapter performs exhaustive finite checks. Generated Lean is a candidate,
NOT a Lean-verified theorem unless a separate Lean execution succeeds.
"""
from __future__ import annotations

import json

DOMAIN = 'finite'
MAX_STATES = 8


def candidates():
    for n in range(3, MAX_STATES + 1):
        for a in range(n):
            for b in range(n):
                if (a < 2) != (b < 2):
                    yield {'states': n, 'image_of_zero': a, 'image_of_one': b}


def observer(x: int) -> int:
    return 0 if x < 2 else 1


def step(x: int, m: dict) -> int:
    return m['image_of_zero'] if x == 0 else m['image_of_one'] if x == 1 else x


def lean_candidate(m: dict, cycle: int) -> str:
    """Dependency-light Lean draft. This environment does not certify it."""
    n, a, b = m['states'], m['image_of_zero'], m['image_of_one']
    name = f'ExtractedFiniteCycle{cycle:05d}'
    return f'''/- Bounded finite witness. Optional verification: lean <this-file>. -/
namespace {name}

def observer (x : Fin {n}) : Bool := if x.val < 2 then false else true

def oldStep (x : Fin {n}) : Fin {n} := x

def newStep (x : Fin {n}) : Fin {n} :=
  if x = 0 then {a} else if x = 1 then {b} else x

theorem oldSame : observer (oldStep 0) = observer (oldStep 1) := by decide

theorem newDifferent : observer (newStep 0) ≠ observer (newStep 1) := by decide

theorem oldIdentity (x : Fin {n}) : oldStep x = x := by rfl

end {name}
'''


def build(m: dict, cycle: int) -> dict[str, bytes]:
    return {
        'model.json': (json.dumps(m, indent=2, sort_keys=True) + '\n').encode(),
        'witness.lean': lean_candidate(m, cycle).encode(),
    }


def check(m: dict, artifacts: dict[str, bytes], cycle: int) -> dict:
    expected = build(m, cycle)
    sane = all(type(m.get(k)) is int for k in ('states', 'image_of_zero', 'image_of_one'))
    if not sane:
        return {'passed': False, 'reason': 'invalid model types'}
    n = m['states']
    in_range = n in range(3, MAX_STATES + 1) and 0 <= m['image_of_zero'] < n and 0 <= m['image_of_one'] < n
    if not in_range:
        return {'passed': False, 'reason': 'model out of range'}
    checks = {
        'candidate_bytes_match_generator': artifacts == expected,
        'old_states_0_and_1_indistinguishable': observer(0) == observer(1),
        'all_old_actions_are_identity': all(x == x for x in range(n)),
        'new_action_separates_initial_pair': observer(step(0, m)) != observer(step(1, m)),
        'new_action_preserves_other_states': all(step(x, m) == x for x in range(2, n)),
        'all_transitions_in_state_space': all(0 <= step(x, m) < n for x in range(n)),
    }
    return {
        'passed': all(checks.values()),
        'method': 'exhaustive_finite_python',
        'checked_states': n,
        'checks': checks,
        'lean_status': 'NOT_RUN',
        'scope': 'one finite witness, not a theorem about general mathematics',
    }
