import hashlib
import json
from pathlib import Path
import subprocess
import tempfile
import unittest

from export_auto_improvement_sources import export_snapshot


class SourceExportTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.repo = Path(self.temporary.name)
        self.path = "app/src/main/java/salve/core/Example.java"
        self.source = self.repo / self.path
        self.source.parent.mkdir(parents=True)
        self.source.write_bytes("package salve.core;\nclass Example { /* ¿Qué tal? */ }\r\n".encode("utf-8"))
        for arguments in (("init",), ("config", "user.name", "Tests"), ("config", "user.email", "tests@example.invalid"),
                          ("add", "."), ("commit", "-m", "Source")):
            self.git(*arguments)
        self.original = self.source.read_bytes()
        self.output = self.repo / "source-snapshot.json"

    def git(self, *arguments):
        return subprocess.run(["git", *arguments], cwd=self.repo, check=True, capture_output=True).stdout

    def test_exports_committed_bytes_not_dirty_worktree(self):
        self.source.write_text("uncommitted contents")
        result = export_snapshot(self.repo, "HEAD", [self.path], self.output)
        self.assertEqual(self.git("rev-parse", "HEAD").decode().strip(), result["revision"])
        self.assertEqual(self.original, result["files"][0]["source"].encode("utf-8"))
        self.assertEqual(hashlib.sha256(self.original).hexdigest(), result["files"][0]["sha256"])
        self.assertEqual(result, json.loads(self.output.read_text()))

    def test_rejects_duplicate_unsafe_missing_paths_and_revision_option(self):
        for paths in ([self.path, self.path], ["../Example.java"], ["app/build.gradle"],
                      ["app/src/main/java/salve/core/Missing.java"], []):
            with self.subTest(paths=paths), self.assertRaises(ValueError):
                export_snapshot(self.repo, "HEAD", paths, self.output)
        with self.assertRaises(ValueError):
            export_snapshot(self.repo, "--help", [self.path], self.output)
        self.assertFalse(self.output.exists())

    def test_rejects_symlink_and_oversized_source_without_replacing_previous_snapshot(self):
        export_snapshot(self.repo, "HEAD", [self.path], self.output)
        previous = self.output.read_bytes()
        self.source.write_text("x" * 8_001)
        self.git("add", self.path)
        self.git("commit", "-m", "Too big")
        with self.assertRaises(ValueError):
            export_snapshot(self.repo, "HEAD", [self.path], self.output)
        self.source.unlink()
        self.source.symlink_to("../../../../../../../private.txt")
        self.git("add", self.path)
        self.git("commit", "-m", "Symlink")
        with self.assertRaises(ValueError):
            export_snapshot(self.repo, "HEAD", [self.path], self.output)
        self.assertEqual(previous, self.output.read_bytes())


if __name__ == "__main__":
    unittest.main()
