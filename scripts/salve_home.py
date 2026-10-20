#!/usr/bin/env python3
"""Pasarela local entre Salve y Home Assistant, sin nubes.

El token de Home Assistant vive solo en este PC; el móvil nunca lo ve. Salve solo puede tocar las
entidades que Bryan ha permitido una por una, y en cada tipo solo unas acciones concretas con
valores acotados. Home Assistant ya habla Zigbee, Matter y MQTT: esta pasarela no reimplementa esos
protocolos, usa su API REST local.
"""
from __future__ import annotations

import http.client
import ipaddress
import json
import re
import threading
import time
from urllib.parse import urlsplit

ENTITY = re.compile(r'(light|switch|fan|cover|climate|scene)\.[a-z0-9_]{1,64}')
SERVICES = {
    'light': {'turn_on', 'turn_off', 'toggle'},
    'switch': {'turn_on', 'turn_off', 'toggle'},
    'fan': {'turn_on', 'turn_off'},
    'cover': {'open_cover', 'close_cover', 'stop_cover'},
    'climate': {'set_temperature', 'turn_on', 'turn_off'},
    'scene': {'turn_on'},
}
ATTRIBUTES = ('friendly_name', 'brightness', 'current_temperature', 'temperature', 'current_position')
MAX_ACTIONS_PER_MINUTE = 30


class HomeError(ValueError):
    def __init__(self, code):
        super().__init__(code)
        self.code = code


def check_url(raw):
    """Home Assistant debe estar en la red local: IP privada, loopback o nombre .local / sin punto."""
    parts = urlsplit(raw or '')
    if parts.scheme not in ('http', 'https') or not parts.hostname or parts.username or parts.query or parts.fragment \
            or parts.path not in ('', '/'):
        raise ValueError('URL de Home Assistant no válida (ej.: http://192.168.1.10:8123)')
    host = parts.hostname
    try:
        address = ipaddress.ip_address(host)
        if not (address.is_private or address.is_loopback) or address.is_link_local or address.is_multicast:
            raise ValueError('Home Assistant debe estar en tu red local')
    except ValueError as not_ip:
        if 'red local' in str(not_ip):
            raise
        if not (host.endswith('.local') or host.endswith('.lan') or host.endswith('.home.arpa') or '.' not in host):
            raise ValueError('Home Assistant debe estar en tu red local (IP privada o nombre .local)')
    return parts.scheme, host, parts.port or (443 if parts.scheme == 'https' else 8123)


def parse_entities(raw):
    items = [e.strip() for e in (raw or '').replace('\n', ',').split(',') if e.strip()]
    for e in items:
        if not ENTITY.fullmatch(e):
            raise ValueError(f'Entidad no admitida: {e} (tipos: luces, enchufes, ventiladores, persianas, clima, escenas)')
    if not items or len(items) > 64:
        raise ValueError('Indica de 1 a 64 entidades permitidas')
    return tuple(dict.fromkeys(items))


class HomeBridge:
    def __init__(self, url, token, entities, *, timeout=5, clock=time.monotonic, connector=None):
        self.scheme, self.host, self.port = check_url(url)
        if not isinstance(token, str) or not 20 <= len(token) <= 1024 or any(c.isspace() for c in token):
            raise ValueError('Token de Home Assistant no válido')
        self.token, self.entities, self.timeout, self.clock = token, parse_entities(','.join(entities)), timeout, clock
        self.connector = connector
        self.recent, self.lock = [], threading.Lock()

    def _request(self, method, path, body=None):
        if self.connector is not None:
            connection = self.connector()
        elif self.scheme == 'https':
            connection = http.client.HTTPSConnection(self.host, self.port, timeout=self.timeout)
        else:
            connection = http.client.HTTPConnection(self.host, self.port, timeout=self.timeout)
        try:
            payload = None if body is None else json.dumps(body).encode()
            headers = {'Authorization': 'Bearer ' + self.token, 'Content-Type': 'application/json'}
            connection.request(method, path, body=payload, headers=headers)
            response = connection.getresponse()
            data = response.read(256 * 1024)
            if response.status in (401, 403):
                raise HomeError('home_token_rejected')
            if response.status == 404:
                raise HomeError('home_entity_unknown')
            if response.status >= 400:
                raise HomeError('home_failed')
            return json.loads(data or b'null')
        except (OSError, http.client.HTTPException):
            raise HomeError('home_unreachable')
        except json.JSONDecodeError:
            raise HomeError('home_failed')
        finally:
            connection.close()

    @staticmethod
    def _view(entity, state):
        attributes = state.get('attributes', {}) if isinstance(state, dict) else {}
        kept = {k: attributes[k] for k in ATTRIBUTES if k in attributes and isinstance(attributes[k], (str, int, float))}
        name = str(kept.pop('friendly_name', entity))[:80]
        return {'entity': entity, 'domain': entity.split('.')[0], 'name': name,
                'state': str(state.get('state', 'unknown'))[:40] if isinstance(state, dict) else 'unknown',
                'attributes': kept, 'services': sorted(SERVICES[entity.split('.')[0]])}

    def states(self):
        out = []
        for entity in self.entities:
            try:
                out.append(self._view(entity, self._request('GET', f'/api/states/{entity}')))
            except HomeError as failure:
                if failure.code in ('home_token_rejected', 'home_unreachable'):
                    raise
                out.append({'entity': entity, 'domain': entity.split('.')[0], 'name': entity, 'state': 'unavailable',
                            'attributes': {}, 'services': sorted(SERVICES[entity.split('.')[0]])})
        return out

    def act(self, entity, service, value=None):
        if entity not in self.entities:
            raise HomeError('entity_not_allowed')
        domain = entity.split('.')[0]
        if service not in SERVICES[domain]:
            raise HomeError('service_not_allowed')
        data = {'entity_id': entity}
        if value is not None:
            if type(value) not in (int, float) or value != value:
                raise HomeError('invalid_value')
            if domain == 'light' and service == 'turn_on' and 1 <= value <= 100:
                data['brightness_pct'] = int(round(value))
            elif domain == 'climate' and service == 'set_temperature' and 5 <= value <= 35:
                data['temperature'] = round(float(value) * 2) / 2
            else:
                raise HomeError('invalid_value')
        elif domain == 'climate' and service == 'set_temperature':
            raise HomeError('invalid_value')
        with self.lock:
            now = self.clock()
            self.recent = [t for t in self.recent if now - t < 60]
            if len(self.recent) >= MAX_ACTIONS_PER_MINUTE:
                raise HomeError('too_many_actions')
            self.recent.append(now)
        self._request('POST', f'/api/services/{domain}/{service}', data)
        try:
            return self._view(entity, self._request('GET', f'/api/states/{entity}'))
        except HomeError:
            return {'entity': entity, 'domain': domain, 'name': entity, 'state': 'unknown', 'attributes': {},
                    'services': sorted(SERVICES[domain])}
