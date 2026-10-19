#!/usr/bin/env python3
"""Prepara un nodo propio: mide hardware y genera un QR de emparejamiento privado.

python salve_node_setup.py preparar --url https://10.77.0.2:8766
No usa descubrimiento mDNS, proveedores VPN externos ni geolocalización de IPs.
El QR opcional (pip install qrcode) contiene el token de consulta. Guárdalo privado.
Consulta PRIVATE_FAMILY_NETWORK.md para WireGuard, certificados y arranque HTTPS.
"""
from __future__ import annotations

import argparse
import ctypes
import json
import ipaddress
import os
from pathlib import Path
import platform
import re
import secrets
import shutil
import socket
import subprocess
import sys
import time
from urllib.parse import quote, urlsplit

SERVICE_TYPE = '_salvenode._tcp'
PAIRING_VERSION = '1'
NODE_PORT = 8766
TOKEN_RE = re.compile(r'[A-Za-z0-9_-]{32,256}')
NAME_RE = re.compile(r'[^\x00-\x1f\x7f]{1,60}')

# Tamaño aproximado de archivo GGUF Q4_K_M por familia de parámetros, en GB.
# Se suma margen para la caché KV (contexto ~8k) y el propio proceso.
TIERS = (
    (70, 42.0, 'Modelos de 70B'),
    (32, 19.5, 'Modelos de 27-32B'),
    (14, 9.0, 'Modelos de 12-14B'),
    (8, 5.0, 'Modelos de 7-8B'),
    (4, 2.6, 'Modelos de 3-4B'),
    (1.5, 1.1, 'Modelos de 1-2B'),
)
KV_MARGIN_GB = 1.5
# La app corta cada tarea a los ~50 s y pide hasta 1024 tokens: en CPU solo los modelos
# pequeños generan lo bastante rápido (~20 tokens/s) para terminar a tiempo.
CPU_MAX_PARAMS = 4


# ---------------------------------------------------------------- hardware

def ram_gb():
    """Devuelve (total, disponible) en GB, o (None, None) si no se puede medir."""
    system = platform.system()
    try:
        if system == 'Linux':
            info = {}
            with open('/proc/meminfo', encoding='ascii') as meminfo:
                for line in meminfo:
                    key, value = line.split(':', 1)
                    info[key] = int(value.split()[0]) * 1024
            available = info.get('MemAvailable', info.get('MemFree', 0))
            return info['MemTotal'] / 1e9, available / 1e9
        if system == 'Windows':
            class MemoryStatus(ctypes.Structure):
                _fields_ = [('dwLength', ctypes.c_ulong), ('dwMemoryLoad', ctypes.c_ulong),
                            ('ullTotalPhys', ctypes.c_ulonglong), ('ullAvailPhys', ctypes.c_ulonglong),
                            ('ullTotalPageFile', ctypes.c_ulonglong), ('ullAvailPageFile', ctypes.c_ulonglong),
                            ('ullTotalVirtual', ctypes.c_ulonglong), ('ullAvailVirtual', ctypes.c_ulonglong),
                            ('ullAvailExtendedVirtual', ctypes.c_ulonglong)]
            status = MemoryStatus()
            status.dwLength = ctypes.sizeof(MemoryStatus)
            if ctypes.windll.kernel32.GlobalMemoryStatusEx(ctypes.byref(status)):
                return status.ullTotalPhys / 1e9, status.ullAvailPhys / 1e9
        if system == 'Darwin':
            total = int(_run(['sysctl', '-n', 'hw.memsize']) or 0)
            stats = _run(['vm_stat']) or ''
            page = int(re.search(r'page size of (\d+)', stats).group(1)) if 'page size' in stats else 4096
            free_pages = sum(int(m) for m in re.findall(r'Pages (?:free|inactive|speculative):\s+(\d+)', stats))
            return total / 1e9, (free_pages * page / 1e9) or total * 0.5 / 1e9
    except (OSError, ValueError, KeyError, AttributeError):
        pass
    return None, None


