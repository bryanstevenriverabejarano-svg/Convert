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
  if 'mouthBox' in rig:
    x0, y0, x1, y1 = rig['mouthBox']
    # Tres bocas (H1-H3) viven en astral_mouth_h123.png; las versiones de dos, en astral_mouth.png.
    mouth_file = 'astral_mouth_h123.png' if len(rig['mouthFrames']) == 3 else 'astral_mouth.png'
    with Image.open(ASSETS / mouth_file) as mouth:
      # PNG RGBA o con paleta y transparencia: Android los decodifica con alfa igual.
      alpha = mouth.convert('RGBA').getchannel('A').getextrema()
      assert mouth.size == (x1 - x0, (y1 - y0) * len(rig['mouthFrames'])), (body, 'mouth frames')
      assert alpha[0] < 255, (body, 'mouth frames without transparency')
  for name, spec in manifest['sheets'].items():
      path = ASSETS / (name + '.png')
      assert hashlib.sha256(path.read_bytes()).hexdigest() == spec['sha256'], name
      with Image.open(path) as image:
          image.load()
          assert image.mode == 'RGBA', name
          assert image.size == (spec['width'], spec['height']), name
          alpha = image.getchannel('A')
          histogram = alpha.histogram()
          # Las ilustraciones de rodillas (wake) llenan su celda con el pelo: 25 % de fondo transparente basta
          # para distinguir un sprite recortado de una imagen con fondo.
          assert histogram[0] > image.width * image.height * .25, (name, 'background is not transparent')
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
