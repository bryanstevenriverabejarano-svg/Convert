import base64
import ipaddress
import json
from pathlib import Path
import socket
import ssl
import tempfile
import threading
import unittest
from unittest.mock import Mock, patch

from cryptography import x509

import salve_compute_node as node
import salve_private_location as locations
import salve_private_network as network
from test_private_family import READ, WRITE, certificates, location


class LocationVersionTests(unittest.TestCase):
    def setUp(self):
        folder = tempfile.TemporaryDirectory(); self.addCleanup(folder.cleanup)
        self.store = locations.LocationStore(Path(folder.name) / 'location')
        self.app = node.ComputeApplication(READ, Mock(model='m'), location_store=self.store, location_token=WRITE)

    def status(self, known=None):
        return self.app.handle('GET', '/v1/family/status', 'Bearer ' + READ, known_location=known)[1]

    def test_coordinates_travel_only_when_they_change(self):
        first = self.status()
        self.assertIsNone(first['location']); self.assertEqual('none', first['location_version'])
        self.assertEqual({'protocol': 1, 'available': True, 'unchanged': True, 'location_version': 'none'}, self.status('none'))
        self.store.write(location())
        changed = self.status('none')
        self.assertEqual(location(), changed['location'])
        version = changed['location_version']
        same = self.status(version)
        self.assertTrue(same['unchanged']); self.assertNotIn('location', same)
        self.store.write(location(city='Otra'))
        self.assertEqual('Otra', self.status(version)['location']['city'])
        self.store.write(None)
        self.assertIsNone(self.status(version)['location'])

    def test_version_is_stable_and_bound_to_content(self):
        self.assertEqual(locations.location_version(location()), locations.location_version(dict(reversed(list(location().items())))))
        self.assertNotEqual(locations.location_version(location()), locations.location_version(location(zone='Z')))


