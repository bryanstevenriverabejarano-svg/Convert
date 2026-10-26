#!/usr/bin/env python3
"""Monta las hojas de animación de Salve a partir de las 32 ilustraciones aprobadas (1024×1536, RGBA).

Orden de las imágenes (01..32), el mismo que usan las hojas de la v1:
  01-08 vistas giratorias (frente, 3/4, perfil, 3/4 espalda, espalda, 3/4 espalda, perfil, 3/4)
  09-16 andar · 17-24 baile
  25-32 posturas: sentada, de rodillas, agachada, tumbada, saludo, explicar, escuchar, ojos cerrados
La 01 es además base.png (cuerpo vivo: párpados, pelo, cabeza y luz por las líneas).

Uso: python3 scripts/build_salve_sheets.py <carpeta con 01.png..32.png> [carpeta de salida]
Las imágenes originales no se guardan en el repositorio (≈ 60 MB); sí las hojas resultantes.
"""
import hashlib, json, os, sys
from PIL import Image

CELL_W, CELL_H = 448, 672           # misma proporción 2:3 que los originales
NAMES = ("views", "walk", "dance", "poses")

def load(src, n):
    im = Image.open(os.path.join(src, "%02d.png" % n)).convert("RGBA")
    return im

def fit(im, w, h):
    """Escala sin deformar y centra abajo (los pies tocan el suelo de la celda)."""
    scale = min(w / im.width, h / im.height)
    small = im.resize((round(im.width * scale), round(im.height * scale)), Image.LANCZOS)
    cell = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    cell.paste(small, ((w - small.width) // 2, h - small.height), small)
    return cell

def grid_sheet(src, first):
    sheet = Image.new("RGBA", (CELL_W * 4, CELL_H * 2), (0, 0, 0, 0))
    frames = []
    for i in range(8):
        x, y = (i % 4) * CELL_W, (i // 4) * CELL_H
        sheet.paste(fit(load(src, first + i), CELL_W, CELL_H), (x, y))
        frames.append([x, y, x + CELL_W, y + CELL_H])
    return sheet, frames, [CELL_H] * 8

def poses_sheet(src):
    # Fila 1: sentada, de rodillas, agachada (celdas verticales) y tumbada (celda horizontal).
    # Fila 2: saludo, explicar, escuchar, ojos cerrados.
    lying_w, lying_h = CELL_H, CELL_W
    sheet = Image.new("RGBA", (CELL_W * 3 + lying_w, CELL_H * 2), (0, 0, 0, 0))
    frames = []
    for i in range(3):
        x = i * CELL_W
        sheet.paste(fit(load(src, 25 + i), CELL_W, CELL_H), (x, 0))
        frames.append([x, 0, x + CELL_W, CELL_H])
    x = CELL_W * 3
    sheet.paste(fit(load(src, 28), lying_w, lying_h), (x, CELL_H - lying_h))
    frames.append([x, CELL_H - lying_h, x + lying_w, CELL_H])
    for i in range(4):
        x = i * CELL_W
        sheet.paste(fit(load(src, 29 + i), CELL_W, CELL_H), (x, CELL_H))
        frames.append([x, CELL_H, x + CELL_W, CELL_H * 2])
    # Altura de referencia común: la tumbada se dibuja a su escala real, no estirada.
    return sheet, frames, [CELL_H] * 8

def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    src = sys.argv[1]
    out = sys.argv[2] if len(sys.argv) > 2 else os.path.join(
        os.path.dirname(os.path.abspath(__file__)), "..", "app", "src", "main", "assets", "avatar", "realistic")
    os.makedirs(out, exist_ok=True)
    sheets = {}
    base = load(src, 1)
    base.save(os.path.join(out, "base.png"), optimize=True)
    sheets["base"] = (None, [[0, 0, base.width, base.height]], [base.height])
    for name, first in zip(NAMES, (1, 9, 17)):
        sheet, frames, heights = grid_sheet(src, first)
        sheet.save(os.path.join(out, name + ".png"), optimize=True)
        sheets[name] = (sheet, frames, heights)
    sheet, frames, heights = poses_sheet(src)
    sheet.save(os.path.join(out, "poses.png"), optimize=True)
    sheets["poses"] = (sheet, frames, heights)
    manifest = {
        "version": 3,
        "identity": "Salve — 32 ilustraciones aprobadas el 2026-10-26 (la v1 del 2026-10-12 se conserva en avatar/realistic_v1)",
        "rendering": "Native Canvas 2D; generated RGBA sprites, no 3D mesh. base.png es además el cuerpo vivo (capa astral).",
        "sheets": {},
    }
    for name in ("base",) + NAMES:
        path = os.path.join(out, name + ".png")
        with Image.open(path) as im:
            w, h = im.size
        _, frames, heights = sheets[name]
        manifest["sheets"][name] = {
            "width": w, "height": h,
            "sha256": hashlib.sha256(open(path, "rb").read()).hexdigest(),
            "frames": frames, "frameHeights": heights,
        }
    with open(os.path.join(out, "manifest.json"), "w", encoding="utf-8") as f:
        json.dump(manifest, f, indent=2, ensure_ascii=False)
    for name in ("base",) + NAMES:
        print(name, manifest["sheets"][name]["width"], manifest["sheets"][name]["height"], os.path.getsize(os.path.join(out, name + ".png")))

if __name__ == "__main__":
    main()
