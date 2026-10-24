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


def mutual_certificates(folder):
    """Como salve_private_network: CA, servidor (127.0.0.1) y cliente del móvil; el servidor exige cliente."""
    import salve_private_network as network
    folder = Path(folder)
    ca_key, ca = network.make_ca(days=2)
    server_key, server_cert = network.issue(ca_key, ca, ['127.0.0.1'], days=2)
    client_key, client_cert = network.issue_client(ca_key, ca, days=2)
    for name, obj in (('ca.pem', ca), ('server.pem', server_cert), ('client.pem', client_cert)):
        (folder / name).write_bytes(obj.public_bytes(serialization.Encoding.PEM))
    for name, key in (('server.key', server_key), ('client.key', client_key)):
        (folder / name).write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
        (folder / name).chmod(0o600)
    server = node.server_tls(folder / 'server.pem', folder / 'server.key', folder / 'ca.pem')
    client = ssl.create_default_context(cafile=folder / 'ca.pem')
    client.load_cert_chain(folder / 'client.pem', folder / 'client.key')
    return server, client


def foreign_client(folder):
    import salve_private_network as network
    folder = Path(folder)
    other_key, other = network.make_ca(days=2)
    key, cert = network.issue_client(other_key, other, days=2)
    (folder / 'foreign.pem').write_bytes(cert.public_bytes(serialization.Encoding.PEM))
    (folder / 'foreign.key').write_bytes(key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption()))
    context = ssl.create_default_context(cafile=folder / 'ca.pem')
    context.load_cert_chain(folder / 'foreign.pem', folder / 'foreign.key')
    return context


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
        original = location()
        self.store.write(original)
        ciphertext = self.store.path.read_bytes()
        self.assertNotIn(b'Ciudad', ciphertext)
        self.assertNotIn(b'latitude', ciphertext)
        self.assertEqual(original, LocationStore(self.folder / 'location').read())
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

    def test_static_location_from_parameters_is_served_and_cannot_be_overwritten(self):
        fixed = node.StaticLocation(42.8467, -2.6716, 'Vitoria-Gasteiz', 'España', 'Casa')
        app = node.ComputeApplication(READ, self.backend, location_store=fixed, location_token=WRITE)
        status = app.handle('GET', '/v1/family/status', 'Bearer ' + READ)[1]
        self.assertEqual('Vitoria-Gasteiz', status['location']['city'])
        self.assertEqual('Casa, España', status['location']['zone'])
        self.assertEqual('static', status['location']['source'])
        self.assertEqual(409, app.handle('POST', '/v1/location', 'Bearer ' + WRITE, json.dumps({'location': location()}).encode())[0])
        with self.assertRaises(ValueError):
            node.StaticLocation(95, 0, 'X')

    def test_ip_allow_list_is_optional_and_exact(self):
        allow = node.IpAllowList(['192.168.1.0/24', '203.0.113.7'])
        self.assertTrue(allow('192.168.1.40')); self.assertTrue(allow('203.0.113.7'))
        self.assertFalse(allow('203.0.113.8')); self.assertFalse(allow('no-es-ip'))

    def test_real_mutual_tls_requires_the_phone_certificate(self):
        tls, client = mutual_certificates(self.folder)
        server = node.NodeServer(('127.0.0.1', 0), self.app, tls=tls)
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
        # Sin certificado de cliente (aunque confíe en la CA del nodo) no hay respuesta.
        anonymous = ssl.create_default_context(cafile=self.folder / 'ca.pem')
        with self.assertRaises((ssl.SSLError, OSError, http.client.HTTPException)):
            read(anonymous)
        # Un certificado de cliente de otra CA tampoco vale.
        with self.assertRaises((ssl.SSLError, OSError, http.client.HTTPException)):
            read(foreign_client(self.folder))
        with socket.create_connection(('127.0.0.1', port), timeout=2) as plain:
            plain.sendall(b'GET /v1/family/status HTTP/1.0\r\n\r\n')
            try: self.assertNotIn(b'Ciudad', plain.recv(1024))
            except (ConnectionResetError, socket.timeout): pass

if __name__ == '__main__':
    unittest.main()
