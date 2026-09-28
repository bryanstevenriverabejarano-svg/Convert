"""Authenticated WSGI tool bridge. Public reads + disposable offline Docker execution.

No shell on the host, no credentials in tool arguments, no automatic image pulls.
Run behind a maintained HTTPS reverse proxy/WSGI server; the CLI binds loopback for development.
"""
from __future__ import annotations

import hashlib
import hmac
import http.client
from html.parser import HTMLParser
import ipaddress
import json
import os
from pathlib import Path
import re
import selectors
import shutil
import socket
import sqlite3
import ssl
import subprocess
import threading
import tempfile
import time
from urllib.parse import urljoin, urlsplit, urlencode
import uuid

MAX_BODY = 32_000
MAX_PAGE = 256_000
MAX_OUTPUT = 24_000
IMAGE = re.compile(r"(?:[a-z0-9][a-z0-9._:/-]*@)?sha256:[0-9a-f]{64}")
TOKEN = re.compile(r"[A-Za-z0-9_-]{32,256}")


class ToolError(Exception):
    pass


def public_url(url: str) -> tuple:
    try:
        p = urlsplit(url)
        if (len(url) > 500 or p.scheme != "https" or not p.hostname or p.username or p.password
                or p.port is not None or p.fragment or "\\" in url or any(ord(c) < 33 for c in url)):
            raise ValueError()
        host = p.hostname.encode("idna").decode("ascii")
        if host.endswith((".localhost", ".local", ".internal")) or host == "localhost":
            raise ValueError()
        addresses = socket.getaddrinfo(host, 443, type=socket.SOCK_STREAM)
        def allowed(raw):
            address = ipaddress.ip_address(raw)
            if not address.is_global or address.is_multicast or address.is_reserved or address.is_link_local:
                return False
            if address.version == 6 and (address.ipv4_mapped is not None or address in ipaddress.ip_network("64:ff9b::/96")
                                         or address in ipaddress.ip_network("64:ff9b:1::/48")):
                return False
            return True
        if not addresses or any(not allowed(a[4][0]) for a in addresses):
            raise ValueError()
        return p, host, addresses
    except (ValueError, OSError, UnicodeError) as error:
        raise ToolError("Destino HTTPS público no válido") from error


class PinnedHTTPS(http.client.HTTPSConnection):
    def __init__(self, host, address, timeout):
        super().__init__(host, timeout=timeout, context=ssl.create_default_context())
        self.address = address

    def connect(self):
        family, kind, proto, _, address = self.address
        raw = socket.socket(family, kind, proto)
        try:
            raw.settimeout(self.timeout)
            raw.connect(address)  # Connect the checked IP; TLS still verifies the original hostname.
            self.sock = self._context.wrap_socket(raw, server_hostname=self.host)
        except BaseException:
            raw.close()
            raise


def fetch_public(url: str, *, headers=None):
    deadline = time.monotonic() + 12
    for hop in range(4):
        p, host, addresses = public_url(url)
        remaining = deadline - time.monotonic()
        if remaining <= 0:
            raise ToolError("Tiempo de lectura agotado")
        connection = PinnedHTTPS(host, addresses[0], min(5, remaining))
        try:
            path = p.path or "/"
            if p.query:
                path += "?" + p.query
            connection.request("GET", path, headers={"User-Agent": "Salve-Tool-Bridge/1", "Accept-Encoding": "identity", **(headers or {})})
            response = connection.getresponse()
            if 300 <= response.status < 400:
                location = response.getheader("Location")
                if not location or hop == 3 or headers:
                    raise ToolError("Redirección no admitida")
                url = urljoin(url, location)
                continue
            content_type = response.getheader("Content-Type", "").split(";")[0].strip().lower()
            if response.status != 200 or response.getheader("Content-Encoding", "identity") != "identity" or not (content_type.startswith("text/") or content_type == "application/json"):
                raise ToolError("Respuesta pública no utilizable")
            chunks, size = [], 0
            while True:
                if time.monotonic() >= deadline:
                    raise ToolError("Tiempo de lectura agotado")
                if connection.sock:
                    connection.sock.settimeout(max(.1, min(5, deadline - time.monotonic())))
                chunk = response.read(4096)
                if not chunk:
                    break
                size += len(chunk)
                if size > MAX_PAGE:
                    raise ToolError("Página demasiado grande")
                chunks.append(chunk)
            return url, b"".join(chunks).decode("utf-8", errors="replace"), content_type
        finally:
            connection.close()
    raise ToolError("Lectura incompleta")