class WindowsNodeTests(unittest.TestCase):
    @unittest.skipUnless(__import__('os').name == 'posix', 'los bits de modo solo existen en POSIX')
    def test_private_files_ignore_posix_mode_bits_on_windows(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'token'; path.write_text('x'); path.chmod(0o644)
            with self.assertRaises(ValueError):
                locations.private_read(path)
            with patch.object(locations, 'POSIX', False):
                self.assertEqual(b'x', locations.private_read(path))


class PrivateNetworkSetupTests(unittest.TestCase):
    def create(self, folder, **kw):
        return network.create(Path(folder), kw.pop('local', '192.168.1.20'), kw.pop('remote', 'casa.duckdns.org'), **kw)

    def test_creates_everything_once_and_reuses_it(self):
        with tempfile.TemporaryDirectory() as folder:
            paths, made = self.create(folder)
            for item in ('CA', 'certificado del nodo', 'certificado del móvil', 'token'):
                self.assertIn(item, made)
            for name in ('ca.pem', 'nodo.pem', 'movil.pem', 'token', 'salve-perfil.json'):
                self.assertTrue(paths[name].exists(), name)
            ca_before = paths['ca.pem'].read_bytes()
            _, again = self.create(folder)
            self.assertEqual([], again)
            _, moved = self.create(folder, remote='otro.duckdns.org')
            self.assertEqual(['certificado del nodo'], moved, 'cambiar el host remoto solo reemite el del nodo')
            self.assertEqual(ca_before, paths['ca.pem'].read_bytes())
            _, rebuilt = self.create(folder, rebuild=True)
            self.assertIn('CA', rebuilt)

    def test_profile_carries_addresses_token_and_phone_identity(self):
        with tempfile.TemporaryDirectory() as folder:
            paths, _ = self.create(folder, port=8443, name='Torre')
            profile = json.loads(paths['salve-perfil.json'].read_text())
            self.assertEqual(('nodo', 2, 'Torre'), (profile['salve'], profile['v'], profile['n']))
            self.assertEqual('https://192.168.1.20:8443', profile['u'])
            self.assertEqual('https://casa.duckdns.org:8443', profile['r'])
            self.assertEqual(paths['token'].read_text().strip(), profile['t'])
            client = x509.load_der_x509_certificate(base64.b64decode(profile['c']))
            ca = x509.load_der_x509_certificate(base64.b64decode(profile['ca']))
            self.assertEqual(ca.subject, client.issuer)
            usage = client.extensions.get_extension_for_class(x509.ExtendedKeyUsage).value
            self.assertIn(x509.oid.ExtendedKeyUsageOID.CLIENT_AUTH, list(usage))
            self.assertNotIn('ca.key', paths['salve-perfil.json'].read_text())
            self.assertLess(len(paths['salve-perfil.json'].read_bytes()), 2300, 'debe caber en un QR')
            without_remote, _ = network.create(Path(folder) / 'b', '192.168.1.20', None)
            self.assertNotIn('r', json.loads(without_remote['salve-perfil.json'].read_text()))

    def test_server_certificate_names_local_ip_and_remote_host(self):
        with tempfile.TemporaryDirectory() as folder:
            paths, _ = self.create(folder)
            cert = x509.load_pem_x509_certificate(paths['nodo.pem'].read_bytes())
            sans = cert.extensions.get_extension_for_class(x509.SubjectAlternativeName).value
            self.assertEqual([ipaddress.ip_address('192.168.1.20')], sans.get_values_for_type(x509.IPAddress))
            self.assertEqual(['casa.duckdns.org'], sans.get_values_for_type(x509.DNSName))
            cert.extensions.get_extension_for_class(x509.AuthorityKeyIdentifier)

    def test_qr_svg_when_qrcode_is_available(self):
        try:
            import qrcode  # noqa: F401
        except ImportError:
            self.skipTest('qrcode no instalado')
        with tempfile.TemporaryDirectory() as folder:
            paths, _ = self.create(folder)
            self.assertIn(b'<svg', paths['salve-perfil.svg'].read_bytes()[:300])

    def test_strict_mutual_tls_handshake_with_generated_certificates(self):
        from test_private_family import mutual_certificates
        with tempfile.TemporaryDirectory() as folder:
            server_tls, client_tls = mutual_certificates(folder)
            client_tls.verify_flags |= ssl.VERIFY_X509_STRICT  # como Python 3.13+: exige AKI/SKI
            listener = socket.socket(); listener.bind(('127.0.0.1', 0)); listener.listen(1)
            port = listener.getsockname()[1]
            def serve():
                connection, _ = listener.accept()
                with server_tls.wrap_socket(connection, server_side=True) as tls:
                    tls.sendall(b'ok')
            threading.Thread(target=serve, daemon=True).start()
            with socket.create_connection(('127.0.0.1', port), timeout=5) as raw:
                with client_tls.wrap_socket(raw, server_hostname='127.0.0.1') as tls:
                    self.assertEqual(b'ok', tls.recv(2))
            listener.close()

    def test_hosts_and_ports_are_validated(self):
        for bad in ('', 'http://casa.example', '127.0.0.1', '0.0.0.0', 'casa', 'mal_host.example', 'a..b'):
            with self.assertRaises(ValueError, msg=bad):
                network.check_host(bad, 'Host')
        self.assertEqual('203.0.113.7', network.check_host('203.0.113.7', 'Host'))
        self.assertEqual('casa.duckdns.org', network.check_host('Casa.DuckDNS.org.', 'Host'))
        for bad in (0, 70000):
            with self.assertRaises(ValueError):
                network.check_port(bad)
        self.assertEqual('https://[2001:db8::1]:8443', network.endpoint('2001:db8::1', 8443))


class NodeAppTests(unittest.TestCase):
    def test_self_test_runs_a_real_mutual_tls_request(self):
        import salve_nodo_app as app
        self.assertEqual(0, app.self_test())

    def test_node_arguments_carry_fixed_location_or_owner_store(self):
        import salve_nodo_app as app
        paths = {name: Path('/x') / name for name in ('nodo.pem', 'nodo.key', 'ca.pem', 'token', 'token-ubicacion')}
        config = {'port': 8443, 'model': 'm', 'location': {'lat': 42.8, 'lon': -2.6, 'city': 'Vitoria-Gasteiz', 'country': 'España', 'zone': ''}}
        args = app.node_args(config, paths)
        self.assertIn('--client-ca', args); self.assertEqual('8443', args[args.index('--port') + 1])
        self.assertEqual('Vitoria-Gasteiz', args[args.index('--ciudad') + 1])
        self.assertNotIn('--location-directory', args)
        del config['location']
        self.assertIn('--location-directory', app.node_args(config, paths))


if __name__ == '__main__':
    unittest.main()
