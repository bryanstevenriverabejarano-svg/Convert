#!/usr/bin/env python3
"""Genera la capa viva del cuerpo astral de Salve a partir de <cuerpo>/base.png.

Salidas (todas derivadas del PNG aprobado, que no se modifica):
  astral_eyes.png  - parpados entrecerrado / cerrado para el recuadro de los ojos
  astral_glow.png  - mascara de los circuitos cian (media resolucion) para el pulso de luz
  astral.json      - recuadro de ojos, pivotes, inicio del pelo y pesos de malla (pelo y cabeza)

Cuerpos (avatar/<cuerpo>/):
  realistic     - Salve actual (base aprobada el 26-10-2026)
  realistic_v1  - Salve v1 (referencia aprobada el 12-10-2026), conservada en Vestuario

Uso: python3 scripts/build_astral_body.py [realistic|realistic_v1]   (requiere numpy y opencv-python)
"""
import json, math, os, sys
import numpy as np
import cv2

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
AVATAR = os.path.join(ROOT, "app", "src", "main", "assets", "avatar")

# Coordenadas medidas a mano sobre cada base.png (píxeles de la ilustración).
BODIES = {
    "realistic_v1": dict(
        eye_box=(345, 145, 500, 195),
        eyes=[  # centro, semiejes de la mascara, extremos externo / interno del parpado
            dict(c=(379, 171), ax=(27, 14), outer=(356, 168), inner=(402, 168)),
            dict(c=(465, 171), ax=(27, 14), outer=(488, 168), inner=(443, 168)),
        ],
        neck=(427, 322), heart=(427, 520), feet=(427, 1840),
        mesh=(28, 60), hair_top=300, hair_span=700, hang=(300, 820), head=(262, 338),
        # Circuitos: (tono min, tono max, saturacion min, brillo min) en HSV de OpenCV; desenfoque.
        glow=[(90, 125, 110, 150)], glow_blur=2.5,
        # Capsulas del cuerpo que NO deben ondear (brazos, manos, torso, piernas, cabeza).
        capsules=[
            ((313, 330), (240, 600), 40), ((240, 600), (153, 787), 34), ((153, 787), (122, 900), 52),
            ((540, 330), (613, 600), 40), ((613, 600), (693, 787), 34), ((693, 787), (727, 900), 52),
            ((427, 360), (427, 500), 112), ((427, 560), (427, 620), 82), ((427, 690), (427, 790), 135),
            ((365, 800), (372, 1840), 78), ((490, 800), (482, 1840), 78),
            ((427, 120), (427, 300), 92), ((427, 250), (427, 340), 60),
        ]),
    "realistic": dict(
        eye_box=(500, 140, 660, 186),
        eyes=[
            dict(c=(533, 164), ax=(25, 12), outer=(510, 160), inner=(557, 166)),
            dict(c=(621, 164), ax=(25, 12), outer=(645, 160), inner=(599, 166)),
        ],
        # El núcleo luminoso del cuello es el corazón: de ahí sale la luz que recorre las líneas.
        neck=(575, 255), heart=(575, 300), feet=(575, 1695),
        mesh=(38, 56), hair_top=240, hair_span=720, hang=(240, 760), head=(200, 270),
        # Azul intenso de los paneles y cian de las líneas finas; el pelo lavanda (tono ~115,
        # saturacion < 50) queda fuera.
        glow=[(88, 128, 110, 150), (80, 104, 40, 170)], glow_blur=1.6,
        capsules=[
            ((480, 325), (418, 590), 44), ((418, 590), (330, 795), 38), ((330, 795), (275, 930), 55),
            ((670, 325), (732, 590), 44), ((732, 590), (825, 795), 38), ((825, 795), (880, 930), 55),
            ((575, 320), (575, 560), 102), ((575, 560), (575, 690), 88), ((575, 700), (575, 810), 132),
            ((527, 820), (540, 1695), 64), ((625, 820), (612, 1695), 64),
            ((575, 40), (575, 200), 138), ((575, 200), (575, 290), 62),
        ]),
}
BODY = None