class Document(HTMLParser):
    def __init__(self, base):
        super().__init__(convert_charrefs=True)
        self.base, self.text, self.links, self.hidden = base, [], [], 0

    def handle_starttag(self, tag, attrs):
        if tag in ("script", "style", "noscript"):
            self.hidden += 1
        if tag == "a" and len(self.links) < 12:
            for key, value in attrs:
                if key == "href" and value:
                    target = urljoin(self.base, value)
                    p = urlsplit(target)
                    if len(target) <= 500 and p.scheme == "https" and p.hostname and not p.username and not p.fragment:
                        if target not in self.links:
                            self.links.append(target)

    def handle_endtag(self, tag):
        if tag in ("script", "style", "noscript") and self.hidden:
            self.hidden -= 1

    def handle_data(self, data):
        if not self.hidden:
            self.text.append(data)


def document(raw, base, content_type="text/html"):
    if content_type == "application/json":
        return raw[:6000], []
    parser = Document(base)
    parser.feed(raw)
    return " ".join(" ".join(parser.text).split())[:6000], parser.links


class DockerExecutor:
    def __init__(self, image=None):
        self.image = image or os.environ.get("SALVE_AGENT_IMAGE", "")

    def available(self):
        return bool(IMAGE.fullmatch(self.image) and shutil.which("docker"))

    def command(self, name, tool):
        if not self.available() or tool not in ("code.python", "browser.render"):
            raise ToolError("Sandbox Docker no configurado; no se ejecuta código en el anfitrión")
        docker = shutil.which("docker")
        return [docker, "--host", "unix:///var/run/docker.sock", "run", "--rm", "--name", name,
                "--pull", "never", "--network", "none", "--read-only", "--user", "65534:65534",
                "--cap-drop", "ALL", "--security-opt", "no-new-privileges=true", "--pids-limit", "96",
                "--cpus", "1", "--memory", "768m", "--memory-swap", "768m", "--log-driver", "none",
                "--no-healthcheck", "--tmpfs", "/tmp:rw,nosuid,nodev,size=192m,mode=1777", "-i",
                "--entrypoint", "python", self.image, "-I", "/opt/salve/worker.py", tool]

    def run(self, tool, payload):
        with tempfile.TemporaryDirectory(prefix="salve-agent-docker-") as config:
            return self._run(tool, payload, config)

    def _run(self, tool, payload, config):
        name = "salve-agent-" + uuid.uuid4().hex
        command = self.command(name, tool)
        command[1:1] = ["--config", config]
        prefix = command[:5]
        environment = {"PATH": os.defpath, "LANG": "C.UTF-8"}
        # Docker image-declared volumes would create unintended writable mounts.
        check = subprocess.run(prefix + ["image", "inspect",
                                "--format", "{{json .Config.Volumes}}", self.image], capture_output=True,
                               timeout=5, env=environment, check=False)
        if check.returncode != 0 or json.loads(check.stdout) not in (None, {}):
            raise ToolError("Imagen ausente o con volúmenes no permitidos")
        encoded = json.dumps(payload, ensure_ascii=False).encode("utf-8")
        if len(encoded) > 300_000:
            raise ToolError("Entrada del sandbox demasiado grande")
        process = subprocess.Popen(command, stdin=subprocess.PIPE, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, env=environment)
        writer_errors = []

        def write_input():
            try:
                process.stdin.write(encoded)
                process.stdin.close()
            except (OSError, ValueError) as error:
                writer_errors.append(type(error).__name__)

        writer = threading.Thread(target=write_input, daemon=True)
        writer.start()
        output, deadline = bytearray(), time.monotonic() + 20
        try:
            with selectors.DefaultSelector() as selector:
                selector.register(process.stdout, selectors.EVENT_READ)
                while selector.get_map():
                    if time.monotonic() >= deadline:
                        raise ToolError("Sandbox: tiempo agotado")
                    for key, _ in selector.select(.2):
                        chunk = os.read(key.fileobj.fileno(), 4096)
                        if not chunk:
                            selector.unregister(key.fileobj)
                            continue
                        output.extend(chunk)
                        if len(output) > MAX_OUTPUT:
                            raise ToolError("Sandbox: salida demasiado grande")
            code = process.wait(timeout=max(.1, deadline - time.monotonic()))
            return code, output.decode("utf-8", errors="replace")
        finally:
            try:
                subprocess.run(prefix + ["rm", "-f", name], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL,
                               timeout=5, env=environment, check=False)
            finally:
                if process.poll() is None:
                    process.kill()
                process.wait(timeout=5)
                writer.join(timeout=1)
                process.stdout.close()


