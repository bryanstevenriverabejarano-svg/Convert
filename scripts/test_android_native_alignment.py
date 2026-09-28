import struct
import tempfile
import unittest
import zipfile
from pathlib import Path
from check_android_native_alignment import elf_errors, check_apk, PAGE


def elf(align=16384, off=0, addr=0, relro_end=16384, rw_size=0, relro_start=0):
    data = bytearray(64 + 2 * 56)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 2)
    struct.pack_into('<IIQQQQQQ', data, 64, 1, 6 if rw_size else 5, off, addr, 0, 4096, rw_size or 4096, align)
    struct.pack_into('<IIQQQQQQ', data, 120, 0x6474E552, 4, 0, relro_start, 0, relro_end-relro_start, relro_end-relro_start, 1)
    return data


class NativeAlignmentTest(unittest.TestCase):
    def test_16k_and_64k_are_supported(self):
        self.assertEqual([], elf_errors(elf()))
        self.assertEqual([], elf_errors(elf(65536)))

    def test_4k_load_is_rejected(self):
        self.assertIn('LOAD', elf_errors(elf(4096))[0])

    def test_congruent_offsets_need_not_be_zero(self):
        self.assertEqual([], elf_errors(elf(off=4096, addr=20480)))
        self.assertIn('LOAD', elf_errors(elf(off=4096, addr=8192))[0])

    def test_old_relro_layout_is_rejected(self):
        self.assertIn('GNU_RELRO', elf_errors(elf(relro_end=4096, rw_size=8192))[0])

    def test_rounding_relro_over_padding_without_writable_data_is_safe(self):
        self.assertEqual([], elf_errors(elf(relro_end=4096, rw_size=4096)))

    def test_writable_prefix_is_also_protected_by_bionic_rounding(self):
        self.assertIn('GNU_RELRO', elf_errors(elf(relro_start=4096, rw_size=8192))[0])

    def test_invalid_library_cannot_pass(self):
        self.assertTrue(elf_errors(b'broken'))

    def test_actual_apk_checks_zip_offset_as_well_as_elf(self):
        with tempfile.TemporaryDirectory() as folder:
            apk = Path(folder) / 'fixture.apk'
            name = 'lib/arm64-v8a/libtest.so'
            with zipfile.ZipFile(apk, 'w') as z:
                z.writestr(name, elf())
            self.assertIn('APK offset', check_apk(apk)[0]['errors'][0])
            info = zipfile.ZipInfo(name)
            padding = PAGE - (30 + len(name))
            info.extra = struct.pack('<HH', 0xcafe, padding - 4) + bytes(padding - 4)
            with zipfile.ZipFile(apk, 'w') as z:
                z.writestr(info, elf())
            self.assertEqual([], check_apk(apk)[0]['errors'])

    def test_compressed_apk_still_requires_valid_elf_alignment(self):
        with tempfile.TemporaryDirectory() as folder:
            apk = Path(folder) / 'fixture.apk'
            with zipfile.ZipFile(apk, 'w', compression=zipfile.ZIP_DEFLATED) as z:
                z.writestr('lib/arm64-v8a/libtest.so', elf(align=4096))
            self.assertIn('LOAD', check_apk(apk)[0]['errors'][0])


if __name__ == '__main__':
    unittest.main()
