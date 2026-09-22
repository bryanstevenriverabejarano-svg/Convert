#!/usr/bin/env python3
"""Fixed-seed tests for novelty, domain bounds and honest holdout accounting."""
import argparse
import collections
import copy
import json
import unittest

import evaluate_autonomous_tools as independent
import evaluate_autonomy_generalization as generalization


class UnseenCorpusTest(unittest.TestCase):
    def test_seed_reproduces_128_unique_new_inputs_with_32_per_family(self):
        corpus = generalization.generate_holdout(83729471023)
        self.assertEqual(corpus,generalization.generate_holdout(83729471023))
        self.assertNotEqual(corpus,generalization.generate_holdout(83729471024))
        self.assertEqual(128,len(corpus))
        self.assertEqual({family:32 for family in independent.FAMILIES},dict(collections.Counter(record["family"] for record in corpus)))
        self.assertEqual(128,len({record["id"] for record in corpus}))
        new = {generalization.fingerprint(record["family"],record["input"]) for record in corpus}
        known = {generalization.fingerprint(record["family"],record["input"]) for record in independent.generate_corpus()}
        self.assertEqual(128,len(new))
        self.assertFalse(new & known)
        self.assertTrue(all("expected" not in record and "answer" not in record for record in corpus))

    def test_novelty_does_not_count_harmless_reordering(self):
        for record in independent.generate_corpus()[:104]:
            family,data = record["family"],record["input"]
            altered = copy.deepcopy(data)
            key = "edges" if family in ("route","dependencies") else "items" if family=="knapsack" else "jobs"
            altered[key].reverse()
            self.assertEqual(generalization.fingerprint(family,data),generalization.fingerprint(family,altered))

    def test_multiple_fixed_seeds_stay_bounded_and_include_nontrivial_graph_conditions(self):
        for seed in [0,1,2**128-1]:
            negative = unreachable = cyclic = 0
            for record in generalization.generate_holdout(seed):
                family,data = record["family"],record["input"]
                if family in ("route","dependencies"):
                    self.assertTrue(1 <= data["nodes"] <= 14)
                    for edge in data["edges"]:
                        self.assertTrue(0 <= edge[0] < data["nodes"])
                        self.assertTrue(0 <= edge[1] < data["nodes"])
                    if family == "route":
                        distances = independent._floyd(data)
                        negative += any(edge[2] < 0 for edge in data["edges"])
                        unreachable += distances[data["source"]][data["target"]] == float("inf")
                    else:
                        cyclic += not independent._acyclic(data)
                elif family == "knapsack":
                    self.assertTrue(1 <= data["capacity"] <= 100)
                    self.assertLessEqual(len(data["items"]),14)
                    self.assertTrue(all(item["weight"] > 0 and item["value"] >= 0 for item in data["items"]))
                else:
                    self.assertLessEqual(len(data["jobs"]),14)
                    self.assertTrue(all(job["start"] < job["end"] and job["value"] >= 0 for job in data["jobs"]))
            self.assertGreaterEqual(negative,8)
            self.assertGreaterEqual(unreachable,8)
            self.assertGreaterEqual(cyclic,16)

    def test_seed_argument_has_explicit_bounds(self):
        self.assertEqual(0,generalization._seed("0"))
        for value in ["-1",str(2**128),"abc","1.5"]:
            with self.assertRaises(argparse.ArgumentTypeError):
                generalization._seed(value)


