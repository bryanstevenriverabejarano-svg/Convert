#!/usr/bin/env python3
"""Salve text worker: authenticated tasks -> a fixed, loopback llama.cpp backend.

Python 3.10+, standard library. Put behind an HTTPS reverse proxy; this development
server binds loopback only. No shell, tools, replication, provisioning or downloads.
"""
from __future__ import annotations

import argparse
from collections import OrderedDict, deque
import hashlib
import hmac
import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import ipaddress
import json
import os
from pathlib import Path
import re
import socket
import threading
import time
from urllib.parse import urlsplit
import uuid

MAX_BODY = 300_000
MAX_PROMPT = 48_000
MAX_TEXT = 60_000
MAX_TOKENS = 2048
ROLES = frozenset(('CONVERSACIONAL', 'REFLEXION', 'SISTEMA', 'PLANIFICADOR', 'OBSERVADOR',
                   'SINTETIZADOR', 'EVALUADOR', 'CREADOR', 'CURIOSO', 'NARRADOR'))
TOKEN = re.compile(r'[A-Za-z0-9_-]{32,256}')


class NodeError(Exception):
    pass


class LocalBackend:
    """The operator fixes the address/model. Tasks cannot supply a backend URL or tools."""
    def __init__(self, url, model, timeout=40):
        parsed = urlsplit(url)
        try:
            if (parsed.scheme != 'http' or not ipaddress.ip_address(parsed.hostname).is_loopback
                    or parsed.username or parsed.password or parsed.query or parsed.fragment
                    or parsed.path not in ('', '/') or not 1 <= (parsed.port or 8080) <= 65535):
                raise ValueError()
        except (ValueError, TypeError):
            raise ValueError('Backend must be a literal loopback HTTP address') from None
        if not isinstance(model, str) or not re.fullmatch(r'[^\x00-\x1f\x7f]{1,120}', model):
            raise ValueError('Invalid fixed model name')
        self.host, self.port, self.model, self.timeout = parsed.hostname, parsed.port or 8080, model, timeout

    def request(self, method, path, payload=None, timeout=None):
        connection = http.client.HTTPConnection(self.host, self.port, timeout=timeout or self.timeout)
        deadline = time.monotonic() + (timeout or self.timeout)
        timer = threading.Timer(timeout or self.timeout, lambda: self._abort(connection))
        timer.daemon = True
        timer.start()
        try:
            body = None if payload is None else json.dumps(payload, ensure_ascii=False).encode('utf-8')
            connection.request(method, path, body, {'Content-Type': 'application/json', 'Accept-Encoding': 'identity'})
            response = connection.getresponse()
            if response.status != 200 or response.getheader('Content-Encoding', 'identity') != 'identity':
                raise NodeError('Backend unavailable')
            chunks, size = [], 0
            while True:
                if time.monotonic() >= deadline:
                    raise NodeError('Backend deadline exceeded')
                chunk = response.read1(8192)
                if not chunk:
                    break
                size += len(chunk)
                if size > MAX_BODY:
                    raise NodeError('Backend response too large')
                chunks.append(chunk)
            return json.loads(b''.join(chunks))
        except (OSError, ValueError, http.client.HTTPException) as error:
            raise NodeError('Backend request failed') from error
        finally:
            timer.cancel()
            connection.close()

    @staticmethod
    def _abort(connection):
        try:
            if connection.sock:
                connection.sock.shutdown(socket.SHUT_RDWR)
        except OSError:
            pass
        connection.close()

    def ready(self):
        try:
            return self.request('GET', '/health', timeout=1).get('status') == 'ok'
        except (NodeError, AttributeError):
            return False

    def generate(self, task):
        response = self.request('POST', '/v1/chat/completions', {
            'model': self.model, 'stream': False, 'max_tokens': task['max_tokens'],
            'temperature': .65 if task['role'] == 'NARRADOR' else .35,
            'messages': [{'role': 'user', 'content': task['prompt']}],
        })
        try:
            choice = response['choices'][0]
            # Truncated/error responses must not be committed as successful memory consolidation.
            if choice.get('finish_reason') != 'stop':
                raise NodeError('Backend did not finish')
            text = choice['message']['content']
            if not isinstance(text, str) or not text.strip() or len(text) > MAX_TEXT:
                raise NodeError('Invalid text result')
            return text
        except (KeyError, TypeError, IndexError) as error:
            raise NodeError('Invalid backend result') from error


