#!/usr/bin/env python3
"""Salve Nodo: el nodo de Salve con doble clic (también empaquetado como SalveNodo.exe).

La primera vez pregunta lo imprescindible (nombre, dirección remota opcional, puerto, ubicación
fija opcional y modelo), crea la CA, los certificados y el perfil para Salve, y abre el QR para
escanearlo desde la app. Las siguientes veces arranca el modelo (si lo configuraste) y el nodo.

  SalveNodo.exe                 arrancar (o configurar la primera vez)
  SalveNodo.exe --reconfigurar  volver a responder las preguntas
  SalveNodo.exe --qr            volver a abrir el QR del perfil
  SalveNodo.exe --autoprueba    comprobar certificados y TLS mutuo sin tocar tu configuración
"""
from __future__ import annotations

import argparse
import getpass
import http.client
import json
import os
from pathlib import Path
import shutil
import socket
import ssl
import subprocess
import sys
import tempfile
import threading
import time
import webbrowser

import salve_compute_node as node
import salve_node_setup as hardware
import salve_private_network as network
import salve_home as home

VERSION = '1.0'
FOLDER = Path('~/.salve/red').expanduser()
CONFIG = FOLDER / 'nodo.json'
BACKEND = 'http://127.0.0.1:8080'


def ask(question, default=''):
    suffix = f' [{default}]' if default else ''
    answer = input(f'{question}{suffix}: ').strip()
    return answer or default


def ask_float(question):
    while True:
        raw = ask(question).replace(',', '.')
        if not raw:
            return None
        try:
            return float(raw)
        except ValueError:
            print('  Escribe un número, por ejemplo 42.8467')


def recommend():
    total, available = hardware.ram_gb()
    cards = hardware.gpus()
    best_gpu = max((c['free_gb'] for c in cards if c['free_gb']), default=None)
    tier, budget, where = hardware.recommend(available, best_gpu)
    if total:
        print(f'  RAM: {total:.1f} GB · ' + (', '.join(c['name'] for c in cards) or 'sin GPU con aceleración'))
    if tier:
        print(f'  Recomendado: {tier[2]} en GGUF Q4_K_M (~{tier[1]:.0f} GB), en {where}.')


def find_llama():
    here = Path(sys.executable if getattr(sys, 'frozen', False) else __file__).resolve().parent
    for candidate in (here / 'llama-server.exe', here / 'llama-server'):
        if candidate.exists():
            return str(candidate)
    return shutil.which('llama-server') or ''


def configure(previous=None):
    previous = previous or {}
    print('\n== Configurar Salve Nodo ==')
    print('Pulsa Enter para aceptar lo que aparece entre corchetes.\n')
    recommend()
    config = {
        'name': ask('Nombre del nodo en Salve', previous.get('name') or socket.gethostname() or 'Mi PC'),
        'local_host': ask('IP de este PC en tu red', previous.get('local_host') or network.lan_ip() or ''),
        'remote_host': ask('IP pública o dominio DDNS para fuera de casa (Enter si no)', previous.get('remote_host', '')),
        'port': int(ask('Puerto', str(previous.get('port', network.DEFAULT_PORT)))),
    }
    print('\nUbicación fija que verá Salve en la ficha (Enter en latitud para no compartirla).')
    lat = ask_float('Latitud')
    if lat is not None:
        lon = None
        while lon is None:
            lon = ask_float('Longitud')
        config['location'] = {'lat': lat, 'lon': lon, 'city': ask('Ciudad'), 'country': ask('País'), 'zone': ask('Zona (opcional)')}
    print('\nGuardián: este PC puede guardar copias de los Salventes cifradas en el móvil con una frase que solo')
    print('conoces tú. El PC no puede leerlas; sirven para recuperarlos si pierdes o cambias de móvil.')
    previous_guardian = 's' if previous.get('guardian') else 'n'
    config['guardian'] = ask('¿Usar este PC también como Guardián? (s/n)', previous_guardian).strip().lower().startswith('s')
    print('\nHogar: Salve puede encender o apagar luces, enchufes, ventiladores, persianas y el clima a través de tu')
    print('Home Assistant local (siempre pidiéndote confirmación). El token se queda en este PC.')
    previous_home = previous.get('home') or {}
    if ask('¿Conectar con Home Assistant? (s/n)', 's' if previous_home else 'n').strip().lower().startswith('s'):
        url = ask('URL de Home Assistant en tu red', previous_home.get('url', 'http://homeassistant.local:8123'))
        home.check_url(url)
        token = getpass.getpass('Token de larga duración (Perfil → Seguridad en Home Assistant; Enter para mantener el actual): ').strip()
        if token:
            FOLDER.mkdir(parents=True, exist_ok=True)
            network._private_write(FOLDER / 'ha-token', token.encode('utf-8'))
        elif not (FOLDER / 'ha-token').exists():
            raise ValueError('Falta el token de Home Assistant')
        entities = ask('Entidades que Salve puede controlar (light.salon,switch.enchufe…)', ','.join(previous_home.get('entities', [])))
        config['home'] = {'url': url, 'entities': list(home.parse_entities(entities))}
    print('\nModelo. Si indicas el archivo .gguf, Salve Nodo arrancará llama-server por ti.')
    model_file = ask('Ruta del modelo .gguf (Enter si ya tienes llama-server en marcha)', previous.get('model_file', ''))
    config['model_file'] = model_file.strip('"')
    if config['model_file']:
        config['llama_server'] = ask('Ruta de llama-server', previous.get('llama_server') or find_llama()).strip('"')
    config['model'] = ask('Nombre del modelo', previous.get('model') or (Path(config['model_file']).stem if config['model_file'] else 'modelo'))
    network.check_host(config['local_host'], 'IP local')
    if config['remote_host']:
        network.check_host(config['remote_host'], 'Host remoto')
    network.check_port(config['port'])
    FOLDER.mkdir(parents=True, exist_ok=True)
    CONFIG.write_text(json.dumps(config, indent=2, ensure_ascii=False), encoding='utf-8')
    return config


