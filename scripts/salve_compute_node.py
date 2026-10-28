#!/usr/bin/env python3
"""Nodo de Salve: tareas por HTTPS con TLS mutuo (mTLS), sin VPN ni adaptadores virtuales.

Python 3.10+ en Windows, Linux o macOS. Solo acepta conexiones que presenten un certificado
de cliente firmado por la CA propia (el del móvil) y, además, el token de consulta.
Sin shell, herramientas, replicación, descargas ni geolocalización por IP.
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
import shutil
from salve_guardian import MAX_BLOB, BlindVault, VaultError
from salve_home import HomeBridge, HomeError
from salve_private_location import LocationStore, location_version, private_read, validate_location
import sys
import threading
import time
from urllib.parse import urlsplit
import uuid

MAX_BODY = 300_000
MAX_PROMPT = 48_000
MAX_TEXT = 60_000
MAX_TOKENS = 2048
ROLES = frozenset(('CONVERSACIONAL', 'REFLEXION', 'SISTEMA', 'PLANIFICADOR', 'OBSERVADOR',
                   'SINTETIZADOR', 'EVALUADOR', 'CREADOR', 'CURIOSO', 'NARRADOR', 'SALVENTE'))
TOKEN = re.compile(r'[A-Za-z0-9_-]{32,256}')


class NodeError(Exception):
    pass


class LocalBackend:
    """The operator fixes the address/model. Tasks cannot supply a backend URL or tools."""
    def __init__(self, url, model, timeout=40, cafile=None):
        parsed = urlsplit(url)
        try:
            if (parsed.scheme not in ('http', 'https') or not ipaddress.ip_address(parsed.hostname).is_loopback
                    or parsed.username or parsed.password or parsed.query or parsed.fragment
                    or parsed.path not in ('', '/') or not 1 <= (parsed.port or 8080) <= 65535):
                raise ValueError()
        except (ValueError, TypeError):
            raise ValueError('Backend must be a literal loopback HTTP(S) address') from None
        if not isinstance(model, str) or not re.fullmatch(r'[^\x00-\x1f\x7f]{1,120}', model):
            raise ValueError('Invalid fixed model name')
        # El modelo escucha en loopback: ese tráfico no sale del equipo. HTTPS es opcional
        # (llama-server de Windows no suele traer TLS); si se usa, se valida con su CA.
        self.tls = None
        if parsed.scheme == 'https':
            self.tls = ssl.create_default_context(cafile=cafile)
            self.tls.minimum_version = ssl.TLSVersion.TLSv1_2
        self.host, self.port, self.model, self.timeout = parsed.hostname, parsed.port or 8080, model, timeout

    def request(self, method, path, payload=None, timeout=None):
        connection = (http.client.HTTPSConnection(self.host, self.port, timeout=timeout or self.timeout, context=self.tls)
                      if self.tls else http.client.HTTPConnection(self.host, self.port, timeout=timeout or self.timeout))
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
            'temperature': .65 if task['role'] == 'NARRADOR' else .6 if task['role'] == 'SALVENTE' else .35,
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


class GpuTelemetry:
    """GPU, VRAM y carga desde nvidia-smi, con caché: no lanza un proceso por cada petición.
    Sin NVIDIA (o sin nvidia-smi) informa None: no se inventan datos."""

    def __init__(self, ttl=15, clock=time.monotonic, runner=None):
        self.ttl, self.clock = ttl, clock
        self.runner = runner or self._nvidia_smi
        self.cached, self.at, self.lock = None, None, threading.Lock()

    @staticmethod
    def _nvidia_smi():
        if not shutil.which('nvidia-smi'):
            return None
        try:
            return subprocess.run(['nvidia-smi', '--query-gpu=name,memory.used,memory.total,utilization.gpu',
                                   '--format=csv,noheader,nounits'], capture_output=True, text=True, timeout=3).stdout
        except (OSError, subprocess.SubprocessError):
            return None

    def read(self):
        with self.lock:
            now = self.clock()
            if self.at is not None and now - self.at < self.ttl:
                return self.cached
            self.at, self.cached = now, self._parse(self.runner())
            return self.cached

    @staticmethod
    def _parse(output):
        for line in (output or '').splitlines():
            parts = [p.strip() for p in line.split(',')]
            if len(parts) != 4 or not parts[0]:
                continue
            try:
                used, total, load = float(parts[1]) / 1024, float(parts[2]) / 1024, int(float(parts[3]))
            except ValueError:
                continue
            if 0 < total < 1024 and 0 <= used <= total and 0 <= load <= 100:
                return {'name': parts[0][:80], 'vram_used_gb': round(used, 2), 'vram_total_gb': round(total, 2), 'load_pct': load}
        return None


SATELLITE_KINDS = frozenset(('compute', 'screen', 'sensors'))
SAT_ID = re.compile(r'[A-Za-z0-9_-]{3,64}')
SAT_MAX_BODY = 300_000


class SatelliteBroker:
    """Punto de encuentro entre la Salve del dueño y sus satélites (móviles que la ayudan).

    El nodo es un servidor con dirección fija al que llegan los dos, estén donde estén. El dueño
    encola trabajo (enqueue); cada satélite, al latir, recoge uno que ofrezca y devuelve el resultado.
    Todo en memoria y acotado: no guarda prompts ni resultados más allá de lo imprescindible.
    """

    def __init__(self, clock=time.monotonic, max_satellites=32, max_queue=64, max_results=128, ttl=180):
        self.clock, self.ttl = clock, ttl
        self.max_satellites, self.max_queue, self.max_results = max_satellites, max_queue, max_results
        self.lock = threading.Lock()
        self.satellites = OrderedDict()   # id -> {'name', 'caps', 'seen'}
        self.queue = deque()              # trabajos pendientes: {'id','kind','params'}
        self.results = OrderedDict()      # task_id -> output (lo que devolvió el satélite)

    @staticmethod
    def _caps(raw):
        if not isinstance(raw, list):
            return set()
        return {c for c in raw if isinstance(c, str) and c in SATELLITE_KINDS}

    def register(self, body):
        payload = _load(body, SAT_MAX_BODY)
        if not isinstance(payload, dict) or payload.get('v') != 1:
            raise ValueError()
        sid = payload.get('id')
        if not isinstance(sid, str) or not SAT_ID.fullmatch(sid):
            raise ValueError()
        name = payload.get('name') if isinstance(payload.get('name'), str) else ''
        with self.lock:
            self.satellites[sid] = {'name': name[:60], 'caps': self._caps(payload.get('capabilities')), 'seen': self.clock()}
            self.satellites.move_to_end(sid)
            while len(self.satellites) > self.max_satellites:
                self.satellites.popitem(last=False)
        return 200, {'protocol': 1, 'registered': True, 'id': sid}

    def heartbeat(self, body):
        payload = _load(body, SAT_MAX_BODY)
        if not isinstance(payload, dict) or payload.get('v') != 1:
            raise ValueError()
        sid = payload.get('id')
        if not isinstance(sid, str) or not SAT_ID.fullmatch(sid):
            raise ValueError()
        available = bool(payload.get('available'))
        caps = self._caps(payload.get('capabilities'))
        with self.lock:
            known = self.satellites.get(sid)
            if known is None:
                self.satellites[sid] = known = {'name': '', 'caps': caps, 'seen': 0}
            known['caps'], known['seen'] = caps, self.clock()
            if not available:
                return 200, {'protocol': 1}
            for index, job in enumerate(self.queue):
                if job['kind'] in caps:
                    del self.queue[index]
                    return 200, {'protocol': 1, 'task': job}
        return 200, {'protocol': 1}

    def enqueue(self, body):
        """El dueño reparte un trabajo. Devuelve su id para recoger luego el resultado."""
        payload = _load(body, SAT_MAX_BODY)
        if not isinstance(payload, dict) or set(payload) - {'kind', 'params'} or 'kind' not in payload:
            raise ValueError()
        kind = payload.get('kind')
        if kind not in SATELLITE_KINDS:
            raise ValueError()
        params = payload.get('params', {})
        if not isinstance(params, dict):
            raise ValueError()
        task_id = uuid.uuid4().hex
        with self.lock:
            if len(self.queue) >= self.max_queue:
                return 429, {'error': 'queue_full'}
            if not any(kind in s['caps'] and self.clock() - s['seen'] <= self.ttl for s in self.satellites.values()):
                return 503, {'error': 'no_satellite_for', 'kind': kind}
            self.queue.append({'id': task_id, 'kind': kind, 'params': params})
        return 200, {'protocol': 1, 'id': task_id}

    def result(self, body):
        payload = _load(body, SAT_MAX_BODY)
        if not isinstance(payload, dict) or payload.get('v') != 1:
            raise ValueError()
        task_id, output = payload.get('task'), payload.get('output')
        if not isinstance(task_id, str) or not output or not isinstance(output, dict):
            raise ValueError()
        with self.lock:
            self.results[task_id] = output
            self.results.move_to_end(task_id)
            while len(self.results) > self.max_results:
                self.results.popitem(last=False)
        return 200, {'protocol': 1, 'stored': True}

    def result_of(self, task_id):
        with self.lock:
            output = self.results.get(task_id)
        if output is None:
            return 404, {'protocol': 1, 'ready': False}
        return 200, {'protocol': 1, 'ready': True, 'output': output}

    def listing(self):
        now = self.clock()
        with self.lock:
            connected = [{'id': sid, 'name': s['name'], 'capabilities': sorted(s['caps']),
                          'online': now - s['seen'] <= self.ttl}
                         for sid, s in self.satellites.items()]
        return 200, {'protocol': 1, 'satellites': connected}


def _load(body, maximum):
    if len(body) > maximum:
        raise ValueError()
    return json.loads(body)


class ComputeApplication:
    def __init__(self, token, backend, *, slots=1, hourly_limit=60, max_prompt=MAX_PROMPT, clock=time.monotonic, location_store=None, location_token=None,
                 vault=None, gpu=None, home=None):
        if not isinstance(token, str) or not TOKEN.fullmatch(token):
            raise ValueError('Token must have 32-256 URL-safe characters')
        if not 1 <= slots <= 4 or not 1 <= hourly_limit <= 600 or not 1000 <= max_prompt <= MAX_PROMPT:
            raise ValueError('Invalid resource limits')
        if location_token is not None and (not TOKEN.fullmatch(location_token) or location_token == token):
            raise ValueError('Location writer token must be valid and different from the read token')
        self.location_store, self.location_token = location_store, location_token
        self.vault = vault  # Guardián ciego: solo copias selladas que este PC no puede leer
        self.gpu = gpu if gpu is not None else GpuTelemetry()
        self.home = home  # pasarela local con Home Assistant (lista de entidades permitidas)
        self.token, self.backend, self.slots, self.hourly_limit = token, backend, slots, hourly_limit
        self.max_prompt, self.clock = max_prompt, clock
        self.lock = threading.Lock()
        self.active, self.calls = set(), deque()
        self.receipts = OrderedDict()  # hashes/status only: no prompts or generated memories retained
        self.satellites = SatelliteBroker(clock=clock)  # móviles que ayudan a la Salve del dueño

    def authorized(self, authorization, path='/v1/health'):
        token = self.location_token if path == '/v1/location' else self.token
        return token is not None and isinstance(authorization, str) and hmac.compare_digest(
            authorization.encode('utf-8'), ('Bearer ' + token).encode('utf-8'))

    @property
    def roles(self):
        return ['inferencia'] + (['guardian'] if self.vault is not None else []) + (['hogar'] if self.home is not None else [])

    def handle_vault(self, method, path, body):
        if self.vault is None:
            return 404, {'error': 'guardian_not_enabled'}
        if method == 'GET' and path == '/v1/vault':
            return 200, {'protocol': 1, 'guardian': True, 'blobs': self.vault.list(), 'usage': self.vault.usage()}
        name = path[len('/v1/vault/'):]
        try:
            if method == 'GET':
                data = self.vault.get(name)
                return (200, data) if data is not None else (404, {'error': 'not_found'})
            if method == 'POST':
                return 200, {'protocol': 1, 'saved': True, **self.vault.put(name, body)}
            if method == 'DELETE':
                return 200, {'protocol': 1, 'deleted': self.vault.delete(name)}
        except VaultError as refused:
            return (413 if refused.code in ('too_large', 'quota_exceeded', 'too_many') else 400), {'error': refused.code}
        except OSError:
            return 503, {'error': 'guardian_unavailable'}
        return 405, {'error': 'method_not_allowed'}

    HOME_STATUS = {'entity_not_allowed': 403, 'service_not_allowed': 403, 'invalid_value': 400, 'too_many_actions': 429,
                   'home_entity_unknown': 404, 'home_token_rejected': 502, 'home_failed': 502, 'home_unreachable': 504}

    def handle_home(self, method, path, body):
        if self.home is None:
            return 404, {'error': 'home_not_enabled'}
        try:
            if method == 'GET' and path == '/v1/home':
                return 200, {'protocol': 1, 'entities': self.home.states()}
            if method == 'POST' and path == '/v1/home/action':
                try:
                    payload = json.loads(body)
                    if not isinstance(payload, dict) or not {'entity', 'service'} <= set(payload) <= {'entity', 'service', 'value'}:
                        raise ValueError()
                except (ValueError, TypeError, UnicodeError):
                    return 400, {'error': 'invalid_action'}
                return 200, {'protocol': 1, 'done': True,
                             'entity': self.home.act(payload['entity'], payload['service'], payload.get('value'))}
        except HomeError as refused:
            return self.HOME_STATUS.get(refused.code, 502), {'error': refused.code}
        return 404, {'error': 'unknown_endpoint'}

    def handle_satellite(self, method, path, body):
        """Las rutas de los satélites y las del dueño para repartirles trabajo. Mismo token del nodo."""
        try:
            if method == 'POST' and path == '/v1/satellite/register':
                return self.satellites.register(body)
            if method == 'POST' and path == '/v1/satellite/heartbeat':
                return self.satellites.heartbeat(body)
            if method == 'POST' and path == '/v1/satellite/result':
                return self.satellites.result(body)
            if method == 'POST' and path == '/v1/satellite/enqueue':
                return self.satellites.enqueue(body)
            if method == 'GET' and path == '/v1/satellite/list':
                return self.satellites.listing()
            if method == 'GET' and path.startswith('/v1/satellite/result/'):
                return self.satellites.result_of(path[len('/v1/satellite/result/'):])
        except (ValueError, TypeError, UnicodeError):
            return 400, {'error': 'invalid_satellite_request'}
        return 404, {'error': 'unknown_endpoint'}

    def handle(self, method, path, authorization, body=b'', known_location=None):
        if not self.authorized(authorization, path):
            return 401, {'error': 'unauthorized'}
        if path == '/v1/home' or path.startswith('/v1/home/'):
            return self.handle_home(method, path, body)
        if path == '/v1/vault' or path.startswith('/v1/vault/'):
            return self.handle_vault(method, path, body)
        if path == '/v1/satellite' or path.startswith('/v1/satellite/'):
            return self.handle_satellite(method, path, body)
        if method == 'GET' and path == '/v1/family/status':
            try:
                location = self.location_store.read() if self.location_store else None
            except Exception:
                return 503, {'error': 'location_unavailable'}
            version = location_version(location)
            # El móvil manda la versión que ya tiene: si no ha cambiado, no viajan coordenadas.
            if known_location is not None and hmac.compare_digest(str(known_location), version):
                return 200, {'protocol': 1, 'available': True, 'unchanged': True, 'location_version': version}
            return 200, {'protocol': 1, 'available': True, 'location': location, 'location_version': version,
                         'roles': self.roles}
        if method == 'POST' and path == '/v1/location':
            if self.location_store is None:
                return 409, {'error': 'location_not_enabled_by_owner'}
            if not getattr(self.location_store, 'writable', True):
                return 409, {'error': 'location_fixed_by_node'}
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
                         'free_slots': free, 'max_prompt_chars': self.max_prompt, 'max_output_tokens': MAX_TOKENS,
                         'roles': self.roles, 'gpu': self.gpu.read()}
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
    def __init__(self, address, app, *, tls, connection_guard=None):
        if not isinstance(tls, ssl.SSLContext) or (connection_guard is not None and not callable(connection_guard)):
            raise ValueError('TLS is required')
        self.tls, self.connection_guard = tls, connection_guard or (lambda ip: True)
        self.app = app
        self.connections = threading.BoundedSemaphore(12)
        super().__init__(address, NodeHandler)

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

    def do_DELETE(self):
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
                # Solo las copias del Guardián son binarias y grandes; todo lo demás sigue siendo JSON acotado.
                blob = self.path.startswith('/v1/vault/')
                limit, kind = (MAX_BLOB, 'application/octet-stream') if blob else (MAX_BODY, 'application/json')
                if not 0 <= size <= limit or self.headers.get_content_type() != kind:
                    # Se lee y descarta lo enviado antes de responder: cerrar con datos sin leer provoca un
                    # reset (sobre todo en Windows) y el móvil no llegaría a ver el motivo del rechazo.
                    if 0 <= size <= MAX_BLOB:
                        remaining = size
                        while remaining > 0:
                            chunk = self.rfile.read(min(65536, remaining))
                            if not chunk:
                                break
                            remaining -= len(chunk)
                    raise ValueError()
                body = self.rfile.read(size)
                if len(body) != size:
                    raise ValueError()
            except (ValueError, OSError):
                return self.send_json(400, {'error': 'invalid_body'})
        status, result = app.handle(self.command, self.path, auth, body,
                                    known_location=self.headers.get('X-Salve-Location-Version'))
        if isinstance(result, (bytes, bytearray)):
            return self.send_bytes(status, bytes(result))
        self.send_json(status, result)

    def send_bytes(self, status, body):
        try:
            self.send_response(status)
            self.send_header('Content-Type', 'application/octet-stream')
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store')
            self.send_header('Connection', 'close')
            self.end_headers()
            self.wfile.write(body)
        except (BrokenPipeError, ConnectionResetError):
            pass
        self.close_connection = True

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


def server_tls(cert, key, client_ca):
    """TLS mutuo: el nodo presenta su certificado y exige uno de cliente firmado por client_ca."""
    tls = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER)
    tls.minimum_version = ssl.TLSVersion.TLSv1_2
    private_read(key)  # la clave debe ser un archivo privado normal
    tls.load_cert_chain(cert, key)
    tls.load_verify_locations(cafile=str(client_ca))
    tls.verify_mode = ssl.CERT_REQUIRED
    return tls


class StaticLocation:
    """Ubicación fijada por parámetros al arrancar el nodo. No se deduce de la IP ni se puede
    cambiar desde la red: se muestra tal cual en la ficha del nodo en Salve."""
    writable = False

    def __init__(self, latitude, longitude, city='', country='', zone='', now=None):
        parts = [part.strip() for part in (zone, country) if part and part.strip()]
        self.value = validate_location({
            'latitude': float(latitude), 'longitude': float(longitude), 'city': (city or '').strip(),
            'zone': ', '.join(parts), 'source': 'static', 'captured_at': int(now if now is not None else time.time()),
            'accuracy_m': 0}, now)

    def read(self):
        return dict(self.value)

    def write(self, value):
        raise PermissionError('La ubicación de este nodo es fija')


class IpAllowList:
    """Filtro opcional por IP de origen, además del certificado de cliente y el token."""
    def __init__(self, entries):
        self.networks = [ipaddress.ip_network(entry, strict=False) for entry in entries]

    def __call__(self, client):
        try:
            address = ipaddress.ip_address(client)
        except ValueError:
            return False
        return any(address in network for network in self.networks)


def main(argv=None, ready=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--token-file', type=Path, required=True)
    parser.add_argument('--backend', default='http://127.0.0.1:8080',
                        help='Modelo local en loopback; https://127.0.0.1:8080 si llama-server tiene TLS')
    parser.add_argument('--backend-ca', type=Path, help='CA del backend, solo si --backend usa https')
    parser.add_argument('--bind', default='0.0.0.0', help='Dirección donde escuchar (por defecto, todas)')
    parser.add_argument('--port', type=int, default=8443)
    parser.add_argument('--tls-cert', type=Path, required=True, help='Certificado del nodo (nodo.pem)')
    parser.add_argument('--tls-key', type=Path, required=True, help='Clave del nodo (nodo.key)')
    parser.add_argument('--client-ca', type=Path, required=True, help='CA que firma el certificado del móvil (ca.pem)')
    parser.add_argument('--allow-ip', action='append', default=[], help='Opcional: IP o red de origen permitida; repetible')
    parser.add_argument('--location-directory', type=Path, help='Ubicación que el dueño publica desde Salve (cifrada)')
    parser.add_argument('--location-token-file', type=Path)
    parser.add_argument('--lat', type=float, help='Ubicación fija: latitud')
    parser.add_argument('--lon', type=float, help='Ubicación fija: longitud')
    parser.add_argument('--ciudad', default='', help='Ubicación fija: ciudad')
    parser.add_argument('--pais', default='', help='Ubicación fija: país')
    parser.add_argument('--zona', default='', help='Ubicación fija: zona o barrio')
    parser.add_argument('--vault-directory', type=Path,
                        help='Activa el Guardián ciego: guarda copias selladas del móvil que este PC no puede leer')
    parser.add_argument('--ha-url', help='Home Assistant en tu red local, p. ej. http://192.168.1.10:8123')
    parser.add_argument('--ha-token-file', type=Path, help='Archivo privado con el token de larga duración de Home Assistant')
    parser.add_argument('--ha-entities', default='', help='Entidades permitidas, separadas por comas (light.salon,switch.enchufe)')
    parser.add_argument('--model', required=True)
    parser.add_argument('--slots', type=int, default=1)
    parser.add_argument('--hourly-limit', type=int, default=60)
    parser.add_argument('--max-prompt-chars', type=int, default=12000)
    args = parser.parse_args(argv)
    token = private_read(args.token_file, 300).decode('ascii').strip()
    if (args.lat is None) != (args.lon is None):
        parser.error('La ubicación fija necesita --lat y --lon')
    writer = None
    if args.lat is not None:
        location = StaticLocation(args.lat, args.lon, args.ciudad, args.pais, args.zona)
    else:
        writer = private_read(args.location_token_file, 300).decode('ascii').strip() if args.location_token_file else None
        if writer and not args.location_directory:
            parser.error('Location writer requires --location-directory')
        location = LocationStore(args.location_directory) if args.location_directory else None
    home = None
    if args.ha_url or args.ha_token_file:
        if not (args.ha_url and args.ha_token_file and args.ha_entities):
            parser.error('Home Assistant necesita --ha-url, --ha-token-file y --ha-entities')
        home = HomeBridge(args.ha_url, private_read(args.ha_token_file, 2048).decode('utf-8').strip(), args.ha_entities.split(','))
    tls = server_tls(args.tls_cert, args.tls_key, args.client_ca)
    app = ComputeApplication(token, LocalBackend(args.backend, args.model, cafile=args.backend_ca),
                             slots=args.slots, hourly_limit=args.hourly_limit, max_prompt=args.max_prompt_chars,
                             location_store=location, location_token=writer,
                             vault=BlindVault(args.vault_directory) if args.vault_directory else None, home=home)
    guard = IpAllowList(args.allow_ip) if args.allow_ip else None
    server = NodeServer((args.bind, args.port), app, tls=tls, connection_guard=guard)
    print(f'Nodo de Salve escuchando en {args.bind}:{args.port} con TLS mutuo (solo el móvil con su certificado).'
          + (' Guardián ciego activo.' if args.vault_directory else ''))
    if ready:
        ready(server)
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
