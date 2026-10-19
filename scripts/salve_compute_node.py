#!/usr/bin/env python3
"""Salve private worker: authenticated HTTPS tasks over an existing WireGuard interface.

Python 3.10+ on Linux. TLS is required, including the fixed loopback model backend.
No shell, tools, replication, provisioning, geolocation APIs or downloads.
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
import ssl
import subprocess
from salve_private_location import LocationStore, private_read
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
    def __init__(self, url, model, timeout=40, cafile=None):
        parsed = urlsplit(url)
        try:
            if (parsed.scheme != 'https' or not ipaddress.ip_address(parsed.hostname).is_loopback
                    or parsed.username or parsed.password or parsed.query or parsed.fragment
                    or parsed.path not in ('', '/') or not 1 <= (parsed.port or 8080) <= 65535):
                raise ValueError()
        except (ValueError, TypeError):
            raise ValueError('Backend must be a literal loopback HTTPS address') from None
        if not isinstance(model, str) or not re.fullmatch(r'[^\x00-\x1f\x7f]{1,120}', model):
            raise ValueError('Invalid fixed model name')
        self.tls = ssl.create_default_context(cafile=cafile)
        self.tls.minimum_version = ssl.TLSVersion.TLSv1_2
        self.host, self.port, self.model, self.timeout = parsed.hostname, parsed.port or 8080, model, timeout

    def request(self, method, path, payload=None, timeout=None):
        connection = http.client.HTTPSConnection(self.host, self.port, timeout=timeout or self.timeout, context=self.tls)
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
    def __init__(self, token, backend, *, slots=1, hourly_limit=60, max_prompt=MAX_PROMPT, clock=time.monotonic, location_store=None, location_token=None):
        if not isinstance(token, str) or not TOKEN.fullmatch(token):
            raise ValueError('Token must have 32-256 URL-safe characters')
        if not 1 <= slots <= 4 or not 1 <= hourly_limit <= 600 or not 1000 <= max_prompt <= MAX_PROMPT:
            raise ValueError('Invalid resource limits')
        if location_token is not None and (not TOKEN.fullmatch(location_token) or location_token == token):
            raise ValueError('Location writer token must be valid and different from the read token')
        self.location_store, self.location_token = location_store, location_token
        self.token, self.backend, self.slots, self.hourly_limit = token, backend, slots, hourly_limit
        self.max_prompt, self.clock = max_prompt, clock
        self.lock = threading.Lock()
        self.active, self.calls = set(), deque()
        self.receipts = OrderedDict()  # hashes/status only: no prompts or generated memories retained

    def authorized(self, authorization, path='/v1/health'):
        token = self.location_token if path == '/v1/location' else self.token
        return token is not None and isinstance(authorization, str) and hmac.compare_digest(
            authorization.encode('utf-8'), ('Bearer ' + token).encode('utf-8'))

    def handle(self, method, path, authorization, body=b''):
        if not self.authorized(authorization, path):
            return 401, {'error': 'unauthorized'}
        if method == 'GET' and path == '/v1/family/status':
            try:
                location = self.location_store.read() if self.location_store else None
            except Exception:
                return 503, {'error': 'location_unavailable'}
            return 200, {'protocol': 1, 'available': True, 'location': location}
        if method == 'POST' and path == '/v1/location':
            if self.location_store is None:
                return 409, {'error': 'location_not_enabled_by_owner'}
            try:
                if len(body) > 4096:
                    raise ValueError()
                payload = json.loads(body)
                if not isinstance(payload, dict) or set(payload) != {'location'}:
                    raise ValueError()
                self.location_store.write(payload['location'])
                return 200, {'protocol': 1, 'saved': True}
            except (ValueError, TypeError, UnicodeError):
                return 400, {'error': 'invalid_location'}
            except Exception:
                return 503, {'error': 'location_unavailable'}
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
    def __init__(self, address, app, *, tls, connection_guard, interface=None):
        if not isinstance(tls, ssl.SSLContext) or not callable(connection_guard):
            raise ValueError('TLS and explicit connection guard are required')
        self.tls, self.connection_guard, self.interface = tls, connection_guard, interface
        self.app = app
        self.connections = threading.BoundedSemaphore(12)
        super().__init__(address, NodeHandler)

    def server_bind(self):
        if self.interface:
            # Kernel-enforced ingress restriction; fail closed if CAP_NET_RAW is missing.
            self.socket.setsockopt(socket.SOL_SOCKET, socket.SO_BINDTODEVICE, self.interface.encode() + b'\0')
        super().server_bind()

    def handle_error(self, request, client_address):
        pass  # No client addresses, tokens or coordinates in server logs.

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
        wrapped = None
        try:
            request.settimeout(5)
            if not self.connection_guard(client_address[0]):
                return
            wrapped = self.tls.wrap_socket(request, server_side=True)
            super().process_request_thread(wrapped, client_address)
        except (OSError, ValueError):
            pass
        finally:
            if wrapped:
                wrapped.close()
            request.close()
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
        if not app.authorized(auth, self.path):
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


def private_ipv4(raw):
    try:
        address = ipaddress.IPv4Address(raw)
        return any(address in ipaddress.ip_network(net) for net in ('10.0.0.0/8', '172.16.0.0/12', '192.168.0.0/16'))
    except (ValueError, TypeError):
        return False


class WireGuardIngress:
    """Explicit client list plus kernel interface binding; no location inferred from addresses."""
    def __init__(self, interface, bind, clients):
        if not re.fullmatch(r'[A-Za-z0-9_-]{1,15}', interface) or not private_ipv4(bind):
            raise ValueError('A WireGuard interface and private IPv4 bind address are required')
        if not 1 <= len(clients) <= 16 or any(not private_ipv4(client) or client == bind for client in clients):
            raise ValueError('Specify private VPN IPv4 addresses of authorized clients')
        self.interface, self.bind, self.clients = interface, bind, set(clients)
        if not self.interface_valid():
            raise ValueError('The bind address must belong to an existing Linux WireGuard interface')

    def interface_valid(self):
        try:
            command = subprocess.run(['ip', '-j', '-d', 'address', 'show', 'dev', self.interface], capture_output=True, timeout=2, check=True)
            links = json.loads(command.stdout)
            return any(link.get('linkinfo', {}).get('info_kind') == 'wireguard'
                       and any(item.get('local') == self.bind for item in link.get('addr_info', [])) for link in links)
        except (OSError, ValueError, subprocess.SubprocessError):
            return False

    def __call__(self, client):
        return client in self.clients and self.interface_valid()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--token-file', type=Path, required=True)
    parser.add_argument('--backend', default='https://127.0.0.1:8080')
    parser.add_argument('--backend-ca', type=Path, required=True)
    parser.add_argument('--bind', required=True, help='Private IPv4 assigned to the WireGuard interface')
    parser.add_argument('--interface', required=True, help='Existing Linux WireGuard interface, e.g. wg0')
    parser.add_argument('--allow-client', action='append', required=True, help='Authorized phone VPN IPv4; repeat per phone')
    parser.add_argument('--tls-cert', type=Path, required=True)
    parser.add_argument('--tls-key', type=Path, required=True)
    parser.add_argument('--location-directory', type=Path)
    parser.add_argument('--location-token-file', type=Path)
    parser.add_argument('--model', required=True)
    parser.add_argument('--port', type=int, default=8766)
    parser.add_argument('--slots', type=int, default=1)
    parser.add_argument('--hourly-limit', type=int, default=60)
    parser.add_argument('--max-prompt-chars', type=int, default=12000)
    args = parser.parse_args()
    guard = WireGuardIngress(args.interface, args.bind, args.allow_client)
    token = private_read(args.token_file, 300).decode('ascii').strip()
    writer = private_read(args.location_token_file, 300).decode('ascii').strip() if args.location_token_file else None
    if writer and not args.location_directory:
        parser.error('Location writer requires --location-directory')
    location = LocationStore(args.location_directory) if args.location_directory else None
    tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER); tls.minimum_version = ssl.TLSVersion.TLSv1_2
    private_read(args.tls_key)  # Require a private regular file before loading it.
    tls.load_cert_chain(args.tls_cert, args.tls_key)
    app = ComputeApplication(token, LocalBackend(args.backend, args.model, cafile=args.backend_ca),
                             slots=args.slots, hourly_limit=args.hourly_limit, max_prompt=args.max_prompt_chars,
                             location_store=location, location_token=writer)
    server = NodeServer((args.bind, args.port), app, tls=tls, connection_guard=guard, interface=args.interface)
    print('Salve private node ready: HTTPS restricted to its WireGuard interface.')
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
