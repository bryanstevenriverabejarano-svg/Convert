#!/usr/bin/env python3
"""Valida una propuesta de Salve, la prueba aisladamente y abre un PR."""

from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path, PurePosixPath
import re
import shutil
import subprocess
import sys
import tempfile
import uuid

if __package__:
    from . import auto_improvement_sandbox as sandbox
else:
    import auto_improvement_sandbox as sandbox


ALLOWED_SOURCE_ROOT = PurePosixPath("app/src/main/java/salve/core")
JAVA_SOURCE_ROOT = PurePosixPath("app/src/main/java")
JAVA_TEST_ROOT = PurePosixPath("app/src/test/java")
MAX_PROPOSAL_BYTES = 1_000_000
MAX_PATCH_BYTES = 500_000
MAX_GENERATED_TEST_BYTES = 250_000


class ProposalError(ValueError):
    pass


def run(command: list[str], cwd: Path, *, capture: bool = False) -> subprocess.CompletedProcess[str]:
    return subprocess.run(command, cwd=cwd, check=True, text=True, capture_output=capture)


def unique_fields(pairs: list[tuple[str, object]]) -> dict:
    result = {}
    for key, value in pairs:
        if key in result:
            raise ProposalError(f"Campo JSON duplicado: {key}")
        result[key] = value
    return result


def load_proposal(path: Path) -> dict:
    if not path.is_file() or path.stat().st_size > MAX_PROPOSAL_BYTES:
        raise ProposalError("La propuesta no existe o supera el tamaño permitido")
    try:
        proposal = json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_fields)
    except (OSError, UnicodeError, json.JSONDecodeError) as error:
        raise ProposalError(f"JSON de propuesta inválido: {error}") from error
    validate_proposal(proposal)
    return proposal


def validate_proposal(proposal: dict) -> None:
    if not isinstance(proposal, dict):
        raise ProposalError("La propuesta debe ser un objeto JSON")
    if type(proposal.get("schemaVersion")) is not int or proposal["schemaVersion"] != 3:
        raise ProposalError("Versión de esquema no compatible")
    for field in ("proposalId", "targetPath", "targetClass", "issueSummary"):
        if not isinstance(proposal.get(field), str) or not proposal[field].strip():
            raise ProposalError(f"Campo de texto inválido: {field}")
    try:
        parsed_id = uuid.UUID(proposal["proposalId"])
    except ValueError as error:
        raise ProposalError("Identificador de propuesta inválido") from error
    if str(parsed_id) != proposal["proposalId"]:
        raise ProposalError("El UUID debe usar su representación canónica")
    target = PurePosixPath(proposal["targetPath"])
    if target.is_absolute() or ".." in target.parts:
        raise ProposalError("Ruta objetivo insegura")
    try:
        target.relative_to(ALLOWED_SOURCE_ROOT)
    except ValueError as error:
        raise ProposalError("La ruta queda fuera del núcleo permitido") from error
    if target.suffix != ".java":
        raise ProposalError("La propuesta solo puede modificar código Java del núcleo")
    if proposal.get("syntheticValidationPassed") is not True:
        raise ProposalError("La validación sintética de origen no fue superada")
    if proposal.get("ethicalReviewPassed") is not True:
        raise ProposalError("La revisión ética de origen no fue superada")
    patch = proposal.get("patch")
    if not isinstance(patch, str) or not patch.startswith("diff --git "):
        raise ProposalError("El parche no es un unified diff")
    if len(patch.encode("utf-8")) > MAX_PATCH_BYTES:
        raise ProposalError("El parche supera el tamaño permitido")
    if changed_paths(patch) != {target.as_posix()}:
        raise ProposalError("El parche modifica rutas distintas del objetivo declarado")
    if "sourceRevision" in proposal or "sourceSha256" in proposal:
        for field, length in (("sourceRevision", 40), ("sourceSha256", 64)):
            value = proposal.get(field)
            if not isinstance(value, str) or re.fullmatch(r"[a-f0-9]{" + str(length) + r"}", value) is None:
                raise ProposalError(f"Identidad de fuente inválida: {field}")
    generated_test(proposal)


