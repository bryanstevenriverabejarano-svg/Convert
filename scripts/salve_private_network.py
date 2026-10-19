#!/usr/bin/env python3
"""Prepara la red privada de Salve en este PC, sin servicios de terceros ni openssl.

  python salve_private_network.py crear --endpoint 192.168.1.20

Genera en ~/.salve/red (o --carpeta):
  ca.pem / ca.key          CA propia que firma el HTTPS del nodo (ca.key no sale de aquí)
  nodo.pem / nodo.key      certificado del nodo para su IP del túnel (10.77.0.2)
  wg-nodo-salve.conf       túnel WireGuard de este PC (impórtalo en WireGuard para Windows o wg-quick)
  salve-movil.conf         perfil del móvil: impórtalo en Salve → Red privada junto con ca.pem
  token / token-ubicacion  tokens de consulta y de escritura de ubicación

Todo se genera aquí con la biblioteca `cryptography` (requirements-family.txt). No contacta con
nadie. Si ya existen, se reutilizan; --rehacer crea claves y certificados nuevos.
"""
from __future__ import annotations

import argparse
import base64
import datetime
import ipaddress
import os
from pathlib import Path
import secrets
import socket
import sys

from cryptography import x509
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import ec, x25519
from cryptography.x509.oid import ExtendedKeyUsageOID, NameOID

NODE_IP = '10.77.0.2'
PHONE_IP = '10.77.0.1'
WG_PORT = 51820
NODE_PORT = 8766


def _private_write(path: Path, data: bytes):
    path.parent.mkdir(parents=True, exist_ok=True)
    if path.exists():
        path.unlink()
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_EXCL | getattr(os, 'O_BINARY', 0), 0o600)
    with os.fdopen(fd, 'wb') as handle:
        handle.write(data)


def _pem_key(key) -> bytes:
    return key.private_bytes(serialization.Encoding.PEM, serialization.PrivateFormat.PKCS8, serialization.NoEncryption())


def make_ca(days=3650):
    """CA con Subject Key Identifier: Python 3.13+ y Android rechazan cadenas sin identificadores."""
    key = ec.generate_private_key(ec.SECP256R1())
    name = x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, 'Salve Family CA')])
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder().subject_name(name).issuer_name(name).public_key(key.public_key())
            .serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(minutes=5)).not_valid_after(now + datetime.timedelta(days=days))
            .add_extension(x509.BasicConstraints(ca=True, path_length=0), critical=True)
            .add_extension(x509.KeyUsage(digital_signature=False, content_commitment=False, key_encipherment=False,
                                         data_encipherment=False, key_agreement=False, key_cert_sign=True, crl_sign=True,
                                         encipher_only=False, decipher_only=False), critical=True)
            .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False)
            .sign(key, hashes.SHA256()))
    return key, cert


def issue(ca_key, ca_cert, ips, days=825, common_name='Salve node'):
    """Certificado de servidor para IPs concretas (SAN), con AKI/SKI y uso serverAuth."""
    key = ec.generate_private_key(ec.SECP256R1())
    now = datetime.datetime.now(datetime.timezone.utc)
    cert = (x509.CertificateBuilder()
            .subject_name(x509.Name([x509.NameAttribute(NameOID.COMMON_NAME, common_name)]))
            .issuer_name(ca_cert.subject).public_key(key.public_key()).serial_number(x509.random_serial_number())
            .not_valid_before(now - datetime.timedelta(minutes=5)).not_valid_after(now + datetime.timedelta(days=days))
            .add_extension(x509.SubjectAlternativeName([x509.IPAddress(ipaddress.ip_address(ip)) for ip in ips]), critical=False)
            .add_extension(x509.BasicConstraints(ca=False, path_length=None), critical=True)
            .add_extension(x509.KeyUsage(digital_signature=True, content_commitment=False, key_encipherment=False,
                                         data_encipherment=False, key_agreement=False, key_cert_sign=False, crl_sign=False,
                                         encipher_only=False, decipher_only=False), critical=True)
            .add_extension(x509.ExtendedKeyUsage([ExtendedKeyUsageOID.SERVER_AUTH]), critical=False)
            .add_extension(x509.SubjectKeyIdentifier.from_public_key(key.public_key()), critical=False)
            .add_extension(x509.AuthorityKeyIdentifier.from_issuer_public_key(ca_key.public_key()), critical=False)
            .sign(ca_key, hashes.SHA256()))
    return key, cert


