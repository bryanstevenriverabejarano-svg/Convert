import importlib.util
import sys
import unittest
from pathlib import Path
from unittest import mock


SCRIPTS = Path(__file__).parent
sys.path.insert(0, str(SCRIPTS))
MODULE_PATH = SCRIPTS / "auto_improvement_adb_bridge.py"
SPEC = importlib.util.spec_from_file_location("auto_improvement_adb_bridge", MODULE_PATH)
bridge = importlib.util.module_from_spec(SPEC)
assert SPEC.loader is not None
SPEC.loader.exec_module(bridge)


class AdbBridgeTest(unittest.TestCase):
    def test_rejects_package_command_injection(self):
        with self.assertRaises(bridge.BridgeError):
            bridge.validate_identity("com.salve.app;rm")

    def test_rejects_filename_path_traversal(self):
        with self.assertRaises(bridge.BridgeError):
            bridge.validate_identity("com.salve.app", "../proposal.json")

    @mock.patch.object(bridge.subprocess, "run")
    def test_lists_only_complete_proposals(self, run):
        run.return_value = mock.Mock(
            returncode=0,
            stdout="100-aaaa.json\n101-bbbb.json.tmp\n",
            stderr="",
        )
        self.assertEqual(["100-aaaa.json"], bridge.list_proposals("com.salve.app", None))

    @mock.patch.object(bridge, "archive_proposal")
    @mock.patch.object(bridge.auto_improvement_runner, "execute", return_value="https://example/pr/1")
    @mock.patch.object(bridge, "fetch_proposal")
    @mock.patch.object(bridge, "list_proposals", return_value=["100-aaaa.json"])
    def test_archives_only_after_success(self, _list, _fetch, _execute, archive):
        result = bridge.process_next("com.salve.app", None, Path("."), "main", False)
        self.assertEqual("https://example/pr/1", result)
        archive.assert_called_once_with("com.salve.app", None, "100-aaaa.json")

    @mock.patch.object(bridge, "archive_proposal")
    @mock.patch.object(bridge.auto_improvement_runner, "execute", side_effect=RuntimeError("fallo"))
    @mock.patch.object(bridge, "fetch_proposal")
    @mock.patch.object(bridge, "list_proposals", return_value=["100-aaaa.json"])
    def test_keeps_proposal_when_runner_fails(self, _list, _fetch, _execute, archive):
        with self.assertRaises(RuntimeError):
            bridge.process_next("com.salve.app", None, Path("."), "main", False)
        archive.assert_not_called()


if __name__ == "__main__":
    unittest.main()