class ComputeApplication:
    def __init__(self, token, backend, *, slots=1, hourly_limit=60, max_prompt=MAX_PROMPT, clock=time.monotonic):
        if not isinstance(token, str) or not TOKEN.fullmatch(token):
            raise ValueError('Token must have 32-256 URL-safe characters')
        if not 1 <= slots <= 4 or not 1 <= hourly_limit <= 600 or not 1000 <= max_prompt <= MAX_PROMPT:
            raise ValueError('Invalid resource limits')
        self.token, self.backend, self.slots, self.hourly_limit = token, backend, slots, hourly_limit
        self.max_prompt, self.clock = max_prompt, clock
        self.lock = threading.Lock()
        self.active, self.calls = set(), deque()
        self.receipts = OrderedDict()  # hashes/status only: no prompts or generated memories retained

    def authorized(self, authorization):
        return isinstance(authorization, str) and hmac.compare_digest(
            authorization.encode('utf-8'), ('Bearer ' + self.token).encode('utf-8'))

    def handle(self, method, path, authorization, body=b''):
        if not self.authorized(authorization):
            return 401, {'error': 'unauthorized'}
        if method == 'GET' and path == '/v1/health':
            ready = self.backend.ready()
            with self.lock:
                free = max(0, self.slots - len(self.active)) if ready else 0
            return 200, {'protocol': 1, 'ready': ready, 'model': self.backend.model,
                         'free_slots': free, 'max_prompt_chars': self.max_prompt, 'max_output_tokens': MAX_TOKENS}
        if method != 'POST' or path != '/v1/generate':
            return 404, {'error': 'unknown_endpoint'}
        try:
            task = self.validate(body)
        except (ValueError, TypeError, UnicodeError):
            return 400, {'error': 'invalid_task'}
        identity = task['id']
        digest = hashlib.sha256(body).hexdigest()
        with self.lock:
            now = self.clock()
            while self.calls and self.calls[0] <= now - 3600:
                self.calls.popleft()
            # UUIDs are deliberately never automatically retried by the phone. Duplicate submissions
            # return a receipt, not another inference, including after a failed or uncertain result.
            if identity in self.receipts:
                previous, status = self.receipts[identity]
                return 409, {'protocol': 1, 'id': identity, 'status': status if previous == digest else 'CONFLICT'}
            if len(self.active) >= self.slots:
                return 429, {'error': 'busy'}
            if len(self.calls) >= self.hourly_limit:
                return 429, {'error': 'hourly_limit'}
            self.calls.append(now)
            self.active.add(identity)
            self.receipts[identity] = (digest, 'RUNNING')
        status = 'ERROR'
        try:
            text = self.backend.generate(task)
            if not isinstance(text, str) or not text.strip() or len(text) > MAX_TEXT:
                raise NodeError('Invalid result')
            status = 'SUCCESS'
            return 200, {'protocol': 1, 'id': identity, 'status': status, 'text': text}
        except Exception:
            return 503, {'protocol': 1, 'id': identity, 'status': 'ERROR'}
        finally:
            with self.lock:
                self.active.discard(identity)
                self.receipts[identity] = (digest, status)
                # Keep a bounded 1,024-receipt window; deduplication does not survive restarts.
                while len(self.receipts) > 1024:
                    oldest = next((key for key in self.receipts if key not in self.active), None)
                    if oldest is None:
                        break
                    del self.receipts[oldest]

    def validate(self, body):
        if len(body) > MAX_BODY:
            raise ValueError()
        task = json.loads(body)
        if not isinstance(task, dict) or set(task) != {'protocol', 'id', 'role', 'prompt', 'max_tokens'}:
            raise ValueError()
        if type(task['protocol']) is not int or task['protocol'] != 1:
            raise ValueError()
        if not isinstance(task['id'], str) or str(uuid.UUID(task['id'])) != task['id']:
            raise ValueError()
        if not isinstance(task['role'], str) or task['role'] not in ROLES:
            raise ValueError()
        if not isinstance(task['prompt'], str) or not task['prompt'].strip() or len(task['prompt']) > self.max_prompt:
            raise ValueError()
        if type(task['max_tokens']) is not int or not 1 <= task['max_tokens'] <= MAX_TOKENS:
            raise ValueError()
        return task


