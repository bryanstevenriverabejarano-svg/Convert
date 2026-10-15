#!/usr/bin/env python3
"""Biblioteca de trazos de Salve para dibujar con epiciclos de Fourier.

Cada motivo es una o varias líneas (SVG path, lienzo 200x200). El script las muestrea por longitud de
arco, las normaliza a 0..1000 y añade el autorretrato de Salve trazado desde su propia ilustración
(silueta + circuitos de luz). Salida: app/src/main/assets/sketch/library.json

Uso: python3 scripts/build_sketch_library.py [--preview carpeta]   (numpy y opencv para el autorretrato)
"""
import json, math, os, re, sys

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..")
OUT = os.path.join(ROOT, "app", "src", "main", "assets", "sketch", "library.json")
BASE = os.path.join(ROOT, "app", "src", "main", "assets", "avatar", "realistic", "base.png")

def circle(cx, cy, r, start=0.0):
    """Círculo como cuatro curvas de Bézier, empezando en el ángulo dado (grados)."""
    k = 0.5522847498 * r
    a = math.radians(start)
    pts = []
    for i in range(4):
        t0 = a + i * math.pi / 2
        t1 = t0 + math.pi / 2
        p0 = (cx + r * math.cos(t0), cy + r * math.sin(t0))
        p3 = (cx + r * math.cos(t1), cy + r * math.sin(t1))
        c1 = (p0[0] - k * math.sin(t0), p0[1] + k * math.cos(t0))
        c2 = (p3[0] + k * math.sin(t1), p3[1] - k * math.cos(t1))
        pts.append((p0, c1, c2, p3))
    d = "M%.1f %.1f " % pts[0][0]
    for p0, c1, c2, p3 in pts:
        d += "C%.1f %.1f %.1f %.1f %.1f %.1f " % (c1 + c2 + p3)
    return d + "Z"

def star(cx, cy, ro, ri, n=5, rot=-90):
    d = ""
    for i in range(2 * n):
        r = ro if i % 2 == 0 else ri
        a = math.radians(rot + i * 180 / n)
        d += ("M" if i == 0 else "L") + "%.1f %.1f " % (cx + r * math.cos(a), cy + r * math.sin(a))
    return d + "Z"

def rays(cx, cy, r0, r1, n, rot=0):
    return [("M%.1f %.1f L%.1f %.1f" % (cx + r0 * math.cos(math.radians(rot + i * 360 / n)),
                                         cy + r0 * math.sin(math.radians(rot + i * 360 / n)),
                                         cx + r1 * math.cos(math.radians(rot + i * 360 / n)),
                                         cy + r1 * math.sin(math.radians(rot + i * 360 / n)))) for i in range(n)]

