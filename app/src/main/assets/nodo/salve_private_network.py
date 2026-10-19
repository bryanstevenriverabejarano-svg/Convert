#!/usr/bin/env python3
"""Prepara la conexión segura entre Salve y este PC: TLS mutuo (mTLS), sin VPN ni openssl.

  python salve_private_network.py crear --host-remoto midominio.duckdns.org --puerto 8443

Genera en ~/.salve/red (o --carpeta):
  ca.pem / ca.key        CA propia (ca.key nunca sale de este PC)
  nodo.pem / nodo.key    certificado del nodo para su IP local y su host remoto
  movil.pem / movil.key  certificado de cliente del móvil (va dentro del perfil)
  token / token-ubicacion
  salve-perfil.json      perfil para Salve: direcciones, token, CA y certificado del móvil
  salve-perfil.svg       el mismo perfil como QR (si está instalado el paquete qrcode)

El perfil y su QR contienen la clave del móvil: trátalos como una contraseña. No contacta con nadie.
"""
from __future__ import annotations

import argparse
import base64
import datetime
import ipaddress
import json
import os
from pathlib import Path
import secrets
import socket
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID

DEFAULT_PORT = 8443
PROFILE_VERSION = 2


def _private_write(path: Path, data: bytes):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        path.unlink()
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, 'O_BINARY', 0), 0o600)
    with os.fdopen(fd, 'wb') as handle:
        handle.write(data)


def _pem_key(key) -> bytes:
    return key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())


def _key_usage(signing: bool, ca: bool = False):
    return x509.KeyUsage(digital_signature=signing, content_commitment=False, key_encipherment=False,
                         data_encipherment=False, key_agreement=False, key_cert_sign=ca, crl_sign=ca,
                         encipher_only=False, decipher_only=False)


def make_ca(days=3650):
    """CA con Subject Key Identifier: Python 3.13+ y Android rechazan cadenas sin identificadores."""
    key = ec.generate_private_key(ec.SECP256R1())
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Salve Family CA')])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(minutes=5)).not_valid_after(now + datetime.timedelta(days=days))
            .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
            .add_extension(_key_usage(False, ca=True), critical=True)
            .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False)
            .sign(key, hashes.SHA256()))
    return key, cert


def _leaf(ca_key, ca_cert, common_name, usage, sans, days):
    key = ec.generate_private_key(ec.SECP256R1())
    now = datetime.datetime.now(datetime.timezone.utc)
    builder = (x509.CertificateBuilder()
               .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, common_name)]))
               .issuer_name(ca_cert.subject).public_key(key.public_key()).serial_number(x509.random_serial_number())
               .not_valid_before(now - datetime.timedelta(minutes=5)).not_valid_after(now + datetime.timedelta(days=days))
               .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
               .add_extension(_key_usage(True), critical=True)
               .add_extension(x509.ExtendedKeyUsage([usage]), critical=False)
               .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False)
               .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()), critical=False))
    if sans:
        builder = builder.add_extension(x509.SubjectAlternativeName(sans), critical=False)
    return key, builder.sign(ca_key, hashes.SHA256())


def san_for(host):
    """IP literal → IPAddress; nombre (DDNS) → DNSName."""
    try:
        return x509.IPAddress(ipaddress.ip_address(host))
    except ValueError:
        return x509.DNSName(host)


def issue(ca_key, ca_cert, hosts, days=825, common_name='Salve node'):
    """Certificado de servidor para las IP o nombres por los que el móvil llegará al nodo."""
    unique = list(dict.fromkeys(h for h in hosts if h))
    return _leaf(ca_key, ca_cert, common_name, ExtendedKeyUsageOID.SERVER_AUTH, [san_for(h) for h in unique], days)


def issue_client(ca_key, ca_cert, days=825, common_name='Salve phone'):
    """Certificado de cliente del móvil: con él, y solo con él, el nodo acepta la conexión."""
    return _leaf(ca_key, ca_cert, common_name, ExtendedKeyUsageOID.CLIENT_AUTH, [], days)


