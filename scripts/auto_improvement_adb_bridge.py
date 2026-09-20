#!/usr/bin/env python3
"""Transporta propuestas desde una app depurable al ejecutor externo."""

from __future__ import annotations

import argparse
from contextlib import contextmanager
import fcntl
import hashlib
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time

import auto_improvement_runner


DEFAULT_PACKAGE = "com.salve.app"
OUTBOX = "files/auto-improvement/outbox"
PROCESSED = "files/auto-improvement/processed"
PACKAGE_PATTERN = re.compile(r"[A-Za-z][A-Za-z0-9_]*(?:\.[A-Za-z][A-Za-z0-9_]*)+")
PROPOSAL_PATTERN = re.compile(r"[0-9]+-[0-9a-fA-F-]+\.json")


class BridgeError(RuntimeError):
    pass


@contextmanager
def single_instance_lock(repo: Path):
    identity = hashlib.sha256(str(repo.resolve()).encode("utf-8")).hexdigest()[:16]
    lock_path = Path(tempfile.gettempdir()) / f"salve-auto-improvement-{identity}.lock"
    handle = lock_path.open("a+", encoding="utf-8")
    try:
        try:
            fcntl.flock(handle.fileno(), fcntl.LOCK_EX | fcntl.LOCK_NB)
        except BlockingIOError as error:
            raise BridgeError("Ya existe un supervisor activo para este repositorio") from error
        handle.seek(0)
        handle.truncate()
        handle.write(str(Path.cwd()))
        handle.flush()
        yield
    finally:
        try:
            fcntl.flock(handle.fileno(), fcntl.LOCK_UN)
        finally:
            handle.close()


def adb_prefix(serial: str | None) -> list[str]:
    return ["adb", "-s", serial] if serial else ["adb"]


def validate_identity(package: str, filename: str | None = None) -> None:
    if PACKAGE_PATTERN.fullmatch(package) is None:
        raise BridgeError("Identificador de paquete inválido")
    if filename is not None and PROPOSAL_PATTERN.fullmatch(filename) is None:
        raise BridgeError("Nombre de propuesta inválido")


def adb_text(serial: str | None, arguments: list[str], *, check: bool = True) -> str:
    result = subprocess.run(
        adb_prefix(serial) + arguments,
        check=check,
        text=True,
        capture_output=True,
    )
    return result.stdout.strip()


def list_proposals(package: str, serial: str | None) -> list[str]:
    validate_identity(package)
    result = subprocess.run(
        adb_prefix(serial) + ["shell", "run-as", package, "ls", "-1", OUTBOX],
        check=False,
        text=True,
        capture_output=True,
    )
    if result.returncode != 0:
        detail = (result.stderr or result.stdout).strip()
        if "No such file" in detail:
            return []
        raise BridgeError(f"No se pudo leer la bandeja privada: {detail}")
    proposals = []
    for filename in result.stdout.splitlines():
        filename = filename.strip()
        if not filename or filename.endswith(".tmp"):
            continue
        validate_identity(package, filename)
        proposals.append(filename)
    return sorted(proposals)


def fetch_proposal(package: str, serial: str | None, filename: str, destination: Path) -> None:
    validate_identity(package, filename)
    result = subprocess.run(
        adb_prefix(serial) + ["exec-out", "run-as", package, "cat", f"{OUTBOX}/{filename}"],
        check=False,
        capture_output=True,
    )
    if result.returncode != 0:
        raise BridgeError(result.stderr.decode("utf-8", errors="replace").strip())
    destination.write_bytes(result.stdout)
    auto_improvement_runner.load_proposal(destination)


def archive_proposal(package: str, serial: str | None, filename: str) -> None:
    validate_identity(package, filename)
    adb_text(serial, ["shell", "run-as", package, "mkdir", "-p", PROCESSED])
    adb_text(
        serial,
        ["shell", "run-as", package, "mv", f"{OUTBOX}/{filename}", f"{PROCESSED}/{filename}"],
    )


def process_next(package: str, serial: str | None, repo: Path, base: str, dry_run: bool) -> str:
    proposals = list_proposals(package, serial)
    if not proposals:
        return "No hay propuestas pendientes."
    filename = proposals[0]
    with tempfile.TemporaryDirectory(prefix="salve-adb-proposal-") as temporary:
        local_proposal = Path(temporary) / filename
        fetch_proposal(package, serial, filename, local_proposal)
        result = auto_improvement_runner.execute(local_proposal, repo, base, dry_run)
    if not dry_run:
        archive_proposal(package, serial, filename)
    return result


def supervise(package: str,
              serial: str | None,
              repo: Path,
              base: str,
              interval_seconds: int,
              max_consecutive_failures: int) -> None:
    failures = 0
    while True:
        try:
            result = process_next(package, serial, repo, base, False)
            print(result, flush=True)
            failures = 0
            time.sleep(interval_seconds)
        except KeyboardInterrupt:
            return
        except Exception as error:
            failures += 1
            print(f"Ciclo autónomo fallido ({failures}): {error}", file=sys.stderr, flush=True)
            if failures >= max_consecutive_failures:
                raise BridgeError(
                    f"Supervisor detenido tras {failures} fallos consecutivos"
                ) from error
            delay = min(interval_seconds * (2 ** failures), 15 * 60)
            time.sleep(delay)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--package", default=DEFAULT_PACKAGE)
    parser.add_argument("--serial")
    parser.add_argument("--base", default="main")
    parser.add_argument("--dry-run", action="store_true")
    parser.add_argument("--watch", action="store_true")
    parser.add_argument("--interval-seconds", type=int, default=60)
    parser.add_argument("--max-consecutive-failures", type=int, default=3)
    args = parser.parse_args()
    if not shutil.which("adb"):
        parser.error("adb no está disponible")
    if args.watch and args.dry_run:
        parser.error("--watch y --dry-run no pueden combinarse")
    if args.interval_seconds < 5:
        parser.error("--interval-seconds debe ser al menos 5")
    if args.max_consecutive_failures < 1:
        parser.error("--max-consecutive-failures debe ser al menos 1")
    try:
        with single_instance_lock(args.repo):
            if args.watch:
                supervise(
                    args.package,
                    args.serial,
                    args.repo,
                    args.base,
                    args.interval_seconds,
                    args.max_consecutive_failures,
                )
            else:
                print(process_next(args.package, args.serial, args.repo, args.base, args.dry_run))
    except (BridgeError, auto_improvement_runner.ProposalError,
            auto_improvement_runner.sandbox.SandboxError, subprocess.CalledProcessError) as error:
        print(f"Puente detenido: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
