#!/usr/bin/env python3
"""Convierte la issue «[Salve] Propuesta…» en un PR, en dos fases separadas.

prepare  (job sin permisos de escritura ni secretos): extrae la propuesta JSON de la issue, la valida
         con auto_improvement_runner, comprueba la huella de la fuente, aplica el parche sobre main y
         deja el árbol listo para que Gradle ejecute las pruebas. Nunca publica nada.
publish  (job con permiso de escritura que NO ejecuta código propuesto): vuelve a aplicar el mismo
         parche sobre la misma revisión, exige el mismo árbol probado, crea la rama salve/auto-…,
         la sube y abre el PR.
merge    Salve fusiona sola, solo si la compilación completa de Android (android.yml) termina en verde
         sobre exactamente el commit publicado. Bryan puede apagarlo con la variable SALVE_AUTOMERGE=false.
"""

from __future__ import annotations

import argparse
import json
import os
from pathlib import Path
import re
import subprocess
import sys
import time
import urllib.error
import urllib.request

if __package__:
    from . import auto_improvement_runner as runner
else:
    import auto_improvement_runner as runner

MARKER = "<!-- salve-self-pr v1 -->"
TITLE_PREFIX = "[Salve] "
MAX_BODY_CHARS = 65_536  # límite de GitHub para el cuerpo de una issue


def extract(body: str) -> dict:
    """La propuesta es el bloque ```json que sigue al marcador; el resto de la issue es para Bryan."""
    if not isinstance(body, str) or len(body) > MAX_BODY_CHARS:
        raise runner.ProposalError("La issue no contiene una propuesta legible")
    at = body.find(MARKER)
    if at < 0 or body.find(MARKER, at + 1) >= 0:
        raise runner.ProposalError("La issue debe contener exactamente un marcador de propuesta de Salve")
    match = re.match(r"\s*```json\n(.*?)\n```\s*$", body[at + len(MARKER):], re.DOTALL)
    if match is None:
        raise runner.ProposalError("Falta el bloque ```json de la propuesta al final de la issue")
    try:
        proposal = json.loads(match.group(1), object_pairs_hook=runner.unique_fields)
    except json.JSONDecodeError as error:
        raise runner.ProposalError(f"JSON de propuesta inválido: {error}") from error
    runner.validate_proposal(proposal)
    if "sourceSha256" not in proposal:
        raise runner.ProposalError("Las propuestas desde el teléfono deben llevar la huella de la fuente")
    check_limits(proposal)
    return proposal


MAX_CHANGED_LINES = 60
# Lo mismo que SelfPatch.RISKY en el teléfono: se vuelve a comprobar aquí porque Salve fusiona sola.
RISKY = re.compile(
    r"Runtime\s*\.\s*getRuntime|ProcessBuilder|\.exec\s*\(|DexClassLoader|PathClassLoader|System\s*\.\s*load|"
    r"setAccessible|Class\s*\.\s*forName|new\s+Socket|HttpURLConnection|URLConnection|https?://|"
    r"\.delete\s*\(|deleteRecursively|Files\s*\.\s*delete|PackageInstaller|Settings\s*\.\s*Secure")


def check_limits(proposal: dict) -> None:
    """Cambios pequeños y sin llamadas peligrosas: condición para que pueda fusionarse sin revisión humana."""
    added, removed = [], []
    for line in proposal["patch"].splitlines():
        if line.startswith("+++") or line.startswith("---"):
            continue
        if line.startswith("+"):
            added.append(line[1:])
        elif line.startswith("-"):
            removed.append(line[1:])
    if len(added) + len(removed) > MAX_CHANGED_LINES:
        raise runner.ProposalError(f"Cambia {len(added) + len(removed)} líneas; el máximo es {MAX_CHANGED_LINES}")
    risky = sorted({m.group() for m in RISKY.finditer("\n".join(added))})
    if risky:
        raise runner.ProposalError("Añade llamadas no permitidas: " + ", ".join(risky))
    if proposal.get("generatedTests", "").strip():
        raise runner.ProposalError("Las propuestas que se fusionan solas no pueden añadir suites generadas")


def apply(proposal: dict, worktree: Path) -> tuple[str, str | None]:
    """Aplica el parche (y la suite generada, si hay) y devuelve el árbol exacto que se prueba."""
    runner.check_source_context(proposal, worktree)
    patch_file = worktree / ".git-salve-proposal.patch"
    try:
        patch_file.write_text(proposal["patch"], encoding="utf-8")
        runner.run(["git", "apply", "--index", "--check", str(patch_file)], worktree)
        runner.run(["git", "apply", "--index", str(patch_file)], worktree)
    except subprocess.CalledProcessError as error:
        raise runner.ProposalError("El parche no se aplica limpiamente sobre main") from error
    finally:
        patch_file.unlink(missing_ok=True)
    changed = runner.run(["git", "diff", "--cached", "--name-status", "--no-renames", "-z"], worktree, capture=True).stdout
    if changed != "M\0" + proposal["targetPath"] + "\0":
        raise runner.ProposalError("El índice debe contener solo una modificación del objetivo")
    generated = runner.install_generated_test(proposal, worktree)
    tree = runner.run(["git", "write-tree"], worktree, capture=True).stdout.strip()
    # Deja también el árbol de trabajo igual que el índice, para que Gradle pruebe exactamente eso.
    runner.run(["git", "checkout-index", "-a", "-f"], worktree)
    return tree, generated[0] if generated else None


