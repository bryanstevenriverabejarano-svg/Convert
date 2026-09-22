#!/usr/bin/env python3
"""Freeze Salve's solver, generate new bounded inputs and measure finite generalization.

The default seed is created with secrets after source hashes are recorded. The
same real Java laboratory sees the known 416 inputs, reloads its journal and then
receives 128 new inputs. Independent oracles never enter the solver process.
Failure results are retained, not tuned away. This tests only four known domains.
"""
from __future__ import annotations

import argparse
from datetime import datetime, timezone
import json
import os
from pathlib import Path
import random
import re
import secrets
import sys
import tempfile

import evaluate_autonomous_tools as independent
import run_autonomous_tool_evaluation as infrastructure


def fingerprint(family: str, data: dict) -> str:
    """Ignore harmless list reordering when checking novelty against known inputs."""
    normalized = json.loads(json.dumps(data))
    if family in ("route","dependencies"):
        normalized["edges"] = sorted(normalized["edges"])
    elif family == "knapsack":
        normalized["items"] = sorted(normalized["items"],key=lambda item: (item["weight"],item["value"]))
    else:
        normalized["jobs"] = sorted(normalized["jobs"],key=lambda job: (job["start"],job["end"],job["value"]))
    return family+":"+json.dumps(normalized,sort_keys=True,separators=(",",":"))


def _route(rng: random.Random, mode: int) -> tuple[dict,str]:
    n = rng.randint(5,14)
    edges: dict[tuple[int,int],int] = {}
    source,target = 0,n-1
    if mode == 0:
        order = list(range(n)); rng.shuffle(order)
        source,target = order[0],order[-1]
        for i in range(n):
            for j in range(i+1,n):
                if j == i+1 or rng.random() < .25:
                    edges[order[i],order[j]] = rng.randint(-9,24)
        edges[order[0],order[1]] = -rng.randint(1,9)
        motif = "DAG aleatorio con etiquetas permutadas y costes negativos"
    elif mode == 2:
        split = rng.randint(2,n-2)
        for u in range(n):
            for v in range(n):
                if u != v and (u < split) == (v < split) and rng.random() < .4:
                    edges[u,v] = rng.randint(0,35)
        motif = "Dos componentes aleatorias con destino inalcanzable"
    else:
        density = rng.uniform(.12,.65)
        for u in range(n):
            for v in range(n):
                if u != v and rng.random() < density:
                    edges[u,v] = rng.randint(0,35)
        for i in range(n-1):
            edges[i,i+1] = rng.randint(1,25)
        edges[n-1,0] = rng.randint(1,25)
        motif = "Grafo dirigido aleatorio con ciclos de coste no negativo"
        if mode == 3:
            potential = [rng.randint(-30,30) for _ in range(n)]
            edges = {(u,v): w+potential[v]-potential[u] for (u,v),w in edges.items()}
            motif = "Grafo cíclico aleatorio con potenciales: posibles arcos negativos sin ciclos negativos"
    return {"nodes":n,"edges":[[u,v,w] for (u,v),w in edges.items()],"source":source,"target":target},motif


