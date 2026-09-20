import importlib.util
import json
import tempfile
import unittest
from unittest import mock
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
            "schemaVersion": 3,
            "proposalId": "12345678-1234-4234-8234-123456789abc",
            "targetClass": "Example",
            "targetPath": path,
            "issueSummary": "Simplificar",
            "patch": f"diff --git a/{path} b/{path}\n--- a/{path}\n+++ b/{path}\n",
            "syntheticValidationPassed": True,
            "ethicalReviewPassed": True,
        }

    def test_accepts_single_core_java_target(self):
        runner.validate_proposal(self.proposal())

    def test_rejects_truthy_non_boolean_gates(self):
        for field in ("syntheticValidationPassed", "ethicalReviewPassed"):
            for value in ("false", "true", 1, [True], {"ok": True}, None, False):
                with self.subTest(field=field, value=value):
                    proposal = self.proposal()
                    proposal[field] = value
                    with self.assertRaises(runner.ProposalError):
                        runner.validate_proposal(proposal)

    def test_rejects_non_object_json(self):
        for value in (None, [], True, 3, "proposal"):
            with self.subTest(value=value), self.assertRaises(runner.ProposalError):
                runner.validate_proposal(value)

    def test_rejects_invalid_field_types(self):
        for field in ("proposalId", "targetPath", "targetClass", "issueSummary"):
            for value in (None, 1, [], ""):
                proposal = self.proposal()
                proposal[field] = value
                with self.subTest(field=field, value=value), self.assertRaises(runner.ProposalError):
                    runner.validate_proposal(proposal)

    def test_rejects_noncanonical_uuid(self):
        proposal = self.proposal()
        proposal["proposalId"] = proposal["proposalId"].replace("-", "")
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)

    def test_rejects_float_schema_version(self):
        proposal = self.proposal()
        proposal["schemaVersion"] = 3.0
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)

    @mock.patch.object(runner, "run")
    def test_invalid_json_never_starts_git(self, run):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "proposal.json"
            duplicate = json.dumps(self.proposal())[:-1] + ', "ethicalReviewPassed": true}'
            for payload in (duplicate.encode(), b'\xff', b'[]'):
                path.write_bytes(payload)
                with self.assertRaises(runner.ProposalError):
                    runner.execute(path, Path(directory), "main", False)
            run.assert_not_called()

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

    def test_rejects_invalid_proposal_id(self):
        proposal = self.proposal()
        proposal["proposalId"] = "../../otro"
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)

    @mock.patch.object(runner, "run")
    def test_finds_existing_pull_request(self, run):
        run.return_value = mock.Mock(stdout='[{"url":"https://github.com/example/pr/7"}]')
        self.assertEqual(
            "https://github.com/example/pr/7",
            runner.find_existing_pr(Path("."), "12345678-1234-4234-8234-123456789abc"),
        )


if __name__ == "__main__":
    unittest.main()