def generated_test(proposal: dict) -> tuple[str, str] | None:
    """Bound one Java test compilation unit to a derived path, never a model-supplied path.

    This is a structural precheck only. Compilation and test execution stay inside Docker.
    Missing/empty suites preserve compatibility with older schema-3 proposals explicitly.
    """
    source = proposal.get("generatedTests", "")
    if not isinstance(source, str):
        raise ProposalError("generatedTests debe ser código Java en texto")
    try:
        size = len(source.encode("utf-8"))
    except UnicodeError as invalid:
        raise ProposalError("La suite generada contiene Unicode inválido") from invalid
    if size > MAX_GENERATED_TEST_BYTES:
        raise ProposalError("La suite generada supera el tamaño permitido")
    if not source.strip():
        return None
    if "\x00" in source or re.search(r"\\u+[0-9a-fA-F]{4}", source):
        raise ProposalError("La suite contiene escapes o bytes incompatibles con el prechequeo")
    target = PurePosixPath(proposal["targetPath"])
    relative = target.relative_to(JAVA_SOURCE_ROOT)
    if any(re.fullmatch(r"[A-Za-z_$][A-Za-z0-9_$]*", part) is None
           for part in (*relative.parts[:-1], target.stem)):
        raise ProposalError("La ruta Java no permite derivar un paquete de pruebas inequívoco")
    test_class = target.stem + "TestHarness"
    package = ".".join(relative.parts[:-1])
    # Mask comments and literal values before looking for declarations; the compiler is authoritative.
    masked = re.sub(r'//[^\r\n]*|/\*[\s\S]*?\*/|"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'',
                    lambda match: re.sub(r"[^\r\n]", " ", match.group()), source)
    declarations = re.findall(r"\bpublic\s+(?:(?:final|strictfp)\s+)*class\s+([A-Za-z_$][A-Za-z0-9_$]*)\b", masked)
    if declarations != [test_class]:
        raise ProposalError(f"La suite debe declarar una única clase pública {test_class}")
    packages = re.findall(r"\bpackage\s+([A-Za-z_$][A-Za-z0-9_$]*(?:\s*\.\s*[A-Za-z_$][A-Za-z0-9_$]*)*)\s*;", masked)
    if packages and (len(packages) != 1 or re.sub(r"\s+", "", packages[0]) != package):
        raise ProposalError("El paquete de la suite no coincide con el objetivo")
    if not re.search(r"@(?:org\s*\.\s*junit\s*\.\s*)?Test\b", masked):
        raise ProposalError("La suite debe contener al menos un método JUnit @Test")
    if re.search(r"@(?:org\s*\.\s*junit\s*\.\s*)?Ignore\b", masked):
        raise ProposalError("La suite generada no puede desactivar sus pruebas con @Ignore")
    normalized = source if packages else f"package {package};\n\n" + source
    path = JAVA_TEST_ROOT.joinpath(*relative.parts[:-1], test_class + ".java").as_posix()
    return path, normalized


def install_generated_test(proposal: dict, worktree: Path) -> tuple[str, str] | None:
    suite = generated_test(proposal)
    if suite is None:
        return None
    relative, source = suite
    destination = worktree / relative
    cursor = worktree
    for part in PurePosixPath(relative).parts[:-1]:
        cursor /= part
        if cursor.is_symlink() or (cursor.exists() and not cursor.is_dir()):
            raise ProposalError("La ruta de pruebas contiene un enlace o un directorio inválido")
    if destination.exists() or destination.is_symlink():
        raise ProposalError("La suite generada colisiona con un archivo existente; no se sobrescribe")
    try:
        destination.parent.mkdir(parents=True, exist_ok=True)
        with destination.open("x", encoding="utf-8", newline="") as output:
            output.write(source)
        destination.chmod(0o644)
    except OSError as unavailable:
        raise ProposalError("No se pudo añadir la suite generada al snapshot") from unavailable
    run(["git", "add", "--", relative], worktree)
    entry = run(["git", "ls-files", "--stage", "--", relative], worktree, capture=True).stdout
    if not entry.startswith("100644 "):
        raise ProposalError("La suite debe ser un archivo Java regular")
    indexed_source = subprocess.run(["git", "show", ":" + relative], cwd=worktree,
                                    check=True, capture_output=True).stdout
    return relative, hashlib.sha256(indexed_source).hexdigest()