def prepare(config):
    paths, made = network.create(FOLDER, config['local_host'], config.get('remote_host') or None,
                                 config['port'], config['name'])
    if made:
        print('Generado: ' + ', '.join(made))
    return paths


def open_qr(paths):
    target = paths['salve-perfil.svg'] if paths['salve-perfil.svg'].exists() else paths['salve-perfil.json']
    print(f'\nEscanea el QR en Salve: Nodos de cómputo → Añadir un nodo → Escanear QR\n  {target}')
    print('El QR contiene la clave del móvil: ciérralo cuando termines y no lo compartas.')
    try:
        if hasattr(os, 'startfile'):
            os.startfile(str(target))  # Windows: lo abre el navegador o el visor
        else:
            webbrowser.open(target.as_uri())
    except OSError:
        pass


def start_llama(config):
    if not config.get('model_file'):
        return None
    llama = config.get('llama_server') or find_llama()
    if not llama or not Path(llama).exists():
        print('No encuentro llama-server. Ponlo junto a SalveNodo.exe o vuelve a configurar con --reconfigurar.')
        return None
    print('Arrancando el modelo (solo en este PC, 127.0.0.1:8080)…')
    process = subprocess.Popen([llama, '-m', config['model_file'], '--host', '127.0.0.1', '--port', '8080',
                                '--alias', config['model']])
    deadline = time.time() + 180
    while time.time() < deadline and process.poll() is None:
        try:
            connection = http.client.HTTPConnection('127.0.0.1', 8080, timeout=2)
            connection.request('GET', '/health')
            if connection.getresponse().status == 200:
                print('Modelo listo.')
                return process
        except OSError:
            pass
        time.sleep(2)
    print('El modelo aún no responde; el nodo arranca igualmente y dirá «no listo» hasta que cargue.')
    return process


def node_args(config, paths):
    args = ['--port', str(config['port']), '--tls-cert', str(paths['nodo.pem']), '--tls-key', str(paths['nodo.key']),
            '--client-ca', str(paths['ca.pem']), '--token-file', str(paths['token']), '--model', config['model'],
            '--backend', BACKEND]
    location = config.get('location')
    if location:
        args += ['--lat', str(location['lat']), '--lon', str(location['lon']), '--ciudad', location.get('city', ''),
                 '--pais', location.get('country', ''), '--zona', location.get('zone', '')]
    else:
        args += ['--location-directory', str(FOLDER / 'ubicacion'), '--location-token-file', str(paths['token-ubicacion'])]
    if config.get('guardian'):
        args += ['--vault-directory', str(FOLDER / 'guardian')]
    if config.get('home'):
        args += ['--ha-url', config['home']['url'], '--ha-token-file', str(FOLDER / 'ha-token'),
                 '--ha-entities', ','.join(config['home']['entities'])]
    return args


