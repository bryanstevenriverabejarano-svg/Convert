#!/usr/bin/env python3
"""Adversarial tests for the independent autonomous-tool benchmark verifier."""
import collections
import copy
import json
from pathlib import Path
import tempfile
import unittest

import evaluate_autonomous_tools as evaluation


def challenge(family, data):
    return {"family": family,"input": data}


class CorpusTest(unittest.TestCase):
    def test_104_explicit_cases_each_have_four_distinct_rounds(self):
        corpus = evaluation.generate_corpus()
        self.assertEqual(416,len(corpus))
        self.assertEqual(416,len({record["id"] for record in corpus}))
        groups = collections.defaultdict(list)
        for record in corpus:
            groups[record["caseId"]].append(record)
            self.assertNotIn("expected",record)
            self.assertNotIn("answer",record)
        self.assertEqual(104,len(groups))
        for records in groups.values():
            self.assertEqual({1,2,3,4},{record["round"] for record in records})
            self.assertEqual(4,len({json.dumps(record["input"],sort_keys=True) for record in records}))
        for family in evaluation.FAMILIES:
            bases = [record for record in corpus if record["family"]==family and record["round"]==1]
            self.assertEqual(26,len(bases))
            self.assertEqual(26,len({json.dumps(record["input"],sort_keys=True) for record in bases}))

    def test_corpus_stays_in_the_bounded_tool_domains(self):
        for record in evaluation.generate_corpus():
            family,data = record["family"],record["input"]
            if family in ("route","dependencies"):
                self.assertTrue(1 <= data["nodes"] <= 14)
                for edge in data["edges"]:
                    self.assertTrue(0 <= edge[0] < data["nodes"])
                    self.assertTrue(0 <= edge[1] < data["nodes"])
                if family == "route":
                    evaluation._floyd(data)  # Rejects negative cycles.
            elif family == "knapsack":
                self.assertTrue(0 <= data["capacity"] <= 100)
                self.assertLessEqual(len(data["items"]),14)
                for item in data["items"]:
                    self.assertGreater(item["weight"],0)
                    self.assertGreaterEqual(item["value"],0)
            else:
                self.assertLessEqual(len(data["jobs"]),14)
                for job in data["jobs"]:
                    self.assertLess(job["start"],job["end"])
                    self.assertGreaterEqual(job["value"],0)

    def test_generation_is_reproducible_and_serializable(self):
        first = evaluation.generate_corpus()
        self.assertEqual(first,evaluation.generate_corpus())
        self.assertEqual(first,json.loads(json.dumps(first)))

    def test_each_round_is_a_complete_pass_before_restoring_the_lab(self):
        corpus = evaluation.generate_corpus()
        for round_number in range(1,5):
            block = corpus[(round_number-1)*104:round_number*104]
            self.assertEqual({round_number},{record["round"] for record in block})
            self.assertEqual(104,len({record["caseId"] for record in block}))


class RouteCertificateTest(unittest.TestCase):
    def setUp(self):
        self.input = {"nodes":4,"edges":[[0,1,1],[1,3,9],[0,2,4],[2,3,2]],"source":0,"target":3}

    def test_accepts_optimal_route(self):
        result = evaluation.verify_result(challenge("route",self.input),{"status":"ok","cost":6,"path":[0,2,3]})
        self.assertEqual(6,result["optimum"])

    def test_rejects_feasible_but_suboptimal_route(self):
        with self.assertRaisesRegex(ValueError,"no óptima"):
            evaluation.verify_result(challenge("route",self.input),{"status":"ok","cost":10,"path":[0,1,3]})

    def test_rejects_invented_edge_and_dishonest_cost(self):
        for result in [{"status":"ok","cost":6,"path":[0,3]},
                       {"status":"ok","cost":1,"path":[0,2,3]}]:
            with self.assertRaises(ValueError):
                evaluation.verify_result(challenge("route",self.input),result)

    def test_negative_edges_use_an_independent_optimality_certificate(self):
        data = {"nodes":4,"edges":[[0,1,2],[0,2,5],[2,1,-7],[1,3,1]],"source":0,"target":3}
        result = evaluation.verify_result(challenge("route",data),{"status":"ok","cost":-1,"path":[0,2,1,3]})
        self.assertEqual(-1,result["optimum"])

    def test_unreachable_and_source_equals_target(self):
        data = {"nodes":2,"edges":[],"source":0,"target":1}
        self.assertFalse(evaluation.verify_result(challenge("route",data),{"status":"unreachable"})["reachable"])
        with self.assertRaises(ValueError):
            evaluation.verify_result(challenge("route",self.input),{"status":"unreachable"})
        data["target"] = 0
        self.assertEqual(0,evaluation.verify_result(challenge("route",data),{"status":"ok","cost":0,"path":[0]})["optimum"])

    def test_duplicate_vertices_and_bool_cost_are_not_valid_integers(self):
        for result in [{"status":"ok","cost":6,"path":[0,2,2,3]},
                       {"status":"ok","cost":True,"path":[0,2,3]}]:
            with self.assertRaises(ValueError):
                evaluation.verify_result(challenge("route",self.input),result)