def check_host(raw, field):
    """IP o nombre DNS sin esquema, ruta ni puerto."""
    value = (raw or '').strip().lower()
    if not value:
        raise ValueError(f'Falta {field}')
    try:
        address = ipaddress.ip_address(value)
        if address.is_loopback or address.is_unspecified or address.is_multicast or address.is_link_local:
            raise ValueError(f'{field}: usa la dirección por la que el móvil llega a este PC')
        return str(address)
    except ValueError as error:
        if str(error).startswith(field):
            raise
    labels = value.rstrip('.').split('.')
    if len(value) > 253 or len(labels) < 2 or not all(
            label and len(label) <= 63 and label[0] != '-' and label[-1] != '-'
            and all(c.isalnum() or c == '-' for c in label) for label in labels):
        raise ValueError(f'{field}: escribe una IP o un dominio como midominio.duckdns.org')
    return value.rstrip('.')


def check_port(port):
    if not isinstance(port, int) or not 1 <= port <= 65535:
        raise ValueError('El puerto debe estar entre 1 y 65535')
    return port


def endpoint(host, port):
    return f'https://[{host}]:{port}' if ':' in host else f'https://{host}:{port}'


def lan_ip():
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(('192.0.2.1', 9))  # TEST-NET-1: no se envía nada
        return probe.getsockname()[0]
    except OSError:
        return None
    finally:
        probe.close()


def build_profile(name, local_url, remote_url, token, ca_cert, client_cert, client_key):
    """Perfil compacto (DER en base64) para que quepa en un QR."""
    der = lambda cert: base64.b64encode(cert.public_bytes(serialization.Encoding.DER)).decode()
    profile = {'salve': 'nodo', 'v': PROFILE_VERSION, 'n': name, 'u': local_url, 't': token,
               'ca': der(ca_cert), 'c': der(client_cert),
               'k': base64.b64encode(client_key.private_bytes(serialization.Encoding.DER, serialization.PrivateFormat.PKCS8,
                                                               serialization.NoEncryption())).decode()}
    if remote_url:
        profile['r'] = remote_url
    return json.dumps(profile, separators=(',', ':'), ensure_ascii=False)


def write_qr_svg(text, path: Path):
    try:
        import qrcode
        import qrcode.image.svg
    except ImportError:
        return False
    image = qrcode.make(text, image_factory=qrcode.image.svg.SvgPathImage,
                        error_correction=qrcode.constants.ERROR_CORRECT_L, box_size=10, border=4)
    with open(path, 'wb') as handle:
        image.save(handle)
    return True


def create(folder: Path, local_host: str, remote_host: str = None, port: int = DEFAULT_PORT,
           name: str = 'Mi PC', rebuild=False):
    """Genera (o reutiliza) CA, certificados, tokens y perfil. Devuelve (rutas, lo_generado)."""
    folder = folder.expanduser()
    folder.mkdir(parents=True, exist_ok=True)
    if os.name == 'posix':
        folder.chmod(0o700)
    local_host = check_host(local_host, 'Host local')
    remote_host = check_host(remote_host, 'Host remoto') if remote_host else None
    port = check_port(port)
    paths = {n: folder / n for n in ('ca.key', 'ca.pem', 'nodo.key', 'nodo.pem', 'movil.key', 'movil.pem',
                                     'token', 'token-ubicacion', 'salve-perfil.json', 'salve-perfil.svg', 'hosts.json')}
    made = []
    if rebuild or not paths['ca.pem'].exists() or not paths['ca.key'].exists():
        ca_key, ca_cert = make_ca()
        _private_write(paths['ca.key'], _pem_key(ca_key))
        paths['ca.pem'].write_bytes(ca_cert.public_bytes(serialization.Encoding.PEM))
        made.append('CA')
        rebuild = True  # una CA nueva obliga a reemitir todo lo que firma
    else:
        ca_key = serialization.load_pem_private_key(paths['ca.key'].read_bytes(), None)
        ca_cert = x509.load_pem_x509_certificate(paths['ca.pem'].read_bytes())
    hosts = [local_host] + ([remote_host] if remote_host else [])
    previous = json.loads(paths['hosts.json'].read_text()) if paths['hosts.json'].exists() else None
    if rebuild or not paths['nodo.pem'].exists() or previous != hosts:
        key, cert = issue(ca_key, ca_cert, hosts)
        _private_write(paths['nodo.key'], _pem_key(key))
        paths['nodo.pem'].write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        paths['hosts.json'].write_text(json.dumps(hosts))
        made.append('certificado del nodo')
    if rebuild or not paths['movil.pem'].exists():
        key, cert = issue_client(ca_key, ca_cert)
        _private_write(paths['movil.key'], _pem_key(key))
        paths['movil.pem'].write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        made.append('certificado del móvil')
    for token_name in ('token', 'token-ubicacion'):
        if rebuild or not paths[token_name].exists():
            _private_write(paths[token_name], (secrets.token_urlsafe(32) + '\n').encode())
            made.append(token_name)
    client_key = serialization.load_pem_private_key(paths['movil.key'].read_bytes(), None)
    client_cert = x509.load_pem_x509_certificate(paths['movil.pem'].read_bytes())
    profile = build_profile(name, endpoint(local_host, port), endpoint(remote_host, port) if remote_host else None,
                            paths['token'].read_text().strip(), ca_cert, client_cert, client_key)
    _private_write(paths['salve-perfil.json'], profile.encode())
    if paths['salve-perfil.svg'].exists():
        paths['salve-perfil.svg'].unlink()
    write_qr_svg(profile, paths['salve-perfil.svg'])
    return paths, made