def self_test():
    """Crea una red temporal y hace una petición real con TLS mutuo: valida el empaquetado."""
    from unittest.mock import Mock
    with tempfile.TemporaryDirectory() as folder:
        paths, _ = network.create(Path(folder), '192.168.1.20', 'prueba.duckdns.org', 8443, 'Prueba')
        # Certificado de servidor para 127.0.0.1 firmado por la misma CA, solo para la prueba local.
        from cryptography import x509
        from cryptography.hazmat.primitives import serialization
        ca_key = serialization.load_pem_private_key(paths['ca.key'].read_bytes(), None)
        ca = x509.load_pem_x509_certificate(paths['ca.pem'].read_bytes())
        key, cert = network.issue(ca_key, ca, ['127.0.0.1'])
        (Path(folder) / 'local.pem').write_bytes(cert.public_bytes(serialization.Encoding.PEM))
        network._private_write(Path(folder) / 'local.key', network._pem_key(key))
        tls = node.server_tls(Path(folder) / 'local.pem', Path(folder) / 'local.key', paths['ca.pem'])
        token = paths['token'].read_text().strip()
        from salve_guardian import MAGIC, BlindVault
        app = node.ComputeApplication(token, Mock(model='prueba', ready=Mock(return_value=True)),
                                      location_store=node.StaticLocation(42.85, -2.67, 'Vitoria-Gasteiz', 'España'),
                                      vault=BlindVault(Path(folder) / 'guardian'))
        server = node.NodeServer(('127.0.0.1', 0), app, tls=tls)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        try:
            client = ssl.create_default_context(cafile=str(paths['ca.pem']))
            client.load_cert_chain(paths['movil.pem'], paths['movil.key'])
            connection = http.client.HTTPSConnection('127.0.0.1', server.server_address[1], context=client, timeout=5)
            connection.request('GET', '/v1/family/status', headers={'Authorization': 'Bearer ' + token})
            body = json.loads(connection.getresponse().read())
            assert body['location']['city'] == 'Vitoria-Gasteiz', body
            assert body['roles'] == ['inferencia', 'guardian'], body
            blob = MAGIC + os.urandom(400)
            upload = http.client.HTTPSConnection('127.0.0.1', server.server_address[1], context=client, timeout=5)
            upload.request('POST', '/v1/vault/autoprueba', body=blob, headers={
                'Authorization': 'Bearer ' + token, 'Content-Type': 'application/octet-stream', 'Content-Length': str(len(blob))})
            assert upload.getresponse().status == 200
            download = http.client.HTTPSConnection('127.0.0.1', server.server_address[1], context=client, timeout=5)
            download.request('GET', '/v1/vault/autoprueba', headers={'Authorization': 'Bearer ' + token})
            assert download.getresponse().read() == blob, 'el Guardián no devolvió la copia intacta'
            anonymous = ssl.create_default_context(cafile=str(paths['ca.pem']))
            try:
                plain = http.client.HTTPSConnection('127.0.0.1', server.server_address[1], context=anonymous, timeout=5)
                plain.request('GET', '/v1/family/status', headers={'Authorization': 'Bearer ' + token})
                plain.getresponse().read()
                raise AssertionError('aceptó un cliente sin certificado')
            except (ssl.SSLError, OSError, http.client.HTTPException):
                pass
        finally:
            server.shutdown(); server.server_close()
    print('Autoprueba correcta: certificados, perfil, QR, TLS mutuo y Guardián funcionan en este equipo.')
    return 0


def run(argv=None):
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument('--reconfigurar', action='store_true')
    parser.add_argument('--qr', action='store_true')
    parser.add_argument('--autoprueba', action='store_true')
    parser.add_argument('--version', action='version', version=f'Salve Nodo {VERSION}')
    args = parser.parse_args(argv)
    if args.autoprueba:
        return self_test()
    print(f'Salve Nodo {VERSION}')
    previous = json.loads(CONFIG.read_text(encoding='utf-8')) if CONFIG.exists() else None
    first_time = previous is None or args.reconfigurar
    config = configure(previous) if first_time else previous
    paths = prepare(config)
    if first_time or args.qr:
        open_qr(paths)
        if args.qr:
            return 0
    llama = start_llama(config)
    print(f'\nSalve Nodo en marcha en el puerto {config["port"]}. Deja esta ventana abierta; ciérrala para pararlo.')
    if config.get('guardian'):
        print(f'Guardián activo: las copias selladas se guardan en {FOLDER / "guardian"} (este PC no puede leerlas).')
    print(f'Si Windows pregunta por el cortafuegos, permite el acceso en redes privadas. Fuera de casa: '
          f'reenvía TCP {config["port"]} del router a este PC.')
    try:
        node.main(node_args(config, paths))
    finally:
        if llama and llama.poll() is None:
            llama.terminate()
    return 0


def main():
    try:
        code = run()
    except KeyboardInterrupt:
        code = 0
    except Exception as error:  # una ventana de doble clic no debe cerrarse sin explicar qué pasó
        print(f'\nNo se pudo arrancar Salve Nodo: {error}')
        code = 1
    if getattr(sys, 'frozen', False) and code != 0:
        input('\nPulsa Enter para cerrar…')
    return code


if __name__ == '__main__':
    sys.exit(main())