# id, nombre con artículo, palabras (sin tildes), lugar en una escena, trazos
MOTIFS = [
    ("arbol", "un árbol", ["arbol", "arboles", "arbolito", "roble", "pino"], "ground", [
        "M88 190 C90 165 92 150 90 128 C78 130 62 126 54 114 C38 116 24 104 28 88 C16 78 22 58 40 56 "
        "C40 36 60 26 76 34 C86 18 112 16 122 32 C140 26 160 40 156 58 C176 62 180 86 166 96 "
        "C172 114 154 126 138 120 C130 130 118 132 110 128 C108 150 110 168 114 190 Z",
        "M100 128 C102 118 110 110 120 106",
        "M60 190 L146 190",
    ]),
    ("casa", "una casa", ["casa", "casita", "hogar", "cabana"], "ground", [
        "M40 190 L40 100 L100 50 L160 100 L160 190 Z",
        "M128 76 L128 46 L144 46 L144 90",
        "M86 190 L86 146 C86 136 114 136 114 146 L114 190",
        "M54 116 L76 116 L76 138 L54 138 Z M65 116 L65 138 M54 127 L76 127",
        "M124 116 L146 116 L146 138 L124 138 Z M135 116 L135 138 M124 127 L146 127",
    ]),
    ("sol", "el sol", ["sol", "amanecer", "atardecer"], "sky",
        [circle(100, 100, 38, -90)] + rays(100, 100, 50, 76, 12) + [
        "M84 92 C86 88 90 88 92 92", "M108 92 C110 88 114 88 116 92", "M84 112 C94 122 106 122 116 112"]),
    ("luna", "la luna", ["luna", "noche", "lunita"], "sky", [
        "M120 32 C80 34 54 66 56 104 C58 142 92 172 132 168 C104 156 88 130 88 100 C88 70 102 44 120 32 Z",
        star(150, 70, 10, 4), star(140, 130, 6, 2.5)]),
    ("estrella", "una estrella", ["estrella", "estrellas", "lucero"], "sky", [star(100, 104, 80, 33)]),
    ("nube", "una nube", ["nube", "nubes", "cielo"], "sky", [
        "M44 140 C22 140 18 112 40 106 C36 82 62 70 80 82 C90 56 132 56 140 84 C162 76 184 96 172 118 "
        "C190 124 186 148 164 148 L48 148 C46 146 45 142 44 140 Z"]),
    ("montana", "unas montañas", ["montana", "montanas", "monte", "cumbre", "volcan"], "ground", [
        "M10 180 L70 60 L100 116 L128 74 L190 180 Z",
        "M52 96 L62 104 L72 94 L82 104 L88 94",
        "M118 90 L128 98 L138 92"]),
    ("flor", "una flor", ["flor", "flores", "rosa", "margarita"], "ground", [
        "M100 82 C88 50 112 50 100 82 C120 60 136 78 106 90 C136 96 124 122 102 100 C108 130 88 130 96 100 "
        "C70 116 64 96 92 90 C64 82 74 58 100 82 Z",
        circle(100, 90, 9),
        "M100 100 C96 130 104 160 100 190",
        "M101 150 C116 140 134 142 140 128 C124 124 108 132 101 150",
        "M99 168 C86 158 70 160 62 148 C78 142 92 152 99 168"]),
    ("corazon", "un corazón", ["corazon", "amor", "carino"], "any", [
        "M100 176 C70 150 26 122 26 82 C26 52 56 36 78 44 C90 48 96 58 100 66 C104 58 110 48 122 44 "
        "C144 36 174 52 174 82 C174 122 130 150 100 176 Z"]),
    ("gato", "un gato", ["gato", "gata", "gatito", "minino", "michi"], "ground", [
        "M70 186 C56 186 50 170 56 150 C60 132 62 118 66 108 C56 100 52 84 56 70 L50 38 L74 56 "
        "C84 52 96 52 106 56 L130 38 L124 70 C128 84 124 100 114 108 C120 120 126 136 130 152 "
        "C134 170 126 186 112 186 Z",
        "M130 170 C150 172 166 160 160 140 C156 126 170 118 178 124",
        "M74 78 C76 74 80 74 82 78", "M98 78 C100 74 104 74 106 78",
        "M86 88 L94 88 L90 92 Z", "M90 92 C88 98 82 98 80 94 M90 92 C92 98 98 98 100 94",
        "M76 90 L54 86 M76 94 L56 98 M104 90 L126 86 M104 94 L124 98"]),
    ("perro", "un perro", ["perro", "perrito", "cachorro"], "ground", [
        "M28 84 C30 72 42 62 56 62 C66 62 74 68 78 80 C86 92 96 96 110 96 L150 96 C158 96 162 92 166 84 "
        "C170 76 176 72 180 74 C178 84 172 96 166 102 C166 120 164 150 164 186 L150 186 L148 136 L112 136 "
        "L110 186 L96 186 L94 132 C86 126 82 116 80 108 C70 110 60 106 52 100 L36 100 C28 100 24 94 28 84 Z",
        "M56 64 C64 66 70 74 68 92 C62 92 56 84 56 64 Z",
        circle(44, 76, 3),
        "M78 96 C82 104 86 110 88 116"]),
    ("pajaro", "un pájaro", ["pajaro", "pajarito", "ave", "aves", "golondrina", "paloma"], "sky", [
        "M40 110 C46 84 74 70 98 78 C110 62 136 62 144 78 L168 82 L146 92 C146 120 124 140 92 142 "
        "C70 144 52 136 40 128 L16 132 L32 116 Z",
        "M70 102 C90 84 118 84 126 104 C110 116 86 118 70 102 Z",
        circle(130, 80, 3),
        "M92 142 L88 166 M108 140 L110 166"]),
    ("pez", "un pez", ["pez", "peces", "pescado", "pececito"], "any", [
        "M30 100 C56 58 120 56 150 96 L182 70 L176 100 L182 130 L150 104 C120 144 56 142 30 100 Z",
        circle(58, 92, 5),
        "M92 74 C100 90 100 110 92 126 M112 72 C120 90 120 110 112 128",
        "M86 60 C98 44 120 44 128 64"]),
    ("barco", "un barco", ["barco", "barca", "velero", "bote", "nave"], "ground", [
        "M24 140 L176 140 L152 172 L48 172 Z",
        "M100 140 L100 26",
        "M104 30 L160 128 L104 128 Z",
        "M96 40 C70 70 56 100 46 128 L96 128 Z",
        "M100 26 L122 34 L100 40",
        "M10 182 C30 174 50 190 70 182 C90 174 110 190 130 182 C150 174 170 190 190 182"]),
    ("castillo", "un castillo", ["castillo", "palacio", "fortaleza", "torre", "torres"], "ground", [
        "M20 190 L20 70 L20 58 L28 58 L28 66 L36 66 L36 58 L44 58 L44 66 L52 66 L52 58 L60 58 L60 110 "
        "L80 110 L80 100 L90 100 L90 110 L110 110 L110 100 L120 100 L120 110 L140 110 L140 58 L148 58 L148 66 "
        "L156 66 L156 58 L164 58 L164 66 L172 66 L172 58 L180 58 L180 190 Z",
        "M86 190 L86 156 C86 140 114 140 114 156 L114 190",
        "M36 92 L36 80 C36 74 44 74 44 80 L44 92 Z M156 92 L156 80 C156 74 164 74 164 80 L164 92 Z",
        "M40 58 L40 22 L60 30 L40 38",
        "M160 58 L160 22 L180 30 L160 38"]),
    ("dragon", "un dragón", ["dragon", "dragones", "dragona"], "any", [
        "M20 150 C40 150 60 140 70 128 C78 118 86 112 100 112 L128 112 C136 100 144 86 150 74 L146 60 "
        "L158 66 L162 54 L170 66 C180 66 188 72 190 80 L176 84 C170 86 164 90 160 96 C156 110 152 122 150 134 "
        "L156 170 L146 170 L140 142 L112 142 L110 170 L100 170 L96 140 C86 144 78 152 66 160 "
        "C50 170 30 170 20 150 L8 142 L12 156 L20 150",
        "M110 112 C100 90 96 66 104 40 C112 52 122 56 134 54 C132 66 138 76 146 82 C138 88 134 98 128 112",
        "M104 40 L118 108 M134 54 L124 108",
        circle(170, 74, 2.5),
        "M70 128 L74 118 L80 124 L86 114 L92 120",
        "M192 76 C198 72 200 66 196 60 M192 84 C200 86 202 92 198 98"]),
    ("mariposa", "una mariposa", ["mariposa", "mariposas"], "sky", [
        "M100 96 C82 50 30 40 34 80 C36 104 70 108 98 102 C70 110 46 130 58 156 C72 178 96 146 100 108 "
        "C104 146 128 178 142 156 C154 130 130 110 102 102 C130 108 164 104 166 80 C170 40 118 50 100 96 Z",
        "M100 86 L100 136",
        "M100 86 C96 70 90 62 82 58 M100 86 C104 70 110 62 118 58"]),
    ("llave", "una llave", ["llave", "llaves"], "any", [
        circle(54, 100, 30, 0),
        "M84 100 L176 100 L176 122 L162 122 L162 110 L150 110 L150 126 L136 126 L136 110 L84 110",
        circle(54, 100, 12, 0)]),
    ("corona", "una corona", ["corona", "rey", "reina", "reino"], "any", [
        "M30 150 L22 64 L66 108 L100 46 L134 108 L178 64 L170 150 Z",
        "M30 170 L170 170 L170 150 L30 150 Z",
        circle(100, 124, 9), circle(58, 128, 6), circle(142, 128, 6)]),
    ("espada", "una espada", ["espada", "espadas", "caballero", "guerrero"], "any", [
        "M100 14 L112 36 L112 140 L88 140 L88 36 Z",
        "M60 140 L140 140 L140 150 L60 150 Z",
        "M94 150 L94 180 L106 180 L106 150",
        circle(100, 188, 8),
        "M100 40 L100 132"]),
    ("libro", "un libro", ["libro", "libros", "cuento", "historia", "biblioteca"], "any", [
        "M100 60 C80 46 46 44 18 52 L18 160 C46 152 80 154 100 168 C120 154 154 152 182 160 L182 52 "
        "C154 44 120 46 100 60 Z",
        "M100 60 L100 168",
        "M36 74 C54 70 70 72 86 78 M36 94 C54 90 70 92 86 98 M36 114 C54 110 70 112 86 118",
        "M114 78 C130 72 146 70 164 74 M114 98 C130 92 146 90 164 94 M114 118 C130 112 146 110 164 114"]),
    ("vela", "una vela", ["vela", "velas", "fuego", "llama", "hoguera", "luz"], "any", [
        "M76 190 L76 96 L124 96 L124 190 Z",
        "M100 96 L100 84",
        "M100 84 C82 70 84 46 100 20 C116 46 118 70 100 84 Z",
        "M100 78 C94 70 94 58 100 48 C106 58 106 70 100 78",
        "M60 190 L140 190"]),
    ("mar", "el mar", ["mar", "olas", "ola", "oceano", "playa", "rio", "lago", "agua"], "ground", [
        "M10 90 C30 70 50 70 60 90 C70 110 90 110 100 90 C110 70 130 70 140 90 C150 110 170 110 190 90",
        "M10 130 C30 110 50 110 60 130 C70 150 90 150 100 130 C110 110 130 110 140 130 C150 150 170 150 190 130",
        "M10 170 C30 150 50 150 60 170 C70 190 90 190 100 170 C110 150 130 150 140 170 C150 190 170 190 190 170"]),
    ("lluvia", "lluvia", ["lluvia", "llueve", "llover", "tormenta"], "sky", [
        "M44 100 C22 100 18 74 40 68 C36 44 62 34 80 46 C90 22 132 22 140 48 C162 40 184 60 172 82 "
        "C190 88 186 110 164 110 L48 110 C46 106 45 104 44 100 Z",
        "M60 126 L52 148 M92 126 L84 148 M124 126 L116 148 M150 126 L142 148",
        "M74 160 L66 182 M108 160 L100 182 M140 160 L132 182"]),
    ("persona", "una persona", ["persona", "nino", "nina", "chico", "chica", "hombre", "mujer", "amigo", "amiga",
                                 "gente"], "ground", [
        circle(100, 46, 20, 90),
        "M100 66 L100 128 M100 82 L70 112 M100 82 L130 112 M100 128 L78 184 M100 128 L122 184",
        "M92 44 C92 42 94 42 94 44 M106 44 C106 42 108 42 108 44 M92 54 C96 58 104 58 108 54"]),
    ("princesa", "una princesa", ["princesa", "princesas", "hada", "hadas", "dama"], "ground", [
        circle(100, 50, 18, 90),
        "M84 32 L82 14 L92 24 L100 10 L108 24 L118 14 L116 32",
        "M100 68 L88 100 L60 184 L140 184 L112 100 Z",
        "M92 84 L66 116 M108 84 L134 116",
        "M84 40 C76 60 74 80 80 100 M116 40 C124 60 126 80 120 100"]),
    ("mago", "un mago", ["mago", "maga", "bruja", "brujo", "hechicero", "magia"], "ground", [
        "M100 6 L132 70 L68 70 Z",
        "M56 70 L144 70",
        circle(100, 86, 16, 90),
        "M88 98 C90 120 96 130 100 140 C104 130 110 120 112 98",
        "M84 102 L62 184 L138 184 L116 102",
        "M116 112 L150 96 M150 96 L170 60",
        star(172, 54, 9, 3.5)]),
    ("conejo", "un conejo", ["conejo", "conejito", "liebre"], "ground", [
        "M72 186 C50 186 46 160 58 140 C62 124 70 116 82 112 C72 104 68 90 74 80 "
        "C66 60 64 30 76 22 C86 30 88 56 88 74 C92 72 98 72 102 74 C104 56 110 30 122 24 C132 34 126 62 114 82 "
        "C120 94 114 106 104 112 C120 120 132 140 130 162 C128 178 116 186 102 186 Z",
        circle(136, 168, 9),
        "M84 88 C86 84 90 84 90 88", "M96 98 L100 98 L98 101 Z"]),
    ("buho", "un búho", ["buho", "lechuza", "mochuelo"], "any", [
        "M60 50 L76 66 C92 60 108 60 124 66 L140 50 L140 140 C140 170 124 184 100 184 C76 184 60 170 60 140 Z",
        circle(82, 88, 14, 0), circle(118, 88, 14, 0),
        "M94 104 L100 116 L106 104",
        "M74 134 L82 140 L90 134 L98 140 L106 134 L114 140 L122 134",
        "M40 186 L160 186"]),
    ("lobo", "un lobo", ["lobo", "lobos", "zorro"], "ground", [
        "M70 186 C66 160 70 136 80 118 C70 108 62 94 60 78 L48 72 L62 64 L66 40 L80 58 C86 56 92 56 98 60 "
        "L104 38 L110 64 C118 74 118 86 112 96 C126 110 138 132 138 158 C150 160 164 154 172 140 "
        "C176 162 160 180 136 182 C134 184 132 186 128 186 Z",
        "M76 72 C78 70 80 70 82 72", "M60 78 L54 80"]),
    ("seta", "una seta", ["seta", "setas", "hongo", "champinon"], "ground", [
        "M30 104 C30 60 66 34 100 34 C134 34 170 60 170 104 C140 112 60 112 30 104 Z",
        "M82 110 C80 140 76 170 72 188 L128 188 C124 170 120 140 118 110",
        circle(70, 74, 9), circle(116, 60, 8), circle(140, 88, 7), circle(98, 90, 6)]),
    ("cohete", "un cohete", ["cohete", "nave espacial", "astronauta", "espacio"], "sky", [
        "M100 14 C124 36 132 70 128 130 L72 130 C68 70 76 36 100 14 Z",
        "M72 110 L50 150 L74 140 M128 110 L150 150 L126 140",
        circle(100, 72, 12),
        "M86 130 C88 150 94 166 100 186 C106 166 112 150 114 130"]),
    ("planeta", "un planeta", ["planeta", "planetas", "saturno", "mundo", "tierra"], "sky", [
        circle(100, 100, 46, 0),
        "M30 120 C10 140 40 146 100 126 C160 106 190 82 170 76 C160 74 150 76 146 78",
        "M54 82 C70 74 90 74 104 86 M72 112 C88 106 112 108 130 118"]),
    ("faro", "un faro", ["faro", "costa"], "ground", [
        "M76 186 L86 70 L114 70 L124 186 Z",
        "M82 70 L82 46 L118 46 L118 70 M78 46 L100 28 L122 46",
        "M80 110 L120 104 M78 140 L122 134",
        "M124 56 L180 40 M124 60 L180 76 M76 56 L20 40 M76 60 L20 76",
        "M30 186 L170 186"]),
    ("puente", "un puente", ["puente", "puentes"], "ground", [
        "M10 100 L190 100 L190 116 L10 116 Z",
        "M30 186 L30 116 M170 186 L170 116",
        "M42 186 C46 140 154 140 158 186",
        "M10 100 L10 80 M40 100 L40 80 M70 100 L70 80 M100 100 L100 80 M130 100 L130 80 M160 100 L160 80 "
        "M190 100 L190 80 M10 80 L190 80"]),
    ("manzana", "una manzana", ["manzana", "manzanas", "fruta"], "any", [
        "M100 62 C80 44 36 50 36 98 C36 146 72 186 100 170 C128 186 164 146 164 98 C164 50 120 44 100 62 Z",
        "M100 62 C100 46 104 34 112 24",
        "M104 44 C118 26 140 26 150 34 C136 48 118 50 104 44 Z"]),
    ("hoja", "una hoja", ["hoja", "hojas", "otono", "planta"], "any", [
        "M40 168 C40 90 90 34 170 30 C168 110 120 166 40 168 Z",
        "M40 168 C80 130 120 80 160 40",
        "M80 132 L76 100 M100 110 L98 76 M120 88 L120 60 M80 132 L112 134 M100 110 L132 110 M120 88 L146 86"]),
    ("arcoiris", "un arcoíris", ["arcoiris", "arco iris"], "sky", [
        "M20 170 C20 70 180 70 180 170", "M36 170 C36 88 164 88 164 170", "M52 170 C52 106 148 106 148 170",
        "M68 170 C68 124 132 124 132 170"]),
    ("camino", "un camino", ["camino", "sendero", "viaje", "aventura"], "ground", [
        "M40 190 C70 160 88 130 96 100 C100 84 98 70 96 56",
        "M160 190 C130 160 112 130 108 100 C106 84 106 70 104 56",
        "M100 182 L100 166 M101 150 L101 138 M102 124 L102 116 M102 104 L102 98",
        "M10 60 C40 46 70 58 96 56 M104 56 C130 54 160 44 190 58"]),
    ("puerta", "una puerta", ["puerta", "portal", "entrada"], "ground", [
        "M56 190 L56 70 C56 26 144 26 144 70 L144 190 Z",
        "M70 190 L70 76 C70 42 130 42 130 76 L130 190",
        circle(118, 128, 5), "M40 190 L160 190"]),
    ("caballo", "un caballo", ["caballo", "caballos", "yegua", "pony", "unicornio"], "ground", [
        "M40 186 L46 132 C40 120 40 104 52 96 C70 86 100 92 120 88 L132 58 L124 46 L150 30 L176 56 "
        "L168 64 L152 58 L148 90 L150 132 L154 186 L142 186 L138 140 L120 128 L80 130 L72 186 L60 186 "
        "L62 138 L56 138 L54 186 Z",
        "M52 98 C36 100 26 114 24 136",
        "M132 58 C128 50 130 40 140 34", "M156 44 C158 42 160 42 160 44"]),
]