def check_source_context(proposal: dict, repo: Path) -> None:
    """New proposals require byte-identical source, even if a stale hunk would still apply."""
    if "sourceSha256" not in proposal:
        return  # Existing schema-3 proposals predate exact-source export.
    result = subprocess.run(["git", "show", "HEAD:" + proposal["targetPath"]], cwd=repo,
                            check=True, capture_output=True)
    if hashlib.sha256(result.stdout).hexdigest() != proposal["sourceSha256"]:
        raise ProposalError("La fuente cambió desde la instantánea; exporta una revisión nueva y regenera el parche")


def changed_paths(patch: str) -> set[str]:
    paths: set[str] = set()
    for line in patch.splitlines():
        if not line.startswith("diff --git a/"):
            continue
        match = re.fullmatch(r"diff --git a/(\S+) b/(\S+)", line)
        if match is None or match.group(1) != match.group(2):
            raise ProposalError("Cabecera diff inválida o renombrado no permitido")
        paths.add(match.group(1))
    return paths


def slug(value: str) -> str:
    normalized = re.sub(r"[^a-z0-9]+", "-", value.lower()).strip("-")
    return normalized[:40] or "change"


def find_existing_pr(repo: Path, proposal_id: str) -> str | None:
    result = run(
        ["gh", "pr", "list", "--state", "all", "--limit", "1",
         "--search", f'"salve-proposal-id:{proposal_id}" in:body', "--json", "url"],
        repo,
        capture=True,
    )
    try:
        matches = json.loads(result.stdout)
    except json.JSONDecodeError as error:
        raise ProposalError("GitHub devolvió una respuesta de deduplicación inválida") from error
    if not isinstance(matches, list) or not matches:
        return None
    url = matches[0].get("url")
    return url if isinstance(url, str) and url.startswith("https://") else None