def gpus():
    """Lista de dicts {name, vram_gb, free_gb, kind}. Solo datos que el sistema informa."""
    found = []
    nvidia = _run(['nvidia-smi', '--query-gpu=name,memory.total,memory.free',
                   '--format=csv,noheader,nounits'])
    for line in (nvidia or '').splitlines():
        parts = [p.strip() for p in line.split(',')]
        if len(parts) == 3:
            try:
                found.append({'name': parts[0], 'vram_gb': float(parts[1]) / 1024,
                              'free_gb': float(parts[2]) / 1024, 'kind': 'cuda'})
            except ValueError:
                pass
    if not found and platform.system() == 'Darwin' and platform.machine() == 'arm64':
        total, _ = ram_gb()
        if total:
            # Memoria unificada: Metal puede usar ~2/3 de la RAM por defecto.
            found.append({'name': 'Apple Silicon (memoria unificada)', 'vram_gb': total * 0.66,
                          'free_gb': total * 0.66, 'kind': 'metal'})
    if not found and platform.system() == 'Windows':
        names = _run(['powershell', '-NoProfile', '-Command',
                      '(Get-CimInstance Win32_VideoController).Name'])
        for name in (names or '').splitlines():
            if name.strip():
                # Windows no informa bien la VRAM de forma estándar; no se inventa.
                found.append({'name': name.strip(), 'vram_gb': None, 'free_gb': None, 'kind': 'desconocida'})
    return found


def _run(command):
    if not shutil.which(command[0]):
        return None
    try:
        return subprocess.run(command, capture_output=True, text=True, timeout=8, check=True).stdout.strip()
    except (OSError, subprocess.SubprocessError):
        return None


def recommend(ram_available, gpu_free):
    """Elige el mayor tramo que cabe. Devuelve (tramo, presupuesto_gb, dónde)."""
    if gpu_free and gpu_free >= 2:
        budget, where = gpu_free, 'GPU (todas las capas en VRAM)'
    elif ram_available:
        budget, where = ram_available * 0.75, 'CPU (RAM del sistema)'
    else:
        return None, 0.0, 'desconocido'
    for params, size, label in TIERS:
        if where.startswith('CPU') and params > CPU_MAX_PARAMS:
            continue
        if size + KV_MARGIN_GB <= budget:
            return (params, size, label), budget, where
    return None, budget, where


# ---------------------------------------------------------------- red y URL

def validate_url(raw):
    """HTTPS y literal RFC1918: sin DNS, ruta ni parámetros."""
    value = (raw or '').strip().rstrip('/')
    parts = urlsplit(value)
    try:
        port = parts.port
    except ValueError:
        port = -1
    try:
        ip = ipaddress.IPv4Address(parts.hostname)
        private = any(ip in ipaddress.ip_network(net) for net in ('10.0.0.0/8', '172.16.0.0/12', '192.168.0.0/16'))
    except (ValueError, TypeError):
        private = False
    if (not private or parts.scheme != 'https' or not parts.hostname or parts.username or parts.password
            or parts.query or parts.fragment or parts.path or '\\' in value or len(value) > 500
            or (port is not None and not 1 <= port <= 65535)):
        raise ValueError('La URL debe ser https://IPv4-privada[:puerto], sin ruta, credenciales ni parámetros.')
    return value


def pairing_uri(name, url, token):
    return (f'salve://nodo?v={PAIRING_VERSION}&n={quote(name, safe="")}'
            f'&u={quote(url, safe="")}&t={token}')


# ---------------------------------------------------------------- token

def create_token(path: Path, rotate=False):
    if path.exists() and not rotate:
        token = path.read_text(encoding='ascii').strip()
        if TOKEN_RE.fullmatch(token):
            return token, False
    path.parent.mkdir(parents=True, exist_ok=True)
    token = secrets.token_urlsafe(32)  # 43 caracteres URL-safe
    if path.exists():
        path.unlink()
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL, 0o600)
    with os.fdopen(fd, 'w', encoding='ascii') as handle:
        handle.write(token + '\n')
    return token, True


# ---------------------------------------------------------------- salida

