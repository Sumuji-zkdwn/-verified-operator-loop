"""No private code, credentials, network or third-party dependencies."""
from pathlib import Path
import json
import tempfile
import unittest

from loopcore.engine import audit, run_one, load_adapter, read_ledger
from loopcore.adapters import finite


class PublicOperatorTests(unittest.TestCase):
    def test_finite_candidate_catalogue(self):
        models = list(finite.candidates())
        self.assertEqual(len(models), 84)
        self.assertEqual(len({tuple(sorted(m.items())) for m in models}), 84)
        self.assertEqual(models[0], {"states": 3, "image_of_zero": 0, "image_of_one": 2})

    def test_every_finite_candidate_exhaustively(self):
        for index, model in enumerate(finite.candidates(), 1):
            self.assertTrue(finite.check(model, finite.build(model, index), index)["passed"])

    def test_reject_unseparating_candidate(self):
        bad = {"states": 3, "image_of_zero": 0, "image_of_one": 1}
        self.assertFalse(finite.check(bad, finite.build(bad, 1), 1)["passed"])

    def test_reject_mutated_proof_source(self):
        model = next(finite.candidates())
        files = finite.build(model, 1)
        files["witness.lean"] += b"-- injected\n"
        self.assertFalse(finite.check(model, files, 1)["passed"])

    def test_hash_chain_and_real_reentry(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            a = run_one(root, "finite")
            b = run_one(root, "finite")
            self.assertEqual((a["cycle"], b["cycle"]), (1, 2))
            self.assertNotEqual(a["model"], b["model"])
            ledger = read_ledger(root, "finite")
            self.assertEqual(ledger["cycles"][1]["previous_sha256"], ledger["cycles"][0]["record_sha256"])
            self.assertEqual(audit(root, "finite", rerun_checks=True)["cycles"], 2)

    def test_refuse_altered_artifact_and_block_further_growth(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            run_one(root, "finite")
            path = root / "cycles/00001/witness.lean"
            path.write_bytes(path.read_bytes() + b"-- modified\n")
            with self.assertRaisesRegex(ValueError, "hash mismatch"):
                audit(root, "finite")
            with self.assertRaisesRegex(ValueError, "hash mismatch"):
                run_one(root, "finite")

    def test_reject_altered_report(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            run_one(root, "finite")
            (root / "cycles/00001/report.json").write_text('{"passed":true}')
            with self.assertRaisesRegex(ValueError, "report hash mismatch"):
                audit(root, "finite")

    def test_reject_changed_ledger_record(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            run_one(root, "finite")
            path = root / "ledger.json"
            ledger = json.loads(path.read_text())
            ledger["cycles"][0]["model"]["image_of_zero"] = 1
            path.write_text(json.dumps(ledger))
            with self.assertRaisesRegex(ValueError, "record hash mismatch"):
                audit(root, "finite")

    def test_reject_private_domain(self):
        with self.assertRaises(ValueError):
            load_adapter("wfs")
        with self.assertRaises(ValueError):
            load_adapter("quantum")

    def test_finite_exhaustion(self):
        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            for index in range(84):
                self.assertEqual(run_one(root, "finite")["cycle"], index + 1)
            self.assertEqual(run_one(root, "finite")["status"], "exhausted")
            self.assertEqual(audit(root, "finite", rerun_checks=True)["cycles"], 84)


if __name__ == "__main__":
    unittest.main()