class SubsetCertificateTest(unittest.TestCase):
    def setUp(self):
        self.knapsack = challenge("knapsack",{"capacity":10,"items":[{"weight":6,"value":13},{"weight":5,"value":10},{"weight":5,"value":10}]})
        self.schedule = challenge("schedule",{"jobs":[{"start":0,"end":10,"value":15},{"start":0,"end":5,"value":9},{"start":5,"end":10,"value":9}]})

    def test_knapsack_certifies_combination_beating_density_greedy(self):
        self.assertEqual(20,evaluation.verify_result(self.knapsack,{"status":"ok","value":20,"weight":10,"selected":[1,2]})["optimum"])
        with self.assertRaisesRegex(ValueError,"subóptima"):
            evaluation.verify_result(self.knapsack,{"status":"ok","value":13,"weight":6,"selected":[0]})

    def test_knapsack_rejects_duplicates_overcapacity_and_false_totals(self):
        for result in [{"status":"ok","value":20,"weight":10,"selected":[1,1]},
                       {"status":"ok","value":23,"weight":11,"selected":[0,1]},
                       {"status":"ok","value":20,"weight":9,"selected":[1,2]}]:
            with self.assertRaises(ValueError):
                evaluation.verify_result(self.knapsack,result)

    def test_knapsack_empty_is_valid_when_no_resource_can_be_selected(self):
        for data in [{"capacity":0,"items":[{"weight":1,"value":2}]},{"capacity":3,"items":[]}]:
            self.assertEqual(0,evaluation.verify_result(challenge("knapsack",data),{"status":"ok","value":0,"weight":0,"selected":[]})["optimum"])

    def test_schedule_accepts_touching_jobs_and_rejects_greedy(self):
        self.assertEqual(18,evaluation.verify_result(self.schedule,{"status":"ok","value":18,"selected":[1,2]})["optimum"])
        with self.assertRaisesRegex(ValueError,"subóptimo"):
            evaluation.verify_result(self.schedule,{"status":"ok","value":15,"selected":[0]})

    def test_schedule_rejects_overlapping_selection_and_false_total(self):
        for result in [{"status":"ok","value":24,"selected":[0,1]},
                       {"status":"ok","value":99,"selected":[1,2]}]:
            with self.assertRaises(ValueError):
                evaluation.verify_result(self.schedule,result)

    def test_schedule_supports_negative_times_and_empty_calendar(self):
        data = {"jobs":[{"start":-4,"end":-1,"value":5},{"start":-1,"end":2,"value":7}]}
        self.assertEqual(12,evaluation.verify_result(challenge("schedule",data),{"status":"ok","value":12,"selected":[0,1]})["optimum"])
        self.assertEqual(0,evaluation.verify_result(challenge("schedule",{"jobs":[]}),{"status":"ok","value":0,"selected":[]})["optimum"])


class DependencyCertificateTest(unittest.TestCase):
    def setUp(self):
        self.dag = challenge("dependencies",{"nodes":4,"edges":[[0,1],[0,2],[1,3],[2,3]]})

    def test_accepts_parallel_or_serial_feasible_layers(self):
        for layers in [[[0],[1,2],[3]],[[0],[1],[2],[3]]]:
            self.assertTrue(evaluation.verify_result(self.dag,{"status":"ok","order":[0,1,2,3],"layers":layers})["acyclic"])

    def test_rejects_order_and_layer_dependency_violations(self):
        for result in [{"status":"ok","order":[0,3,1,2],"layers":[[0],[1,2],[3]]},
                       {"status":"ok","order":[0,1,2,3],"layers":[[0,1],[2],[3]]},
                       {"status":"ok","order":[0,1,2,3],"layers":[[0],[1],[3]]},
                       {"status":"ok","order":[0,1,2,3],"layers":[[0],[1,1,2],[3]]}]:
            with self.assertRaises(ValueError):
                evaluation.verify_result(self.dag,result)

    def test_requires_real_closed_cycle(self):
        data = challenge("dependencies",{"nodes":4,"edges":[[0,1],[1,2],[2,0],[2,3]]})
        self.assertFalse(evaluation.verify_result(data,{"status":"cycle","cycle":[0,1,2,0]})["acyclic"])
        for cycle in [[0,1,2],[0,3,0],[0,True,2,0]]:
            with self.assertRaises(ValueError):
                evaluation.verify_result(data,{"status":"cycle","cycle":cycle})
        with self.assertRaises(ValueError):
            evaluation.verify_result(self.dag,{"status":"cycle","cycle":[0,1,0]})

    def test_self_loop_is_a_valid_cycle_witness(self):
        data = challenge("dependencies",{"nodes":2,"edges":[[0,1],[1,1]]})
        self.assertFalse(evaluation.verify_result(data,{"status":"cycle","cycle":[1,1]})["acyclic"])


