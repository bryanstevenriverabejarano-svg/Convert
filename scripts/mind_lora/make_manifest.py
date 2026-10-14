#!/usr/bin/env python3
"""Escribe salve-lora.json junto al adaptador GGUF: modelo base, escala y SHA-256.

La app solo aplica el adaptador si el nombre del archivo base coincide con el modelo GGUF activo
y el SHA-256 del adaptador es correcto.
"""
import argparse
import datetime
import hashlib
import json
from pathlib import Path


def sha256(path):
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for block in iter(lambda: stream.read(1 << 16), b""):
            digest.update(block)
    return digest.hexdigest()


def manifest(adapter, base_filename, scale):
    if not 0 <= scale <= 2:
        raise ValueError("La escala debe estar entre 0 y 2")
    return {
        "base": base_filename,
        "scale": scale,
        "sha256": sha256(adapter),
        "trainedOn": datetime.date.today().isoformat(),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--adapter", type=Path, required=True, help="salve-lora.gguf")
    parser.add_argument("--base-filename", required=True,
                        help="Nombre exacto del GGUF base en el móvil, p. ej. Dolphin3.0-Llama3.2-3B-Q4_K_M.gguf")
    parser.add_argument("--scale", type=float, default=0.8)
    args = parser.parse_args()
    data = manifest(args.adapter, args.base_filename, args.scale)
    target = args.adapter.with_suffix(".json")
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"Manifiesto escrito en {target}")


if __name__ == "__main__":
    main()
