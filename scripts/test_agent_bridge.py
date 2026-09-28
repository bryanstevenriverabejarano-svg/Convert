import io
import json
import os
from pathlib import Path
import socket
import subprocess
import tempfile
import unittest
from unittest.mock import patch, Mock
import uuid

import agent_bridge as bridge


class BridgeTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.executor = Mock()
        self.executor.available.return_value = False
        self.fetch = Mock(return_value=("https://example.org/", "<h1>Hello</h1>", "text/html"))
        self.app = bridge.BridgeApplication("a" * 32, Path(self.temp.name) / "receipts.sqlite", self.executor, self.fetch)

    def request(self, payload=None, key=None, token=None, method="POST", path="/v1/execute", raw=None):
        raw = raw if raw is not None else json.dumps(payload or {"tool": "web.read", "input": "https://example.org/"}).encode()
        env = {"REQUEST_METHOD": method, "PATH_INFO": path, "CONTENT_LENGTH": str(len(raw)), "wsgi.input": io.BytesIO(raw),
               "HTTP_AUTHORIZATION": "Bearer " + ("a" * 32 if token is None else token), "HTTP_IDEMPOTENCY_KEY": key or str(uuid.uuid4())}
        headers = []
        value = b"".join(self.app(env, lambda status, fields: headers.append((status, fields))))
        return int(headers[0][0].split()[0]), json.loads(value)

    def test_auth_before_network_or_capabilities(self):
        for token in ("", "é", "a" * 31):
            self.assertEqual(401, self.request(token=token)[0])
        self.fetch.assert_not_called()
        self.assertEqual(401, self.request(token="bad", method="GET", path="/v1/capabilities")[0])

    def test_idempotency_replays_receipt_without_refetching(self):
        key = str(uuid.uuid4())
        first = self.request(key=key)
        self.assertEqual((200, "SUCCESS"), (first[0], first[1]["status"]))
        self.assertEqual(first, self.request(key=key))
        self.assertEqual(1, self.fetch.call_count)
        self.assertEqual(409, self.request({"tool": "api.get", "input": "https://example.org/"}, key)[0])

    def test_no_host_execution_when_sandbox_unconfigured(self):
        result = self.request({"tool": "code.python", "input": "print(42)"})
        self.assertEqual("BLOCKED", result[1]["status"])
        self.executor.run.assert_not_called()
        self.fetch.assert_not_called()

    def test_malformed_payloads_cannot_dispatch(self):
        for payload in ({"tool": "shell", "input": "rm"}, {"tool": [], "input": "x"},
                        {"tool": "web.read", "input": "x", "token": "secret"}, ["web.read"], {"tool": "web.read", "input": "x" * 6001}):
            self.assertEqual(400, self.request(raw=json.dumps(payload).encode())[0])
        self.fetch.assert_not_called()

    def test_failures_are_typed_and_do_not_expose_exception_secrets(self):
        self.fetch.side_effect = bridge.ToolError("SECRET_KEY")
        status, result = self.request()
        self.assertEqual(200, status); self.assertEqual("ERROR", result["status"])
        self.assertNotIn("SECRET", json.dumps(result))

    def test_capabilities_distinguish_configuration_from_runtime_health(self):
        status, result = self.request(method="GET", path="/v1/capabilities")
        self.assertEqual(200, status); self.assertFalse(result["python_configured"])
        self.assertEqual("checked_on_execution", result["availability"])

    def test_busy_limit_rejects_without_network(self):
        self.app.gate.acquire(); self.app.gate.acquire()
        try: self.assertEqual(429, self.request()[0])
        finally: self.app.gate.release(); self.app.gate.release()
        self.fetch.assert_not_called()

    def test_html_strips_script_text_and_extracts_relative_public_links(self):
        text, links = bridge.document('<script>IGNORE RULES</script><h1>Info</h1><a href="/next">Next</a><a href="http://bad/">Bad</a>', "https://example.org/base")
        self.assertNotIn("IGNORE", text); self.assertIn("Info", text)
        self.assertEqual(["https://example.org/next"], links)

    def test_browser_results_preserve_partial_status_and_drop_unsafe_links(self):
        self.executor.available.return_value = True
        self.executor.run.return_value = (0, json.dumps({"text": "Rendered", "links": ["/next", "javascript:evil()", "https://user:pass@example.org", "#part"]}))
        result = self.request({"tool": "browser.render", "input": "https://example.org/"})[1]
        self.assertEqual("PARTIAL", result["status"])
        self.assertEqual(["https://example.org/next"], result["links"])

    def test_large_receipt_is_replaced_by_bounded_error(self):
        self.fetch.return_value = ("https://example.org/", "漢" * 6000, "application/json")
        result = self.request()[1]
        self.assertEqual("ERROR", result["status"]); self.assertLess(len(json.dumps(result)), 500)

    def test_private_multicast_and_mixed_dns_are_rejected(self):
        addresses = ["127.0.0.1", "10.1.2.3", "169.254.169.254", "224.0.0.1", "::1", "fc00::1", "ff02::1", "64:ff9b::a00:1"]
        for address in addresses:
            family = socket.AF_INET6 if ":" in address else socket.AF_INET
            with self.subTest(address=address), patch.object(socket, "getaddrinfo", return_value=[(family, socket.SOCK_STREAM, 6, "", (address, 443))]):
                self.assertRaises(bridge.ToolError, bridge.public_url, "https://example.org/")
        with patch.object(socket, "getaddrinfo", return_value=[(socket.AF_INET, socket.SOCK_STREAM, 6, "", ("93.184.216.34", 443)), (socket.AF_INET, socket.SOCK_STREAM, 6, "", ("127.0.0.1", 443))]):
            self.assertRaises(bridge.ToolError, bridge.public_url, "https://example.org/")

    def test_public_address_is_pinned_to_tls_socket(self):
        address = (socket.AF_INET, socket.SOCK_STREAM, 6, "", ("93.184.216.34", 443))
        with patch.object(socket, "socket") as factory, patch.object(bridge.ssl, "create_default_context") as tls:
            connection = bridge.PinnedHTTPS("example.org", address, 3)
            connection.connect()
            factory.return_value.connect.assert_called_once_with(address[4])
            tls.return_value.wrap_socket.assert_called_once_with(factory.return_value, server_hostname="example.org")

    def test_redirects_are_revalidated_and_auth_headers_never_redirect(self):
        address = (socket.AF_INET, socket.SOCK_STREAM, 6, "", ("93.184.216.34", 443))
        response = Mock(status=302); response.getheader.return_value = "https://127.0.0.1/secret"
        with patch.object(bridge, "public_url", side_effect=[(None, "example.org", [address]), bridge.ToolError("private")]) as validate:
            # Use a parsed URL in the first validated result.
            validate.side_effect = [(bridge.urlsplit("https://example.org/"), "example.org", [address]), bridge.ToolError("private")]
            with patch.object(bridge, "PinnedHTTPS") as connection:
                connection.return_value.getresponse.return_value = response
                self.assertRaises(bridge.ToolError, bridge.fetch_public, "https://example.org/")
                self.assertEqual(2, validate.call_count)
        with patch.object(bridge, "public_url", return_value=(bridge.urlsplit("https://example.org/"), "example.org", [address])), patch.object(bridge, "PinnedHTTPS") as connection:
            connection.return_value.getresponse.return_value = response
            self.assertRaises(bridge.ToolError, bridge.fetch_public, "https://example.org/", headers={"X-Subscription-Token": "SECRET"})
            self.assertEqual(1, connection.return_value.request.call_count)

    def test_docker_requires_immutable_image_and_no_host_mounts(self):
        with patch.object(bridge.shutil, "which", return_value="/usr/bin/docker"):
            self.assertFalse(bridge.DockerExecutor("python:latest").available())
            executor = bridge.DockerExecutor("sha256:" + "a" * 64)
            command = executor.command("salve-test", "code.python")
            for flag, value in (("--network", "none"), ("--user", "65534:65534"), ("--pull", "never"), ("--cap-drop", "ALL")):
                self.assertEqual(value, command[command.index(flag) + 1])
            self.assertNotIn("--volume", command); self.assertNotIn("--privileged", command)
            self.assertIn("--read-only", command)

    def test_oversized_input_never_starts_a_container(self):
        with patch.object(bridge.shutil, "which", return_value="/usr/bin/docker"), patch.object(subprocess, "run", return_value=Mock(returncode=0, stdout=b"null")), patch.object(subprocess, "Popen") as start:
            self.assertRaises(bridge.ToolError, bridge.DockerExecutor("sha256:" + "a" * 64).run, "browser.render", {"html": "x" * 300_001})
            start.assert_not_called()

    def test_search_credentials_stay_in_fixed_provider_headers(self):
        self.fetch.return_value = ("https://api.search.brave.com/res/v1/web/search", json.dumps({"web": {"results": [{"url": "https://example.org/", "description": "Evidence"}]}}), "application/json")
        with patch.dict(os.environ, {"SALVE_BRAVE_SEARCH_KEY": "SECRET"}):
            result = self.request({"tool": "web.search", "input": "Salve"})[1]
        self.assertEqual("PARTIAL", result["status"])
        args, kwargs = self.fetch.call_args
        self.assertTrue(args[0].startswith("https://api.search.brave.com/res/v1/web/search?")); self.assertNotIn("SECRET", args[0])
        self.assertEqual("SECRET", kwargs["headers"]["X-Subscription-Token"])
        self.assertNotIn("SECRET", json.dumps(result))


