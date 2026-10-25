"""Del parche que Salve escribe en el teléfono al PR: extracción, aplicación y publicación con Git real."""

import hashlib
import importlib.util
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import unittest
from unittest import mock

MODULE_PATH = Path(__file__).with_name("self_pr_ci.py")
sys.path.insert(0, str(MODULE_PATH.parent))
SPEC = importlib.util.spec_from_file_location("self_pr_ci", MODULE_PATH)
ci = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(ci)

PATH = "app/src/main/java/salve/core/conversation/TurnChecks.java"
# Igual que SelfPatchTest.SOURCE y el diff que SelfPatch.unifiedDiff genera para él (mismo formato byte a byte).
SOURCE = (
    "package salve.core.conversation;\n"
    "\n"
    "final class TurnChecks {\n"
    "    private TurnChecks() { }\n"
    "\n"
    "    static boolean returnsQuestion(String reply) {\n"
    "        String t = reply == null ? \"\" : reply.trim();\n"
    "        return t.endsWith(\"?\") && t.length() < 40;\n"
    "    }\n"
    "\n"
    "    static boolean announcesWithoutDoing(String reply) {\n"
    "        return reply != null && reply.startsWith(\"Voy a\");\n"
    "    }\n"
    "}\n"
)
PATCH = (
    f"diff --git a/{PATH} b/{PATH}\n"
    f"--- a/{PATH}\n"
    f"+++ b/{PATH}\n"
    "@@ -5,7 +5,7 @@\n"
    " \n"
    "     static boolean returnsQuestion(String reply) {\n"
    "         String t = reply == null ? \"\" : reply.trim();\n"
    "-        return t.endsWith(\"?\") && t.length() < 40;\n"
    "+        return t.endsWith(\"?\") && (t.length() < 40 || t.toLowerCase().contains(\"y a ti\"));\n"
    "     }\n"
    " \n"
    "     static boolean announcesWithoutDoing(String reply) {\n"
)


def git(cwd, *args):
    return subprocess.run(["git", *args], cwd=cwd, check=True, text=True, capture_output=True).stdout.strip()


def proposal(revision, **overrides):
    p = {
        "schemaVersion": 3,
        "proposalId": "a1b2c3d4-1234-4234-8234-123456789abc",
        "targetPath": PATH,
        "targetClass": "TurnChecks",
        "issueSummary": "Devuelve la pregunta en vez de responder (3 fallos en el examen).",
        "patch": PATCH,
        "sourceRevision": revision,
        "sourceSha256": hashlib.sha256(SOURCE.encode("utf-8")).hexdigest(),
        "syntheticValidationPassed": True,
        "ethicalReviewPassed": True,
    }
    p.update(overrides)
    return p


def issue_body(p):
    return ("Propuesta escrita por Salve en el teléfono.\n\n```diff\n" + p["patch"] + "```\n\n"
            + ci.MARKER + "\n```json\n" + json.dumps(p, ensure_ascii=False) + "\n```\n")


