#!/usr/bin/env python3
"""Hojas «sleep» y «wake» de Salve a partir de tools/avatar-realistic/sleep-wake-frames.

sleep.png (celdas horizontales 672×448, cabeza a la izquierda, sobre la almohada):
  0 D1 dormida de lado · 1 D2 acurrucada · 2 D3 hecha un ovillo · 3 D4 abre los ojos
wake.png (celdas 448×672, de rodillas):
  0 E1 se frota el ojo bostezando · 1 E2 se estira · 2 E3 se estira del todo · 3 E4 bosteza tapándose
  4 E5 se frota el ojo despierta · 5 E6 mano en la mejilla · 6 E7 guiño y saludo

Antes de montar, quita la neblina oscura semitransparente que traen algunas ilustraciones y apaga el
brillo lila lejos del cuerpo, conservando pelo, guantes y botas.
Uso: python3 scripts/build_salve_sleep_wake.py [carpeta de salida]
"""
import hashlib, json, os, sys
import numpy as np
from PIL import Image
from scipy import ndimage as nd

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
SRC = os.path.join(ROOT, "tools", "avatar-realistic", "sleep-wake-frames")
CELL_W, CELL_H = 448, 672
SLEEP = [("D1_dormida_de_lado", "mirror"), ("D2_dormida_acurrucada", "rotate"),
         ("D3_dormida_hecha_un_ovillo", "rotate"), ("D4_abre_los_ojos", "rotate")]
WAKE = ["E1_se_frota_el_ojo_bostezando", "E2_se_estira", "E3_se_estira_del_todo", "E4_bosteza_tapandose",
        "E5_se_frota_el_ojo_despierta", "E6_mano_en_la_mejilla", "E7_guino_y_saludo"]

def disk(r):
    y, x = np.ogrid[-r:r + 1, -r:r + 1]; return x * x + y * y <= r * r

def clean(im):
    a = np.array(im.convert("RGBA")).astype(np.float32)
    rgb, al = a[..., :3], a[..., 3]
    lum = rgb.mean(-1)
    bright = nd.binary_opening((al > 40) & (lum >= 115), disk(2))
    lab, n = nd.label(bright)
    keep = bright
    if n:
        sizes = nd.sum(bright, lab, range(1, n + 1))
        keep = np.isin(lab, 1 + np.where(sizes >= max(400, sizes.max() * .002))[0])
    # La neblina es lisa; guantes y botas tienen textura (escamas, brillos).
    m = nd.uniform_filter(lum, 9); m2 = nd.uniform_filter(lum * lum, 9)
    textured = np.sqrt(np.maximum(m2 - m * m, 0)) > 9
    opaque = (al >= 253) | ((al >= 190) & nd.binary_closing(textured, disk(5)))
    lab2, _ = nd.label(opaque | keep)
    touch = np.unique(lab2[keep]); touch = touch[touch > 0]
    keep = np.isin(lab2, touch) & (opaque | keep)
    fig = nd.binary_dilation(nd.binary_fill_holes(nd.binary_closing(keep, disk(18))), disk(3))
    out = al * np.clip(1 - nd.distance_transform_edt(~fig) / 6, 0, 1)
    out[(lum < 60) & (al < 200) & ~nd.binary_erosion(fig, disk(10))] = 0
    core = nd.binary_opening(out >= 235, disk(2))
    d = nd.distance_transform_edt(~core)
    out = np.where(out >= 235, out, out * np.clip(1 - (d - 4) / 16, 0, 1))
    out[out < 10] = 0  # restos invisibles: fondo limpio del todo
    a[..., 3] = out
    return Image.fromarray(a.clip(0, 255).astype(np.uint8), "RGBA")

def fit(im, w, h):
    scale = min(w / im.width, h / im.height)
    small = im.resize((round(im.width * scale), round(im.height * scale)), Image.LANCZOS)
    cell = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cell.paste(small, ((w - small.width) // 2, h - small.height), small)
    return cell

def load(name):
    return clean(Image.open(os.path.join(SRC, name + ".png")))

def strip(cells, w, h):
    sheet = Image.new("RGBA", (w * len(cells), h), (0, 0, 0, 0))
    frames = []
    for i, cell in enumerate(cells):
        sheet.paste(cell, (i * w, 0)); frames.append([i * w, 0, (i + 1) * w, h])
    return sheet, frames

def main():
    out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(ROOT, "app", "src", "main", "assets", "avatar", "realistic")
    cells = []
    for name, how in SLEEP:
        im = load(name)
        # D1 tiene la cabeza a la derecha; las acurrucadas (vistas desde arriba) se giran 90°: cabeza a la izquierda, derecha y sobre la almohada.
        im = im.transpose(Image.FLIP_LEFT_RIGHT) if how == "mirror" else im.rotate(90, expand=True)
        cells.append(fit(im, CELL_H, CELL_W))
    sleep, sleep_frames = strip(cells, CELL_H, CELL_W)
    wake, wake_frames = strip([fit(load(n), CELL_W, CELL_H) for n in WAKE], CELL_W, CELL_H)
    path = os.path.join(out, "manifest.json")
    manifest = json.load(open(path, encoding="utf-8"))
    for name, sheet, frames, ref in (("sleep", sleep, sleep_frames, CELL_W), ("wake", wake, wake_frames, CELL_H)):
        file = os.path.join(out, name + ".png")
        sheet.save(file, optimize=True)
        manifest["sheets"][name] = {
            "width": sheet.width, "height": sheet.height,
            "sha256": hashlib.sha256(open(file, "rb").read()).hexdigest(),
            "frames": frames, "frameHeights": [ref] * len(frames)}
        print(name, sheet.size, os.path.getsize(file))
    with open(path, "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)

if __name__ == "__main__":
    main()
