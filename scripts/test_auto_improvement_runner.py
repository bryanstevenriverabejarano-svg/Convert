import importlib.util
import unittest
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("auto_improvement_runner.py")
SPEC = importlib.util.spec_from_file_location("auto_improvement_runner", MODULE_PATH)
runner = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(runner)


class ProposalValidationTest(unittest.TestCase):
    def proposal(self):
        path = "app/src/main/java/salve/core/Example.java"
        return {
            "schemaVersion": 2,
            "targetClass": "Example",
            "targetPath": path,
            "issueSummary": "Simplificar",
            "patch": f"diff --git a/{path} b/{path}\n--- a/{path}\n+++ b/{path}\n",
            "syntheticValidationPassed": True,
            "ethicalReviewPassed": True,
        }

    def test_accepts_single_core_java_target(self):
        runner.validate_proposal(self.proposal())

    def test_rejects_path_traversal(self):
        proposal = self.proposal()
        proposal["targetPath"] = "app/src/main/java/salve/core/../../build.gradle"
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)

    def test_rejects_second_changed_file(self):
        proposal = self.proposal()
        proposal["patch"] += "diff --git a/README.md b/README.md\n"
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)

    def test_rejects_failed_governance_gate(self):
        proposal = self.proposal()
        proposal["ethicalReviewPassed"] = False
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)


if __name__ == "__main__":
    unittest.main()