def branch_name(proposal: dict) -> str:
    return f"salve/auto-{proposal['proposalId'][:8]}-{runner.slug(proposal['targetClass'])}"


def pr_body(proposal: dict, issue: int, tree: str, generated: str | None) -> str:
    return (
        f"Propuesta escrita por Salve en su teléfono (issue #{issue}).\n\n"
        f"**Diagnóstico:** {proposal['issueSummary']}\n\n"
        f"- Revisión de la fuente: `{proposal['sourceRevision']}` (SHA-256 `{proposal['sourceSha256'][:16]}…` comprobado)\n"
        f"- Árbol probado: `{tree}`\n"
        f"- Suite generada: {generated or 'ninguna; se ejecutaron las pruebas ya versionadas'}\n"
        "- `testDebugUnitTest` superado en GitHub Actions antes de abrir este PR.\n\n"
        "Si la compilación completa de Android (`android.yml`) termina en verde sobre este mismo commit, "
        "**Salve lo fusiona sola** (salvo que Bryan ponga la variable `SALVE_AUTOMERGE=false`). "
        "Para deshacerlo basta con revertir el commit. Después, pídele «hazte el examen» para medir si mejoró.\n\n"
        f"salve-proposal-id:{proposal['proposalId']}\n"
    )


def github(method: str, path: str, token: str, payload: dict | None = None) -> tuple[int, dict]:
    request = urllib.request.Request(
        "https://api.github.com/repos/" + os.environ["GITHUB_REPOSITORY"] + path,
        method=method,
        data=None if payload is None else json.dumps(payload).encode("utf-8"),
        headers={"Authorization": f"Bearer {token}", "Accept": "application/vnd.github+json",
                 "X-GitHub-Api-Version": "2022-11-28", "User-Agent": "salve-self-pr"},
    )
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            text = response.read().decode("utf-8")
            return response.status, json.loads(text) if text else {}
    except urllib.error.HTTPError as error:
        try:
            detail = json.loads(error.read().decode("utf-8"))
        except (ValueError, UnicodeError):
            detail = {}
        return error.code, detail


def publish(proposal: dict, worktree: Path, base: str, expected_tree: str, issue: int, token: str) -> tuple[str, int, str]:
    if runner.run(["git", "rev-parse", "HEAD"], worktree, capture=True).stdout.strip() != base:
        raise runner.ProposalError("La copia de publicación no está en la revisión probada")
    tree, generated = apply(proposal, worktree)
    if tree != expected_tree:
        raise runner.ProposalError("El árbol a publicar difiere del probado; no se publica")
    branch = branch_name(proposal)
    runner.run(["git", "switch", "-c", branch], worktree)
    runner.run(["git", "-c", "user.name=Salve (vía GitHub Actions)", "-c",
                "user.email=41898282+github-actions[bot]@users.noreply.github.com",
                "commit", "-m", f"Auto-mejora de Salve: {proposal['targetClass']}\n\n{proposal['issueSummary']}\n\n"
                f"Propuesta: #{issue}\nsalve-proposal-id:{proposal['proposalId']}"], worktree)
    if runner.run(["git", "rev-parse", "HEAD^{tree}"], worktree, capture=True).stdout.strip() != expected_tree:
        raise runner.ProposalError("El commit difiere del árbol probado; no se publica")
    runner.run(["git", "push", "origin", f"HEAD:refs/heads/{branch}"], worktree)
    status, created = github("POST", "/pulls", token, {
        "title": f"Salve propone: {proposal['targetClass']}",
        "head": branch, "base": "main", "body": pr_body(proposal, issue, tree, generated),
    })
    if status == 201:
        return created["html_url"], int(created["number"]), runner.run(["git", "rev-parse", "HEAD"], worktree, capture=True).stdout.strip()
    if status == 403:
        server = os.environ.get("GITHUB_SERVER_URL", "https://github.com")
        raise runner.ProposalError(
            "La rama está subida pero GitHub no deja a Actions abrir PRs. Actívalo en Settings → Actions → General → "
            "«Allow GitHub Actions to create and approve pull requests», o ábrelo a mano: "
            f"{server}/{os.environ['GITHUB_REPOSITORY']}/compare/main...{branch}")
    raise runner.ProposalError(f"GitHub no abrió el PR (HTTP {status}: {created.get('message', '')})")