def execute(tool, value, *, fetch=fetch_public, executor=None):
    executor = executor or DockerExecutor()
    if tool == "code.python":
        if not executor.available():
            return {"status": "BLOCKED", "text": "Sandbox Python no configurado", "sources": [], "links": []}
        code, text = executor.run(tool, {"code": value})
        return {"status": ("PARTIAL" if len(text) > 6000 else "SUCCESS") if code == 0 else "ERROR",
                "text": text[:5990] + "…" if len(text) > 6000 else text or "Ejecución sin salida de texto", "sources": [], "links": []}
    if tool == "web.search":
        key = os.environ.get("SALVE_BRAVE_SEARCH_KEY", "")
        if not key:
            return {"status": "BLOCKED", "text": "Búsqueda general sin configurar; usa el lector local de Wikipedia", "sources": [], "links": []}
        url = "https://api.search.brave.com/res/v1/web/search?" + urlencode({"q": value, "count": 5})
        _, raw, _ = fetch(url, headers={"X-Subscription-Token": key, "Accept": "application/json"})
        results = json.loads(raw).get("web", {}).get("results", [])[:5]
        text, urls = [], []
        for result in results:
            target = result.get("url", "")
            if len(target) <= 500 and target.startswith("https://"):
                urls.append(target)
                text.append(target + "\n" + str(result.get("description", ""))[:800])
        return {"status": "PARTIAL" if urls else "ERROR", "text": "Resultados de búsqueda; deben contrastarse leyendo las páginas:\n" + "\n".join(text)[:5600], "sources": urls, "links": urls}
    if tool in ("web.read", "api.get", "browser.render"):
        if tool == "browser.render" and not executor.available():
            return {"status": "BLOCKED", "text": "Navegador aislado no configurado", "sources": [], "links": []}
        url, raw, kind = fetch(value)
        if tool == "browser.render":
            code, rendered = executor.run(tool, {"html": raw})
            if code:
                return {"status": "ERROR", "text": "No se pudo renderizar la página", "sources": [], "links": []}
            # Worker emits bounded text and links as JSON, never host actions.
            result = json.loads(rendered)
            links = []
            for item in result.get("links", [])[:12]:
                if isinstance(item, str):
                    target = urljoin(url, item)
                    try:
                        p = urlsplit(target)
                        if p.scheme == "https" and p.hostname and not p.username and not p.port and not p.fragment and len(target) <= 500:
                            links.append(target)
                    except ValueError:
                        pass
            return {"status": "PARTIAL", "text": "Renderizado sin red; recursos externos y sesiones no cargados.\n" + result.get("text", "")[:5700], "sources": [url], "links": links}
        text, links = document(raw, url, kind)
        return {"status": "SUCCESS", "text": text, "sources": [url], "links": links}
    raise ToolError("Herramienta no registrada")