def tokenize(d):
    for m in re.finditer(r"[MLCQHVZmlcqhvz]|-?\d*\.?\d+(?:e-?\d+)?", d):
        yield m.group(0)

def parse_path(d):
    """Devuelve subtrazos (listas de segmentos). Admite M L H V C Q Z absolutos."""
    toks = list(tokenize(d))
    i, cmd = 0, None
    cur = start = (0.0, 0.0)
    subpaths, segs = [], []
    def num():
        nonlocal i
        v = float(toks[i]); i += 1; return v
    while i < len(toks):
        if re.match(r"[A-Za-z]", toks[i]):
            cmd = toks[i]; i += 1
            if cmd in "Zz":
                if cur != start: segs.append(("L", cur, start))
                cur = start
                subpaths.append(segs); segs = []
                continue
        if cmd == "M":
            if segs: subpaths.append(segs); segs = []
            cur = start = (num(), num()); cmd = "L"
        elif cmd == "L":
            p = (num(), num()); segs.append(("L", cur, p)); cur = p
        elif cmd == "H":
            p = (num(), cur[1]); segs.append(("L", cur, p)); cur = p
        elif cmd == "V":
            p = (cur[0], num()); segs.append(("L", cur, p)); cur = p
        elif cmd == "C":
            c1 = (num(), num()); c2 = (num(), num()); p = (num(), num())
            segs.append(("C", cur, c1, c2, p)); cur = p
        elif cmd == "Q":
            c1 = (num(), num()); p = (num(), num())
            segs.append(("Q", cur, c1, p)); cur = p
        else:
            raise ValueError("comando no admitido " + str(cmd))
    if segs: subpaths.append(segs)
    return subpaths

