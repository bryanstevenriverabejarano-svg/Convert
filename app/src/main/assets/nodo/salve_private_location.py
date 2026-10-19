#!/usr/bin/env python3
"""Owner-controlled encrypted location. No network, geocoder or IP-location lookup.

Run `python salve_private_location.py --directory ~/.salve/family` interactively to
approve a static location, or add --withdraw to remove it. Only the last fix is kept.
"""
from __future__ import annotations
import argparse
import hashlib
import json
import math
import os
from pathlib import Path
import secrets
import stat
import sys
import tempfile
import threading
import time
from cryptography.hazmat.primitives.ciphers.aead import AESGCM

GPS_TTL = 900
FIELDS = {'latitude', 'longitude', 'city', 'zone', 'source', 'captured_at', 'accuracy_m'}


def validate_location(value, now=None):
    now = time.time() if now is None else now
    if value is None:
        return None
    if not isinstance(value, dict) or set(value) != FIELDS:
        raise ValueError('Invalid location fields')
    for key, low, high in (('latitude', -90, 90), ('longitude', -180, 180), ('accuracy_m', 0, 100000)):
        number = value[key]
        if type(number) not in (float, int) or not math.isfinite(number) or not low <= number <= high:
            raise ValueError('Invalid coordinate')
    for key in ('city', 'zone'):
        if not isinstance(value[key], str) or len(value[key]) > 100 or any(ord(c) < 32 or ord(c) == 127 for c in value[key]):
            raise ValueError('Invalid label')
    if value['source'] not in ('static', 'gps') or type(value['captured_at']) is not int or not 0 < value['captured_at'] <= now + 60:
        raise ValueError('Invalid location source or time')
    if value['source'] == 'gps' and now - value['captured_at'] > GPS_TTL:
        raise ValueError('GPS fix expired')
    return dict(value)


POSIX = os.name == 'posix'


def private_read(path, maximum=16384):
    """Lee un archivo privado. En Linux/macOS exige permisos 600; en Windows la carpeta del
    usuario ya está protegida por ACL y los bits de modo no significan nada."""
    path = Path(path)
    if path.is_symlink():
        raise ValueError('Private files cannot be symbolic links')
    fd = os.open(path, os.O_RDONLY | getattr(os, 'O_NOFOLLOW', 0) | getattr(os, 'O_BINARY', 0))
    with os.fdopen(fd, 'rb') as file:
        mode = os.fstat(file.fileno()).st_mode
        if not stat.S_ISREG(mode) or (POSIX and mode & 0o077):
            raise ValueError('Private files must be regular files with mode 600')
        data = file.read(maximum + 1)
        if len(data) > maximum:
            raise ValueError('Private file too large')
        return data


def location_version(value):
    """Huella corta de la ubicación vigente: el móvil solo descarga coordenadas si cambia."""
    if value is None:
        return 'none'
    return hashlib.sha256(json.dumps(value, sort_keys=True, allow_nan=False).encode()).hexdigest()[:16]


class LocationStore:
    def __init__(self, directory):
        self.directory = Path(directory).expanduser()
        self.directory.mkdir(parents=True, mode=0o700, exist_ok=True)
        if self.directory.is_symlink() or (POSIX and self.directory.stat().st_mode & 0o077):
            raise ValueError('Location directory must be private (chmod 700)')
        keyfile = self.directory / 'location.key'
        try:
            fd = os.open(keyfile, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
        except FileExistsError:
            pass
        else:
            with os.fdopen(fd, 'wb') as file:
                file.write(AESGCM.generate_key(bit_length=256))
        self.cipher = AESGCM(private_read(keyfile, 32))
        self.path = self.directory / 'location.enc'
        self.lock = threading.Lock()

    def read(self):
        with self.lock:
            if not self.path.exists():
                return None
            data = private_read(self.path)
            value = json.loads(self.cipher.decrypt(data[:12], data[12:], b'salve-location-v1'))
            # Expired GPS is not presented as a current location. Static data remains owner-declared.
            if value and value.get('source') == 'gps' and time.time() - value.get('captured_at', 0) > GPS_TTL:
                return None
            return validate_location(value)

    def write(self, value):
        value = validate_location(value)
        with self.lock:
            nonce = os.urandom(12)
            encrypted = nonce + self.cipher.encrypt(nonce, json.dumps(value, allow_nan=False).encode(), b'salve-location-v1')
            fd, name = tempfile.mkstemp(prefix='.location-', dir=self.directory)
            try:
                with os.fdopen(fd, 'wb') as file:
                    file.write(encrypted); file.flush(); os.fsync(file.fileno())
                os.replace(name, self.path)
            finally:
                if os.path.exists(name):
                    os.unlink(name)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--directory', type=Path, required=True)
    parser.add_argument('--withdraw', action='store_true')
    args = parser.parse_args()
    store = LocationStore(args.directory)
    if args.withdraw:
        store.write(None)
        print('Ubicación retirada. Los móviles borrarán su copia al consultar el nodo.')
        return
    value = {'latitude': float(input('Latitud: ')), 'longitude': float(input('Longitud: ')),
             'city': input('Ciudad (opcional): '), 'zone': input('Zona (opcional): '),
             'source': 'static', 'captured_at': int(time.time()), 'accuracy_m': 0}
    validate_location(value)
    if input('¿Compartir estas coordenadas con los móviles autorizados? Escribe COMPARTIR: ') != 'COMPARTIR':
        print('Cancelado.'); return
    store.write(value)
    print('Guardado cifrado. El nodo solo lo enviará por HTTPS con TLS mutuo.')


if __name__ == '__main__':
    main()
