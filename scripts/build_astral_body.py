#!/usr/bin/env python3
"""Genera la capa viva del cuerpo astral de Salve a partir de <cuerpo>/base.png.

Salidas (todas derivadas del PNG aprobado, que no se modifica):
  astral_eyes.png  - parpados entrecerrado / cerrado para el recuadro de los ojos
  astral_glow.png  - mascara de los circuitos cian (media resolucion) para el pulso de luz
  astral.json      - recuadro de ojos, pivotes, inicio del pelo y pesos de malla (pelo y cabeza)

Cuerpos (avatar/<cuerpo>/):
  realistic           - Salve actual: pecho cubierto y cabeza recta (27-10-2026)
  realistic_inclinada - pecho cubierto con la cabeza inclinada (Vestuario)
  realistic_abierto - la misma con el escote abierto (Vestuario)
  realistic_2610    - la base del 26-10-2026 (la 01 de las 32 ilustraciones), conservada en Vestuario
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
    # La base del 26-10-2026 (la 01). Sus bocas H1-H3 (astral_mouth_h123.png) vienen del PR #246.
    "realistic_2610": dict(
        eye_box=(450, 125, 585, 170),
        eyes=[
            dict(c=(482, 145), ax=(20, 10), outer=(462, 139), inner=(500, 148)),
            dict(c=(551, 152), ax=(20, 10), outer=(571, 148), inner=(534, 150)),
        ],
        # El núcleo luminoso del cuello es el corazón: de ahí sale la luz que recorre las líneas.
        neck=(510, 225), heart=(510, 262), feet=(510, 1530),
        mesh=(34, 52), hair_top=230, hair_span=720, hang=(230, 740), head=(180, 245),
        # Ojos cerrados dibujados (recorte alineado de la ilustración 32 aprobada): sustituyen al
        # párpado sintético en el fotograma «cerrado»; el entrecerrado sigue siendo sintético.
        closed_eyes="astral_sources/realistic_closed_eyes.png",
        # Bocas para hablar (la misma 01 cambiando solo la boca): entreabierta «a» y redondeada «o».
        mouth_box=(472, 168, 552, 226),
        mouths=["astral_sources/realistic_mouth_a.png", "astral_sources/realistic_mouth_o.png"],
        # Azul intenso de los paneles y cian de las líneas finas; el pelo lavanda queda fuera.
        glow=[(88, 128, 110, 150), (80, 104, 40, 170)], glow_blur=1.6,
        # Las colas blancas del traje quedan fuera de las cápsulas: ondean con el pelo.
        capsules=[
            ((400, 340), (345, 560), 44), ((345, 560), (275, 700), 38), ((275, 700), (230, 795), 55),
            ((620, 340), (675, 560), 44), ((675, 560), (745, 700), 38), ((745, 700), (790, 795), 55),
            ((510, 330), (510, 560), 110), ((510, 560), (510, 650), 95), ((510, 660), (510, 760), 140),
            ((465, 780), (475, 1530), 70), ((555, 780), (545, 1530), 70),
            ((510, 30), (510, 190), 122), ((510, 190), (510, 270), 56),
        ]),
}
# Base de pecho cubierto (27-10-2026) y su variante de escote abierto: la misma pose y la misma cara.
NEW_BASE = dict(
    eye_box=(455, 128, 585, 190),
    eyes=[
        dict(c=(483, 150), ax=(19, 10), outer=(465, 145), inner=(500, 153)),
        dict(c=(553, 173), ax=(19, 9), outer=(571, 176), inner=(536, 171)),
    ],
    neck=(498, 250), heart=(496, 272), feet=(530, 1510),
    mesh=(34, 52), hair_top=235, hair_span=720, hang=(235, 740), head=(196, 258),
    # Bocas dibujadas sobre la propia boca (sonrisa ligeramente inclinada): H1 entreabierta, H2 «a», H3 «o».
    mouth_box=(470, 186, 540, 226),
    synth_mouths=dict(center=(504, 204), angle=12, sizes=[(11, 2.6), (12, 5.5), (6.5, 5)]),
    lid_thick=.65,
    glow=[(88, 128, 110, 150), (80, 104, 40, 170)], glow_blur=1.6,
    capsules=[
        ((395, 320), (335, 545), 44), ((335, 545), (245, 760), 40), ((245, 760), (222, 830), 50),
        ((635, 320), (690, 545), 44), ((690, 545), (790, 760), 40), ((790, 760), (812, 830), 50),
        ((515, 320), (515, 560), 115), ((515, 560), (525, 650), 95), ((525, 650), (530, 740), 128),
        ((485, 760), (500, 1510), 64), ((580, 760), (556, 1510), 64),
        ((510, 30), (510, 190), 122), ((505, 190), (500, 275), 56),
    ])
# Base principal: pecho cubierto y cabeza recta (27-10-2026).
STRAIGHT = dict(NEW_BASE,
    eye_box=(440, 140, 590, 182),
    eyes=[
        dict(c=(472, 162), ax=(18, 9), outer=(455, 159), inner=(490, 163)),
        dict(c=(554, 162), ax=(18, 9), outer=(572, 159), inner=(537, 163)),
    ],
    neck=(512, 248), heart=(510, 274), feet=(518, 1510), head=(198, 262),
    mouth_box=(478, 188, 548, 228),
    synth_mouths=dict(center=(513, 208), angle=0, sizes=[(11, 2.6), (12, 5.5), (6.5, 5)]),
    capsules=[
        ((410, 310), (355, 590), 44), ((355, 590), (250, 790), 40), ((250, 790), (230, 850), 50),
        ((615, 310), (680, 590), 44), ((680, 590), (785, 790), 40), ((785, 790), (805, 850), 50),
        ((512, 300), (512, 560), 115), ((512, 560), (515, 650), 95), ((515, 650), (515, 740), 128),
        ((470, 760), (495, 1510), 64), ((560, 760), (540, 1510), 64),
        ((512, 30), (512, 195), 122), ((512, 195), (510, 275), 56),
    ])
BODIES["realistic"] = STRAIGHT
BODIES["realistic_inclinada"] = NEW_BASE
BODIES["realistic_abierto"] = NEW_BASE
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
            k = BODY.get("lid_thick", 1.0)  # caras más pequeñas: párpado más fino
            if closure < 1.0:
                shift = -e["ax"][1] + 2 * e["ax"][1] * closure - 3
                draw_lid(big, e, 1.5, shift, 2.6 * k)
            else:
                draw_lid(big, e, 4.0 if k == 1.0 else 4.0 * k + 1, 3.5 * k, 2.4 * k)
        frames.append(cv2.resize(big, (rgba.shape[1], rgba.shape[0]), interpolation=cv2.INTER_AREA))
    if BODY.get("closed_eyes"):
        art = cv2.imread(os.path.join(os.path.dirname(os.path.abspath(__file__)), BODY["closed_eyes"]), cv2.IMREAD_UNCHANGED)
        if art is None or art.shape[:2] != crop.shape[:2]:
            sys.exit("recorte de ojos cerrados ausente o de otro tamaño")
        m = np.zeros(mask.shape, np.uint8)
        for e in EYES:
            cv2.ellipse(m, (e["c"][0] - x0, e["c"][1] - y0), (e["ax"][0] + 6, e["ax"][1] + 5), 0, 0, 360, 255, -1)
        f = cv2.GaussianBlur(m.astype(np.float32) / 255, (0, 0), 3.0)[:, :, None]
        rgb = (art[:, :, :3] * f + bgr * (1 - f)).astype(np.uint8)
        frames[1] = np.dstack([rgb, np.maximum(alpha, (f[:, :, 0] * 255).astype(np.uint8))])
    return np.vstack(frames)

def mouth_frames(base):
    """Bocas dibujadas apiladas, con borde fundido para que encajen sobre la base sin costura."""
    x0, y0, x1, y1 = BODY["mouth_box"]
    h, w = y1 - y0, x1 - x0
    m = np.zeros((h, w), np.float32)
    cv2.ellipse(m, (w // 2, h // 2), (w // 2 - 4, h // 2 - 4), 0, 0, 360, 1.0, -1)
    m = cv2.GaussianBlur(m, (0, 0), 3.5)
    frames = []
    here = os.path.dirname(os.path.abspath(__file__))
    for path in BODY["mouths"]:
        art = cv2.imread(os.path.join(here, path), cv2.IMREAD_UNCHANGED)
        if art is None or art.shape[:2] != (h, w):
            sys.exit("boca ausente o de otro tamaño: " + path)
        frames.append(np.dstack([art[:, :, :3], (m * 255).astype(np.uint8)]))
    return np.vstack(frames)

def synth_mouth_frames(base):
    """Bocas abiertas pintadas sobre la boca de la base: interior rosa oscuro, labio y un poco de dientes."""
    x0, y0, x1, y1 = BODY["mouth_box"]
    spec = BODY["synth_mouths"]
    crop = base[y0:y1, x0:x1].copy()
    h, w = crop.shape[:2]
    cx, cy = spec["center"][0] - x0, spec["center"][1] - y0
    S = 8
    frames = []
    fade = np.zeros((h, w), np.float32)
    cv2.ellipse(fade, (w // 2, h // 2), (w // 2 - 4, h // 2 - 4), 0, 0, 360, 1.0, -1)
    fade = cv2.GaussianBlur(fade, (0, 0), 3.5)
    for k, (rx, ry) in enumerate(spec["sizes"]):
        big = cv2.resize(crop, (w * S, h * S), interpolation=cv2.INTER_CUBIC)
        c = (int(cx * S), int(cy * S + ry * S * .35))
        axes = (int(rx * S), int(ry * S))
        lip = np.zeros(big.shape[:2], np.uint8)
        cv2.ellipse(lip, c, (axes[0] + S, axes[1] + S), spec["angle"], 0, 360, 255, -1)
        inner = np.zeros(big.shape[:2], np.uint8)
        cv2.ellipse(inner, c, axes, spec["angle"], 0, 360, 255, -1)
        out = big[:, :, :3].astype(np.float32)
        lipf = cv2.GaussianBlur(lip, (0, 0), S * .8).astype(np.float32)[:, :, None] / 255
        innf = cv2.GaussianBlur(inner, (0, 0), S * .5).astype(np.float32)[:, :, None] / 255
        out = out * (1 - lipf * .55) + np.array([112, 112, 196], np.float32) * lipf * .55   # labio (BGR)
        mouth = np.zeros_like(out); mouth[:] = (58, 38, 104)                                # interior
        if k == 1:  # «a»: asoman los dientes de arriba
            teeth = np.zeros(big.shape[:2], np.uint8)
            cv2.ellipse(teeth, (c[0], c[1] - int(axes[1] * .55)), (int(axes[0] * .8), int(axes[1] * .35)),
                        spec["angle"], 0, 360, 255, -1)
            t = cv2.GaussianBlur(teeth, (0, 0), S * .4).astype(np.float32)[:, :, None] / 255
            mouth = mouth * (1 - t) + np.array([236, 236, 244], np.float32) * t
        out = out * (1 - innf) + mouth * innf
        small = cv2.resize(out.clip(0, 255).astype(np.uint8), (w, h), interpolation=cv2.INTER_AREA)
        frames.append(np.dstack([small, (fade * 255).astype(np.uint8)]))
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
    if BODY.get("mouths"):
        cv2.imwrite(os.path.join(assets, "astral_mouth.png"), mouth_frames(base))
    if BODY.get("synth_mouths"):
        cv2.imwrite(os.path.join(assets, "astral_mouth_h123.png"), synth_mouth_frames(base))
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
        **({"mouthBox": list(BODY["mouth_box"]), "mouthFrames": ["a", "o"]} if BODY.get("mouths") else {}),
        **({"mouthBox": list(BODY["mouth_box"]), "mouthFrames": ["h1", "h2", "h3"]} if BODY.get("synth_mouths") else {}),
        "mesh": {"cols": cols, "rows": rows,
                 "hair": hair.flatten().tolist(), "head": head.flatten().tolist()},
    }
    with open(os.path.join(assets, "astral.json"), "w") as f:
        json.dump(spec, f, separators=(",", ":"))
    print("ok", name, w, h, "hair>0:", int((hair > 0.05).sum()), "vertices:", hair.size)

if __name__ == "__main__":
    main()
