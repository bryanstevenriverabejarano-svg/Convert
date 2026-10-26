#!/usr/bin/env python3
"""Verify the actual packaged RGBA pixels and the renderer's frame regions."""
import hashlib
import json
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
# avatar/realistic = Salve actual; avatar/realistic_v1 = su primer cuerpo, conservado en el Vestuario.
BODIES = ('realistic', 'realistic_v1')
total = 0
for body in BODIES:
  ASSETS = ROOT / 'app/src/main/assets/avatar' / body
  manifest = json.loads((ASSETS / 'manifest.json').read_text())
  assert 'base' in manifest['sheets'], (body, 'missing base sheet')
  for extra in ('astral.json', 'astral_eyes.png', 'astral_glow.png'):
    assert (ASSETS / extra).is_file(), (body, extra)
  rig = json.loads((ASSETS / 'astral.json').read_text())
  with Image.open(ASSETS / 'base.png') as base_image:
    assert rig['size'] == list(base_image.size), (body, 'astral rig does not match base.png')
  for name, spec in manifest['sheets'].items():
      path = ASSETS / (name + '.png')
      assert hashlib.sha256(path.read_bytes()).hexdigest() == spec['sha256'], name
      with Image.open(path) as image:
          image.load()
          assert image.mode == 'RGBA', name
          assert image.size == (spec['width'], spec['height']), name
          alpha = image.getchannel('A')
          histogram = alpha.histogram()
          assert histogram[0] > image.width * image.height * .30, (name, 'background is not transparent')
          assert sum(histogram[240:]) > image.width * image.height * .10, (name, 'missing opaque character')
          for index, (left, top, right, bottom) in enumerate(spec['frames']):
              assert 0 <= left < right <= image.width and 0 <= top < bottom <= image.height
              assert spec['frameHeights'][index] > 0
              region = alpha.crop((left, top, right, bottom))
              assert region.getextrema()[1] >= 240, (name, index, 'empty frame')
          total += path.stat().st_size
          print(f'{body}/{name}: RGBA {image.width}x{image.height}, {len(spec["frames"])} valid frames')
assert not (ROOT / 'app/src/main/assets/avatar3d').exists(), 'Retired 3D viewer is still bundled'
assert 'PrepareSalve3dAssets' not in (ROOT / 'app/build.gradle').read_text()
print(f'PASS: transparent sprites, source rectangles and SHA-256; {total / 1048576:.2f} MiB total')