def seg_point(s, t):
    if s[0] == "L":
        (x0, y0), (x1, y1) = s[1], s[2]
        return (x0 + (x1 - x0) * t, y0 + (y1 - y0) * t)
    if s[0] == "Q":
        p0, p1, p2 = s[1], s[2], s[3]; u = 1 - t
        return (u*u*p0[0] + 2*u*t*p1[0] + t*t*p2[0], u*u*p0[1] + 2*u*t*p1[1] + t*t*p2[1])
    p0, p1, p2, p3 = s[1], s[2], s[3], s[4]; u = 1 - t
    return (u**3*p0[0] + 3*u*u*t*p1[0] + 3*u*t*t*p2[0] + t**3*p3[0],
            u**3*p0[1] + 3*u*u*t*p1[1] + 3*u*t*t*p2[1] + t**3*p3[1])

def dense(segs):
    pts = []
    for s in segs:
        n = 2 if s[0] == "L" else 40
        for k in range(n + 1):
            if pts and k == 0: continue
            pts.append(seg_point(s, k / n))
    return pts

def resample(pts, spacing):
    if len(pts) < 2: return pts
    out = [pts[0]]; carry = 0.0
    for (x0, y0), (x1, y1) in zip(pts, pts[1:]):
        L = math.hypot(x1 - x0, y1 - y0)
        if L == 0: continue
        pos = spacing - carry
        while pos <= L:
            t = pos / L
            out.append((x0 + (x1 - x0) * t, y0 + (y1 - y0) * t))
            pos += spacing
        carry = L - (pos - spacing)
    if math.hypot(out[-1][0] - pts[-1][0], out[-1][1] - pts[-1][1]) > spacing * .3:
        out.append(pts[-1])
    return out

