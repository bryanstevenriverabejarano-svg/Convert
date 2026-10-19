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
    def test_ingress_without_iproute_checks_the_tunnel_address_is_ours(self):
        with patch.object(node.WireGuardIngress, 'LINUX', False), \
                patch.object(node.WireGuardIngress, 'address_assigned', return_value=True):
            guard = node.WireGuardIngress(None, '10.77.0.2', ['10.77.0.1'])
            self.assertTrue(guard('10.77.0.1')); self.assertFalse(guard('10.77.0.9'))
        with patch.object(node.WireGuardIngress, 'LINUX', False), \
                patch.object(node.WireGuardIngress, 'address_assigned', return_value=False):
            with self.assertRaises(ValueError):  # túnel apagado: no arranca
                node.WireGuardIngress(None, '10.77.0.2', ['10.77.0.1'])

    def test_address_probe_is_real(self):
        guard = object.__new__(node.WireGuardIngress)
        guard.bind = '127.0.0.1'; self.assertTrue(guard.address_assigned())
        guard.bind = '10.255.254.253'; self.assertFalse(guard.address_assigned())

    def test_private_files_ignore_posix_mode_bits_on_windows(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'token'; path.write_text('x'); path.chmod(0o644)
            with self.assertRaises(ValueError):
                locations.private_read(path)
            with patch.object(locations, 'POSIX', False):
                self.assertEqual(b'x', locations.private_read(path))


class PrivateNetworkSetupTests(unittest.TestCase):
    def test_creates_everything_once_and_reuses_it(self):
        with tempfile.TemporaryDirectory() as folder:
            paths, made = network.create(Path(folder), '192.168.1.20')
            self.assertIn('CA', made); self.assertIn('claves WireGuard', made)
            for path in paths.values():
                self.assertTrue(path.exists(), path)
            again_paths, again = network.create(Path(folder), '192.168.1.20')
            self.assertEqual([], again)
            self.assertEqual(paths['salve-movil.conf'].read_bytes(), again_paths['salve-movil.conf'].read_bytes())
            _, rebuilt = network.create(Path(folder), '192.168.1.20', rebuild=True)
            self.assertIn('CA', rebuilt)

    def test_phone_profile_matches_what_salve_accepts(self):
        with tempfile.TemporaryDirectory() as folder:
            paths, _ = network.create(Path(folder), '192.168.1.20')
            phone = paths['salve-movil.conf'].read_text(); pc = paths['wg-nodo-salve.conf'].read_text()
            self.assertIn('Address = 10.77.0.1/32', phone); self.assertIn('AllowedIPs = 10.77.0.2/32', phone)
            self.assertIn('Endpoint = 192.168.1.20:51820', phone); self.assertNotIn('DNS', phone)
            self.assertIn('Address = 10.77.0.2/32', pc); self.assertIn('AllowedIPs = 10.77.0.1/32', pc)
            phone_public = [l.split(' = ')[1] for l in pc.splitlines() if l.startswith('PublicKey')][0]
            node_public = [l.split(' = ')[1] for l in phone.splitlines() if l.startswith('PublicKey')][0]
            self.assertNotEqual(phone_public, node_public)
            self.assertEqual(32, len(__import__('base64').b64decode(node_public)))

    def test_certificate_names_the_tunnel_ip_and_carries_identifiers(self):
        with tempfile.TemporaryDirectory() as folder:
            paths, _ = network.create(Path(folder), '192.168.1.20')
            cert = x509.load_pem_x509_certificate(paths['nodo.pem'].read_bytes())
            sans = cert.extensions.get_extension_for_class(x509.SubjectAlternativeName).value.get_values_for_type(x509.IPAddress)
            self.assertEqual([ipaddress.ip_address('10.77.0.2')], sans)
            cert.extensions.get_extension_for_class(x509.AuthorityKeyIdentifier)
            ca = x509.load_pem_x509_certificate(paths['ca.pem'].read_bytes())
            self.assertTrue(ca.extensions.get_extension_for_class(x509.BasicConstraints).value.ca)

    def test_strict_tls_handshake_with_generated_certificates(self):
        with tempfile.TemporaryDirectory() as folder:
            server_tls, client_tls = certificates(folder)
            client_tls.verify_flags |= ssl.VERIFY_X509_STRICT  # como Python 3.13+: exige AKI/SKI
            listener = socket.socket(); listener.bind(('127.0.0.1', 0)); listener.listen(1)
            port = listener.getsockname()[1]
            def serve():
                connection, _ = listener.accept()
                with server_tls.wrap_socket(connection, server_side=True) as tls:
                    tls.sendall(b'ok')
            thread = threading.Thread(target=serve, daemon=True); thread.start()
            with socket.create_connection(('127.0.0.1', port), timeout=5) as raw:
                with client_tls.wrap_socket(raw, server_hostname='127.0.0.1') as tls:
                    self.assertEqual(b'ok', tls.recv(2))
            listener.close()

    def test_endpoint_must_be_numeric_ipv4(self):
        for bad in ('casa.example', '', '127.0.0.1', '0.0.0.0', '::1'):
            with self.assertRaises(ValueError):
                network.check_endpoint(bad)
        self.assertEqual('203.0.113.7', network.check_endpoint('203.0.113.7'))


if __name__ == '__main__':
    unittest.main()