def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)

def capsule_distance(px, py):
    best = np.full(np.shape(px), 1e9, dtype=np.float64)
    for (ax, ay), (bx, by), r in BODY["capsules"]:
        dx, dy = bx - ax, by - ay
        t = np.clip(((px - ax) * dx + (py - ay) * dy) / max(1e-9, dx * dx + dy * dy), 0, 1)
        d = np.hypot(px - (ax + t * dx), py - (ay + t * dy)) - r
        best = np.minimum(best, d)
    return best

def lid_curve(eye, sag, y_shift):
    (ox, oy), (ix, iy) = eye["outer"], eye["inner"]
    pts = []
    for i in range(25):
        t = i / 24.0
        x = ox + (ix - ox) * t
        y = oy + (iy - oy) * t + y_shift + sag * math.sin(math.pi * t)
        pts.append((x, y))
    return pts

def draw_lid(img, eye, sag, y_shift, thick):
    pts = lid_curve(eye, sag, y_shift)
    scale = 4  # dibujo con antialias por supermuestreo
    x0, y0 = BODY["eye_box"][0], BODY["eye_box"][1]
    for i in range(len(pts) - 1):
        t = i / (len(pts) - 1)
        w = thick * (1.25 - 0.55 * t)  # mas grueso hacia el angulo externo
        p = (int((pts[i][0] - x0) * scale), int((pts[i][1] - y0) * scale))
        q = (int((pts[i + 1][0] - x0) * scale), int((pts[i + 1][1] - y0) * scale))
        cv2.line(img, p, q, (38, 24, 30, 255), max(1, int(w * scale)), cv2.LINE_AA)
    # dos pestanas hacia fuera en el angulo externo
    (ox, oy) = pts[0]
    d = -1 if eye["outer"][0] < eye["inner"][0] else 1
    for k, (lx, ly) in enumerate(((d * 6, 3), (d * 4, 6))):
        p = (int((ox - x0) * scale), int((oy + 1 - y0) * scale))
        q = (int((ox + lx - x0) * scale), int((oy + 1 + ly - y0) * scale))
        cv2.line(img, p, q, (38, 24, 30, 255), int(1.6 * scale), cv2.LINE_AA)

def eye_frames(base):
    EYES = BODY["eyes"]
    x0, y0, x1, y1 = BODY["eye_box"]
    crop = base[y0:y1, x0:x1].copy()
    bgr, alpha = crop[:, :, :3].copy(), crop[:, :, 3]
    mask = np.zeros(bgr.shape[:2], np.uint8)
    for e in EYES:
        cv2.ellipse(mask, (e["c"][0] - x0, e["c"][1] - y0), e["ax"], 0, 0, 360, 255, -1)
    skin = cv2.inpaint(bgr, mask, 7, cv2.INPAINT_TELEA)
    skin = cv2.GaussianBlur(skin, (0, 0), 1.2)
    frames = []
    for closure in (0.55, 1.0):
        out = bgr.copy()
        feather = np.zeros(mask.shape, np.float32)
        for e in EYES:
            m = np.zeros(mask.shape, np.uint8)
            cv2.ellipse(m, (e["c"][0] - x0, e["c"][1] - y0), e["ax"], 0, 0, 360, 255, -1)
            if closure < 1.0:  # solo la mitad superior: el parpado baja hasta la linea
                lid_y = e["c"][1] - y0 - e["ax"][1] + int(2 * e["ax"][1] * closure)
                m[lid_y + 2:, :] = 0
            feather = np.maximum(feather, cv2.GaussianBlur(m.astype(np.float32) / 255, (0, 0), 1.6))
        f = feather[:, :, None]
        out = (skin * f + bgr * (1 - f)).astype(np.uint8)
        rgba = np.dstack([out, np.maximum(alpha, (feather * 255).astype(np.uint8))])
        big = cv2.resize(rgba, (rgba.shape[1] * 4, rgba.shape[0] * 4), interpolation=cv2.INTER_CUBIC)
        for e in EYES:
            if closure < 1.0:
                shift = -e["ax"][1] + 2 * e["ax"][1] * closure - 3
                draw_lid(big, e, 1.5, shift, 2.6)
            else:
                draw_lid(big, e, 4.0, 3.5, 2.4)
        frames.append(cv2.resize(big, (rgba.shape[1], rgba.shape[0]), interpolation=cv2.INTER_AREA))
    return np.vstack(frames)

