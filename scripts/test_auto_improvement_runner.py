import importlib.util
import hashlib
import json
import tempfile
import unittest
import sys
import subprocess
import tarfile
from unittest import mock
from pathlib import Path


MODULE_PATH = Path(__file__).with_name("auto_improvement_runner.py")
sys.path.insert(0, str(MODULE_PATH.parent))
SPEC = importlib.util.spec_from_file_location("auto_improvement_runner", MODULE_PATH)
runner = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(runner)

GENERATED_TESTS = """package salve.core;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
public class ExampleTestHarness {
    @Test public void correctedValue() { assertEquals(2, new Example().value); }
    @Test public void stableValue() { assertEquals(new Example().value, new Example().value); }
}
"""
GENERATED_PATH = "app/src/test/java/salve/core/ExampleTestHarness.java"


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

    def test_source_identity_is_optional_but_must_be_complete_and_strict(self):
        proposal = self.proposal()
        proposal.update(sourceRevision="a" * 40, sourceSha256="b" * 64)
        runner.validate_proposal(proposal)
        for field in ("sourceRevision", "sourceSha256"):
            for value in (None, 42, "", "A" * 64):
                invalid = dict(proposal)
                invalid[field] = value
                with self.subTest(field=field, value=value), self.assertRaises(runner.ProposalError):
                    runner.validate_proposal(invalid)
            invalid = dict(proposal)
            del invalid[field]
            with self.assertRaises(runner.ProposalError):
                runner.validate_proposal(invalid)

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

    def test_generated_suite_has_derived_path_and_preserves_valid_source(self):
        proposal = self.proposal()
        proposal["generatedTests"] = GENERATED_TESTS
        runner.validate_proposal(proposal)
        self.assertEqual((GENERATED_PATH, GENERATED_TESTS), runner.generated_test(proposal))

    def test_generated_suite_without_package_is_bound_to_target_package(self):
        proposal = self.proposal()
        proposal["generatedTests"] = GENERATED_TESTS.replace("package salve.core;\n", "")
        path, source = runner.generated_test(proposal)
        self.assertEqual(GENERATED_PATH, path)
        self.assertTrue(source.startswith("package salve.core;\n\n"))
        self.assertIn("public class ExampleTestHarness", source)

    def test_generated_suite_rejects_wrong_type_size_class_package_and_ignored_tests(self):
        for source in (None, 1, [], "a" * (runner.MAX_GENERATED_TEST_BYTES + 1),
                       GENERATED_TESTS.replace("salve.core", "other.package"),
                       GENERATED_TESTS.replace("ExampleTestHarness", "AnotherTest"),
                       GENERATED_TESTS.replace("@Test", "@org.junit.Ignore @Test"),
                       GENERATED_TESTS.replace("@Test", "/* @Test */"),
                       GENERATED_TESTS + "\x00", GENERATED_TESTS + r"\u0061"):
            proposal = self.proposal()
            proposal["generatedTests"] = source
            with self.subTest(source=str(source)[:50]), self.assertRaises(runner.ProposalError):
                runner.validate_proposal(proposal)

    def test_comments_and_strings_cannot_spoof_test_declarations(self):
        proposal = self.proposal()
        proposal["generatedTests"] = '// public class ExampleTestHarness { @Test }\nclass Decoy {}'
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)
        proposal["generatedTests"] = GENERATED_TESTS.replace("@Test", "") + '\n// @Test\n'
        with self.assertRaises(runner.ProposalError):
            runner.validate_proposal(proposal)

    def test_legacy_missing_or_blank_suite_remains_explicitly_supported(self):
        for source in (None, "", "  \n"):
            proposal = self.proposal()
            if source is not None:
                proposal["generatedTests"] = source
            runner.validate_proposal(proposal)
            self.assertIsNone(runner.generated_test(proposal))

    @mock.patch.object(runner, "run")
    def test_finds_existing_pull_request(self, run):
        run.return_value = mock.Mock(stdout='[{"url":"https://github.com/example/pr/7"}]')
        self.assertEqual(
            "https://github.com/example/pr/7",
            runner.find_existing_pr(Path("."), "12345678-1234-4234-8234-123456789abc"),
        )