def execute(proposal_path: Path, repo: Path, base: str, dry_run: bool) -> str:
    proposal = load_proposal(proposal_path)
    repo = repo.resolve()
    run(["git", "rev-parse", "--show-toplevel"], repo, capture=True)
    if not dry_run:
        existing_pr = find_existing_pr(repo, proposal["proposalId"])
        if existing_pr:
            return existing_pr
    image = sandbox.configured_image()
    run(["git", "fetch", "origin", base], repo)
    branch = (
        f"salve/auto-{proposal['proposalId'][:8]}-"
        f"{slug(proposal.get('targetClass', 'change'))}"
    )

    with tempfile.TemporaryDirectory(prefix="salve-auto-") as temporary:
        worktree = Path(temporary) / "worktree"
        run(["git", "worktree", "add", "--detach", str(worktree), f"origin/{base}"], repo)
        branch_created = False
        try:
            check_source_context(proposal, worktree)
            patch_file = Path(temporary) / "proposal.patch"
            patch_file.write_text(proposal["patch"], encoding="utf-8")
            run(["git", "apply", "--index", "--check", str(patch_file)], worktree)
            run(["git", "apply", "--index", str(patch_file)], worktree)
            changed = run(["git", "diff", "--cached", "--name-status", "--no-renames", "-z"],
                          worktree, capture=True).stdout
            if changed != "M\0" + proposal["targetPath"] + "\0":
                raise ProposalError("El índice debe contener solo una modificación del objetivo")
            entry = run(["git", "ls-files", "--stage", "--", proposal["targetPath"]],
                        worktree, capture=True).stdout
            if not entry.startswith("100644 "):
                raise ProposalError("El objetivo debe seguir siendo un archivo Java regular")
            generated = install_generated_test(proposal, worktree)
            expected_changes = "M\0" + proposal["targetPath"] + "\0"
            if generated is not None:
                expected_changes += "A\0" + generated[0] + "\0"
            final_changes = run(["git", "diff", "--cached", "--name-status", "--no-renames", "-z"],
                                worktree, capture=True).stdout
            if final_changes != expected_changes:
                raise ProposalError("El snapshot debe contener solo el objetivo y la nueva suite declarada")
            tested_tree = run(["git", "write-tree"], worktree, capture=True).stdout.strip()
            archive = Path(temporary) / "source.tar"
            run(["git", "archive", "--format=tar", "--output=" + str(archive), tested_tree], worktree)
            sandbox.run_sandbox(archive, image)
            if dry_run:
                return branch
            run(["git", "switch", "-c", branch], worktree)
            branch_created = True
            run(["git", "commit", "-m", f"Auto-mejora: {proposal['targetClass']}"], worktree)
            committed_tree = run(["git", "rev-parse", "HEAD^{tree}"], worktree, capture=True).stdout.strip()
            if committed_tree != tested_tree:
                raise ProposalError("El commit difiere del árbol probado; no se publica")
            run(["git", "push", "--set-upstream", "origin", branch], worktree)
            source_note = (
                f"Fuente de contexto: {proposal['sourceRevision']}\nSHA-256 de fuente: {proposal['sourceSha256']}\n\n"
                if "sourceRevision" in proposal
                else "Propuesta anterior sin huella de fuente; solo comprobación del diff y pruebas.\n\n"
            )
            test_note = (
                f"Suite generada añadida al árbol probado: {generated[0]}\n"
                f"SHA-256 de la suite añadida: {generated[1]}\n"
                "El prechequeo de la suite es estructural; la compilación y la tarea de pruebas se ejecutan en Docker.\n\n"
                if generated is not None
                else "Propuesta sin suite generada: se ejecutaron únicamente las pruebas ya versionadas.\n\n"
            )
            body = (
                "Propuesta generada por Salve; tarea testDebugUnitTest ejecutada en Docker sin red.\n\n"
                f"Diagnóstico:\n{proposal.get('issueSummary', '')}\n\n"
                f"Árbol probado: {tested_tree}\nImagen del sandbox: {image}\n\n"
                f"{source_note}{test_note}"
                f"salve-proposal-id:{proposal['proposalId']}"
            )
            body_file = Path(temporary) / "pr-body.md"
            body_file.write_text(body, encoding="utf-8")
            result = run(
                ["gh", "pr", "create", "--base", base, "--head", branch,
                 "--title", f"Auto-mejora: {proposal['targetClass']}", "--body-file", str(body_file)],
                worktree,
                capture=True,
            )
            return result.stdout.strip()
        finally:
            subprocess.run(
                ["git", "worktree", "remove", "--force", str(worktree)],
                cwd=repo,
                check=False,
                text=True,
                capture_output=True,
            )
            if branch_created:
                subprocess.run(
                    ["git", "branch", "-D", branch],
                    cwd=repo,
                    check=False,
                    text=True,
                    capture_output=True,
                )


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("proposal", type=Path)
    parser.add_argument("--repo", type=Path, default=Path.cwd())
    parser.add_argument("--base", default="main")
    parser.add_argument("--dry-run", action="store_true")
    args = parser.parse_args()
    if not shutil.which("git"):
        parser.error("git no está disponible")
    if not args.dry_run and not shutil.which("gh"):
        parser.error("gh no está disponible o no está autenticado")
    try:
        result = execute(args.proposal.resolve(), args.repo, args.base, args.dry_run)
    except KeyboardInterrupt:
        return 130
    except (ProposalError, sandbox.SandboxError, subprocess.CalledProcessError) as error:
        print(f"Auto-mejora rechazada: {error}", file=sys.stderr)
        return 1
    print(result)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
