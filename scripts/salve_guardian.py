#!/usr/bin/env python3
"""Guardián ciego de Salve: guarda copias selladas que este PC no puede leer.

El móvil cifra cada copia con una clave derivada de una frase que solo conoce Bryan (AES-256-GCM
con PBKDF2) antes de enviarla por TLS mutuo. Aquí solo hay bytes opacos: aunque alguien se haga con
este PC, no puede leer los Salventes. Por diseño, el Guardián rechaza todo lo que no llegue sellado
(cabecera SLVB1), para que un fallo del móvil nunca deje datos en claro en el disco.

Guarda la versión anterior de cada copia: una subida dañada no destruye la buena.
"""
from __future__ import annotations

import hashlib
import os
from pathlib import Path
import re
import tempfile
import threading
import time

MAGIC = b'SLVB1'
NAME = re.compile(r'[a-z0-9][a-z0-9-]{2,63}')
MAX_BLOB = 16 * 1024 * 1024
MAX_TOTAL = 256 * 1024 * 1024
MAX_BLOBS = 32
MIN_SEALED = 5 + 4 + 16 + 12 + 16
POSIX = os.name == 'posix'


class VaultError(ValueError):
    def __init__(self, code):
        super().__init__(code)
        self.code = code


class BlindVault:
    def __init__(self, directory, max_blob=MAX_BLOB, max_total=MAX_TOTAL, max_blobs=MAX_BLOBS):
        self.directory = Path(directory).expanduser()
        self.directory.mkdir(parents=True, mode=0o700, exist_ok=True)
        if self.directory.is_symlink() or (POSIX and self.directory.stat().st_mode & 0o077):
            raise ValueError('La carpeta del Guardián debe ser privada (chmod 700)')
        self.max_blob, self.max_total, self.max_blobs = max_blob, max_total, max_blobs
        self.lock = threading.Lock()

    def _path(self, name, previous=False):
        if not isinstance(name, str) or not NAME.fullmatch(name):
            raise VaultError('invalid_name')
        return self.directory / (name + ('.anterior' if previous else '.sellado'))

    def _usage(self):
        files = [p for p in self.directory.iterdir() if p.suffix in ('.sellado', '.anterior') and p.is_file()]
        return len([p for p in files if p.suffix == '.sellado']), sum(p.stat().st_size for p in files)

    def put(self, name, data):
        path = self._path(name)
        if not isinstance(data, (bytes, bytearray)) or len(data) < MIN_SEALED or not data.startswith(MAGIC):
            raise VaultError('not_sealed')
        if len(data) > self.max_blob:
            raise VaultError('too_large')
        with self.lock:
            count, total = self._usage()
            previous = self._path(name, previous=True)
            dropped = previous.stat().st_size if previous.exists() and path.exists() else 0
            if not path.exists() and count >= self.max_blobs:
                raise VaultError('too_many')
            if total - dropped + len(data) > self.max_total:
                raise VaultError('quota_exceeded')
            fd, temporary = tempfile.mkstemp(prefix='.subida-', dir=self.directory)
            try:
                if POSIX:
                    os.fchmod(fd, 0o600)
                with os.fdopen(fd, 'wb') as file:
                    file.write(data); file.flush(); os.fsync(file.fileno())
                if path.exists():
                    os.replace(path, previous)   # la buena se conserva como anterior
                os.replace(temporary, path)
            finally:
                if os.path.exists(temporary):
                    os.unlink(temporary)
            return self._info(name, path)

    def get(self, name):
        path = self._path(name)
        with self.lock:
            if not path.is_file():
                return None
            return path.read_bytes()

    def delete(self, name):
        with self.lock:
            removed = False
            for path in (self._path(name), self._path(name, previous=True)):
                if path.is_file():
                    path.unlink(); removed = True
            return removed

    def list(self):
        with self.lock:
            out = []
            for path in sorted(self.directory.glob('*.sellado')):
                if NAME.fullmatch(path.stem):
                    out.append(self._info(path.stem, path))
            return out

    @staticmethod
    def _info(name, path):
        data = path.read_bytes()
        return {'name': name, 'size': len(data), 'sha256': hashlib.sha256(data).hexdigest(),
                'updated_at': int(path.stat().st_mtime)}

    def usage(self):
        with self.lock:
            count, total = self._usage()
        return {'blobs': count, 'bytes': total, 'max_bytes': self.max_total, 'max_blob': self.max_blob,
                'checked_at': int(time.time())}