def length(pts):
    return sum(math.hypot(b[0] - a[0], b[1] - a[1]) for a, b in zip(pts, pts[1:]))

def finish(strokes, budget=720, min_pts=12, max_pts=360):
    """Normaliza a 0..1000 conservando proporciones y reparte puntos por longitud."""
    xs = [x for s in strokes for x, _ in s]; ys = [y for s in strokes for _, y in s]
    x0, x1, y0, y1 = min(xs), max(xs), min(ys), max(ys)
    scale = 1000.0 / max(x1 - x0, y1 - y0, 1e-6)
    ox = (1000 - (x1 - x0) * scale) / 2; oy = (1000 - (y1 - y0) * scale) / 2
    norm = [[((x - x0) * scale + ox, (y - y0) * scale + oy) for x, y in s] for s in strokes]
    total = sum(length(s) for s in norm)
    out = []
    for s in norm:
        L = length(s)
        n = max(min_pts, min(max_pts, int(budget * L / max(total, 1e-6))))
        r = resample(s, max(L / n, 1e-3))
        if len(r) >= 2:
            out.append([round(v, 1) for p in r for v in p])
    return out, round((x1 - x0) / max(y1 - y0, 1e-6), 3)

def motif_strokes(paths):
    strokes = []
    for d in paths:
        for segs in parse_path(d):
            pts = dense(segs)
            if len(pts) >= 2: strokes.append(pts)
    return strokes