class SelfPrCiTest(unittest.TestCase):
    def setUp(self):
        self.tmp = tempfile.TemporaryDirectory()
        root = Path(self.tmp.name)
        self.origin = root / "origin.git"
        git(root, "init", "--bare", "-b", "main", str(self.origin))
        self.repo = root / "repo"
        git(root, "clone", "-q", str(self.origin), str(self.repo))
        git(self.repo, "config", "user.email", "t@t"); git(self.repo, "config", "user.name", "t")
        target = self.repo / PATH
        target.parent.mkdir(parents=True)
        target.write_text(SOURCE, encoding="utf-8")
        git(self.repo, "add", "."); git(self.repo, "commit", "-qm", "base"); git(self.repo, "push", "-q", "origin", "HEAD:main")
        self.base = git(self.repo, "rev-parse", "HEAD")

    def tearDown(self):
        self.tmp.cleanup()

    def test_extracts_only_the_marked_json_and_validates_it(self):
        p = proposal(self.base)
        self.assertEqual(p, ci.extract(issue_body(p)))
        for body in ("", "sin marcador", issue_body(p) + ci.MARKER, issue_body(p).replace("```json", "```"),
                     issue_body(proposal(self.base, targetPath="app/build.gradle")),
                     issue_body(proposal(self.base, ethicalReviewPassed=False)),
                     "x" * 70_000):
            with self.subTest(body=body[:40]), self.assertRaises(ci.runner.ProposalError):
                ci.extract(body)
        without_hash = proposal(self.base); del without_hash["sourceSha256"]; del without_hash["sourceRevision"]
        with self.assertRaises(ci.runner.ProposalError):
            ci.extract(issue_body(without_hash))

    def test_duplicate_keys_cannot_smuggle_a_second_value(self):
        body = issue_body(proposal(self.base)).replace('"schemaVersion": 3', '"schemaVersion": 3, "schemaVersion": 3')
        with self.assertRaises(ci.runner.ProposalError):
            ci.extract(body)

    def test_the_phone_patch_applies_and_leaves_the_tested_tree_on_disk(self):
        tree, generated = ci.apply(proposal(self.base), self.repo)
        self.assertIsNone(generated)
        self.assertIn('contains("y a ti")', (self.repo / PATH).read_text(encoding="utf-8"))
        self.assertEqual(tree, git(self.repo, "write-tree"))
        self.assertFalse((self.repo / ".git-salve-proposal.patch").exists())

    def test_a_stale_source_is_rejected_even_if_the_hunk_would_apply(self):
        (self.repo / PATH).write_text(SOURCE + "// cambio posterior\n", encoding="utf-8")
        git(self.repo, "commit", "-qam", "later")
        with self.assertRaisesRegex(ci.runner.ProposalError, "fuente cambió"):
            ci.apply(proposal(self.base), self.repo)

    def test_prepare_reports_rejections_without_failing_the_job(self):
        out = Path(self.tmp.name) / "out"
        env = dict(os.environ, ISSUE_BODY="no es una propuesta", GITHUB_OUTPUT=str(Path(self.tmp.name) / "gh_out"))
        with mock.patch.dict(os.environ, env), mock.patch.object(sys, "argv", ["x", "prepare", "--repo", str(self.repo), "--out", str(out)]):
            self.assertEqual(0, ci.main())
        self.assertIn("status=rejected", (Path(self.tmp.name) / "gh_out").read_text())
        self.assertIn("marcador", (out / "reason.txt").read_text(encoding="utf-8"))

    def test_publish_pushes_the_identical_tree_without_touching_main(self):
        tested = Path(self.tmp.name) / "tested"
        git(Path(self.tmp.name), "clone", "-q", str(self.origin), str(tested))
        tree, _ = ci.apply(proposal(self.base), tested)
        calls = []

        def fake_github(method, path, token, payload=None):
            calls.append((method, path, payload))
            return 201, {"html_url": "https://github.com/o/r/pull/300", "number": 300}

        with mock.patch.object(ci, "github", fake_github):
            url, number, sha = ci.publish(proposal(self.base), self.repo, self.base, tree, 42, "token")
        self.assertEqual("https://github.com/o/r/pull/300", url)
        self.assertEqual(300, number)
        branch = ci.branch_name(proposal(self.base))
        self.assertEqual("salve/auto-a1b2c3d4-turnchecks", branch)
        self.assertEqual(tree, git(self.origin, "rev-parse", f"refs/heads/{branch}^{{tree}}"))
        self.assertEqual(sha, git(self.origin, "rev-parse", f"refs/heads/{branch}"))
        self.assertEqual(self.base, git(self.origin, "rev-parse", "refs/heads/main"), "main no se toca")
        self.assertEqual([("POST", "/pulls")], [(m, p) for m, p, _ in calls], "publicar solo abre el PR")
        self.assertIn("#42", calls[0][2]["body"])
        self.assertIn("SALVE_AUTOMERGE=false", calls[0][2]["body"])

    def test_publish_refuses_a_tree_that_was_not_tested(self):
        with mock.patch.object(ci, "github", lambda *a, **k: self.fail("no debe llegar a GitHub")):
            with self.assertRaisesRegex(ci.runner.ProposalError, "difiere del probado"):
                ci.publish(proposal(self.base), self.repo, self.base, "0" * 40, 42, "token")
        self.assertEqual("", git(self.origin, "branch", "--list", "salve/*"))

    def test_without_pr_permission_the_branch_link_is_explained(self):
        tested = Path(self.tmp.name) / "tested"
        git(Path(self.tmp.name), "clone", "-q", str(self.origin), str(tested))
        tree, _ = ci.apply(proposal(self.base), tested)
        with mock.patch.dict(os.environ, {"GITHUB_REPOSITORY": "o/r"}), \
                mock.patch.object(ci, "github", lambda *a, **k: (403, {"message": "not permitted"})):
            with self.assertRaisesRegex(ci.runner.ProposalError, "Allow GitHub Actions to create"):
                ci.publish(proposal(self.base), self.repo, self.base, tree, 42, "token")


    def test_big_or_dangerous_changes_never_reach_automerge(self):
        exec_patch = PATCH.replace('contains(\"y a ti\"));', 'contains(\"y a ti\")) && Runtime.getRuntime() != null;')
        with self.assertRaisesRegex(ci.runner.ProposalError, "Runtime"):
            ci.extract(issue_body(proposal(self.base, patch=exec_patch)))
        url_patch = PATCH.replace('contains(\"y a ti\"));', 'contains(\"https://x.example\"));')
        with self.assertRaisesRegex(ci.runner.ProposalError, "no permitidas"):
            ci.extract(issue_body(proposal(self.base, patch=url_patch)))
        lines = "".join(f"+        int x{i} = {i};\n" for i in range(61))
        big = PATCH.replace("@@ -5,7 +5,7 @@", "@@ -5,7 +5,68 @@") + lines
        with self.assertRaisesRegex(ci.runner.ProposalError, "máximo"):
            ci.extract(issue_body(proposal(self.base, patch=big)))
        with self.assertRaisesRegex(ci.runner.ProposalError, "suites generadas"):
            ci.check_limits(proposal(self.base, generatedTests="public class X {}"))