def wg_keypair():
    """Par de claves WireGuard (Curve25519, base64), igual que `wg genkey | wg pubkey`."""
    private = x25519.X25519PrivateKey.generate()
    raw = private.private_bytes(serialization.Encoding.Raw, serialization.PrivateFormat.Raw, serialization.NoEncryption())
    pub = private.public_key().public_bytes(serialization.Encoding.Raw, serialization.PublicFormat.Raw)
    return base64.b64encode(raw).decode(), base64.b64encode(pub).decode()


def node_conf(node_private, phone_public, node_ip=NODE_IP, phone_ip=PHONE_IP, port=WG_PORT):
    return (f'[Interface]\nPrivateKey = {node_private}\nAddress = {node_ip}/32\nListenPort = {port}\n\n'
            f'[Peer]\n# Móvil con Salve\nPublicKey = {phone_public}\nAllowedIPs = {phone_ip}/32\n')


def phone_conf(phone_private, node_public, endpoint, node_ip=NODE_IP, phone_ip=PHONE_IP, port=WG_PORT):
    """Perfil que acepta Salve: una dirección /32, rutas /32 privadas, endpoint IPv4 numérico y sin DNS."""
    return (f'[Interface]\nPrivateKey = {phone_private}\nAddress = {phone_ip}/32\n\n'
            f'[Peer]\nPublicKey = {node_public}\nAllowedIPs = {node_ip}/32\nEndpoint = {endpoint}:{port}\n'
            'PersistentKeepalive = 25\n')


def check_endpoint(raw):
    try:
        address = ipaddress.IPv4Address(raw)
    except ValueError:
        raise ValueError('El endpoint debe ser una IPv4 numérica (la de tu PC en casa o la pública de tu router)') from None
    if address.is_loopback or address.is_unspecified or address.is_multicast:
        raise ValueError('Usa la IPv4 por la que el móvil llegará a este PC')
    return str(address)


def lan_ip():
    probe = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        probe.connect(('192.0.2.1', 9))  # TEST-NET-1: no se envía nada
        return probe.getsockname()[0]
    except OSError:
        return None
    finally:
        probe.close()


def create(folder: Path, endpoint: str, rebuild=False):
    folder = folder.expanduser()
    folder.mkdir(parents=True, exist_ok=True)
    if os.name == 'posix':
        folder.chmod(0o700)
    paths = {name: folder / name for name in ('ca.key', 'ca.pem', 'nodo.key', 'nodo.pem', 'wg-nodo-salve.conf',
                                              'salve-movil.conf', 'token', 'token-ubicacion')}
    made = []
    if rebuild or not paths['ca.pem'].exists() or not paths['ca.key'].exists():
        ca_key, ca_cert = make_ca()
        _private_write(paths['ca.key'], _pem_key(ca_key))
        paths['ca.pem'].write_bytes(ca_cert.public_bytes(serialization.Encoding.PEM))
        made.append('CA')
        rebuild = True  # una CA nueva obliga a reemitir el certificado del nodo
    else:
        ca_key = serialization.load_pem_private_key(paths['ca.key'].read_bytes(), None)
        ca_cert = x509.load_pem_x509_certificate(paths['ca.pem'].read_bytes())
    if rebuild or not paths['nodo.pem'].exists():
        key, cert = issue(ca_key, ca_cert, [NODE_IP])
        _private_write(paths['nodo.key'], _pem_key(key))
        paths['nodo.pem'].write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        made.append('certificado del nodo')
    if rebuild or not paths['wg-nodo-salve.conf'].exists() or not paths['salve-movil.conf'].exists():
        node_private, node_public = wg_keypair()
        phone_private, phone_public = wg_keypair()
        _private_write(paths['wg-nodo-salve.conf'], node_conf(node_private, phone_public).encode())
        _private_write(paths['salve-movil.conf'], phone_conf(phone_private, node_public, endpoint).encode())
        made.append('claves WireGuard')
    for name in ('token', 'token-ubicacion'):
        if rebuild or not paths[name].exists():
            _private_write(paths[name], (secrets.token_urlsafe(32) + '\n').encode())
            made.append(name)
    return paths, made