def merge(number: int, branch: str, sha: str, token: str, wait: float = 3600, sleep=time.sleep) -> str:
    """Fusiona solo si android.yml terminó en verde sobre exactamente `sha` y el PR sigue apuntando a él."""
    status, _ = github("POST", "/actions/workflows/android.yml/dispatches", token, {"ref": branch})
    if status != 204:
        raise runner.ProposalError(f"No pude lanzar la compilación completa (HTTP {status}); no fusiono")
    deadline, run = time.monotonic() + wait, None
    while True:
        status, runs = github("GET", f"/actions/runs?branch={branch}&event=workflow_dispatch&per_page=20", token)
        if status == 200:
            mine = [r for r in runs.get("workflow_runs", [])
                    if r.get("head_sha") == sha and r.get("path", "").endswith("android.yml")]
            run = mine[0] if mine else None
            if run and run.get("status") == "completed":
                break
        if time.monotonic() > deadline:
            raise runner.ProposalError("La compilación completa no terminó a tiempo; no fusiono")
        sleep(30)
    if run.get("conclusion") != "success":
        raise runner.ProposalError(f"La compilación completa de Android terminó en «{run.get('conclusion')}»; no fusiono")
    status, pr = github("GET", f"/pulls/{number}", token)
    if status != 200 or pr.get("state") != "open" or pr.get("head", {}).get("sha") != sha:
        raise runner.ProposalError("El PR cambió o se cerró mientras se probaba; no fusiono")
    status, merged = github("PUT", f"/pulls/{number}/merge", token, {
        "sha": sha, "merge_method": "squash", "commit_title": f"{pr.get('title', 'Salve')} (#{number})"})
    if status != 200 or not merged.get("merged"):
        raise runner.ProposalError(f"GitHub no permitió fusionar (HTTP {status}: {merged.get('message', '')})")
    # Un push hecho con GITHUB_TOKEN no dispara otros flujos: se pide a mano el APK de main.
    github("POST", "/actions/workflows/android.yml/dispatches", token, {"ref": "main"})
    github("DELETE", f"/git/refs/heads/{branch}", token)
    return merged.get("sha", "")


def write_outputs(**values: str) -> None:
    target = os.environ.get("GITHUB_OUTPUT")
    lines = "".join(f"{key}={value}\n" for key, value in values.items())
    if target:
        with open(target, "a", encoding="utf-8") as output:
            output.write(lines)
    else:
        sys.stdout.write(lines)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest="command", required=True)
    prepare_cmd = sub.add_parser("prepare")
    prepare_cmd.add_argument("--repo", type=Path, default=Path.cwd())
    prepare_cmd.add_argument("--out", type=Path, required=True, help="carpeta para proposal.json y reason.txt")
    publish_cmd = sub.add_parser("publish")
    publish_cmd.add_argument("--repo", type=Path, default=Path.cwd())
    publish_cmd.add_argument("--proposal", type=Path, required=True)
    publish_cmd.add_argument("--base", required=True)
    publish_cmd.add_argument("--tree", required=True)
    publish_cmd.add_argument("--issue", type=int, required=True)
    publish_cmd.add_argument("--out", type=Path, required=True)
    merge_cmd = sub.add_parser("merge")
    merge_cmd.add_argument("--number", type=int, required=True)
    merge_cmd.add_argument("--branch", required=True)
    merge_cmd.add_argument("--sha", required=True)
    merge_cmd.add_argument("--out", type=Path, required=True)
    args = parser.parse_args()

    args.out.mkdir(parents=True, exist_ok=True)
    reason = args.out / "reason.txt"
    if args.command == "prepare":
        try:
            proposal = extract(os.environ.get("ISSUE_BODY", ""))
            (args.out / "proposal.json").write_text(json.dumps(proposal), encoding="utf-8")
            tree, generated = apply(proposal, args.repo.resolve())
        except (runner.ProposalError, subprocess.CalledProcessError) as error:
            reason.write_text(str(error), encoding="utf-8")
            write_outputs(status="rejected")
            return 0  # el rechazo se comunica en la issue, no como fallo del flujo
        reason.write_text(f"Parche aplicado sobre main. Suite generada: {generated or 'ninguna'}", encoding="utf-8")
        write_outputs(status="ready", tree=tree)
        return 0

    if args.command == "merge":
        try:
            if not re.fullmatch(r"[0-9a-f]{40}", args.sha) or not args.branch.startswith("salve/auto-"):
                raise runner.ProposalError("Solo se fusionan ramas salve/auto-… con un commit concreto")
            merged = merge(args.number, args.branch, args.sha, os.environ["GH_TOKEN"])
        except (runner.ProposalError, KeyError, ValueError) as error:
            reason.write_text(str(error), encoding="utf-8")
            write_outputs(merged="false")
            return 0
        (args.out / "merged_sha.txt").write_text(merged, encoding="utf-8")
        write_outputs(merged="true")
        return 0

    try:
        proposal = json.loads(args.proposal.read_text(encoding="utf-8"), object_pairs_hook=runner.unique_fields)
        runner.validate_proposal(proposal)
        check_limits(proposal)
        url, number, sha = publish(proposal, args.repo.resolve(), args.base, args.tree, args.issue, os.environ["GH_TOKEN"])
    except (runner.ProposalError, subprocess.CalledProcessError, KeyError, ValueError) as error:
        reason.write_text(str(error), encoding="utf-8")
        write_outputs(published="false")
        return 0
    (args.out / "pr_url.txt").write_text(url, encoding="utf-8")
    write_outputs(published="true", branch=branch_name(proposal), number=str(number), sha=sha)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
