#!/usr/bin/env python3
"""Iconos de líneas para que Salve tenga referencias limpias sin internet.

Convierte los iconos de contorno de Tabler Icons (licencia MIT, https://tabler.io/icons) en trazos
para dibujar con epiciclos: cada subtrayecto SVG se muestrea por longitud de arco y se normaliza al
lienzo 0..1000 de su repertorio. Sólo cosas dibujables (animales, edificios, comida, vehículos…);
fuera marcas, interfaz, letras, números y variantes tachadas («-off»).

Salida: app/src/main/assets/sketch/icons.json y app/src/main/assets/sketch/ICONS_LICENSE.txt

Uso:
    npm pack @tabler/icons@3.50.0 && tar xzf tabler-icons-3.50.0.tgz
    python3 scripts/build_icon_library.py package/   (necesita: pip install svgpathtools)
"""
import json, math, os, sys

from svgpathtools import parse_path

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT_DIR = os.path.join(ROOT, "app", "src", "main", "assets", "sketch")

KEEP = {"Animals", "Buildings", "Food", "Vehicles", "Weather", "Nature", "Sport", "Health", "Devices", "Games",
        "Mood", "Shapes", "Photography", "Gestures", "E-commerce", "Electrical", "Media", "Zodiac", "Map",
        "Computers", "Symbols"}
# Objetos dibujables que Tabler clasifica como interfaz o diseño («door» está en «System»).
EXTRA = {"door", "key", "lock", "clock", "bell", "lamp", "book", "umbrella", "star", "crown", "candle", "ladder",
         "sofa", "bucket", "scissors", "hammer", "pencil", "brush", "palette", "mailbox"}
TOLERANCE = 0.045  # simplificación (unidades de 24): invisible al dibujar; las rectas quedan en 2 puntos
STEP = 0.35  # unidades del lienzo de 24: un punto cada ~1,5 % del icono


def resample(pts, step):
    """Puntos equiespaciados a lo largo de la polilínea."""
    out = [pts[0]]
    carry = 0.0
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        seg = math.hypot(x1 - x0, y1 - y0)
        pos = step - carry
        while pos <= seg:
            t = pos / seg
            out.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
            pos += step
        carry = seg - (pos - step)
    if math.hypot(out[-1][0] - pts[-1][0], out[-1][1] - pts[-1][1]) > step * 0.3:
        out.append(pts[-1])
    return out


def simplify(pts, tol):
    """Douglas-Peucker: quita los puntos que no cambian la forma."""
    if len(pts) < 3:
        return pts
    (x0, y0), (x1, y1) = pts[0], pts[-1]
    dx, dy = x1 - x0, y1 - y0
    norm = math.hypot(dx, dy)
    worst, index = -1.0, 0
    for i in range(1, len(pts) - 1):
        px, py = pts[i]
        d = abs(dy * px - dx * py + x1 * y0 - y1 * x0) / norm if norm > 1e-9 else math.hypot(px - x0, py - y0)
        if d > worst:
            worst, index = d, i
    if worst <= tol:
        return [pts[0], pts[-1]]
    return simplify(pts[:index + 1], tol)[:-1] + simplify(pts[index:], tol)


def strokes_of(nodes):
    """Polilíneas (en 0..24) de los trayectos de un icono."""
    out = []
    for tag, attrs in nodes:
        if tag != "path" or "d" not in attrs:
            continue
        for sub in parse_path(attrs["d"]).continuous_subpaths():
            dense = []
            for seg in sub:
                k = 2 if type(seg).__name__ == "Line" else 24
                for i in range(k):
                    z = seg.point(i / k)
                    dense.append((z.real, z.imag))
            z = sub[-1].point(1.0)
            dense.append((z.real, z.imag))
            length = sum(math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(dense, dense[1:]))
            if length < 0.2:
                continue
            out.append(simplify(dense, TOLERANCE))
    return out


def normalize(strokes):
    xs = [x for s in strokes for x, _ in s]
    ys = [y for s in strokes for _, y in s]
    minx, maxx, miny, maxy = min(xs), max(xs), min(ys), max(ys)
    w, h = max(1e-6, maxx - minx), max(1e-6, maxy - miny)
    scale = 1000.0 / max(w, h)
    ox, oy = (1000 - w * scale) / 2, (1000 - h * scale) / 2
    flat = []
    for s in strokes:
        line = []
        for x, y in s:
            line += [round((x - minx) * scale + ox), round((y - miny) * scale + oy)]
        flat.append(line)
    return flat, round(w / h, 3)


def main(package):
    nodes = json.load(open(os.path.join(package, "tabler-nodes-outline.json")))
    meta = json.load(open(os.path.join(package, "icons.json")))
    icons = []
    for name, info in sorted(meta.items()):
        if (info.get("category") not in KEEP and name not in EXTRA) or name.endswith("-off") or "-off-" in name \
                or name.startswith("brand-"):
            continue
        if name not in nodes:
            continue
        strokes = [s for s in strokes_of(nodes[name]) if len(s) >= 2]
        if not strokes:
            continue
        flat, aspect = normalize(strokes)
        tags = [t.lower() for t in info.get("tags", []) if isinstance(t, str) and t.isascii()]
        icons.append({"n": name, "c": info.get("category", ""), "t": tags[:14], "a": aspect, "s": flat})
    os.makedirs(OUT_DIR, exist_ok=True)
    with open(os.path.join(OUT_DIR, "icons.json"), "w") as f:
        json.dump({"version": 1, "source": "Tabler Icons 3.50.0 (MIT) — https://tabler.io/icons", "icons": icons},
                  f, separators=(",", ":"))
    with open(os.path.join(package, "LICENSE")) as src, open(os.path.join(OUT_DIR, "ICONS_LICENSE.txt"), "w") as dst:
        dst.write("Los iconos de icons.json se derivan de Tabler Icons (https://tabler.io/icons).\n\n" + src.read())
    size = os.path.getsize(os.path.join(OUT_DIR, "icons.json"))
    print(f"{len(icons)} iconos · {size / 1024:.0f} KB")


if __name__ == "__main__":
    main(sys.argv[1] if len(sys.argv) > 1 else "package")
