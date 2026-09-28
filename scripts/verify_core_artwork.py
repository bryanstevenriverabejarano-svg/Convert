"""Validate actual packaged core artwork with stdlib PNG headers, hashes and catalog integration."""
import hashlib
import json
import re
import struct
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'app/src/main/assets'
manifest = json.loads((ASSETS / 'avatar/core/manifest.json').read_text())
assert manifest['representation'] == '2d_multiview'
assert len(manifest['views']) == 20
seen = set()
java = (ROOT / 'app/src/main/java/salve/avatar/CoreViewCatalog.java').read_text()
for entry in manifest['views']:
    assert entry['id'] not in seen
    seen.add(entry['id'])
    path = ASSETS / entry['path']
    data = path.read_bytes()
    assert data[:8] == b'\x89PNG\r\n\x1a\n'
    width, height = struct.unpack('>II', data[16:24])
    assert (width, height) == (entry['width'], entry['height'])
    assert data[25] == 6, 'Expected RGBA originals'
    assert hashlib.sha256(data).hexdigest() == entry['sha256']
    expected = f'new Entry("{entry["id"]}", "{entry["label"]}", "{entry["group"]}", {width}, {height})'
    assert expected in java
front = (ASSETS / 'avatar/core/front.png').read_bytes()
assert front == (ROOT / 'app/src/main/res/drawable/salve_imagen.png').read_bytes()
rig = json.loads((ASSETS / 'avatar/core/rig.json').read_text())
assert rig['sourceSha256'] == hashlib.sha256(front).hexdigest()
assert rig['profile'] == 'core'
for point in [rig[k] for k in ('leftEye','rightEye','mouth','headPivot','bodyPivot')] + list(rig['joints'].values()):
    assert 0 <= point[0] < rig['width'] and 0 <= point[1] < rig['height']
print('PASS: 20 original RGBA views, dimensions, hashes, Java catalog, base portrait and core anchors')