def mirror(d, width=853):
    """Refleja un path de pares x,y (solo M L C) respecto al eje vertical de la ilustración."""
    out, pair = [], 0
    for tok in tokenize(d):
        if re.match(r"[A-Za-z]", tok):
            out.append(tok); pair = 0; continue
        v = float(tok)
        out.append("%.1f" % (width - v if pair % 2 == 0 else v)); pair += 1
    return " ".join(out)

def spline(points):
    """Catmull-Rom → Bézier: una curva suave que pasa por los puntos medidos."""
    d = "M%.1f %.1f" % points[0]
    for i in range(len(points) - 1):
        p0 = points[max(0, i - 1)]; p1 = points[i]; p2 = points[i + 1]; p3 = points[min(len(points) - 1, i + 2)]
        c1 = (p1[0] + (p2[0] - p0[0]) / 6, p1[1] + (p2[1] - p0[1]) / 6)
        c2 = (p2[0] - (p3[0] - p1[0]) / 6, p2[1] - (p3[1] - p1[1]) / 6)
        d += " C%.1f %.1f %.1f %.1f %.1f %.1f" % (c1 + c2 + p2)
    return d

def ellipse(cx, cy, rx, ry):
    k = 0.5522847498
    return ("M%.1f %.1f C%.1f %.1f %.1f %.1f %.1f %.1f C%.1f %.1f %.1f %.1f %.1f %.1f "
            "C%.1f %.1f %.1f %.1f %.1f %.1f C%.1f %.1f %.1f %.1f %.1f %.1f Z") % (
        cx + rx, cy, cx + rx, cy + k * ry, cx + k * rx, cy + ry, cx, cy + ry,
        cx - k * rx, cy + ry, cx - rx, cy + k * ry, cx - rx, cy,
        cx - rx, cy - k * ry, cx - k * rx, cy - ry, cx, cy - ry,
        cx + k * rx, cy - ry, cx + rx, cy - k * ry, cx + rx, cy)

