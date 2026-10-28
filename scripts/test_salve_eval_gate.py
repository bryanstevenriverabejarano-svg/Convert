"""Offline contract tests for paired improvements without external packages."""
import copy
import unittest
import salve_eval_gate as gate


def fixture(revision: str, successes: int = 25):
    cases = {}
    for i in range(30):
        cid = "case-%02d" % i
        cases[cid] = {
            "id": cid,
            "domain": gate.DOMAINS[i % len(gate.DOMAINS)],
            "passed": i < successes,
            "verified": i < successes,
            "duration_ms": 100,
            "privacy_violation": False,
            "safety_violation": False,
            "unsupported_claim": False,
        }
    return {"revision": revision, "model_sha256": "a" * 64, "cases": cases}


class GateTests(unittest.TestCase):
    def setUp(self):
        self.base = fixture("1" * 40)
        self.good = fixture("2" * 40, 28)

    def test_verified_improvement_requires_manual_review(self):
        result = gate.compare(self.base, self.good)
        self.assertEqual("CANDIDATE", result["verdict"])
        self.assertEqual(0, result["new_failures_on_previous_successes"])
        self.assertEqual(3, result["candidate_passed"] - result["baseline_passed"])

    def test_no_candidate_after_regression_even_if_net_improved(self):
        trial = copy.deepcopy(self.good)
        trial["cases"]["case-00"]["passed"] = False
        trial["cases"]["case-00"]["verified"] = False
        self.assertEqual("REJECTED", gate.compare(self.base, trial)["verdict"])

    def test_private_incident_blocks_promotion(self):
        trial = copy.deepcopy(self.good)
        trial["cases"]["case-29"]["unsupported_claim"] = True
        self.assertEqual("REJECTED", gate.compare(self.base, trial)["verdict"])

    def test_changing_model_requires_new_control(self):
        trial = copy.deepcopy(self.good)
        trial["model_sha256"] = "b" * 64
        with self.assertRaisesRegex(ValueError, "digest"):
            gate.compare(self.base, trial)

    def test_changed_case_set_never_counts_as_improvement(self):
        trial = copy.deepcopy(self.good)
        trial["cases"].pop("case-29")
        with self.assertRaisesRegex(ValueError, "frozen"):
            gate.compare(self.base, trial)

    def test_missing_domain_has_insufficient_evidence(self):
        trial = fixture("2" * 40, 28)
        base = copy.deepcopy(self.base)
        for obj in (base, trial):
            for cid, entry in obj["cases"].items():
                entry["domain"] = "memory"
        self.assertEqual("INSUFFICIENT", gate.compare(base, trial)["verdict"])

    def test_latency_regressions_are_rejected(self):
        trial = copy.deepcopy(self.good)
        for row in trial["cases"].values():
            row["duration_ms"] = 180
        self.assertEqual("REJECTED", gate.compare(self.base, trial)["verdict"])

    def test_unverified_success_never_passes_schema(self):
        import json
        import tempfile
        from pathlib import Path
        with tempfile.TemporaryDirectory() as tmp:
            report = fixture("2" * 40)
            entry = report["cases"]["case-00"]
            entry["verified"] = False
            path = Path(tmp) / "bad.json"
            path.write_text(json.dumps({
                **report, "cases": list(report["cases"].values())
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "independent verification"):
                gate.read(path)


if __name__ == "__main__":
    unittest.main()
