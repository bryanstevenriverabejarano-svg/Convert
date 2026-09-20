import os
from pathlib import Path
import subprocess
import sys
import io
import signal
import tarfile
import tempfile
import unittest
from unittest import mock

sys.path.insert(0, str(Path(__file__).parent))
import auto_improvement_sandbox as sandbox


IMAGE = "sha256:" + "a" * 64


class SandboxTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.archive = Path(self.temporary.name) / "source.tar"
        self.archive.write_bytes(b"snapshot fixture")
        self.environment = mock.patch.dict(os.environ, {
            "SALVE_SANDBOX_IMAGE": IMAGE, "SALVE_DOCKER_HOST": "unix:///run/docker.sock",
            "GH_TOKEN": "must-not-leak", "GITHUB_TOKEN": "must-not-leak",
            "SSH_AUTH_SOCK": "/private/agent", "HTTPS_PROXY": "https://private.invalid",
            "DOCKER_HOST": "tcp://private.invalid", "DOCKER_CONFIG": "/private/docker",
        }, clear=True)
        self.environment.start()
        self.addCleanup(self.environment.stop)
        self.which = mock.patch.object(sandbox.shutil, "which", return_value="/usr/bin/docker")
        self.which.start()
        self.addCleanup(self.which.stop)

    def completed(self, stdout="", returncode=0, stderr=""):
        return subprocess.CompletedProcess([], returncode, stdout, stderr)

    def test_requires_fixed_image_and_available_runtime(self):
        for image in ("", "salve:latest", "--privileged", "sha256:short"):
            with self.subTest(image=image), mock.patch.dict(os.environ, {"SALVE_SANDBOX_IMAGE": image}):
                with self.assertRaises(sandbox.SandboxError):
                    sandbox.configured_image()
        with mock.patch.object(sandbox.shutil, "which", return_value=None):
            with self.assertRaises(sandbox.SandboxError):
                sandbox.configured_image()
        self.assertEqual(IMAGE, sandbox.configured_image())

    def test_rejects_remote_daemon(self):
        with mock.patch.dict(os.environ, {"SALVE_DOCKER_HOST": "tcp://example.invalid:2375"}):
            with self.assertRaises(sandbox.SandboxError):
                sandbox.docker_prefix(Path(self.temporary.name))

    @mock.patch.object(sandbox.subprocess, "run")
    def test_credentials_and_git_are_not_exposed(self, run):
        run.side_effect = [self.completed("null"), self.completed(), self.completed()]
        sandbox.run_sandbox(self.archive, IMAGE)
        command = run.call_args_list[1].args[0]
        for flag, value in (("--network", "none"), ("--user", "65534:65534"),
                            ("--cap-drop", "ALL"), ("--pull", "never")):
            self.assertEqual(value, command[command.index(flag) + 1])
        self.assertIn("--read-only", command)
        self.assertIn("no-new-privileges=true", command)
        self.assertEqual(1, command.count("--mount"))
        self.assertEqual(f"type=bind,src={self.archive},dst=/input/source.tar,readonly",
                         command[command.index("--mount") + 1])
        for call in run.call_args_list:
            self.assertEqual({"PATH", "LANG"}, set(call.kwargs["env"]))
            self.assertNotIn("must-not-leak", str(call))
            self.assertNotIn("/private/", str(call))
        self.assertEqual(sandbox.TIMEOUT_SECONDS, run.call_args_list[1].kwargs["timeout"])
        self.assertIn("--offline", command[-1])
        self.assertIn("--rerun-tasks", command[-1])

    @mock.patch.object(sandbox.subprocess, "run")
    def test_failure_timeout_and_interrupt_remove_the_container(self, run):
        for error in (subprocess.CalledProcessError(1, ["docker"]),
                      subprocess.TimeoutExpired(["docker"], 600), KeyboardInterrupt()):
            with self.subTest(error=type(error).__name__):
                run.reset_mock()
                run.side_effect = [self.completed("null"), error, self.completed()]
                expected = KeyboardInterrupt if isinstance(error, KeyboardInterrupt) else sandbox.SandboxError
                with self.assertRaises(expected):
                    sandbox.run_sandbox(self.archive, IMAGE)
                command = run.call_args_list[1].args[0]
                name = command[command.index("--name") + 1]
                self.assertEqual(["rm", "--force", name], run.call_args_list[2].args[0][-3:])

    @mock.patch.object(sandbox.subprocess, "run")
    def test_cleanup_failure_blocks_success(self, run):
        run.side_effect = [self.completed("null"), self.completed(),
                           self.completed(returncode=1, stderr="daemon disconnected")]
        with self.assertRaises(sandbox.SandboxError):
            sandbox.run_sandbox(self.archive, IMAGE)

    @mock.patch.object(sandbox.subprocess, "run")
    def test_sigterm_cleans_up_and_restores_handler(self, run):
        previous = signal.getsignal(signal.SIGTERM)
        def fallback(signum, frame):
            raise AssertionError("El sandbox no instaló su manejador de parada")
        signal.signal(signal.SIGTERM, fallback)
        def invoke(command, **kwargs):
            if "inspect" in command:
                return self.completed("null")
            if "run" in command:
                signal.raise_signal(signal.SIGTERM)
            return self.completed()
        run.side_effect = invoke
        try:
            with self.assertRaises(KeyboardInterrupt):
                sandbox.run_sandbox(self.archive, IMAGE)
            self.assertIn("rm", run.call_args_list[-1].args[0])
            self.assertIs(fallback, signal.getsignal(signal.SIGTERM))
        finally:
            signal.signal(signal.SIGTERM, previous)

    @mock.patch.object(sandbox.subprocess, "run")
    def test_already_removed_container_is_success(self, run):
        run.side_effect = [self.completed("null"), self.completed(),
                           self.completed(returncode=1, stderr="No such container: test")]
        sandbox.run_sandbox(self.archive, IMAGE)

    @mock.patch.object(sandbox.subprocess, "run")
    def test_image_volumes_and_missing_image_prevent_execution(self, run):
        for inspection in (self.completed('{"/unsafe": {}}'),
                           subprocess.CalledProcessError(1, ["docker", "image", "inspect"])):
            run.reset_mock()
            run.side_effect = [inspection]
            with self.assertRaises(sandbox.SandboxError):
                sandbox.run_sandbox(self.archive, IMAGE)
            self.assertEqual(1, run.call_count)


