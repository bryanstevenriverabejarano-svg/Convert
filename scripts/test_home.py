from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import threading
import unittest
from unittest.mock import Mock

import salve_compute_node as node
from salve_home import HomeBridge, HomeError, check_url, parse_entities

HA_TOKEN = 'h' * 40
TOKEN = 'n' * 40


class FakeHomeAssistant:
    """Home Assistant mínimo: estados y servicios, con su token."""

    def __init__(self):
        self.states = {'light.salon': {'state': 'off', 'attributes': {'friendly_name': 'Luz salón', 'brightness': 0}},
                       'climate.casa': {'state': 'heat', 'attributes': {'friendly_name': 'Calefacción', 'temperature': 20,
                                                                        'secret_attr': 'no debe viajar'}}}
        self.calls = []
        owner = self

        class Handler(BaseHTTPRequestHandler):
            def log_message(self, *args):
                pass

            def reply(self, status, body):
                data = json.dumps(body).encode()
                self.send_response(status); self.send_header('Content-Type', 'application/json')
                self.send_header('Content-Length', str(len(data))); self.end_headers(); self.wfile.write(data)

            def do_GET(self):
                if self.headers.get('Authorization') != 'Bearer ' + HA_TOKEN:
                    return self.reply(401, {})
                entity = self.path.rsplit('/', 1)[-1]
                if entity not in owner.states:
                    return self.reply(404, {})
                self.reply(200, owner.states[entity])

            def do_POST(self):
                if self.headers.get('Authorization') != 'Bearer ' + HA_TOKEN:
                    return self.reply(401, {})
                body = json.loads(self.rfile.read(int(self.headers['Content-Length'])))
                owner.calls.append((self.path, body))
                entity = body['entity_id']
                if self.path.endswith('/turn_on'):
                    owner.states[entity]['state'] = 'on'
                elif self.path.endswith('/turn_off'):
                    owner.states[entity]['state'] = 'off'
                self.reply(200, [])

        self.server = ThreadingHTTPServer(('127.0.0.1', 0), Handler)
        threading.Thread(target=self.server.serve_forever, daemon=True).start()
        self.url = f'http://127.0.0.1:{self.server.server_address[1]}'

    def close(self):
        self.server.shutdown(); self.server.server_close()


class HomeBridgeTests(unittest.TestCase):
    def setUp(self):
        self.ha = FakeHomeAssistant(); self.addCleanup(self.ha.close)
        self.bridge = HomeBridge(self.ha.url, HA_TOKEN, ['light.salon', 'climate.casa'])

    def test_lists_only_allowed_entities_with_safe_attributes(self):
        states = {s['entity']: s for s in self.bridge.states()}
        self.assertEqual({'light.salon', 'climate.casa'}, set(states))
        self.assertEqual('Luz salón', states['light.salon']['name'])
        self.assertNotIn('secret_attr', states['climate.casa']['attributes'])
        self.assertIn('set_temperature', states['climate.casa']['services'])

    def test_acts_with_bounded_values_and_reports_new_state(self):
        result = self.bridge.act('light.salon', 'turn_on', 40)
        self.assertEqual('on', result['state'])
        self.assertEqual(('/api/services/light/turn_on', {'entity_id': 'light.salon', 'brightness_pct': 40}), self.ha.calls[-1])
        self.bridge.act('climate.casa', 'set_temperature', 21.3)
        self.assertEqual(21.5, self.ha.calls[-1][1]['temperature'])

    def test_refuses_what_bryan_did_not_allow(self):
        for entity, service, value, code in (('switch.horno', 'turn_on', None, 'entity_not_allowed'),
                                             ('light.salon', 'delete', None, 'service_not_allowed'),
                                             ('light.salon', 'turn_on', 300, 'invalid_value'),
                                             ('climate.casa', 'set_temperature', 80, 'invalid_value'),
                                             ('climate.casa', 'set_temperature', None, 'invalid_value'),
                                             ('light.salon', 'turn_on', 'mucho', 'invalid_value')):
            with self.assertRaises(HomeError) as refused:
                self.bridge.act(entity, service, value)
            self.assertEqual(code, refused.exception.code)
        self.assertEqual([], self.ha.calls)

    def test_rate_limit_and_wrong_token(self):
        now = [0.0]
        bridge = HomeBridge(self.ha.url, HA_TOKEN, ['light.salon'], clock=lambda: now[0])
        for _ in range(30):
            bridge.act('light.salon', 'toggle')
        with self.assertRaises(HomeError) as limited:
            bridge.act('light.salon', 'toggle')
        self.assertEqual('too_many_actions', limited.exception.code)
        now[0] += 61
        bridge.act('light.salon', 'toggle')
        with self.assertRaises(HomeError) as rejected:
            HomeBridge(self.ha.url, 'x' * 40, ['light.salon']).states()
        self.assertEqual('home_token_rejected', rejected.exception.code)

    def test_validation_of_url_and_entities(self):
        for url in ('http://8.8.8.8:8123', 'https://casa.example.com', 'ftp://192.168.1.2', 'http://u:p@192.168.1.2',
                    'http://169.254.1.1:8123', 'http://192.168.1.2:8123/api'):
            with self.assertRaises(ValueError):
                check_url(url)
        for url in ('http://192.168.1.10:8123', 'http://homeassistant.local:8123', 'https://homeassistant:8123'):
            check_url(url)
        with self.assertRaises(ValueError):
            parse_entities('lock.puerta')   # las cerraduras no: abrir una puerta no es una orden por voz
        with self.assertRaises(ValueError):
            parse_entities('')
        self.assertEqual(('light.a', 'switch.b'), parse_entities('light.a, switch.b,light.a'))


class HomeNodeTests(unittest.TestCase):
    def setUp(self):
        self.ha = FakeHomeAssistant(); self.addCleanup(self.ha.close)
        backend = Mock(model='m'); backend.ready.return_value = True
        self.app = node.ComputeApplication(TOKEN, backend, home=HomeBridge(self.ha.url, HA_TOKEN, ['light.salon']))
        self.plain = node.ComputeApplication(TOKEN, backend)

    def call(self, app, method, path, body=b''):
        return app.handle(method, path, 'Bearer ' + TOKEN, body)

    def test_routes_roles_and_errors(self):
        self.assertIn('hogar', self.call(self.app, 'GET', '/v1/health')[1]['roles'])
        self.assertEqual((404, {'error': 'home_not_enabled'}), self.call(self.plain, 'GET', '/v1/home'))
        self.assertEqual('Luz salón', self.call(self.app, 'GET', '/v1/home')[1]['entities'][0]['name'])
        status, result = self.call(self.app, 'POST', '/v1/home/action', json.dumps({'entity': 'light.salon', 'service': 'turn_on'}).encode())
        self.assertEqual((200, 'on'), (status, result['entity']['state']))
        self.assertEqual(403, self.call(self.app, 'POST', '/v1/home/action', json.dumps({'entity': 'switch.x', 'service': 'turn_on'}).encode())[0])
        self.assertEqual(400, self.call(self.app, 'POST', '/v1/home/action', b'{"entity":"light.salon"}')[0])
        self.assertEqual(401, self.app.handle('GET', '/v1/home', 'Bearer ' + 'z' * 40)[0])


if __name__ == '__main__':
    unittest.main()
