import hashlib
import http.client
import json
import os
from pathlib import Path
import tempfile
import threading
import unittest
from unittest.mock import Mock

import salve_compute_node as node
from salve_guardian import MAGIC, BlindVault, VaultError
from test_private_family import foreign_client, mutual_certificates

TOKEN = 'g' * 40


def sealed(size=200, fill=b'x'):
    return MAGIC + fill * (size - len(MAGIC))


class BlindVaultTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory(); self.addCleanup(folder.cleanup)
        self.dir = Path(folder.name) / 'guardian'
        self.vault = BlindVault(self.dir, max_blob=1000, max_total=2500, max_blobs=2)

    def test_stores_returns_lists_and_deletes_sealed_blobs(self):
        info = self.vault.put('salventes', sealed())
        self.assertEqual(hashlib.sha256(sealed()).hexdigest(), info['sha256'])
        self.assertEqual(sealed(), self.vault.get('salventes'))
        self.assertEqual(['salventes'], [b['name'] for b in self.vault.list()])
        self.assertTrue(self.vault.delete('salventes'))
        self.assertIsNone(self.vault.get('salventes'))
        self.assertFalse(self.vault.delete('salventes'))

    def test_keeps_previous_version_so_a_bad_upload_does_not_destroy_the_good_one(self):
        self.vault.put('salventes', sealed(fill=b'a'))
        self.vault.put('salventes', sealed(fill=b'b'))
        self.assertEqual(sealed(fill=b'b'), self.vault.get('salventes'))
        self.assertEqual(sealed(fill=b'a'), (self.dir / 'salventes.anterior').read_bytes())

    def test_refuses_plaintext_bad_names_and_quotas(self):
        for data in (b'{"salventes": []}', b'SLVB1', 'texto'):
            with self.assertRaises(VaultError):
                self.vault.put('salventes', data)
        for name in ('../x', 'A', 'ab', 'con espacio', 'x' * 65, None):
            with self.assertRaises(VaultError):
                self.vault.put(name, sealed())
        with self.assertRaises(VaultError) as big:
            self.vault.put('grande', sealed(1001))
        self.assertEqual('too_large', big.exception.code)
        self.vault.put('uno', sealed(900)); self.vault.put('dos', sealed(900))
        with self.assertRaises(VaultError) as many:
            self.vault.put('tres', sealed())
        self.assertEqual('too_many', many.exception.code)
        with self.assertRaises(VaultError) as quota:
            self.vault.put('uno', sealed(990))   # 900 (anterior) + 990 + 900 > 2500
        self.assertEqual('quota_exceeded', quota.exception.code)
        self.assertEqual(sealed(900), self.vault.get('uno'))
        self.assertEqual([], [p.name for p in self.dir.iterdir() if p.name.startswith('.subida-')])

    @unittest.skipUnless(os.name == 'posix', 'permisos POSIX')
    def test_files_and_folder_are_private(self):
        self.vault.put('salventes', sealed())
        self.assertEqual(0o700, self.dir.stat().st_mode & 0o777)
        self.assertEqual(0o600, (self.dir / 'salventes.sellado').stat().st_mode & 0o777)


class GuardianNodeTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory(); self.addCleanup(folder.cleanup); self.folder = Path(folder.name)
        backend = Mock(model='test'); backend.ready.return_value = True
        self.app = node.ComputeApplication(TOKEN, backend, vault=BlindVault(self.folder / 'boveda'))
        self.plain = node.ComputeApplication(TOKEN, backend)

    def call(self, app, method, path, body=b''):
        return app.handle(method, path, 'Bearer ' + TOKEN, body)

    def test_roles_are_announced(self):
        self.assertEqual(['inferencia', 'guardian'], self.call(self.app, 'GET', '/v1/health')[1]['roles'])
        self.assertEqual(['inferencia'], self.call(self.plain, 'GET', '/v1/health')[1]['roles'])
        self.assertIn('roles', self.call(self.app, 'GET', '/v1/family/status')[1])

    def test_vault_routes_require_token_and_enabled_guardian(self):
        self.assertEqual(401, self.app.handle('GET', '/v1/vault', 'Bearer ' + 'x' * 40)[0])
        self.assertEqual((404, {'error': 'guardian_not_enabled'}), self.call(self.plain, 'GET', '/v1/vault'))
        self.assertEqual(400, self.call(self.app, 'POST', '/v1/vault/salventes', b'{"en":"claro"}')[0])
        self.assertEqual(404, self.call(self.app, 'GET', '/v1/vault/salventes')[0])
        status, info = self.call(self.app, 'POST', '/v1/vault/salventes', sealed())
        self.assertEqual((200, True), (status, info['saved']))
        self.assertEqual((200, sealed()), self.call(self.app, 'GET', '/v1/vault/salventes'))
        self.assertEqual(['salventes'], [b['name'] for b in self.call(self.app, 'GET', '/v1/vault')[1]['blobs']])
        self.assertEqual(400, self.call(self.app, 'GET', '/v1/vault/../token')[0])
        self.assertTrue(self.call(self.app, 'DELETE', '/v1/vault/salventes')[1]['deleted'])

    def test_real_mtls_upload_and_download(self):
        server_tls, client = mutual_certificates(self.folder)
        server = node.NodeServer(('127.0.0.1', 0), self.app, tls=server_tls)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close); self.addCleanup(server.shutdown)
        port = server.server_address[1]
        blob = sealed(300_000)   # mayor que el límite JSON: solo las copias son binarias y grandes

        def request(method, path, body=None, kind='application/octet-stream', context=client):
            connection = http.client.HTTPSConnection('127.0.0.1', port, context=context, timeout=10)
            headers = {'Authorization': 'Bearer ' + TOKEN}
            if body is not None:
                headers.update({'Content-Type': kind, 'Content-Length': str(len(body))})
            connection.request(method, path, body=body, headers=headers)
            response = connection.getresponse()
            return response.status, response.getheader('Content-Type'), response.read()

        status, _, body = request('POST', '/v1/vault/salventes', blob)
        self.assertEqual(200, status)
        self.assertEqual(hashlib.sha256(blob).hexdigest(), json.loads(body)['sha256'])
        status, kind, body = request('GET', '/v1/vault/salventes')
        self.assertEqual((200, 'application/octet-stream', blob), (status, kind, body))
        self.assertEqual(400, request('POST', '/v1/vault/salventes', blob, kind='application/json')[0])
        self.assertEqual(400, request('POST', '/v1/generate', blob)[0])   # el resto no acepta binario
        with self.assertRaises(Exception):
            request('GET', '/v1/vault/salventes', context=foreign_client(self.folder))


if __name__ == '__main__':
    unittest.main()