class HoldoutAccountingTest(unittest.TestCase):
    def setUp(self):
        self.challenge = {"schema":1,"id":"holdout-route-001","family":"route","description":"route test",
                          "input":{"nodes":3,"edges":[[0,1,1],[1,2,1],[0,2,9]],"source":0,"target":2}}
        state = {"family":"route","persisted":True,"fullJournalSha256":"a"*64,"familyState":{"revision":20}}
        self.report = {"challenge":self.challenge,"verified":True,"program":{"strategy":"BELLMAN_FORD"},
                       "stateBefore":state,"stateAfter":copy.deepcopy(state),"proceduralContextBefore":"Recorded tool evidence",
                       "result":{"status":"ok","cost":2,"path":[0,1,2]},"attempts":[]}

    def test_optimal_result_is_independently_certified(self):
        result = generalization.assess_holdout([self.challenge],[self.report],"a"*64)
        self.assertEqual(1,len(result))
        self.assertTrue(result[0]["passed"])
        self.assertEqual(2,result[0]["certificate"]["optimum"])

    def test_failures_are_recorded_without_changing_the_problem(self):
        bad = copy.deepcopy(self.report)
        bad["result"] = {"status":"ok","cost":9,"path":[0,2]}
        assessment = generalization.assess_holdout([self.challenge],[bad])[0]
        self.assertFalse(assessment["passed"])
        self.assertIn("no óptima",assessment["error"])
        self.assertEqual(9,bad["result"]["cost"])

    def test_modified_duplicate_missing_and_unverified_reports_fail(self):
        bad = copy.deepcopy(self.report)
        bad["challenge"]["input"]["edges"][0][2] = 8
        self.assertFalse(generalization.assess_holdout([self.challenge],[bad])[0]["passed"])
        self.assertEqual(1,sum(not item["passed"] for item in generalization.assess_holdout([self.challenge],[self.report,self.report])))
        self.assertFalse(generalization.assess_holdout([self.challenge],[])[0]["passed"])
        bad = copy.deepcopy(self.report); bad["verified"] = False
        self.assertFalse(generalization.assess_holdout([self.challenge],[bad])[0]["passed"])

    def test_lost_or_wrong_memory_trace_is_not_accepted(self):
        self.assertFalse(generalization.assess_holdout([self.challenge],[self.report],"b"*64)[0]["passed"])
        for field in ["stateBefore","stateAfter"]:
            bad = copy.deepcopy(self.report)
            bad[field]["persisted"] = False
            self.assertFalse(generalization.assess_holdout([self.challenge],[bad])[0]["passed"])
            bad = copy.deepcopy(self.report)
            bad[field]["family"] = "knapsack"
            self.assertFalse(generalization.assess_holdout([self.challenge],[bad])[0]["passed"])

    def test_individual_record_has_all_requested_fields_without_invented_model_or_cause(self):
        assessment = generalization.assess_holdout([self.challenge],[self.report])[0]
        record = generalization.detailed_record(self.report,assessment)
        required = {"identifier","objective","initialState","capabilitiesUsed","planCreated","toolsUsed","modelsUsed",
                    "memoriesRetrieved","actionsExecuted","result","passFail","failureRootCause","improvementHypothesis",
                    "modificationPerformed","newExecution","newResult","possibleSideEffects","regressionTests","conclusion"}
        self.assertTrue(required <= record.keys())
        self.assertEqual("not_applicable",record["modelsUsed"]["status"])
        self.assertEqual("not_applicable",record["failureRootCause"]["status"])
        self.assertEqual("PASS",record["passFail"])
        self.assertEqual(record,json.loads(json.dumps(record)))

    def test_failed_attempt_feedback_is_not_invented_as_causal_reasoning(self):
        report = copy.deepcopy(self.report)
        report["attempts"] = [{"strategy":"FEWEST_EDGES","passed":False,"feedback":"Coste subóptimo","regressionChecks":0},
                              {"strategy":"BELLMAN_FORD","passed":True,"regressionChecks":3}]
        record = generalization.detailed_record(report,{"id":self.challenge["id"],"passed":True})
        self.assertEqual("not_established",record["failureRootCause"]["status"])
        self.assertEqual(["Coste subóptimo"],record["failureRootCause"]["observedFeedback"])
        self.assertEqual("not_recorded",record["improvementHypothesis"]["status"])
        self.assertEqual(1,len(record["newExecution"]))


if __name__ == "__main__":
    unittest.main()