def _knapsack(rng: random.Random, mode: int) -> tuple[dict,str]:
    count = rng.randint(4,14)
    capacity = rng.randint(1,100)
    items = []
    for _ in range(count):
        weight = rng.randint(1,40)
        value = rng.randint(0,100)
        if mode == 1:
            value = weight*rng.randint(1,3)+rng.randint(0,7)
        elif mode == 2:
            weight = rng.choice([max(1,capacity//2),capacity+1,rng.randint(1,max(1,capacity))])
        elif mode == 3:
            weight = rng.randint(1,12)*2
            value = rng.choice([0,rng.randint(5,80)])
        items.append({"weight":weight,"value":value})
    descriptions = ["Recursos aleatorios con valores y pesos independientes",
                    "Valores correlacionados con peso y perturbación aleatoria",
                    "Mezcla de alternativas factibles y recursos que exceden el presupuesto",
                    "Pesos pares, recompensas dispersas y valores cero"]
    return {"capacity":capacity,"items":items},descriptions[mode]


def _schedule(rng: random.Random, mode: int) -> tuple[dict,str]:
    count = rng.randint(4,14)
    jobs = []
    for i in range(count):
        start = rng.randint(-20,35)
        end = start+rng.randint(1,15)
        if mode == 1:
            start = rng.randint(-20,0); end = rng.randint(1,25)
        elif mode == 2:
            start = rng.choice([-5,0,7,15]); end = start+rng.randint(1,12)
        elif mode == 3:
            cluster = rng.choice([0,50,100])
            start = cluster+rng.randint(0,12); end = start+rng.randint(1,9)
        jobs.append({"start":start,"end":end,"value":rng.randint(0,100)})
    descriptions = ["Calendario aleatorio con distintos grados de solapamiento",
                    "Intervalos aleatorios que comparten una región central",
                    "Empates de comienzo con duraciones y recompensas diferentes",
                    "Grupos temporales separados con conflictos locales"]
    rng.shuffle(jobs)
    return {"jobs":jobs},descriptions[mode]


def _dependencies(rng: random.Random, mode: int) -> tuple[dict,str]:
    n = rng.randint(4,14)
    order = list(range(n)); rng.shuffle(order)
    edges = set()
    if mode in (0,1):
        for i in range(n):
            for j in range(i+1,n):
                if rng.random() < (.2 if mode == 0 else .65):
                    edges.add((order[i],order[j]))
        motif = "DAG aleatorio disperso" if mode == 0 else "DAG aleatorio denso con etiquetas permutadas"
    else:
        for u in range(n):
            for v in range(n):
                if u != v and rng.random() < .14:
                    edges.add((u,v))
        if mode == 2:
            cycle = rng.sample(range(n),rng.randint(2,n))
            edges.update(zip(cycle,cycle[1:]+cycle[:1]))
            motif = "Dependencias aleatorias con un ciclo impuesto de longitud variable"
        else:
            node = rng.randrange(n); edges.add((node,node))
            motif = "Autodependencia insertada en una red aleatoria"
    return {"nodes":n,"edges":[list(edge) for edge in sorted(edges)]},motif


def generate_holdout(seed: int) -> list[dict]:
    rng = random.Random(seed)
    excluded = {fingerprint(record["family"],record["input"]) for record in independent.generate_corpus()}
    generators = {"route":_route,"knapsack":_knapsack,"schedule":_schedule,"dependencies":_dependencies}
    output = []
    for family in independent.FAMILIES:
        for index in range(1,33):
            for _ in range(1000):
                data,description = generators[family](rng,(index-1)%4)
                identity = fingerprint(family,data)
                if identity not in excluded:
                    break
            else:
                raise ValueError("No se obtuvo un reto nuevo dentro del límite de generación")
            excluded.add(identity)
            output.append({"schema":1,"id":f"holdout-{family}-{index:03d}","family":family,
                           "description":description,"input":data})
    # Interleave domains so retrieval must switch families and cannot assume that
    # all upcoming tasks match the last stored program.
    rng.shuffle(output)
    return output


def assess_holdout(challenges: list[dict], reports: list[dict], expected_initial_journal_hash: str | None = None) -> list[dict]:
    expected = {challenge["id"]:challenge for challenge in challenges}
    seen = set()
    assessments = []
    previous_journal_hash = expected_initial_journal_hash
    for report in reports:
        assessment = {"id":None,"passed":False}
        try:
            challenge = report["challenge"]
            identifier = challenge["id"]
            assessment["id"] = identifier
            if identifier not in expected or identifier in seen:
                raise ValueError("Identificador desconocido o duplicado")
            seen.add(identifier)
            if json.dumps(challenge,sort_keys=True) != json.dumps(expected[identifier],sort_keys=True):
                raise ValueError("El ejecutor modificó el reto retenido")
            if report.get("verified") is not True:
                raise ValueError("El laboratorio no verificó el resultado")
            if not isinstance(report.get("program"),dict) or not report["program"]:
                raise ValueError("Falta el programa declarativo ejecutado")
            for field in ("stateBefore","stateAfter"):
                state = report.get(field)
                if not isinstance(state,dict) or state.get("family") != challenge["family"] or state.get("persisted") is not True:
                    raise ValueError("Falta el estado persistido de la familia antes o después")
                if not isinstance(state.get("fullJournalSha256"),str) or not re.fullmatch("[0-9a-f]{64}",state["fullJournalSha256"]):
                    raise ValueError("Falta el hash del diario completo")
                if not isinstance(state.get("familyState"),dict):
                    raise ValueError("Falta la instantánea del estado de la familia")
            if previous_journal_hash is not None and report["stateBefore"]["fullJournalSha256"] != previous_journal_hash:
                raise ValueError("El estado inicial no continúa el diario de la ejecución anterior")
            previous_journal_hash = report["stateAfter"]["fullJournalSha256"]
            assessment["certificate"] = independent.verify_result(challenge,report.get("result"))
            assessment["passed"] = True
        except (ValueError,TypeError,KeyError,IndexError) as error:
            assessment["error"] = str(error)
        assessments.append(assessment)
    assessments += [{"id":identifier,"passed":False,"error":"No se recibió esta ejecución"}
                    for identifier in sorted(set(expected)-seen)]
    return assessments


def detailed_record(report: dict, assessment: dict) -> dict:
    attempts = report.get("attempts",[])
    failed = [attempt for attempt in attempts if attempt.get("passed") is False]
    before,after = report.get("stateBefore"),report.get("stateAfter")
    na = {"status":"not_applicable","detail":"No se observó este evento en la ejecución"}
    record = {
        "identifier": report["challenge"]["id"],
        "objective": report["challenge"].get("description","Resolver y verificar el reto estructurado"),
        "initialState": before,
        "capabilitiesUsed": ["síntesis simbólica acotada","ejecución de herramientas","verificación de candidatos","consulta del diario persistido"],
        "planCreated": [{"strategy":attempt.get("strategy"),"programSha256":attempt.get("programSha256"),
                         "programReference":f"actionsExecuted[{index}].program"} for index,attempt in enumerate(attempts)],
        "toolsUsed": [attempt.get("strategy") for attempt in attempts],
        "modelsUsed": {"status":"not_applicable","detail":"Ningún LLM. Se ejecuta el laboratorio Java real con kernels simbólicos."},
        "memoriesRetrieved": {"proceduralContext":report.get("proceduralContextBefore",""),"snapshotReference":"initialState.familyState"},
        "actionsExecuted": attempts,
        "result": report.get("result"),
        "passFail": "PASS" if assessment["passed"] else "FAIL",
        "failureRootCause": {"status":"not_established","observedFeedback":[attempt.get("feedback") for attempt in failed],
                             "detail":"Feedback de validación observable; no equivale a un diagnóstico causal completo."} if failed else na,
        "improvementHypothesis": {"status":"not_recorded","detail":"El laboratorio selecciona candidatos de estrategias disponibles; no genera una hipótesis causal explícita."} if failed else na,
        "modificationPerformed": {"strategySequence":[attempt.get("strategy") for attempt in attempts],
                                 "promoted":report.get("promoted"),"version":report.get("version"),
                                 "scope":"Cambios de programa declarativo y diario; no se modifica código fuente ni pesos del modelo."},
        "newExecution": attempts[1:] if len(attempts)>1 else na,
        "newResult": report.get("result") if len(attempts)>1 else na,
        "possibleSideEffects": {"stateAfterReference":"stateAfter","journalChanged":before != after,
                               "unmeasured":"No se evalúan efectos externos; este proceso no usa red, apps ni sensores."},
        "regressionTests": [{"strategy":attempt.get("strategy"),"checks":attempt.get("regressionChecks"),"passed":attempt.get("passed")} for attempt in attempts],
        "conclusion": {"independentAssessment":assessment,"decisionSummary":report.get("decisionSummary"),
                       "scope":"Evidencia finita sobre un nuevo input de una familia conocida."},
        "stateAfter":after,"challenge":report["challenge"],"operations":report.get("operations"),
        "elapsedNanos":report.get("elapsedNanos"),"adapted":report.get("adapted"),"reused":report.get("reused"),
    }
    return record


def _jsonl(path: Path, records: list[dict]) -> None:
    path.write_text("".join(json.dumps(record,ensure_ascii=False,separators=(",",":"))+"\n" for record in records),encoding="utf-8")


def _load(path: Path) -> list[dict]:
    return [json.loads(line) for line in path.read_text(encoding="utf-8").splitlines() if line.strip()]


def render_records(directory: Path, reports: list[dict], assessments: list[dict], group_warmup: bool = False) -> None:
    directory.mkdir(parents=True,exist_ok=True)
    by_id = {assessment["id"]:assessment for assessment in assessments}
    records = [detailed_record(report,by_id[report["challenge"]["id"]]) for report in reports]
    _jsonl(directory.parent/(directory.name+"-records.jsonl"),records)
    groups = {}
    for record in records:
        identifier = record["challenge"].get("caseId",record["identifier"]) if group_warmup else record["identifier"]
        groups.setdefault(identifier,[]).append(record)
    for identifier,group in groups.items():
        if not re.fullmatch("[A-Za-z0-9_-]+",identifier):
            raise ValueError("Identificador de informe no compatible con un nombre de archivo")
        parts = [f"# {identifier}\n", "Registro observable; modelos, causas e hipótesis ausentes se indican expresamente.\n"]
        for record in group:
            parts += ["## "+record["identifier"]+"\n"]
            for key,value in record.items():
                parts += [f"### {key}\n","```json\n"+json.dumps(value,ensure_ascii=False,indent=2)+"\n```\n"]
        (directory/(identifier+".md")).write_text("\n".join(parts),encoding="utf-8")


def run(repo: Path, output: Path, gson: Path, java: str, timeout: int, replay_seed: int | None = None) -> dict:
    solver_sources = sorted((repo/"app/src/main/java/salve/core/autonomy").glob("*.java"))
    harness = repo/"tools/autonomy/AutonomyGeneralization.java"
    if not solver_sources or not harness.is_file():
        raise infrastructure.EvaluationError("Faltan fuentes del solver o CLI de generalización")
    sources = solver_sources+[harness,Path(__file__).resolve(),Path(independent.__file__).resolve(),Path(infrastructure.__file__).resolve()]
    frozen = {str(path.relative_to(repo)):infrastructure._hash(path) for path in sources}
    gson_hash = infrastructure._hash(gson)
    freeze_time = datetime.now(timezone.utc).isoformat()
    # First freeze, then obtain entropy. These inputs are never used to choose a
    # solver revision or to modify an acceptance threshold.
    seed = secrets.randbits(128) if replay_seed is None else replay_seed
    generated_time = datetime.now(timezone.utc).isoformat()
    holdout = generate_holdout(seed)
    def assert_frozen():
        current_sources = sorted((repo/"app/src/main/java/salve/core/autonomy").glob("*.java"))
        if current_sources != solver_sources or frozen != {str(path.relative_to(repo)):infrastructure._hash(path) for path in sources} or gson_hash != infrastructure._hash(gson):
            raise infrastructure.EvaluationError("Las fuentes cambiaron después de congelarlas; no se certifica esta ejecución")
    assert_frozen()
    with tempfile.TemporaryDirectory(prefix="salve-generalization-") as temporary:
        workspace = Path(temporary)
        staged = workspace/"evidence"; staged.mkdir()
        logs = staged/"logs"; logs.mkdir()
        classes = workspace/"classes"; classes.mkdir()
        warmup_file,holdout_file = staged/"warmup-corpus.jsonl",staged/"holdout-corpus.jsonl"
        warmup_reports,holdout_reports = staged/"warmup-reports.jsonl",staged/"holdout-reports.jsonl"
        _jsonl(warmup_file,independent.generate_corpus()); _jsonl(holdout_file,holdout)
        stages = [infrastructure.run_stage("compile-generalization",[java,"-m","jdk.compiler/com.sun.tools.javac.Main","--release","11",
                    "-encoding","UTF-8","-cp",str(gson),"-d",str(classes),*[str(path) for path in solver_sources],str(harness)],timeout,repo,logs)]
        assert_frozen()
        stages += [infrastructure.run_stage("execute-generalization",[java,"-Xmx256m","-cp",str(classes)+os.pathsep+str(gson),
                    "AutonomyGeneralization",str(warmup_file),str(holdout_file),str(warmup_reports),str(holdout_reports)],timeout,repo,logs)]
        assert_frozen()
        warmup_results,new_results = _load(warmup_reports),_load(holdout_reports)
        warmup_summary = independent.verify_reports(warmup_results)
        initial_hash = warmup_results[-1].get("stateAfter",{}).get("fullJournalSha256") if warmup_results else None
        assessments = assess_holdout(holdout,new_results,initial_hash)
        passed = sum(assessment["passed"] for assessment in assessments)
        summary = {"schema":1,"warmup_expected":416,"warmup_passed":warmup_summary["passed_executions"],
                   "holdout_expected":128,"holdout_received":len(new_results),"holdout_passed":passed,
                   "passed":warmup_summary["passed"] and passed==128 and len(assessments)==128 and len(new_results)==128,
                   "seed":str(seed),"seed_origin":"secrets_after_source_freeze" if replay_seed is None else "explicit_replay",
                   "source_frozen_at":freeze_time,"inputs_generated_at":generated_time,
                   "source_sha256":frozen,"gson_sha256":gson_hash,
                   "novelty":"No semantic input fingerprint overlaps any of the 416 known inputs; list reordering is ignored.",
                   "family_results":{family:{"expected":32,"passed":sum(item["passed"] and str(item["id"]).startswith("holdout-"+family+"-") for item in assessments)} for family in independent.FAMILIES},
                   "observed_metrics":independent._observed_metrics(new_results),
                   "assessments":assessments,
                   "baseline_before_module":{"status":"not_available","detail":"The old main branch lacks this module; no fabricated 0/104 score is assigned."},
                   "scope":"Finite holdout generalization inside the same four bounded symbolic domains. No LLM or neural learning, new tool primitives, internet research or cross-domain intelligence is demonstrated.",
                   "adaptation_policy":"The frozen laboratory may update its journal while evaluating sequential inputs. Source code, corpus and independent oracles remain fixed.",
                   "stages":stages}
        render_records(staged/"warmup-cases",warmup_results,warmup_summary["assessments"],group_warmup=True)
        render_records(staged/"holdout-cases",new_results,assessments)
        (staged/"warmup-summary.json").write_text(json.dumps(warmup_summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        summary["artifacts_sha256"] = {path.name:infrastructure._hash(path) for path in (warmup_file,holdout_file,warmup_reports,holdout_reports)}
        (staged/"summary.json").write_text(json.dumps(summary,ensure_ascii=False,indent=2)+"\n",encoding="utf-8")
        assert_frozen()
        infrastructure.publish_evidence(staged,output)
    return summary


def _seed(value: str) -> int:
    try:
        parsed = int(value)
    except ValueError as error:
        raise argparse.ArgumentTypeError("La semilla debe ser un entero de 128 bits") from error
    if not 0 <= parsed < 2**128:
        raise argparse.ArgumentTypeError("La semilla debe estar entre 0 y 2^128-1")
    return parsed


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--output",type=Path,default=Path("docs/evidence/autonomy-generalization"))
    parser.add_argument("--gson-jar",type=Path)
    parser.add_argument("--java")
    parser.add_argument("--timeout-seconds",type=infrastructure.positive_timeout,default=180)
    parser.add_argument("--seed",type=_seed,help="Solo para reproducir una semilla publicada; por defecto genera una nueva después de congelar fuentes")
    args = parser.parse_args(argv)
    repo = Path(__file__).resolve().parents[1]
    output = args.output.expanduser()
    if not output.is_absolute(): output = repo/output
    try:
        summary = run(repo,output.resolve(),infrastructure.find_gson(repo,args.gson_jar),
                      infrastructure.java_executable(args.java),args.timeout_seconds,args.seed)
    except (infrastructure.EvaluationError,OSError,ValueError) as error:
        print(f"No se pudo completar el holdout: {error}",file=sys.stderr)
        return 2
    print(json.dumps({key:summary[key] for key in ("passed","warmup_passed","holdout_passed","holdout_received","seed","seed_origin")},ensure_ascii=False))
    return 0 if summary["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