# Autorretrato en coordenadas de base.png (853x1844): lado izquierdo, que se refleja.
SELF_SIDE = [
    "M300 205 C268 330 214 440 150 560 C96 660 44 760 46 820 C50 900 104 980 196 1040",   # melena exterior
    "M296 270 C262 420 196 600 120 800",                                                   # mechones
    "M318 300 C300 480 250 700 214 960",
    "M392 298 C360 308 330 316 313 330 C296 380 282 450 262 520 C246 570 220 640 190 712 "
    "C176 744 162 770 153 787",                                                            # hombro y brazo exterior
    "M345 390 C330 460 300 540 272 610 C246 680 210 744 178 794",                          # brazo interior
    "M153 787 C130 830 112 870 100 930 M164 798 C150 846 140 884 134 934 "
    "M176 796 C178 840 172 882 162 922",                                                   # dedos
    # costado, cadera y pierna exterior medidos sobre la silueta (filas de 100 px)
    "M345 390 C336 450 340 530 356 600 C340 650 306 700 296 770",
    None,
    None,
    "M350 1790 C380 1800 405 1800 420 1790",                                               # pie
    "M335 410 C338 470 380 500 427 476",                                                   # pecho
    "M396 250 L392 298",                                                                   # cuello
    ellipse(314, 112, 24, 50),                                                             # auricular
]
SELF_SIDE[SELF_SIDE.index(None)] = spline([(296, 770), (300, 880), (318, 980), (326, 1050), (333, 1150),
    (313, 1250), (324, 1350), (353, 1450), (361, 1550), (352, 1650), (339, 1750), (350, 1790)])
SELF_SIDE[SELF_SIDE.index(None)] = spline([(427, 940), (420, 1050), (423, 1150), (419, 1250), (421, 1350),
    (411, 1450), (416, 1550), (421, 1650), (420, 1750), (420, 1790)])