def cmd_crear(args):
    try:
        paths, made = create(Path(args.carpeta), args.host_local or lan_ip() or '', args.host_remoto,
                             args.puerto, args.nombre or socket.gethostname() or 'Mi PC', args.rehacer)
    except ValueError as error:
        print(error)
        return 2
    print('\n== Conexión segura de Salve (TLS mutuo) ==\n')
    print('Generado: ' + (', '.join(made) if made else 'nada nuevo (se reutiliza lo existente)'))
    print(f'Carpeta: {paths["ca.pem"].parent}\n')
    if paths['salve-perfil.svg'].exists():
        print(f'1. Abre el QR en pantalla y escanéalo en Salve (Nodos de cómputo → Añadir un nodo → Escanear QR):\n   {paths["salve-perfil.svg"]}')
    else:
        print('1. Instala el paquete qrcode para ver el QR, o pasa al móvil por un medio privado:')
    print(f'   o importa {paths["salve-perfil.json"]} en Salve → Conexión segura → Importar perfil.')
    print('2. Arranca el modelo y el nodo:')
    print('   llama-server -m modelo.gguf --host 127.0.0.1 --port 8080')
    print(f'   python salve_compute_node.py --port {args.puerto} --tls-cert "{paths["nodo.pem"]}" --tls-key "{paths["nodo.key"]}" '
          f'--client-ca "{paths["ca.pem"]}" --token-file "{paths["token"]}" --model <nombre-del-modelo>')
    print(f'3. Permite el puerto TCP {args.puerto} en el cortafuegos. Fuera de casa: reenvía TCP {args.puerto} '
          'del router a este PC y usa --host-remoto con tu IP pública o tu dominio DDNS.')
    print('\nEl perfil y el QR contienen la clave del móvil: no los compartas. ca.key no debe salir de este PC.')
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest='orden', required=True)
    crear = sub.add_parser('crear', help='generar CA, certificados, tokens, perfil y QR')
    crear.add_argument('--host-local', help='IP de este PC en tu red (por defecto, la detectada)')
    crear.add_argument('--host-remoto', help='IP pública o dominio DDNS para conectar fuera de casa')
    crear.add_argument('--puerto', type=int, default=DEFAULT_PORT)
    crear.add_argument('--carpeta', default='~/.salve/red')
    crear.add_argument('--nombre', help='nombre del nodo en la app')
    crear.add_argument('--rehacer', action='store_true', help='generar CA, certificados y tokens nuevos')
    args = parser.parse_args(argv)
    return cmd_crear(args)


if __name__ == '__main__':
    sys.exit(main())