def glow_mask(base):
    hsv = cv2.cvtColor(base[:, :, :3], cv2.COLOR_BGR2HSV).astype(np.int32)
    h, s, v, a = hsv[:, :, 0], hsv[:, :, 1], hsv[:, :, 2], base[:, :, 3]
    m = np.zeros(h.shape, bool)
    for lo, hi, smin, vmin in BODY["glow"]:
        m |= (h > lo) & (h < hi) & (s > smin) & (v > vmin) & (a > 200)
    m = m.astype(np.float32)
    x0, y0, x1, y1 = BODY["eye_box"]
    m[y0:y1, x0:x1] = 0  # los iris no son circuitos
    m = np.clip(cv2.GaussianBlur(m, (0, 0), BODY["glow_blur"]) * 1.6, 0, 1)
    half = cv2.resize(m, (base.shape[1] // 2, base.shape[0] // 2), interpolation=cv2.INTER_AREA)
    out = np.zeros((half.shape[0], half.shape[1], 4), np.uint8)
    out[:, :, :3] = 255
    out[:, :, 3] = np.clip(half * 255, 0, 255).astype(np.uint8)
    return out, m

def mesh_weights(w, h):
    cols, rows = BODY["mesh"]
    xs = np.linspace(0, w, cols + 1)
    ys = np.linspace(0, h, rows + 1)
    X, Y = np.meshgrid(xs, ys)
    outside = smoothstep(0, 48, capsule_distance(X, Y))
    hanging = smoothstep(*BODY["hang"], Y)
    hair = outside * hanging
    head = 1 - smoothstep(*BODY["head"], Y)
    return np.round(hair, 3), np.round(head, 3)

def main():
    global BODY
    name = sys.argv[1] if len(sys.argv) > 1 else "realistic"
    if name not in BODIES:
        sys.exit("cuerpo desconocido: " + name)
    BODY = BODIES[name]
    assets = os.path.join(AVATAR, name)
    base = cv2.imread(os.path.join(assets, "base.png"), cv2.IMREAD_UNCHANGED)
    if base is None or base.shape[2] != 4:
        sys.exit("base.png ausente o sin alfa")
    h, w = base.shape[:2]
    cv2.imwrite(os.path.join(assets, "astral_eyes.png"), eye_frames(base))
    glow, _ = glow_mask(base)
    cv2.imwrite(os.path.join(assets, "astral_glow.png"), glow)
    hair, head = mesh_weights(w, h)
    cols, rows = BODY["mesh"]
    spec = {
        "version": 1,
        "source": "base.png",
        "size": [w, h],
        "eyeBox": list(BODY["eye_box"]),
        "eyeFrames": ["half", "closed"],
        "neck": list(BODY["neck"]), "heart": list(BODY["heart"]), "feet": list(BODY["feet"]),
        "hairTop": BODY["hair_top"], "hairSpan": BODY["hair_span"],
        "mesh": {"cols": cols, "rows": rows,
                 "hair": hair.flatten().tolist(), "head": head.flatten().tolist()},
    }
    with open(os.path.join(assets, "astral.json"), "w") as f:
        json.dump(spec, f, separators=(",", ":"))
    print("ok", name, w, h, "hair>0:", int((hair > 0.05).sum()), "vertices:", hair.size)

if __name__ == "__main__":
    main()
