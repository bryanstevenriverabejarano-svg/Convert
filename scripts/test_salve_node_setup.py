import os
from pathlib import Path
import stat
import tempfile
import unittest
from urllib.parse import parse_qs, urlsplit

import salve_node_setup as setup


class NodeSetupTests(unittest.TestCase):
    def test_url_rules_match_the_app(self):
        self.assertEqual('https://10.77.0.2', setup.validate_url(' https://10.77.0.2/ '))
        self.assertEqual('https://192.168.20.2:8443', setup.validate_url('https://192.168.20.2:8443'))
        for bad in ('https://example.org', 'https://8.8.8.8', 'https://127.0.0.1', 'http://pc', 'https://pc/v1', 'https://u:p@pc', 'https://pc?x=1',
                    'https://pc#a', 'https://pc:0', 'https://pc:99999', 'ftp://pc', '', 'https://'):
            with self.assertRaises(ValueError, msg=bad):
                setup.validate_url(bad)

    def test_pairing_uri_round_trips_and_carries_no_extra_fields(self):
        token = 'A' * 43
        uri = setup.pairing_uri('Mi PC & co', 'https://10.77.0.2', token)
        parts = urlsplit(uri)
        self.assertEqual(('salve', 'nodo'), (parts.scheme, parts.netloc))
        query = parse_qs(parts.query)
        self.assertEqual({'v', 'n', 'u', 't'}, set(query))
        self.assertEqual('Mi PC & co', query['n'][0])
        self.assertEqual('https://10.77.0.2', query['u'][0])
        self.assertEqual(token, query['t'][0])

    def test_token_is_private_reused_and_rotated(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 'sub' / 'token'
            first, created = setup.create_token(path)
            self.assertTrue(created)
            self.assertRegex(first, setup.TOKEN_RE)
            if os.name != 'nt':
                self.assertEqual(0, stat.S_IMODE(path.stat().st_mode) & 0o077)
            again, created = setup.create_token(path)
            self.assertEqual((first, False), (again, created))
            rotated, created = setup.create_token(path, rotate=True)
            self.assertTrue(created)
            self.assertNotEqual(first, rotated)

    def test_recommendation_prefers_gpu_and_fits_with_margin(self):
        tier, budget, where = setup.recommend(ram_available=64, gpu_free=12)
        self.assertIn('GPU', where)
        self.assertEqual(14, tier[0])  # 9 GB + margen <= 12
        tier, _, where = setup.recommend(ram_available=16, gpu_free=None)
        self.assertIn('CPU', where)
        self.assertEqual(4, tier[0])  # cabe 14B, pero en CPU se limita por velocidad
        tier, _, _ = setup.recommend(ram_available=2, gpu_free=None)
        self.assertIsNone(tier)
        self.assertEqual((None, 0.0, 'desconocido'), setup.recommend(None, None))

    def test_hardware_probes_never_raise(self):
        total, available = setup.ram_gb()
        if total is not None:
            self.assertGreater(total, 0)
            self.assertGreaterEqual(total, available)
        self.assertIsInstance(setup.gpus(), list)

    def test_preparar_without_url_explains_https(self):
        with tempfile.TemporaryDirectory() as folder:
            code = setup.main(['preparar', '--token-file', str(Path(folder) / 't')])
            self.assertEqual(2, code)
            self.assertFalse((Path(folder) / 't').exists())

    def test_preparar_with_url_creates_token(self):
        with tempfile.TemporaryDirectory() as folder:
            path = Path(folder) / 't'
            code = setup.main(['preparar', '--url', 'https://10.77.0.2', '--nombre', 'PC',
                               '--token-file', str(path)])
            self.assertEqual(0, code)
            self.assertRegex(path.read_text().strip(), setup.TOKEN_RE)

    def test_android_assets_are_current_copies(self):
        root = Path(__file__).resolve().parent
        for name in ('salve_node_setup.py', 'salve_compute_node.py', 'salve_private_location.py', 'requirements-family.txt', 'PRIVATE_FAMILY_NETWORK.md'):
            asset = root.parent / 'app' / 'src' / 'main' / 'assets' / 'nodo' / name
            self.assertEqual((root / name).read_bytes(), asset.read_bytes(),
                             f'Copia desactualizada: cp scripts/{name} app/src/main/assets/nodo/')


import unittest.mock  # noqa: E402

if __name__ == '__main__':
    unittest.main()