class NodeServer(ThreadingHTTPServer):
    daemon_threads = True
    def __init__(self, address, app):
        self.app = app
        self.connections = threading.BoundedSemaphore(12)
        super().__init__(address, NodeHandler)

    def process_request(self, request, client_address):
        if not self.connections.acquire(blocking=False):
            self.shutdown_request(request)
            return
        try:
            super().process_request(request, client_address)
        except BaseException:
            self.connections.release()
            raise

    def process_request_thread(self, request, client_address):
        try:
            super().process_request_thread(request, client_address)
        finally:
            self.connections.release()


class NodeHandler(BaseHTTPRequestHandler):
    def setup(self):
        super().setup()
        self.connection.settimeout(5)

    def log_message(self, *args):
        pass  # Never log tokens, prompts, paths with user data or outputs.

    def do_GET(self):
        self.dispatch()

    def do_POST(self):
        self.dispatch()

    def dispatch(self):
        app = self.server.app
        auth = self.headers.get('Authorization', '')
        body = b''
        if not app.authorized(auth):
            return self.send_json(401, {'error': 'unauthorized'})
        if self.command == 'POST':
            try:
                if self.headers.get('Transfer-Encoding') or len(self.headers.get_all('Content-Length', [])) != 1:
                    raise ValueError()
                size = int(self.headers.get('Content-Length', ''))
                if not 0 <= size <= MAX_BODY or self.headers.get_content_type() != 'application/json':
                    raise ValueError()
                body = self.rfile.read(size)
                if len(body) != size:
                    raise ValueError()
            except (ValueError, OSError):
                return self.send_json(400, {'error': 'invalid_body'})
        status, result = app.handle(self.command, self.path, auth, body)
        self.send_json(status, result)

    def send_json(self, status, result):
        body = json.dumps(result, ensure_ascii=False).encode('utf-8')
        try:
            self.send_response(status)
            self.send_header('Content-Type', 'application/json; charset=utf-8')
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Connection', 'close')
            self.end_headers()
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass
        self.close_connection = True


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--token-file', type=Path, required=True)
    parser.add_argument('--backend', default='http://127.0.0.1:8080')
    parser.add_argument('--model', required=True)
    parser.add_argument('--port', type=int, default=8766)
    parser.add_argument('--slots', type=int, default=1)
    parser.add_argument('--hourly-limit', type=int, default=60)
    parser.add_argument('--max-prompt-chars', type=int, default=12000)
    args = parser.parse_args()
    if os.name != 'nt' and args.token_file.stat().st_mode & 0o077:
        parser.error('Token file must be private: chmod 600')
    app = ComputeApplication(args.token_file.read_text().strip(), LocalBackend(args.backend, args.model),
                             slots=args.slots, hourly_limit=args.hourly_limit, max_prompt=args.max_prompt_chars)
    server = NodeServer(('127.0.0.1', args.port), app)
    print(f'Salve compute listening on loopback port {args.port}; expose through an authenticated HTTPS endpoint.')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