SELF_CENTER = [
    "M346 118 C344 170 372 228 427 252 C482 228 510 170 508 118",                          # rostro
    "M300 205 C286 110 330 38 427 32 C524 38 568 110 553 205",                             # cabeza
    "M346 118 C372 96 384 78 394 56 C402 88 420 108 446 118 M430 58 C452 86 480 102 508 118",  # flequillo
    "M358 170 C366 160 392 158 402 168", "M444 168 C454 158 480 160 488 170",              # ojos
    circle(380, 172, 8), circle(466, 172, 8),
    "M424 196 C426 200 428 202 426 204", "M410 222 C420 227 432 227 442 222",              # nariz y boca
]

def self_portrait():
    """Salve se dibuja a sí misma: su figura (pelo, rostro, traje) y los circuitos de base.png."""
    import numpy as np, cv2
    paths = SELF_CENTER + SELF_SIDE + [mirror(d) for d in SELF_SIDE]
    strokes = motif_strokes(paths)
    im = cv2.imread(BASE, cv2.IMREAD_UNCHANGED)
    hsv = cv2.cvtColor(im[:, :, :3], cv2.COLOR_BGR2HSV)
    h, s, v = hsv[:, :, 0], hsv[:, :, 1], hsv[:, :, 2]
    circ = ((h > 90) & (h < 125) & (s > 110) & (v > 150) & (im[:, :, 3] > 200)).astype(np.uint8) * 255
    circ[140:200, 340:510] = 0
    circ = cv2.dilate(circ, np.ones((5, 5), np.uint8))
    cs, _ = cv2.findContours(circ, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_NONE)
    cs = sorted([c for c in cs if cv2.arcLength(c, True) > 90], key=lambda c: -cv2.arcLength(c, True))[:20]
    for c in sorted(cs, key=lambda c: c[:, 0, 1].min()):
        poly = cv2.approxPolyDP(c.astype(np.float32), 1.6, True)[:, 0, :].tolist()
        strokes.append([tuple(p) for p in poly] + [tuple(poly[0])])
    # El lienzo del autorretrato es la ilustración completa (no se recorta a los trazos).
    strokes.append([(0.0, 0.0), (0.0, 0.0)]); strokes.append([(853.0, 1844.0), (853.0, 1844.0)])
    return strokes

def main():
    preview = None
    if "--preview" in sys.argv:
        preview = sys.argv[sys.argv.index("--preview") + 1]
    library = {"version": 1, "units": 1000, "motifs": []}
    for mid, name, words, place, paths in MOTIFS:
        strokes, aspect = finish(motif_strokes(paths))
        library["motifs"].append({"id": mid, "name": name, "words": words, "place": place,
                                  "aspect": aspect, "strokes": strokes})
    strokes, aspect = finish(self_portrait(), budget=1400, min_pts=10, max_pts=700)
    library["motifs"].append({"id": "salve", "name": "a mí misma, Salve", "words": [
        "salve", "ti misma", "autorretrato", "tu cuerpo", "tu cuerpo astral", "tu figura"],
        "place": "any", "aspect": aspect, "strokes": strokes, "self": True})
    os.makedirs(os.path.dirname(OUT), exist_ok=True)
    with open(OUT, "w", encoding="utf-8") as f:
        json.dump(library, f, ensure_ascii=False, separators=(",", ":"))
    print("motivos:", len(library["motifs"]), "bytes:", os.path.getsize(OUT))
    if preview:
        render_preview(library, preview)

def render_preview(library, folder):
    import numpy as np, cv2
    os.makedirs(folder, exist_ok=True)
    cell = 220; motifs = library["motifs"]; cols = 6
    rows = (len(motifs) + cols - 1) // cols
    sheet = np.full((rows * cell, cols * cell, 3), (40, 26, 14), np.uint8)
    for k, m in enumerate(motifs):
        ox, oy = (k % cols) * cell + 10, (k // cols) * cell + 10
        for s in m["strokes"]:
            pts = np.array([(ox + s[i] * .18, oy + s[i + 1] * .18) for i in range(0, len(s), 2)], np.int32)
            cv2.polylines(sheet, [pts], False, (255, 230, 150), 1, cv2.LINE_AA)
        cv2.putText(sheet, m["id"], (ox, oy + 200), cv2.FONT_HERSHEY_SIMPLEX, .45, (200, 200, 200), 1, cv2.LINE_AA)
    cv2.imwrite(os.path.join(folder, "library.png"), sheet)

if __name__ == "__main__":
    main()
