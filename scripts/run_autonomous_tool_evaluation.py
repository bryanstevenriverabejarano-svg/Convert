#!/usr/bin/env python3
"""Compile the real Java laboratory and certify 416 runs plus a memory ablation.

Uses a locally installed JDK and the app's existing Gson dependency; downloads
nothing and needs neither Android SDK nor a device. Generated evidence is only
published after compilation, execution and independent certification succeed.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import time
import zipfile


class EvaluationError(RuntimeError):
    """A reproducible evaluation stage could not be completed."""


def positive_timeout(value: str) -> int:
    try:
        seconds = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("El timeout debe ser un entero de 1 a 3600 segundos") from error
    if not 1 <= seconds <= 3600:
        raise argparse.ArgumentTypeError("El timeout debe estar entre 1 y 3600 segundos")
    return seconds


def find_gson(repo_root: Path, explicit: Path | None, gradle_home: Path | None = None) -> Path:
    if explicit is not None:
        candidates = [explicit.expanduser().resolve()]
    else:
        build_file = repo_root/"app/build.gradle"
        try:
            match = re.search(r"com\.google\.code\.gson:gson:([0-9A-Za-z.\-]+)",build_file.read_text(encoding="utf-8"))
        except OSError as error:
            raise EvaluationError(f"No se puede leer {build_file}") from error
        if not match:
            raise EvaluationError("No se encontró la versión de Gson declarada por app/build.gradle")
        version = match.group(1)
        cache = gradle_home or Path(os.environ.get("GRADLE_USER_HOME",str(Path.home()/".gradle")))
        artifact_directory = cache/"caches/modules-2/files-2.1/com.google.code.gson/gson"/version
        candidates = sorted(artifact_directory.glob(f"*/gson-{version}.jar"))
        if not candidates:
            raise EvaluationError(f"Gson {version} no está en la caché de Gradle. Compila la app primero o indica --gson-jar RUTA.jar.")
    for candidate in candidates:
        try:
            with zipfile.ZipFile(candidate) as jar:
                classes = set(jar.namelist())
                required = {"com/google/gson/JsonObject.class","com/google/gson/JsonParser.class","com/google/gson/Strictness.class"}
                if required <= classes:
                    return candidate.resolve()
        except (OSError,zipfile.BadZipFile):
            pass
    raise EvaluationError("El archivo indicado no es un JAR utilizable de Gson (se requiere JsonObject, JsonParser y Strictness)")


def java_executable(value: str | None) -> str:
    if value:
        resolved = shutil.which(value)
    else:
        java_home = os.environ.get("JAVA_HOME")
        candidate = Path(java_home)/"bin"/("java.exe" if os.name == "nt" else "java") if java_home else None
        resolved = str(candidate) if candidate and candidate.is_file() else shutil.which("java")
    if not resolved:
        raise EvaluationError("No se encontró Java. Instala un JDK 11 o superior, o indica --java RUTA.")
    return str(Path(resolved).resolve())


def java_sources(repo_root: Path) -> list[Path]:
    package = repo_root/"app/src/main/java/salve/core/autonomy"
    sources = sorted(package.glob("*.java"))
    harness = repo_root/"tools/autonomy/AutonomyEvaluation.java"
    if not sources or not harness.is_file():
        raise EvaluationError("Faltan las fuentes reales del laboratorio o tools/autonomy/AutonomyEvaluation.java")
    return sources+[harness]


def run_stage(name: str, command: list[str], timeout: int, repo_root: Path, log_dir: Path,
              expected_returncode: int = 0) -> dict:
    """No shell; arguments containing spaces or metacharacters remain literal."""
    started = time.monotonic()
    log_path = log_dir/(name+".log")
    print(f"[{name}] ejecutando",flush=True)
    try:
        with log_path.open("w",encoding="utf-8") as log:
            completed = subprocess.run(command,cwd=repo_root,stdout=log,stderr=subprocess.STDOUT,
                                       timeout=timeout,check=False)
    except subprocess.TimeoutExpired as error:
        raise EvaluationError(f"{name}: se agotó el límite de {timeout} segundos") from error
    except OSError as error:
        raise EvaluationError(f"{name}: no se pudo iniciar el proceso: {error}") from error
    elapsed = round(time.monotonic()-started,3)
    output = log_path.read_text(encoding="utf-8",errors="replace")
    if completed.returncode != expected_returncode:
        raise EvaluationError(f"{name}: terminó con código {completed.returncode}\n{output[-8000:]}")
    if output.strip():
        print(output[-3000:].rstrip(),flush=True)
    return {"stage":name,"exit_code":completed.returncode,"expected_exit_code":expected_returncode,"elapsed_seconds":elapsed}


def verify_cli_argument_rejection(java: str, classpath: str, workspace: Path,
                                  timeout: int, repo_root: Path, logs: Path) -> dict:
    rejected_report = workspace/"must-not-be-created.jsonl"
    name = "reject-invalid-cli-option"
    stage = run_stage(name,[java,"-cp",classpath,"AutonomyEvaluation",
                           str(workspace/"not-an-input.jsonl"),str(rejected_report),"--unknown"],
                      timeout,repo_root,logs,expected_returncode=1)
    message = (logs/(name+".log")).read_text(encoding="utf-8",errors="replace")
    if "IllegalArgumentException: Uso: corpus.jsonl report.jsonl [--fresh-each-case]" not in message or rejected_report.exists():
        raise EvaluationError("El CLI no rechazó la opción desconocida antes de acceder a los archivos")
    return stage


def _hash(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as source:
        for block in iter(lambda: source.read(1024*1024),b""):
            digest.update(block)
    return digest.hexdigest()


def publish_evidence(staged: Path, output: Path) -> None:
    output.mkdir(parents=True,exist_ok=True)
    # Install the summary last. No output from a failed Java/verifier run can be
    # mistaken for new certified evidence; unrelated destination files stay put.
    files = sorted(path for path in staged.rglob("*") if path.is_file() and path.name != "summary.json")
    files.append(staged/"summary.json")
    for source in files:
        destination = output/source.relative_to(staged)
        destination.parent.mkdir(parents=True,exist_ok=True)
        descriptor,temporary = tempfile.mkstemp(prefix=".salve-evidence-",dir=destination.parent)
        try:
            with os.fdopen(descriptor,"wb") as target,source.open("rb") as content:
                shutil.copyfileobj(content,target)
            os.replace(temporary,destination)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)


def build_ablation(with_memory: dict, without_memory: dict) -> dict:
    """Record observed effects even when retaining memory has a measured cost."""
    keys = ("executions","candidate_attempts","rejected_candidates","operations","reused",
            "adapted","promoted","previous_program_rejected","regression_checks")
    retained = with_memory["observed_metrics"]
    fresh = without_memory["observed_metrics"]
    comparison = {key: {"with_memory":retained[key],"without_memory":fresh[key],
                        "with_minus_without":retained[key]-fresh[key]} for key in keys}
    for key in ("candidate_attempts","rejected_candidates","operations"):
        comparison[key]["reduction_fraction"] = (fresh[key]-retained[key])/fresh[key] if fresh[key] else None
    return {"schema":1,"experiment":"Same corpus and solver; only the tool/regression journal is retained or reset",
            "corpus_cases":104,"rounds":4,"executions_per_condition":416,
            "conditions":{"with_memory":"Journal retained across challenges and reloaded at each full-round boundary",
                          "without_memory":"New empty StateStore and AutonomousToolLab before every challenge"},
            "correctness":{"with_memory":{"passed":with_memory["passed"],"passed_executions":with_memory["passed_executions"]},
                           "without_memory":{"passed":without_memory["passed"],"passed_executions":without_memory["passed_executions"]}},
            "comparison":comparison,
            "host_jvm_latency_ms":{"with_memory":retained["latency_ms"],"without_memory":fresh["latency_ms"]},
            "interpretation":"Positive with_minus_without means more of that metric with memory. A negative reduction is a cost, not an improvement. Regression checks also consume operations.",
            "scope":"Measures reuse of symbolic tools and saved regression cases. No LLM, neural-weight training, phone benchmark or general intelligence claim.",
            "tuning":"This comparison does not modify solver parameters, the corpus, operation budgets or acceptance criteria."}


def run_evaluation(repo_root: Path, output: Path, gson: Path, java: str, timeout: int) -> dict:
    sources = java_sources(repo_root)
    evaluator = repo_root/"scripts/evaluate_autonomous_tools.py"
    if not evaluator.is_file():
        raise EvaluationError("Falta el certificador independiente Python")
    source_files = sources+[evaluator,Path(__file__).resolve()]
    source_hashes = {str(path.relative_to(repo_root)):_hash(path) for path in source_files}
    gson_hash = _hash(gson)
    with tempfile.TemporaryDirectory(prefix="salve-autonomous-evaluation-") as temporary:
        workspace = Path(temporary)
        classes = workspace/"classes"
        classes.mkdir()
        staged = workspace/"evidence"
        staged.mkdir()
        logs = staged/"logs"
        logs.mkdir()
        corpus,reports = staged/"corpus.jsonl",staged/"reports.jsonl"
        stages = []
        stages.append(run_stage("java-version",[java,"--version"],timeout,repo_root,logs))
        stages.append(run_stage("compile",[java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","11",
                                "-encoding","UTF-8","-cp",str(gson),"-d",str(classes),*[str(source) for source in sources]],
                                timeout,repo_root,logs))
        stages.append(verify_cli_argument_rejection(java,str(classes)+os.pathsep+str(gson),workspace,timeout,repo_root,logs))
        stages.append(run_stage("generate",[sys.executable,str(evaluator),"--generate",str(corpus)],timeout,repo_root,logs))
        stages.append(run_stage("execute",[java,"-Xmx256m","-cp",str(classes)+os.pathsep+str(gson),
                                "AutonomyEvaluation",str(corpus),str(reports)],timeout,repo_root,logs))
        stages.append(run_stage("verify",[sys.executable,str(evaluator),"--verify",str(reports),"--output",str(staged)],
                                timeout,repo_root,logs))
        summary = json.loads((staged/"summary.json").read_text(encoding="utf-8"))
        if summary.get("passed") is not True or summary.get("passed_executions") != 416:
            raise EvaluationError("El certificador no acredita las 416 ejecuciones")
        fresh_reports = staged/"reports-without-memory.jsonl"
        fresh_evidence = workspace/"without-memory-verification"
        stages.append(run_stage("execute-without-memory",[java,"-Xmx256m","-cp",str(classes)+os.pathsep+str(gson),
                                "AutonomyEvaluation",str(corpus),str(fresh_reports),"--fresh-each-case"],timeout,repo_root,logs))
        stages.append(run_stage("verify-without-memory",[sys.executable,str(evaluator),"--verify",str(fresh_reports),
                                "--output",str(fresh_evidence)],timeout,repo_root,logs))
        fresh_summary = json.loads((fresh_evidence/"summary.json").read_text(encoding="utf-8"))
        if fresh_summary.get("passed") is not True or fresh_summary.get("passed_executions") != 416:
            raise EvaluationError("La condición sin memoria no acredita las mismas 416 ejecuciones")
        shutil.copyfile(fresh_evidence/"summary.json",staged/"summary-without-memory.json")
        (staged/"ablation.json").write_text(json.dumps(build_ablation(summary,fresh_summary),ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        if source_hashes != {str(path.relative_to(repo_root)):_hash(path) for path in source_files} or gson_hash != _hash(gson):
            raise EvaluationError("Las fuentes o Gson cambiaron durante la ejecución. Repite la evaluación para certificar una única versión.")
        provenance = {"schema":1,"compiled_release":11,"java_heap_limit":"256m","timeout_per_stage_seconds":timeout,
                      "gson":{"filename":gson.name,"sha256":gson_hash},
                      "source_sha256":source_hashes,
                      "corpus_sha256":_hash(corpus),"reports_sha256":_hash(reports),
                      "reports_without_memory_sha256":_hash(fresh_reports),
                      "stages":stages,"scope":"JVM de escritorio; no mide rendimiento ni capacidades generales en el móvil."}
        (staged/"provenance.json").write_text(json.dumps(provenance,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        publish_evidence(staged,output)
    return {"passed":True,"cases":104,"rounds":4,"executions":416,"ablation_executions":416,"output":str(output)}


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--gson-jar",type=Path,help="JAR de Gson; por defecto busca la versión de la app en la caché Gradle")
    parser.add_argument("--java",help="Ejecutable de un JDK con el módulo jdk.compiler")
    parser.add_argument("--output",type=Path,default=Path("docs/evidence/autonomous-tools"))
    parser.add_argument("--timeout-seconds",type=positive_timeout,default=180,help="Límite por fase (1–3600; defecto 180)")
    args = parser.parse_args(argv)
    repo_root = Path(__file__).resolve().parents[1]
    output = args.output.expanduser()
    if not output.is_absolute():
        output = repo_root/output
    try:
        gson = find_gson(repo_root,args.gson_jar)
        java = java_executable(args.java)
        result = run_evaluation(repo_root,output.resolve(),gson,java,args.timeout_seconds)
    except (EvaluationError,OSError,ValueError) as error:
        print(f"Evaluación no completada: {error}",file=sys.stderr)
        return 1
    print(json.dumps(result,ensure_ascii=False))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
