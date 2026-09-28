import struct
import unittest
from check_android_native_alignment import elf_errors


def elf(align=16384, off=0, addr=0, relro_end=16384):
    data = bytearray(64 + 2 * 56)
    data[:6] = b'\x7fELF\x02\x01'
    struct.pack_into('<Q', data, 32, 64)
    struct.pack_into('<HH', data, 54, 56, 2)
    struct.pack_into('<IIQQQQQQ', data, 64, 1, 5, off, addr, 0, 4096, 4096, align)
    struct.pack_into('<IIQQQQQQ', data, 120, 0x6474E552, 4, 0, 0, 0, relro_end, relro_end, 1)
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
        self.assertIn('GNU_RELRO', elf_errors(elf(relro_end=4096))[0])

    def test_invalid_library_cannot_pass(self):
        self.assertTrue(elf_errors(b'broken'))


if __name__ == '__main__':
    unittest.main()