class BridgeApplication:
    def __init__(self, token, database, executor=None, fetch=fetch_public):
        if not TOKEN.fullmatch(token):
            raise ValueError("Configura un token de al menos 32 caracteres seguros")
        self.token, self.database = token, str(database)
        self.executor, self.fetch = executor or DockerExecutor(), fetch
        self.gate = threading.BoundedSemaphore(2)
        Path(database).parent.mkdir(parents=True, exist_ok=True)
        with sqlite3.connect(self.database) as db:
            db.execute("CREATE TABLE IF NOT EXISTS receipts (id TEXT PRIMARY KEY, digest TEXT NOT NULL, started REAL NOT NULL, response TEXT)")
        os.chmod(self.database, 0o600)

    def __call__(self, environ, start_response):
        def respond(code, value):
            body = json.dumps(value, ensure_ascii=False).encode("utf-8")
            start_response(code, [("Content-Type", "application/json; charset=utf-8"), ("Content-Length", str(len(body))), ("Cache-Control", "no-store")])
            return [body]
        supplied = environ.get("HTTP_AUTHORIZATION", "")
        if not hmac.compare_digest(supplied.encode("utf-8"), ("Bearer " + self.token).encode("utf-8")):
            return respond("401 Unauthorized", {"error": "unauthorized"})
        if environ.get("PATH_INFO") == "/v1/capabilities" and environ.get("REQUEST_METHOD") == "GET":
            return respond("200 OK", {"public_read": True, "web_search": bool(os.environ.get("SALVE_BRAVE_SEARCH_KEY")),
                                       "python_configured": self.executor.available(), "browser_offline_render_configured": self.executor.available(),
                                       "availability": "checked_on_execution"})
        if environ.get("PATH_INFO") != "/v1/execute" or environ.get("REQUEST_METHOD") != "POST":
            return respond("404 Not Found", {"error": "not_found"})
        if not self.gate.acquire(blocking=False):
            return respond("429 Too Many Requests", {"error": "busy"})
        try:
            try:
                length = int(environ.get("CONTENT_LENGTH", "0"))
                key = environ.get("HTTP_IDEMPOTENCY_KEY", "")
                if not 1 <= length <= MAX_BODY or not re.fullmatch(r"[a-f0-9-]{36}", key):
                    raise ValueError()
                raw = environ["wsgi.input"].read(length)
                if len(raw) != length:
                    raise ValueError()
                payload = json.loads(raw)
                if not isinstance(payload, dict) or set(payload) != {"tool", "input"} or payload["tool"] not in ("web.search", "web.read", "api.get", "browser.render", "code.python") or not isinstance(payload["input"], str) or not 1 <= len(payload["input"]) <= 6000:
                    raise ValueError()
            except (ValueError, TypeError, KeyError):
                return respond("400 Bad Request", {"error": "invalid_request"})
            digest = hashlib.sha256(raw).hexdigest()
            with sqlite3.connect(self.database, timeout=3, isolation_level=None) as db:
                db.execute("BEGIN IMMEDIATE")
                previous = db.execute("SELECT digest, started, response FROM receipts WHERE id=?", (key,)).fetchone()
                if previous and previous[0] != digest:
                    db.rollback()
                    return respond("409 Conflict", {"error": "idempotency_conflict"})
                if previous and previous[2] is not None:
                    db.rollback()
                    return respond("200 OK", json.loads(previous[2]))
                if previous and time.time() - previous[1] < 60:
                    db.rollback()
                    return respond("409 Conflict", {"error": "in_progress"})
                db.execute("INSERT OR REPLACE INTO receipts VALUES (?, ?, ?, NULL)", (key, digest, time.time()))
                db.commit()
            try:
                result = execute(payload["tool"], payload["input"], executor=self.executor, fetch=self.fetch)
            except (ToolError, OSError, ValueError, subprocess.SubprocessError):
                result = {"status": "ERROR", "text": "La herramienta no completó la ejecución", "sources": [], "links": []}
            # Short, bounded receipts; request text and secrets are never persisted by the bridge.
            encoded = json.dumps(result)
            if len(encoded.encode("utf-8")) > MAX_OUTPUT:
                encoded = json.dumps({"status": "ERROR", "text": "Salida fuera de presupuesto", "sources": [], "links": []})
                result = json.loads(encoded)
            with sqlite3.connect(self.database) as db:
                db.execute("UPDATE receipts SET response=? WHERE id=? AND digest=?", (encoded, key, digest))
                db.execute("DELETE FROM receipts WHERE started < ?", (time.time() - 7 * 86400,))
            return respond("200 OK", result)
        finally:
            self.gate.release()


_application = None
_init_lock = threading.Lock()


def application(environ, start_response):
    global _application
    with _init_lock:
        if _application is None:
            _application = BridgeApplication(os.environ.get("SALVE_AGENT_TOKEN", ""),
                                             os.environ.get("SALVE_AGENT_DB", "agent-state/receipts.sqlite"))
    return _application(environ, start_response)


if __name__ == "__main__":
    from wsgiref.simple_server import make_server
    with make_server("127.0.0.1", 8765, application) as server:
        server.serve_forever()