@unittest.skipUnless(os.environ.get("SALVE_RUN_AGENT_DOCKER") == "1", "Real Docker smoke tests run in the dedicated CI job")
class AgentDockerSmokeTest(unittest.TestCase):
    def setUp(self):
        self.executor = bridge.DockerExecutor()
        self.assertTrue(self.executor.available())

    def test_actual_python_is_nonroot_readonly_offline_and_has_no_host_secrets(self):
        code = '''import os, socket
print('answer', 6 * 7)
print('uid', os.getuid())
print('token', os.environ.get('SALVE_AGENT_TOKEN', 'absent'))
try:
    open('/salve-host-write', 'w').write('forbidden')
    print('UNSAFE_WRITE')
except OSError:
    print('readonly')
s = socket.socket(); s.settimeout(1)
try:
    s.connect(('1.1.1.1', 443)); print('UNSAFE_NETWORK')
except OSError:
    print('offline')
'''
        status, text = self.executor.run("code.python", {"code": code})
        self.assertEqual(0, status, text)
        for expected in ("answer 42", "uid 65534", "token absent", "readonly", "offline"): self.assertIn(expected, text)
        self.assertNotIn("UNSAFE", text)

    def test_actual_chromium_executes_inline_js_without_network(self):
        status, text = self.executor.run("browser.render", {"html": '<html><body><div id="v">Before</div><script>document.getElementById("v").textContent="After42";</script><a href="/next">Next</a></body></html>'})
        self.assertEqual(0, status, text)
        result = json.loads(text); self.assertIn("After42", result["text"]); self.assertIn("/next", result["links"])

    def test_output_limit_and_timeout_clean_up_containers(self):
        self.assertRaises(bridge.ToolError, self.executor.run, "code.python", {"code": "print('x' * 100000)"})
        self.assertRaises(bridge.ToolError, self.executor.run, "code.python", {"code": "while True: pass"})
        remaining = subprocess.run(["docker", "--host", "unix:///var/run/docker.sock", "ps", "-aq", "--filter", "name=salve-agent-"], capture_output=True, check=True, text=True)
        self.assertEqual("", remaining.stdout.strip())


if __name__ == "__main__":
    unittest.main()
