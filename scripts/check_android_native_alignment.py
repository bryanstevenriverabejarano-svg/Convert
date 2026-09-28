#!/usr/bin/env python3
"""Check every 64-bit .so in the actual APK, including transitive/prebuilt SDKs."""
import argparse
import json
from pathlib import Path
import struct
import zipfile

PAGE = 16384


def elf_errors(data):
    if len(data) < 64 or data[:6] != b'\x7fELF\x02\x01':
        return ['not a little-endian ELF64 library']
    offset = struct.unpack_from('<Q', data, 32)[0]
    size, count = struct.unpack_from('<HH', data, 54)
    if size < 56 or count == 0 or offset + size * count > len(data):
        return ['invalid ELF program headers']
    errors, loads, writable, relro = [], 0, [], []
    for i in range(count):
        kind, flags, off, addr, _, file_size, mem_size, align = struct.unpack_from('<IIQQQQQQ', data, offset + size * i)
        if kind == 1:
            loads += 1
            if align < PAGE or align & (align - 1) or (off - addr) % PAGE:
                errors.append(f'LOAD {i}: align={align}, offset={off}, address={addr}')
            if flags & 2:
                writable.append((addr, addr + mem_size))
        if kind == 0x6474E552:
            relro.append((addr, addr + mem_size))
    # Bionic rounds RELRO outward to page boundaries. A non-aligned end alone is
    # not a conflict: modern LLD can leave unmapped padding before the next LOAD.
    # Reject actual writable bytes in the rounded margins, including the prefix.
    # Source: AOSP linker_phdr.cpp, _phdr_table_set_gnu_relro_prot.
    for start, end in relro:
        for low, high in ((start // PAGE * PAGE, start), (end, (end + PAGE - 1) // PAGE * PAGE)):
            for rw_start, rw_end in writable:
                fragments = [(max(low, rw_start), min(high, rw_end))]
                for ro_start, ro_end in relro:
                    fragments = [(a, b) for a, b in fragments if a < b
                                 for a, b in ((a, min(b, ro_start)), (max(a, ro_end), b)) if a < b]
                if any(a < b for a, b in fragments):
                    errors.append(f'GNU_RELRO: 16 KB protection overlaps writable data near {hex(low)}')
    if not loads:
        errors.append('missing LOAD segments')
    return errors


def check_apk(path):
    report = []
    with zipfile.ZipFile(path) as apk, open(path, 'rb') as raw:
        for info in apk.infolist():
            if not info.filename.startswith(('lib/arm64-v8a/', 'lib/x86_64/')) or not info.filename.endswith('.so'):
                continue
            errors = elf_errors(apk.read(info))
            raw.seek(info.header_offset + 26)
            name_size, extra_size = struct.unpack('<HH', raw.read(4))
            data_offset = info.header_offset + 30 + name_size + extra_size
            if info.compress_type == zipfile.ZIP_STORED and data_offset % PAGE:
                errors.append(f'APK offset {data_offset} is not 16 KB aligned')
            report.append({'library': info.filename, 'errors': errors})
    if not report:
        raise ValueError('APK contains no 64-bit native libraries')
    return report


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('apk', type=Path)
    parser.add_argument('--report', type=Path)
    args = parser.parse_args()
    result = check_apk(args.apk)
    output = json.dumps(result, indent=2)
    print(output)
    if args.report:
        args.report.parent.mkdir(parents=True, exist_ok=True)
        args.report.write_text(output + '\n')
    raise SystemExit(1 if any(item['errors'] for item in result) else 0)
