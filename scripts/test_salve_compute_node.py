import http.client
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import threading
import unittest
from unittest.mock import Mock
import uuid
import ssl
import tempfile
from pathlib import Path
from test_private_family import certificates

import salve_compute_node as node

TOKEN = 'n' * 40


def task(**changes):
    result = {'protocol': 1, 'id': str(uuid.uuid4()), 'role': 'NARRADOR',
              'prompt': 'Escribe un cuento en español.', 'max_tokens': 100}
    result.update(changes)
    return result


class NodeTests(unittest.TestCase):
    def setUp(self):
        self.backend = Mock(model='test-model')
        self.backend.ready.return_value = True
        self.backend.generate.return_value = 'Había una vez…'
        self.app = node.ComputeApplication(TOKEN, self.backend)

    def send(self, payload=None, token=TOKEN, method='POST', path='/v1/generate', raw=None):
        return self.app.handle(method, path, 'Bearer ' + token,
                               raw if raw is not None else json.dumps(payload or task()).encode())

    def test_auth_precedes_health_and_inference(self):
        for token in ('', 'x', 'é' * 40):
            self.assertEqual(401, self.send(token=token)[0])
            self.assertEqual(401, self.send(token=token, method='GET', path='/v1/health')[0])
        self.backend.ready.assert_not_called()
        self.backend.generate.assert_not_called()

    def test_health_is_actual_backend_readiness(self):
        status, report = self.send(method='GET', path='/v1/health')
        self.assertEqual(200, status)
        self.assertEqual((True, 1), (report['ready'], report['free_slots']))
        self.backend.ready.return_value = False
        report = self.send(method='GET', path='/v1/health')[1]
        self.assertEqual((False, 0), (report['ready'], report['free_slots']))

    def test_generation_and_duplicate_does_not_run_twice(self):
        payload = task()
        status, reply = self.send(payload)
        self.assertEqual((200, 'SUCCESS', payload['id']), (status, reply['status'], reply['id']))
        self.assertEqual(409, self.send(payload)[0])
        self.assertEqual(1, self.backend.generate.call_count)
        payload['prompt'] = 'another task'
        self.assertEqual('CONFLICT', self.send(payload)[1]['status'])
        self.assertEqual(1, self.backend.generate.call_count)

    def test_reject_unknown_fields_commands_and_invalid_types(self):
        payloads = [task(role='shell'), task(role=[]), task(id='bad'), task(protocol=True), task(max_tokens=True),
                    task(max_tokens=2049), task(prompt=' '), task(prompt='a' * (node.MAX_PROMPT + 1)),
                    task(prompt=[]), task(endpoint='https://other.example'), task(code='print(1)'), []]
        for payload in payloads:
            with self.subTest(payload=str(payload)[:90]):
                self.assertEqual(400, self.send(raw=json.dumps(payload).encode())[0])
        self.assertEqual(400, self.send(raw=b'\xff')[0])
        self.assertEqual(400, self.send(raw=b' ' * (node.MAX_BODY + 1))[0])
        self.backend.generate.assert_not_called()

    def test_operator_prompt_limit(self):
        self.app = node.ComputeApplication(TOKEN, self.backend, max_prompt=1000)
        self.assertEqual(400, self.send(task(prompt='a' * 1001))[0])
        self.backend.generate.assert_not_called()

    def test_failure_releases_slot_and_hides_exception(self):
        self.backend.generate.side_effect = RuntimeError('PRIVATE TOKEN')
        payload = task()
        status, reply = self.send(payload)
        self.assertEqual((503, 'ERROR'), (status, reply['status']))
        self.assertNotIn('PRIVATE', json.dumps(reply))
        self.assertEqual(409, self.send(payload)[0])
        self.assertFalse(self.app.active)
        self.backend.generate.side_effect = None
        self.assertEqual(200, self.send()[0])

    def test_blank_and_oversized_results_fail(self):
        for value in ('', ' ', None, 'a' * (node.MAX_TEXT + 1)):
            self.backend.generate.return_value = value
            self.assertEqual(503, self.send()[0])

    def test_busy_and_duplicate_while_running(self):
        entered, release = threading.Event(), threading.Event()
        def generate(_):
            entered.set()
            release.wait(3)
            return 'Listo'
        self.backend.generate.side_effect = generate
        payload = task()
        thread = threading.Thread(target=lambda: self.send(payload))
        thread.start()
        try:
            self.assertTrue(entered.wait(1))
            self.assertEqual(0, self.send(method='GET', path='/v1/health')[1]['free_slots'])
            self.assertEqual(429, self.send()[0])
            self.assertEqual(409, self.send(payload)[0])
            self.assertEqual(1, self.backend.generate.call_count)
        finally:
            release.set()
            thread.join(2)
        self.assertFalse(self.app.active)

    def test_hourly_budget_expires(self):
        clock = [0]
        self.app = node.ComputeApplication(TOKEN, self.backend, hourly_limit=1, clock=lambda: clock[0])
        self.assertEqual(200, self.send()[0])
        self.assertEqual(429, self.send()[0])
        clock[0] = 3601
        self.assertEqual(200, self.send()[0])

    def test_receipts_store_no_prompts_or_outputs_and_are_bounded(self):
        clock = [0]
        self.app = node.ComputeApplication(TOKEN, self.backend, clock=lambda: clock[0])
        for i in range(1030):
            clock[0] += 3601
            self.assertEqual(200, self.send()[0])
        self.assertEqual(1024, len(self.app.receipts))
        self.assertNotIn('cuento', repr(self.app.receipts))
        self.assertNotIn('Había', repr(self.app.receipts))

    def test_backend_only_accepts_literal_loopback_and_fixed_model(self):
        for url in ('https://example.org', 'http://169.254.169.254', 'http://localhost:8080', 'ftp://127.0.0.1',
                    'http://127.0.0.1@evil.example', 'http://127.0.0.1:8080/x', 'http://127.0.0.1?x=1'):
            with self.assertRaises(ValueError):
                node.LocalBackend(url, 'model')
        self.assertEqual('::1', node.LocalBackend('https://[::1]:8080', 'model').host)
        # Loopback no sale del equipo: HTTP permitido (llama-server de Windows no trae TLS); HTTPS se valida.
        self.assertIsNone(node.LocalBackend('http://127.0.0.1:8080', 'model').tls)
        self.assertIsNotNone(node.LocalBackend('https://127.0.0.1:8080', 'model').tls)