class RunnerIntegrationTest(unittest.TestCase):
    """Git y worktrees reales; Docker y publicación GitHub simulados explícitamente."""

    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        root = Path(self.temporary.name)
        self.repo = root / "repo"
        self.remote = root / "remote.git"
        self.repo.mkdir()
        self.git("init", "--initial-branch=main")
        self.git("config", "user.email", "tests@example.invalid")
        self.git("config", "user.name", "Salve tests")
        self.target = "app/src/main/java/salve/core/Example.java"
        target_file = self.repo / self.target
        target_file.parent.mkdir(parents=True)
        target_file.write_text("package salve.core;\nclass Example { int value = 1; }\n")
        # Si vuelve la ejecución directa, esta prueba revela el efecto en el host.
        self.marker = root / "HOST_EXECUTED"
        (self.repo / "gradlew").write_text(f"#!/bin/sh\ntouch '{self.marker}'\nexit 0\n")
        (self.repo / "gradlew").chmod(0o755)
        self.git("add", ".")
        self.git("commit", "-m", "fixture")
        self.git("init", "--bare", str(self.remote))
        self.git("remote", "add", "origin", str(self.remote))
        self.git("push", "origin", "main")
        (self.repo / "HOST_SECRET").write_text("untracked credential fixture")
        target_file.write_text("package salve.core;\nclass Example { int value = 2; }\n")
        patch = self.git("diff", "--", self.target)
        self.git("checkout", "--", self.target)
        self.proposal = ProposalValidationTest().proposal()
        self.proposal["patch"] = patch
        self.proposal_path = root / "proposal.json"
        self.proposal_path.write_text(json.dumps(self.proposal))
        self.branch = "salve/auto-12345678-example"
        self.image = "sha256:" + "a" * 64
        for patcher in (mock.patch.object(runner, "find_existing_pr", return_value=None),
                        mock.patch.object(runner.sandbox, "configured_image", return_value=self.image)):
            patcher.start()
            self.addCleanup(patcher.stop)
        self.real_run = runner.run
        self.pr_bodies = []

    def git(self, *arguments):
        return subprocess.run(["git", *arguments], cwd=self.repo, check=True,
                              text=True, capture_output=True).stdout

    def quiet_run(self, command, cwd, *, capture=False):
        if command[0] == "gh":
            self.assertEqual(["gh", "pr", "create"], command[:3])
            self.pr_bodies.append(Path(command[command.index("--body-file") + 1]).read_text())
            return subprocess.CompletedProcess(command, 0, "https://github.com/example/repo/pull/1\n")
        return self.real_run(command, cwd, capture=True)

    def assert_cleaned(self):
        self.assertFalse(self.marker.exists(), "Gradle no debe ejecutarse en el host")
        worktrees = self.git("worktree", "list", "--porcelain")
        self.assertEqual(1, worktrees.count("worktree "))
        self.assertEqual("untracked credential fixture", (self.repo / "HOST_SECRET").read_text())
        self.assertEqual("package salve.core;\nclass Example { int value = 1; }\n", (self.repo / self.target).read_text())

    def test_snapshot_excludes_credentials_and_published_tree_matches(self):
        def verify_snapshot(archive, image):
            self.assertEqual(self.image, image)
            with tarfile.open(archive) as snapshot:
                self.assertNotIn("HOST_SECRET", snapshot.getnames())
                self.assertFalse(any(".git" in Path(name).parts for name in snapshot.getnames()))
                self.assertEqual(b"package salve.core;\nclass Example { int value = 2; }\n",
                                 snapshot.extractfile(self.target).read())
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox", side_effect=verify_snapshot):
            url = runner.execute(self.proposal_path, self.repo, "main", False)
        self.assertEqual("https://github.com/example/repo/pull/1", url)
        tree = self.git("rev-parse", f"origin/{self.branch}^{{tree}}").strip()
        self.assertIn(tree, self.pr_bodies[0])
        self.assertIn(self.image, self.pr_bodies[0])
        self.assert_cleaned()

    def with_generated_suite(self):
        self.proposal["generatedTests"] = GENERATED_TESTS
        self.proposal_path.write_text(json.dumps(self.proposal))

    def test_generated_suite_is_in_tested_snapshot_and_published_exact_tree(self):
        self.with_generated_suite()
        observed_hash = []
        def verify_snapshot(archive, image):
            with tarfile.open(archive) as snapshot:
                generated = snapshot.extractfile(GENERATED_PATH).read()
                self.assertEqual(GENERATED_TESTS.encode(), generated)
                self.assertEqual(b"package salve.core;\nclass Example { int value = 2; }\n",
                                 snapshot.extractfile(self.target).read())
                observed_hash.append(hashlib.sha256(generated).hexdigest())
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox", side_effect=verify_snapshot):
            runner.execute(self.proposal_path, self.repo, "main", False)
        self.assertEqual(GENERATED_TESTS, self.git("show", f"origin/{self.branch}:" + GENERATED_PATH))
        self.assertIn(GENERATED_PATH, self.pr_bodies[0])
        self.assertIn(observed_hash[0], self.pr_bodies[0])
        self.assertIn(self.git("rev-parse", f"origin/{self.branch}^{{tree}}").strip(), self.pr_bodies[0])
        self.assert_cleaned()

    def test_generated_suite_failure_never_publishes_or_executes_on_host(self):
        self.with_generated_suite()
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox", side_effect=runner.sandbox.SandboxError("generated suite failed")):
            with self.assertRaises(runner.sandbox.SandboxError):
                runner.execute(self.proposal_path, self.repo, "main", False)
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()

    def test_generated_suite_never_overwrites_existing_versioned_test(self):
        self.with_generated_suite()
        existing = self.repo / GENERATED_PATH
        existing.parent.mkdir(parents=True)
        existing.write_text("// existing trusted test\n")
        self.git("add", GENERATED_PATH)
        self.git("commit", "-m", "existing test fixture")
        self.git("push", "origin", "main")
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox") as sandbox_run:
            with self.assertRaisesRegex(runner.ProposalError, "colisiona"):
                runner.execute(self.proposal_path, self.repo, "main", False)
            sandbox_run.assert_not_called()
        self.assertEqual("// existing trusted test\n", existing.read_text())
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assert_cleaned()

    def test_generated_suite_rejects_symlink_parent_before_writing(self):
        self.with_generated_suite()
        external = Path(self.temporary.name) / "outside-tests"
        external.mkdir()
        (self.repo / "app/src/test").symlink_to(external, target_is_directory=True)
        self.git("add", "app/src/test")
        self.git("commit", "-m", "symlink fixture")
        self.git("push", "origin", "main")
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox") as sandbox_run:
            with self.assertRaisesRegex(runner.ProposalError, "enlace"):
                runner.execute(self.proposal_path, self.repo, "main", False)
            sandbox_run.assert_not_called()
        self.assertEqual([], list(external.iterdir()))
        self.assert_cleaned()

    def test_dry_run_includes_generated_suite_without_publication(self):
        self.with_generated_suite()
        def check(archive, image):
            with tarfile.open(archive) as snapshot:
                self.assertIn(GENERATED_PATH, snapshot.getnames())
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox", side_effect=check):
            runner.execute(self.proposal_path, self.repo, "main", True)
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()

    def test_failed_sandbox_never_pushes_or_creates_pr(self):
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox", side_effect=runner.sandbox.SandboxError("failed")):
            with self.assertRaises(runner.sandbox.SandboxError):
                runner.execute(self.proposal_path, self.repo, "main", False)
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()

    def test_dry_run_validates_without_publication(self):
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox") as sandbox_run:
            runner.execute(self.proposal_path, self.repo, "main", True)
            sandbox_run.assert_called_once()
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()

    def test_bound_source_is_verified_and_recorded_in_pr(self):
        source = (self.repo / self.target).read_bytes()
        self.proposal.update(sourceRevision=self.git("rev-parse", "HEAD").strip(),
                             sourceSha256=hashlib.sha256(source).hexdigest())
        self.proposal_path.write_text(json.dumps(self.proposal))
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox") as sandbox_run:
            runner.execute(self.proposal_path, self.repo, "main", False)
            sandbox_run.assert_called_once()
        self.assertIn(self.proposal["sourceSha256"], self.pr_bodies[0])
        self.assertIn(self.proposal["sourceRevision"], self.pr_bodies[0])
        self.assert_cleaned()

    def test_stale_source_rejected_before_patch_application_or_sandbox(self):
        self.proposal.update(sourceRevision="a" * 40, sourceSha256=hashlib.sha256(b"older source").hexdigest())
        self.proposal_path.write_text(json.dumps(self.proposal))
        with mock.patch.object(runner, "run", side_effect=self.quiet_run) as run, \
                mock.patch.object(runner.sandbox, "run_sandbox") as sandbox_run:
            with self.assertRaisesRegex(runner.ProposalError, "fuente cambió"):
                runner.execute(self.proposal_path, self.repo, "main", False)
            sandbox_run.assert_not_called()
            self.assertFalse(any(call.args[0][:2] == ["git", "apply"] for call in run.call_args_list))
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()

    def test_preexisting_branch_is_not_deleted_on_collision(self):
        self.git("branch", self.branch)
        before = self.git("rev-parse", self.branch)
        with mock.patch.object(runner, "run", side_effect=self.quiet_run), \
                mock.patch.object(runner.sandbox, "run_sandbox"):
            with self.assertRaises(subprocess.CalledProcessError):
                runner.execute(self.proposal_path, self.repo, "main", False)
        self.assertEqual(before, self.git("rev-parse", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()

    def test_changed_commit_is_not_published(self):
        def change_after_commit(command, cwd, **kwargs):
            result = self.quiet_run(command, cwd, **kwargs)
            if command[:2] == ["git", "commit"]:
                (cwd / self.target).write_text("unvalidated change\n")
                self.real_run(["git", "add", self.target], cwd, capture=True)
                self.real_run(["git", "commit", "--amend", "--no-edit"], cwd, capture=True)
            return result
        with mock.patch.object(runner, "run", side_effect=change_after_commit), \
                mock.patch.object(runner.sandbox, "run_sandbox"):
            with self.assertRaises(runner.ProposalError):
                runner.execute(self.proposal_path, self.repo, "main", False)
        self.assertEqual("", self.git("ls-remote", "--heads", "origin", self.branch))
        self.assertEqual([], self.pr_bodies)
        self.assert_cleaned()


if __name__ == "__main__":
    unittest.main()