class ReportIntegrityTest(unittest.TestCase):
    def setUp(self):
        self.report = {"challenge":evaluation.generate_corpus()[0],"verified":True,
                       "program":{"schema":1,"strategy":"demo"},
                       "result":{"status":"ok","cost":7,"path":[0,1]},
                       "attempts":[{"strategy":"demo","feedback":"certified","privateThoughts":"must not be copied"}]}

    def test_missing_runs_cannot_be_reported_as_a_success(self):
        summary = evaluation.verify_reports([self.report])
        self.assertFalse(summary["passed"])
        self.assertEqual(1,summary["passed_executions"])
        self.assertEqual(415,len(summary["missing_executions"]))

    def test_duplicate_runs_are_not_four_successful_iterations(self):
        summary = evaluation.verify_reports([self.report,self.report])
        self.assertFalse(summary["passed"])
        self.assertEqual(1,summary["failed_executions"])

    def test_changed_challenge_false_verified_and_missing_program_fail(self):
        mutations = []
        altered = copy.deepcopy(self.report)
        altered["challenge"]["input"]["edges"][0][2] = 8
        mutations.append(altered)
        altered = copy.deepcopy(self.report)
        altered["challenge"]["schema"] = True
        mutations.append(altered)
        altered = copy.deepcopy(self.report)
        altered["verified"] = False
        mutations.append(altered)
        altered = copy.deepcopy(self.report)
        del altered["program"]
        mutations.append(altered)
        for altered in mutations:
            self.assertEqual(1,evaluation.verify_reports([altered])["failed_executions"])

    def test_reports_preserve_programs_but_do_not_publish_private_thought_fields(self):
        with tempfile.TemporaryDirectory() as temporary:
            directory = Path(temporary)
            evaluation.verify_reports([self.report],directory)
            self.assertEqual(104,len(list((directory/"cases").glob("*.md"))))
            text = (directory/"cases"/"route-001.md").read_text(encoding="utf-8")
            self.assertIn("certified",text)
            self.assertNotIn("privateThoughts",text)
            self.assertNotIn("must not be copied",text)
            self.assertFalse(json.loads((directory/"summary.json").read_text())["passed"])

    def test_observed_metrics_count_certified_runs_and_real_candidate_rejections(self):
        report = copy.deepcopy(self.report)
        report.update(adapted=True,reused=False,promoted=True,previousRejected=True,operations=88,elapsedNanos=2_000_000)
        report["attempts"] = [{"passed":False,"regressionChecks":0},{"passed":True,"regressionChecks":3}]
        invalid = copy.deepcopy(report)
        invalid["verified"] = False
        invalid["elapsedNanos"] = 999_000_000
        summary = evaluation.verify_reports([report,invalid])
        metrics = summary["observed_metrics"]
        self.assertEqual(1,metrics["executions"])
        self.assertEqual(1,metrics["adapted"])
        self.assertEqual(1,metrics["rejected_candidates"])
        self.assertEqual(3,metrics["regression_checks"])
        self.assertEqual(2.0,metrics["latency_ms"]["p95"])
        self.assertEqual(1,summary["rounds"]["1"]["executions"])
        self.assertEqual(0,summary["rounds"]["2"]["executions"])

    def test_latency_percentiles_exclude_missing_samples(self):
        metrics = evaluation._observed_metrics([{"elapsedNanos":1_000_000},{"elapsedNanos":3_000_000},{"elapsedNanos":2_000_000},{}, {"elapsedNanos":True}])
        self.assertEqual(3,metrics["latency_ms"]["samples"])
        self.assertEqual(2.0,metrics["latency_ms"]["p50"])
        self.assertEqual(3.0,metrics["latency_ms"]["p95"])
        self.assertIsNone(evaluation._observed_metrics([])["latency_ms"]["max"])


if __name__ == "__main__":
    unittest.main()
