"""Privacy boundaries: real TLS, owner authorization, at-rest encryption and VPN ingress."""
import datetime
import http.client
import ipaddress
import json
from pathlib import Path
import socket
import ssl
import tempfile
import threading
import time
import unittest
from unittest.mock import Mock, patch
from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa
from cryptography.x509.oid import NameOID
from salve_private_location import LocationStore, validate_location
import salve_compute_node as node

READ = 'r' * 43
WRITE = 'w' * 43


def certificates(folder):
    """Ephemeral test-only CA and IP-SAN leaf, made like salve_private_network makes them for real
    (AKI/SKI included: Python 3.13+ rejects chains without them). No fixture private keys in git."""
    import salve_private_network as network
    ca_key, ca = network.make_ca(days=2)
    key, leaf = network.issue(ca_key, ca, ['127.0.0.1'], days=2, common_name='Test node')
    folder = Path(folder)
    (folder / 'ca.pem').write_bytes(ca.public_bytes(serialization.Encoding.PEM))
    (folder / 'cert.pem').write_bytes(leaf.public_bytes(serialization.Encoding.PEM))
    (folder / 'key.pem').write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    (folder / 'key.pem').chmod(0o600)
    server = ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER); server.minimum_version = ssl.TLSVersion.TLSv1_2
    server.load_cert_chain(folder / 'cert.pem', folder / 'key.pem')
    return server, ssl.create_default_context(cafile=folder / 'ca.pem')


def location(**changes):
    result = dict(latitude=4.6, longitude=-74.0, city='Ciudad privada', zone='Casa privada', source='static', captured_at=int(time.time()), accuracy_m=0)
    result.update(changes)
    return result


class FamilyPrivacyTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory(); self.addCleanup(folder.cleanup); self.folder = Path(folder.name)
        self.store = LocationStore(self.folder / 'location')
        self.backend = Mock(model='test'); self.backend.ready.return_value = False
        self.app = node.ComputeApplication(READ, self.backend, location_store=self.store, location_token=WRITE)

    def call(self, path, token, value=None):
        return self.app.handle('POST' if value is not None else 'GET', path, 'Bearer ' + token, json.dumps(value).encode())

    def test_owner_only_location_changes_and_withdrawal(self):
        self.assertEqual(401, self.call('/v1/location', READ, {'location': location()})[0])
        self.assertIsNone(self.store.read())
        self.assertEqual(200, self.call('/v1/location', WRITE, {'location': location()})[0])
        self.assertEqual(401, self.call('/v1/family/status', WRITE)[0])
        result = self.call('/v1/family/status', READ)[1]
        self.assertTrue(result['available'])  # family availability does not depend on model readiness
        self.backend.ready.assert_not_called()
        self.assertEqual('Ciudad privada', result['location']['city'])
        self.assertEqual(200, self.call('/v1/location', WRITE, {'location': None})[0])
        self.assertIsNone(self.call('/v1/family/status', READ)[1]['location'])

    def test_default_location_is_absent_and_writer_is_separate(self):
        app = node.ComputeApplication(READ, self.backend)
        self.assertIsNone(app.handle('GET', '/v1/family/status', 'Bearer ' + READ)[1]['location'])
        self.assertEqual(401, app.handle('POST', '/v1/location', 'Bearer ' + READ, b'{}')[0])
        with self.assertRaises(ValueError):
            node.ComputeApplication(READ, self.backend, location_token=READ)

    def test_encryption_restart_tamper_and_private_permissions(self):
        self.store.write(location())
        ciphertext = self.store.path.read_bytes()
        self.assertNotIn(b'Ciudad', ciphertext)
        self.assertNotIn(b'latitude', ciphertext)
        self.assertEqual(location(), LocationStore(self.folder / 'location').read())
        self.store.path.write_bytes(ciphertext[:-1] + bytes([ciphertext[-1] ^ 1]))
        self.assertEqual(503, self.call('/v1/family/status', READ)[0])
        key = self.store.directory / 'location.key'; key.chmod(0o644)
        with self.assertRaises(ValueError):
            LocationStore(self.store.directory)

    def test_invalid_location_and_expired_gps_rejected(self):
        bad = [location(latitude=float('nan')), location(longitude=181), location(source='ip'), location(latitude=True), location(city='a' * 101), location(captured_at=int(time.time()) + 100), location(source='gps', captured_at=int(time.time()) - 901)]
        for value in bad:
            self.assertEqual(400, self.call('/v1/location', WRITE, {'location': value})[0])
        now = int(time.time())
        self.store.write(location(source='gps', captured_at=now))
        with patch('salve_private_location.time.time', return_value=now + 901):
            self.assertIsNone(self.store.read())

    def test_vpn_interface_and_client_list_fail_closed(self):
        info = [{'linkinfo': {'info_kind': 'wireguard'}, 'addr_info': [{'local': '10.77.0.2'}]}]
        with patch('salve_compute_node.subprocess.run', return_value=Mock(stdout=json.dumps(info))):
            guard = node.WireGuardIngress('wg0', '10.77.0.2', ['10.77.0.1'])
            self.assertTrue(guard('10.77.0.1')); self.assertFalse(guard('8.8.8.8')); self.assertFalse(guard('10.77.0.3'))
        with patch('salve_compute_node.subprocess.run', side_effect=OSError):
            self.assertFalse(guard('10.77.0.1'))
        for ip in ('8.8.8.8', '127.0.0.1', '169.254.1.1', '100.64.0.1'):
            with self.assertRaises(ValueError): node.WireGuardIngress('wg0', ip, ['10.77.0.1'])

    def test_real_tls_auth_no_plaintext_and_ingress_revocation(self):
        tls, client = certificates(self.folder)
        allowed = [True]
        server = node.NodeServer(('127.0.0.1', 0), self.app, tls=tls, connection_guard=lambda ip: allowed[0])
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close); self.addCleanup(server.shutdown)
        port = server.server_address[1]
        def read(context=client, token=READ):
            connection = http.client.HTTPSConnection('127.0.0.1', port, context=context, timeout=2)
            try:
                connection.request('GET', '/v1/family/status', headers={'Authorization': 'Bearer ' + token})
                response = connection.getresponse(); return response.status, response.read()
            finally: connection.close()
        self.store.write(location())
        self.assertEqual(401, read(token='bad')[0]); self.assertIn(b'Ciudad privada', read()[1])
        with self.assertRaises(ssl.SSLError): read(ssl.create_default_context())
        with socket.create_connection(('127.0.0.1', port), timeout=2) as plain:
            plain.sendall(b'GET /v1/family/status HTTP/1.0\r\n\r\n')
            try: self.assertNotIn(b'Ciudad', plain.recv(1024))
            except ConnectionResetError: pass
        allowed[0] = False
        with self.assertRaises((OSError, http.client.HTTPException)): read()


if __name__ == '__main__':
    unittest.main()
