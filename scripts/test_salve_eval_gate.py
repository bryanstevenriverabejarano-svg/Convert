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


def add_anonymous_memory_receipts(base, candidate):
    """Same independent gold IDs for both versions; no actual memories stored."""
    for i in range(0, 30, 5):
        cid = "case-%02d" % i
        identifier = "%016x" % (1000 + i)
        for report in (base, candidate):
            found = report["cases"][cid]["passed"]
            report["cases"][cid]["memory_evidence"] = {
                "gold": [identifier],
                "retrieved": [identifier] if found else [],
                "cited": [identifier] if found else [],
                "conflict": False, "acknowledged": False,
                "status": "OK" if found else "EMPTY",
            }


class MemoryEvidenceTests(unittest.TestCase):
    def setUp(self):
        self.base = fixture("1" * 40)
        self.good = fixture("2" * 40, 28)
        add_anonymous_memory_receipts(self.base, self.good)

    def test_uses_existing_gate_and_reports_recall_without_private_text(self):
        outcome = gate.compare(self.base, self.good)
        self.assertEqual("CANDIDATE", outcome["verdict"])
        m = outcome["memory_evidence"]
        self.assertEqual(0, m["recall_regressions"])
        self.assertAlmostEqual(5 / 6, m["baseline"]["recall_at_4"], places=4)
        self.assertEqual(1, m["candidate"]["recall_at_4"])
        self.assertEqual(0, m["candidate"]["citation_errors"])

    def test_rejects_recall_regression_even_when_other_domains_improve(self):
        old = self.good["cases"]["case-00"]
        old["memory_evidence"]["retrieved"] = []
        old["memory_evidence"]["cited"] = []
        old["memory_evidence"]["status"] = "EMPTY"
        # Independently verified status is not controlled by this metric.
        self.assertEqual("REJECTED", gate.compare(self.base, self.good)["verdict"])
        self.assertEqual(1, gate.compare(self.base, self.good)["memory_evidence"]["recall_regressions"])

    def test_rejects_citation_without_matching_gold_and_visible_evidence(self):
        receipt = self.good["cases"]["case-00"]["memory_evidence"]
        receipt["cited"] = ["ffffffffffffffff"]
        outcome = gate.compare(self.base, self.good)
        self.assertEqual("REJECTED", outcome["verdict"])
        self.assertEqual(1, outcome["memory_evidence"]["candidate"]["citation_errors"])

    def test_rejects_unacknowledged_contradiction(self):
        for report in (self.base, self.good):
            report["cases"]["case-00"]["memory_evidence"]["conflict"] = True
        outcome = gate.compare(self.base, self.good)
        self.assertEqual("REJECTED", outcome["verdict"])
        self.assertEqual(1, outcome["memory_evidence"]["candidate"]["unacknowledged_conflicts"])

    def test_rejects_changed_gold_annotations(self):
        self.good["cases"]["case-00"]["memory_evidence"]["gold"] = ["0000000000009999"]
        with self.assertRaisesRegex(ValueError, "Frozen memory gold"):
            gate.compare(self.base, self.good)

    def test_incomplete_evidence_cannot_be_candidate(self):
        for report in (self.base, self.good):
            report["cases"]["case-00"].pop("memory_evidence")
        self.assertEqual("INSUFFICIENT", gate.compare(self.base, self.good)["verdict"])

    def test_legacy_results_without_evidence_are_unchanged(self):
        self.assertEqual("CANDIDATE", gate.compare(fixture("1" * 40),
                                                       fixture("2" * 40, 28))["verdict"])

    def test_privacy_schema_rejects_raw_memories_and_partial_citations(self):
        import json
        import tempfile
        from pathlib import Path
        with tempfile.TemporaryDirectory() as tmp:
            path = Path(tmp) / "receipt.json"
            receipt = copy.deepcopy(self.good)
            bad = receipt["cases"]["case-00"]["memory_evidence"]
            bad["gold"] = ["Mi nombre es un dato privado"]
            path.write_text(json.dumps({
                **receipt, "cases": list(receipt["cases"].values())
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "anonymous hex"):
                gate.read(path)
            bad["gold"] = ["0000000000001000"]
            bad["status"] = "PARTIAL"
            path.write_text(json.dumps({
                **receipt, "cases": list(receipt["cases"].values())
            }), encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "cannot provide trusted"):
                gate.read(path)

    def test_empty_gold_with_spurious_candidate_is_rejected(self):
        for report in (self.base, self.good):
            data = report["cases"]["case-00"]["memory_evidence"]
            data["gold"] = []
        outcome = gate.compare(self.base, self.good)
        self.assertEqual("REJECTED", outcome["verdict"])
        self.assertEqual(1, outcome["memory_evidence"]["candidate"]["false_hits_on_empty_gold"])



if __name__ == "__main__":
    unittest.main()