def cmd_crear(args):
    try:
        endpoint = check_endpoint(args.endpoint or lan_ip() or '')
    except ValueError as error:
        print(error)
        return 2
    paths, made = create(Path(args.carpeta), endpoint, args.rehacer)
    print('\n== Red privada de Salve ==\n')
    print('Generado: ' + (', '.join(made) if made else 'nada nuevo (se reutiliza lo existente)'))
    print(f'Carpeta: {paths["ca.pem"].parent}\n')
    print('1. En este PC, importa el túnel en WireGuard (WireGuard para Windows → Importar túnel):')
    print(f'   {paths["wg-nodo-salve.conf"]}')
    print(f'   Abre en el cortafuegos UDP {WG_PORT}. Fuera de casa: reenvía UDP {WG_PORT} del router a este PC.')
    print('2. Pasa al móvil, por cable o un medio privado, estos dos archivos y bórralos de donde los copies:')
    print(f'   {paths["salve-movil.conf"]}   y   {paths["ca.pem"]}')
    print('   En Salve: Nodos de cómputo → Red privada (VPN) → Configurar VPN → Importar .conf / Importar CA → Conectar.')
    print('3. Arranca el modelo y el nodo:')
    print('   llama-server -m modelo.gguf --host 127.0.0.1 --port 8080')
    print(f'   python salve_compute_node.py --bind {NODE_IP} --allow-client {PHONE_IP} '
          f'--tls-cert "{paths["nodo.pem"]}" --tls-key "{paths["nodo.key"]}" --token-file "{paths["token"]}" '
          f'--location-directory "{paths["ca.pem"].parent / "ubicacion"}" --location-token-file "{paths["token-ubicacion"]}" '
          '--model <nombre-del-modelo>' + (' --interface wg-nodo-salve' if sys.platform.startswith('linux') else ''))
    print('4. Registra el nodo en Salve con el QR (Nodos de cómputo → Añadir un nodo → Escanear QR):')
    try:
        from salve_node_setup import pairing_uri, print_qr
        print_qr(pairing_uri(args.nombre or socket.gethostname() or 'Mi PC', f'https://{NODE_IP}:{NODE_PORT}',
                             paths['token'].read_text().strip()))
    except Exception:
        print(f'   URL: https://{NODE_IP}:{NODE_PORT}   Token: {paths["token"]}')
    print('\nca.key firma tus certificados: no la copies a ningún otro sitio.')
    return 0


def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    sub = parser.add_subparsers(dest='orden', required=True)
    crear = sub.add_parser('crear', help='generar CA, certificado, túnel WireGuard, perfil del móvil y tokens')
    crear.add_argument('--endpoint', help='IPv4 por la que el móvil llega a este PC (por defecto, su IP local)')
    crear.add_argument('--carpeta', default='~/.salve/red')
    crear.add_argument('--nombre', help='nombre del nodo en la app')
    crear.add_argument('--rehacer', action='store_true', help='generar claves y certificados nuevos')
    args = parser.parse_args(argv)
    return cmd_crear(args)


if __name__ == '__main__':
    sys.exit(main())