def print_qr(text):
    try:
        import qrcode  # opcional
    except ImportError:
        print('  (Instala el paquete opcional para ver el QR aquí:  pip install qrcode)')
        print('  Mientras tanto, escribe la URL y el token a mano en la app.')
        return False
    qr = qrcode.QRCode(border=2, error_correction=qrcode.constants.ERROR_CORRECT_M)
    qr.add_data(text)
    qr.make(fit=True)
    qr.print_ascii(invert=True)
    return True


def cmd_preparar(args):
    print('\n== Salve · preparar nodo ==\n')
    total, available = ram_gb()
    cards = gpus()
    if total:
        print(f'RAM: {total:.1f} GB en total, {available:.1f} GB libres')
    else:
        print('RAM: no se pudo medir')
    for card in cards:
        vram = f"{card['vram_gb']:.1f} GB (libres {card['free_gb']:.1f})" if card['vram_gb'] else 'VRAM no informada'
        print(f"GPU: {card['name']} · {vram}")
    if not cards:
        print('GPU: no se detectó ninguna con aceleración (se usará la CPU)')
    best_gpu = max((c['free_gb'] for c in cards if c['free_gb']), default=None)
    tier, budget, where = recommend(available, best_gpu)
    print(f'\nPresupuesto para el modelo: ~{budget:.1f} GB en {where}')
    if tier:
        print(f'Recomendación: {tier[2]} en GGUF Q4_K_M (archivo ~{tier[1]:.0f} GB).')
        print('  Un tramo menor deja más margen si usas el PC a la vez.')
        if where.startswith('CPU'):
            print('  En CPU se limita a 3-4B: la app espera ~50 s por respuesta y un modelo mayor')
            print('  no terminaría a tiempo. Con una GPU compatible podrías usar modelos más grandes.')
    else:
        print('Recomendación: este equipo no tiene memoria suficiente ni para un modelo de 1-2B.')

    url = args.url
    if not url:
        print('\nFalta la URL HTTPS del nodo. La app solo acepta https:// con certificado válido.')
        print('Configura WireGuard propio y consulta PRIVATE_FAMILY_NETWORK.md para crear la CA y TLS.')
        print('Después: python salve_node_setup.py preparar --url https://10.77.0.2:8766')
        return 2
    try:
        url = validate_url(url)
    except ValueError as error:
        print(f'\n{error}')
        return 2
    name = args.nombre or platform.node() or 'Mi PC'
    if not NAME_RE.fullmatch(name):
        print('\nEl nombre debe tener de 1 a 60 caracteres imprimibles.')
        return 2
    token_path = Path(args.token_file).expanduser()
    token, created = create_token(token_path, rotate=args.rotar)
    print(f"\nToken {'nuevo' if created else 'existente'} guardado en: {token_path}")

    print('\nArranca el servicio siguiendo PRIVATE_FAMILY_NETWORK.md; backend HTTPS en 127.0.0.1:8080:')
    print(f'  python salve_compute_node.py --token-file "{token_path}" --model <nombre-del-modelo> --port {NODE_PORT} --bind {urlsplit(url).hostname} --interface wg0 --allow-client 10.77.0.1 --tls-cert server.pem --tls-key server.key --backend-ca ca.pem')

    print('\nEn la app: Nodos de cómputo > Añadir un nodo > Escanear QR\n')
    print_qr(pairing_uri(name, url, token))
    print(f'\nURL:   {url}\nToken: {token}')
    print('\nEl QR contiene el token. Cierra o limpia esta ventana cuando termines.')
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest='orden', required=True)
    prep = sub.add_parser('preparar', help='medir el equipo, crear token y mostrar el QR')
    prep.add_argument('--url', help='URL HTTPS con la IPv4 privada del nodo en WireGuard')
    prep.add_argument('--nombre', help='nombre que verás en la app')
    prep.add_argument('--token-file', default='~/.salve/node_token')
    prep.add_argument('--rotar', action='store_true', help='crear un token nuevo aunque ya exista')
    args = parser.parse_args(argv)
    return cmd_preparar(args)


if __name__ == '__main__':
    sys.exit(main())
