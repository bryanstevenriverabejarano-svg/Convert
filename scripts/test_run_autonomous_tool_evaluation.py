#!/usr/bin/env python3
"""Failure-path tests for the real evaluation runner; no mocked success claims."""
import argparse
import json
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest import mock
import zipfile

import run_autonomous_tool_evaluation as runner


def fixture_jar(path, strictness=True):
    path.parent.mkdir(parents=True,exist_ok=True)
    with zipfile.ZipFile(path,"w") as jar:
        for name in ["JsonObject","JsonParser"]+(["Strictness"] if strictness else []):
            jar.writestr(f"com/google/gson/{name}.class",b"test fixture; never executed")


class DependencyResolutionTest(unittest.TestCase):
    def test_resolves_exact_app_version_not_an_unrelated_cached_gson(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root/"app").mkdir()
            (root/"app/build.gradle").write_text("implementation 'com.google.code.gson:gson:2.13.2'\n")
            cache = root/"gradle"
            base = cache/"caches/modules-2/files-2.1/com.google.code.gson/gson"
            fixture_jar(base/"2.10/hash/gson-2.10.jar")
            target = base/"2.13.2/hash/gson-2.13.2.jar"
            fixture_jar(target)
            self.assertEqual(target.resolve(),runner.find_gson(root,None,cache))

    def test_missing_cache_and_corrupt_or_incomplete_jars_fail(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            (root/"app").mkdir()
            (root/"app/build.gradle").write_text("implementation 'com.google.code.gson:gson:2.13.2'\n")
            with self.assertRaisesRegex(runner.EvaluationError,"no está en la caché"):
                runner.find_gson(root,None,root/"empty")
            bad = root/"bad.jar"
            bad.write_text("not a jar")
            with self.assertRaises(runner.EvaluationError):
                runner.find_gson(root,bad)
            fixture_jar(bad,strictness=False)
            with self.assertRaises(runner.EvaluationError):
                runner.find_gson(root,bad)

    def test_absent_jdk_and_sources_produce_actionable_errors(self):
        with mock.patch.object(runner.shutil,"which",return_value=None):
            with self.assertRaisesRegex(runner.EvaluationError,"JDK"):
                runner.java_executable("missing-java")
        with tempfile.TemporaryDirectory() as temporary:
            with self.assertRaisesRegex(runner.EvaluationError,"fuentes reales"):
                runner.java_sources(Path(temporary))


class ProcessLimitsTest(unittest.TestCase):
    def test_timeout_argument_is_bounded(self):
        self.assertEqual(180,runner.positive_timeout("180"))
        for value in ["0","-1","3601","abc","1.5"]:
            with self.assertRaises(argparse.ArgumentTypeError):
                runner.positive_timeout(value)

    def test_process_failure_and_timeout_abort_the_stage(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            with mock.patch.object(runner.subprocess,"run",return_value=subprocess.CompletedProcess(["java"],7)):
                with self.assertRaisesRegex(runner.EvaluationError,"código 7"):
                    runner.run_stage("failed",["java"],1,root,root)
            with mock.patch.object(runner.subprocess,"run",side_effect=subprocess.TimeoutExpired(["java"],1)):
                with self.assertRaisesRegex(runner.EvaluationError,"límite de 1"):
                    runner.run_stage("timeout",["java"],1,root,root)

    def test_arguments_are_passed_without_a_shell(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            command = ["java","path with spaces/$(literal).jar"]
            with mock.patch.object(runner.subprocess,"run",return_value=subprocess.CompletedProcess(command,0)) as called:
                runner.run_stage("literal",command,5,root,root)
            self.assertEqual(command,called.call_args.args[0])
            self.assertNotIn("shell",called.call_args.kwargs)
            self.assertEqual(5,called.call_args.kwargs["timeout"])

    def test_publication_keeps_unrelated_files_and_copies_nested_evidence(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            source,target = root/"staged",root/"published"
            (source/"cases").mkdir(parents=True)
            target.mkdir()
            (source/"summary.json").write_text(json.dumps({"passed":True}))
            (source/"cases/case.md").write_text("verified")
            (target/"notes.md").write_text("keep")
            runner.publish_evidence(source,target)
            self.assertEqual("keep",(target/"notes.md").read_text())
            self.assertEqual("verified",(target/"cases/case.md").read_text())
            self.assertTrue(json.loads((target/"summary.json").read_text())["passed"])
            self.assertEqual([],list(target.rglob(".salve-evidence-*")))

    def test_unknown_cli_flag_is_checked_before_file_access(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            def rejection(name,command,*args,**kwargs):
                self.assertEqual("--unknown",command[-1])
                self.assertEqual(1,kwargs["expected_returncode"])
                (root/(name+".log")).write_text("java.lang.IllegalArgumentException: Uso: corpus.jsonl report.jsonl [--fresh-each-case]\n")
                return {"stage":name,"exit_code":1}
            with mock.patch.object(runner,"run_stage",side_effect=rejection):
                self.assertEqual(1,runner.verify_cli_argument_rejection("java","classes",root,1,root,root)["exit_code"])

    def test_missing_class_is_not_mistaken_for_argument_validation(self):
        with tempfile.TemporaryDirectory() as temporary:
            root = Path(temporary)
            def wrong_failure(name,*args,**kwargs):
                (root/(name+".log")).write_text("ClassNotFoundException")
                return {"stage":name,"exit_code":1}
            with mock.patch.object(runner,"run_stage",side_effect=wrong_failure):
                with self.assertRaises(runner.EvaluationError):
                    runner.verify_cli_argument_rejection("java","classes",root,1,root,root)


class MemoryAblationTest(unittest.TestCase):
    @staticmethod
    def summary(attempts,operations,reused):
        return {"passed":True,"passed_executions":416,"observed_metrics":{
            "executions":416,"candidate_attempts":attempts,"rejected_candidates":attempts-416,
            "operations":operations,"reused":reused,"adapted":3,"promoted":416,
            "previous_program_rejected":2,"regression_checks":900,
            "latency_ms":{"environment":"JVM host","samples":416,"p50":1,"p95":2,"max":3}}}

    def test_memory_cost_is_retained_even_when_attempts_improve(self):
        result = runner.build_ablation(self.summary(420,2000,410),self.summary(600,1000,0))
        self.assertEqual(-180,result["comparison"]["candidate_attempts"]["with_minus_without"])
        self.assertEqual(1000,result["comparison"]["operations"]["with_minus_without"])
        self.assertEqual(-1.0,result["comparison"]["operations"]["reduction_fraction"])
        self.assertTrue(result["correctness"]["with_memory"]["passed"])

    def test_no_rejections_has_no_invented_reduction_ratio(self):
        result = runner.build_ablation(self.summary(416,10,0),self.summary(416,10,0))
        self.assertIsNone(result["comparison"]["rejected_candidates"]["reduction_fraction"])
        self.assertEqual(0,result["comparison"]["operations"]["with_minus_without"])


if __name__ == "__main__":
    unittest.main()