SHA = "b" * 40


class MergeTest(unittest.TestCase):
    """Salve fusiona sola, pero solo exactamente lo que pasó la compilación completa."""

    def fake(self, conclusion="success", pr_sha=SHA, pr_state="open", runs_after=1):
        calls, polls = [], {"n": 0}

        def github(method, path, token, payload=None):
            calls.append((method, path, payload))
            if path.endswith("/dispatches"):
                return 204, {}
            if path.startswith("/actions/runs"):
                polls["n"] += 1
                state = "completed" if polls["n"] > runs_after else "in_progress"
                return 200, {"workflow_runs": [
                    {"head_sha": "c" * 40, "path": ".github/workflows/android.yml", "status": "completed", "conclusion": "success"},
                    {"head_sha": SHA, "path": ".github/workflows/android.yml", "status": state, "conclusion": conclusion}]}
            if path == "/pulls/300" and method == "GET":
                return 200, {"state": pr_state, "head": {"sha": pr_sha}, "title": "Salve propone: TurnChecks"}
            if path == "/pulls/300/merge":
                return 200, {"merged": True, "sha": "d" * 40}
            if method == "DELETE":
                return 204, {}
            return 404, {}
        return github, calls

    def test_merges_the_exact_commit_after_a_green_android_build(self):
        github, calls = self.fake(runs_after=2)
        with mock.patch.object(ci, "github", github):
            merged = ci.merge(300, "salve/auto-a1b2c3d4-turnchecks", SHA, "t", sleep=lambda s: None)
        self.assertEqual("d" * 40, merged)
        merge_call = [c for c in calls if c[1] == "/pulls/300/merge"][0]
        self.assertEqual(SHA, merge_call[2]["sha"], "fusiona solo el commit probado")
        self.assertEqual("squash", merge_call[2]["merge_method"])
        self.assertIn(("POST", "/actions/workflows/android.yml/dispatches", {"ref": "main"}), calls, "pide el APK de main")

    def test_a_red_build_is_never_merged(self):
        github, calls = self.fake(conclusion="failure")
        with mock.patch.object(ci, "github", github), self.assertRaisesRegex(ci.runner.ProposalError, "failure"):
            ci.merge(300, "salve/auto-x", SHA, "t", sleep=lambda s: None)
        self.assertFalse([c for c in calls if c[1].endswith("/merge")])

    def test_a_pr_changed_during_the_build_is_not_merged(self):
        for kwargs in ({"pr_sha": "e" * 40}, {"pr_state": "closed"}):
            github, calls = self.fake(**kwargs)
            with self.subTest(**kwargs), mock.patch.object(ci, "github", github), \
                    self.assertRaisesRegex(ci.runner.ProposalError, "cambió"):
                ci.merge(300, "salve/auto-x", SHA, "t", sleep=lambda s: None)
            self.assertFalse([c for c in calls if c[1].endswith("/merge")])

    def test_gives_up_if_the_build_never_finishes(self):
        github, calls = self.fake(runs_after=10**9)
        with mock.patch.object(ci, "github", github), self.assertRaisesRegex(ci.runner.ProposalError, "a tiempo"):
            ci.merge(300, "salve/auto-x", SHA, "t", wait=0, sleep=lambda s: None)
        self.assertFalse([c for c in calls if c[1].endswith("/merge")])

    def test_only_salve_branches_can_be_merged_from_the_command_line(self):
        out = Path(tempfile.mkdtemp())
        with mock.patch.dict(os.environ, {"GH_TOKEN": "t", "GITHUB_OUTPUT": str(out / "o")}), \
                mock.patch.object(ci, "github", lambda *a, **k: self.fail("no debe llamar a GitHub")), \
                mock.patch.object(sys, "argv", ["x", "merge", "--number", "1", "--branch", "main", "--sha", SHA, "--out", str(out)]):
            self.assertEqual(0, ci.main())
        self.assertIn("merged=false", (out / "o").read_text())


if __name__ == "__main__":
    unittest.main()
