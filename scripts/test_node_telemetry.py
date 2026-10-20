import unittest
from unittest.mock import Mock

import salve_compute_node as node

TOKEN = 't' * 40


class GpuTelemetryTests(unittest.TestCase):
    def test_parses_nvidia_smi_and_caches(self):
        calls = []
        now = [100.0]

        def runner():
            calls.append(1)
            return 'NVIDIA GeForce RTX 4070, 6348, 12282, 35\n'

        telemetry = node.GpuTelemetry(ttl=15, clock=lambda: now[0], runner=runner)
        self.assertEqual({'name': 'NVIDIA GeForce RTX 4070', 'vram_used_gb': 6.2, 'vram_total_gb': 11.99, 'load_pct': 35},
                         telemetry.read())
        telemetry.read(); now[0] += 10; telemetry.read()
        self.assertEqual(1, len(calls))
        now[0] += 6; telemetry.read()
        self.assertEqual(2, len(calls))

    def test_without_nvidia_or_with_garbage_reports_nothing(self):
        for output in (None, '', 'basura', 'GPU, x, y, z', 'GPU, 900, 100, 50', 'GPU, 10, 100, 250'):
            self.assertIsNone(node.GpuTelemetry(runner=lambda o=output: o).read(), output)

    def test_health_announces_gpu_and_roles(self):
        backend = Mock(model='m'); backend.ready.return_value = True
        gpu = node.GpuTelemetry(runner=lambda: 'RTX, 1024, 8192, 10')
        app = node.ComputeApplication(TOKEN, backend, gpu=gpu)
        status, health = app.handle('GET', '/v1/health', 'Bearer ' + TOKEN)
        self.assertEqual(200, status)
        self.assertEqual(['inferencia'], health['roles'])
        self.assertEqual({'name': 'RTX', 'vram_used_gb': 1.0, 'vram_total_gb': 8.0, 'load_pct': 10}, health['gpu'])


if __name__ == '__main__':
    unittest.main()
