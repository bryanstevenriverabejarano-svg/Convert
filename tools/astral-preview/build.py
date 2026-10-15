#!/usr/bin/env python3
"""Genera una vista previa autónoma (HTML) del cuerpo astral y del dibujo por epiciclos.

Usa los mismos recursos que la app: base.png, astral_eyes.png, astral_glow.png, astral.json y la
biblioteca de trazos. Uso: python3 tools/astral-preview/build.py salida.html
"""
import base64, os, sys
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, "..", "..")
A = os.path.join(ROOT, "app", "src", "main", "assets")
def uri(p): return "data:image/png;base64," + base64.b64encode(open(p, "rb").read()).decode()
html = open(os.path.join(HERE, "template.html"), encoding="utf-8").read()
html = (html.replace("__BASE__", uri(os.path.join(A, "avatar/realistic/base.png")))
        .replace("__EYES__", uri(os.path.join(A, "avatar/realistic/astral_eyes.png")))
        .replace("__GLOW__", uri(os.path.join(A, "avatar/realistic/astral_glow.png")))
        .replace("__RIG__", open(os.path.join(A, "avatar/realistic/astral.json"), encoding="utf-8").read())
        .replace("__LIB__", open(os.path.join(A, "sketch/library.json"), encoding="utf-8").read()))
out = sys.argv[1] if len(sys.argv) > 1 else os.path.join(HERE, "salve-astral.html")
open(out, "w", encoding="utf-8").write(html)
print(out, len(html) // 1024, "KiB")
