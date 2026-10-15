#!/usr/bin/env python3
"""Genera la capa viva del cuerpo astral de Salve a partir de realistic/base.png.

Salidas (todas derivadas del PNG aprobado, que no se modifica):
  astral_eyes.png  - parpados entrecerrado / cerrado para el recuadro de los ojos
  astral_glow.png  - mascara de los circuitos cian (media resolucion) para el pulso de luz
  astral.json      - recuadro de ojos, pivotes y pesos de malla (pelo y cabeza)

Uso: python3 scripts/build_astral_body.py   (requiere numpy y opencv-python)
"""
import json, math, os, sys
import numpy as np
import cv2

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
ASSETS = os.path.join(ROOT, "app", "src", "main", "assets", "avatar", "realistic")
BASE = os.path.join(ASSETS, "base.png")

EYE_BOX = (345, 145, 500, 195)          # x0, y0, x1, y1 en base.png
EYES = [  # centro, semiejes de la mascara, extremos externo / interno del parpado
    dict(c=(379, 171), ax=(27, 14), outer=(356, 168), inner=(402, 168)),
    dict(c=(465, 171), ax=(27, 14), outer=(488, 168), inner=(443, 168)),
]
NECK = (427, 322)
HEART = (427, 520)
FEET = (427, 1840)
MESH_COLS, MESH_ROWS = 28, 60
# Capsulas del cuerpo que NO deben ondear (brazos, manos, torso, piernas, cabeza).
CAPSULES = [
    ((313, 330), (240, 600), 40), ((240, 600), (153, 787), 34), ((153, 787), (122, 900), 52),
    ((540, 330), (613, 600), 40), ((613, 600), (693, 787), 34), ((693, 787), (727, 900), 52),
    ((427, 360), (427, 500), 112), ((427, 560), (427, 620), 82), ((427, 690), (427, 790), 135),
    ((365, 800), (372, 1840), 78), ((490, 800), (482, 1840), 78),
    ((427, 120), (427, 300), 92), ((427, 250), (427, 340), 60),
]

def smoothstep(a, b, x):
    t = np.clip((x - a) / (b - a), 0.0, 1.0)
    return t * t * (3 - 2 * t)

def capsule_distance(px, py):
    best = np.full(np.shape(px), 1e9, dtype=np.float64)
    for (ax, ay), (bx, by), r in CAPSULES:
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
    x0, y0 = EYE_BOX[0], EYE_BOX[1]
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
    x0, y0, x1, y1 = EYE_BOX
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
    m = ((h > 90) & (h < 125) & (s > 110) & (v > 150) & (a > 200)).astype(np.float32)
    x0, y0, x1, y1 = EYE_BOX
    m[y0:y1, x0:x1] = 0  # los iris no son circuitos
    m = np.clip(cv2.GaussianBlur(m, (0, 0), 2.5) * 1.6, 0, 1)
    half = cv2.resize(m, (base.shape[1] // 2, base.shape[0] // 2), interpolation=cv2.INTER_AREA)
    out = np.zeros((half.shape[0], half.shape[1], 4), np.uint8)
    out[:, :, :3] = 255
    out[:, :, 3] = np.clip(half * 255, 0, 255).astype(np.uint8)
    return out, m

def mesh_weights(w, h):
    xs = np.linspace(0, w, MESH_COLS + 1)
    ys = np.linspace(0, h, MESH_ROWS + 1)
    X, Y = np.meshgrid(xs, ys)
    outside = smoothstep(0, 48, capsule_distance(X, Y))
    hanging = smoothstep(300, 820, Y)
    hair = outside * hanging
    head = 1 - smoothstep(262, 338, Y)
    return np.round(hair, 3), np.round(head, 3)

def main():
    base = cv2.imread(BASE, cv2.IMREAD_UNCHANGED)
    if base is None or base.shape[2] != 4:
        sys.exit("base.png ausente o sin alfa")
    h, w = base.shape[:2]
    cv2.imwrite(os.path.join(ASSETS, "astral_eyes.png"), eye_frames(base))
    glow, _ = glow_mask(base)
    cv2.imwrite(os.path.join(ASSETS, "astral_glow.png"), glow)
    hair, head = mesh_weights(w, h)
    spec = {
        "version": 1,
        "source": "base.png",
        "size": [w, h],
        "eyeBox": list(EYE_BOX),
        "eyeFrames": ["half", "closed"],
        "neck": list(NECK), "heart": list(HEART), "feet": list(FEET),
        "mesh": {"cols": MESH_COLS, "rows": MESH_ROWS,
                 "hair": hair.flatten().tolist(), "head": head.flatten().tolist()},
    }
    with open(os.path.join(ASSETS, "astral.json"), "w") as f:
        json.dump(spec, f, separators=(",", ":"))
    print("ok", w, h, "hair>0:", int((hair > 0.05).sum()), "vertices:", hair.size)

if __name__ == "__main__":
    main()
