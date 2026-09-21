#!/usr/bin/env python3
"""Exporta fuentes exactas de una revisión Git para el contexto de auto-mejora de Salve."""

from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import tempfile


MAX_FILES = 8
MAX_SOURCE_CHARS = 8_000
MAX_BYTES = 256_000
PATH_PATTERN = re.compile(r"app/src/main/java/salve/core/[A-Za-z_][A-Za-z0-9_]*\.java")


def git(repo: Path, *args: str) -> bytes:
    return subprocess.run(["git", *args], cwd=repo, check=True, capture_output=True).stdout


def export_snapshot(repo: Path, revision: str, paths: list[str], destination: Path) -> dict:
    if not revision or revision.startswith("-"):
        raise ValueError("Revisión inválida")
    if not 1 <= len(paths) <= MAX_FILES or len(set(paths)) != len(paths):
        raise ValueError("Selecciona de uno a ocho archivos distintos")
    for path in paths:
        if PATH_PATTERN.fullmatch(path) is None:
            raise ValueError("Solo se permiten archivos Java directos de salve/core")
    commit = git(repo, "rev-parse", "--verify", revision + "^{commit}").decode("ascii").strip()
    if re.fullmatch(r"[a-f0-9]{40}", commit) is None:
        raise ValueError("Revisión Git no compatible")
    files = []
    for path in paths:
        entry = git(repo, "ls-tree", commit, "--", path).decode("utf-8")
        if not entry.startswith("100644 blob "):
            raise ValueError(f"La fuente no es un archivo regular versionado: {path}")
        if int(git(repo, "cat-file", "-s", commit + ":" + path)) > MAX_SOURCE_CHARS * 4:
            raise ValueError(f"La fuente supera el presupuesto: {path}")
        content = git(repo, "show", commit + ":" + path)
        source = content.decode("utf-8")
        if not source.strip() or "\0" in source or len(source.encode("utf-16-le")) // 2 > MAX_SOURCE_CHARS:
            raise ValueError(f"Fuente vacía o demasiado grande; no se trunca: {path}")
        files.append({"path": path, "sha256": hashlib.sha256(content).hexdigest(), "source": source})
    snapshot = {"schemaVersion": 1, "revision": commit, "files": files}
    encoded = (json.dumps(snapshot, ensure_ascii=False, indent=2) + "\n").encode("utf-8")
    if len(encoded) > MAX_BYTES:
        raise ValueError("La instantánea supera el tamaño permitido")
    destination = destination.resolve()
    destination.parent.mkdir(parents=True, exist_ok=True)
    descriptor, temporary = tempfile.mkstemp(prefix=destination.name + ".", suffix=".tmp", dir=destination.parent)
    try:
        with os.fdopen(descriptor, "wb") as output:
            output.write(encoded)
        os.replace(temporary, destination)
    finally:
        if os.path.exists(temporary):
            os.unlink(temporary)
    return snapshot


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("paths", nargs="+")
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--revision", default="HEAD")
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    try:
        snapshot = export_snapshot(args.repo, args.revision, args.paths, args.output)
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        parser.exit(1, f"No se exportó la fuente: {error}\n")
    print(f"Exportados {len(snapshot['files'])} archivos de {snapshot['revision']} a {args.output}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