class WireTests(unittest.TestCase):
    def start(self, server):
        thread = threading.Thread(target=server.serve_forever, daemon=True)
        thread.start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        return server.server_address[1]

    def test_real_https_roundtrip_to_fixed_backend_and_errors(self):
        requests = []
        finish = ['stop']
        class Backend(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass
            def do_GET(self):
                self.answer({'status': 'ok'})
            def do_POST(self):
                requests.append(json.loads(self.rfile.read(int(self.headers['Content-Length']))))
                self.answer({'choices': [{'finish_reason': finish[0], 'message': {'content': 'Un cuento real en el cable.'}}]})
            def answer(self, data):
                body = json.dumps(data).encode()
                self.send_response(200)
                self.send_header('Content-Length', str(len(body)))
                self.end_headers()
                self.wfile.write(body)
        folder = tempfile.TemporaryDirectory(); self.addCleanup(folder.cleanup)
        tls, client_tls = certificates(folder.name)
        backend_server = ThreadingHTTPServer(('127.0.0.1', 0), Backend)
        backend_server.socket = tls.wrap_socket(backend_server.socket, server_side=True)
        backend_port = self.start(backend_server)
        backend = node.LocalBackend(f'https://127.0.0.1:{backend_port}', 'fixed-model', cafile=Path(folder.name) / 'ca.pem')
        port = self.start(node.NodeServer(('127.0.0.1', 0), node.ComputeApplication(TOKEN, backend), tls=tls, connection_guard=lambda ip: ip == '127.0.0.1'))
        def request(path, payload=None, token=TOKEN):
            connection = http.client.HTTPSConnection('127.0.0.1', port, timeout=3, context=client_tls)
            try:
                connection.request('GET' if payload is None else 'POST', path,
                                   None if payload is None else json.dumps(payload),
                                   {'Authorization': 'Bearer ' + token, 'Content-Type': 'application/json'})
                response = connection.getresponse()
                return response.status, json.loads(response.read())
            finally:
                connection.close()
        self.assertEqual(401, request('/v1/health', token='bad')[0])
        self.assertTrue(request('/v1/health')[1]['ready'])
        payload = task()
        self.assertEqual('SUCCESS', request('/v1/generate', payload)[1]['status'])
        self.assertEqual('fixed-model', requests[0]['model'])
        self.assertEqual(payload['prompt'], requests[0]['messages'][0]['content'])
        self.assertNotIn('tools', requests[0])
        self.assertEqual(409, request('/v1/generate', payload)[0])
        finish[0] = 'length'
        self.assertEqual(503, request('/v1/generate', task())[0])
        self.assertEqual(404, request('/v1/shell', task())[0])


if __name__ == '__main__':
    unittest.main()