@unittest.skipUnless(os.environ.get("SALVE_RUN_DOCKER_SMOKE") == "1",
                     "Prueba Docker real opcional; requiere imagen preparada")
class DockerSmokeTest(unittest.TestCase):
    def test_runtime_blocks_host_access(self):
        image = sandbox.configured_image()
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            secret = root / "host-secret"
            secret.write_text("private host fixture")
            probe = r'''#!/bin/sh
set -eu
test -z "${GH_TOKEN:-}"
test -z "${GITHUB_TOKEN:-}"
test -z "${SSH_AUTH_SOCK:-}"
test -z "${HTTPS_PROXY:-}"
test "$(id -u)" = "65534"
test "$(ls /sys/class/net)" = "lo"
test ! -e .git
test ! -S /var/run/docker.sock
if touch /input/source.tar 2>/dev/null; then exit 1; fi
if touch /root-filesystem-probe 2>/dev/null; then exit 1; fi
'''
            probe += f"test ! -e '{secret}'\n"
            data = probe.encode()
            archive = root / "source.tar"
            with tarfile.open(archive, "w") as snapshot:
                entry = tarfile.TarInfo("gradlew")
                entry.size, entry.mode = len(data), 0o755
                snapshot.addfile(entry, io.BytesIO(data))
            with mock.patch.dict(os.environ, {"GH_TOKEN": "host-only", "GITHUB_TOKEN": "host-only",
                                             "SSH_AUTH_SOCK": "/private/agent",
                                             "HTTPS_PROXY": "https://private.invalid"}):
                sandbox.run_sandbox(archive, image)
            self.assertEqual("private host fixture", secret.read_text())


if __name__ == "__main__":
    unittest.main()
