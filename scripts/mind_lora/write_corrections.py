#!/usr/bin/env python3
"""Convierte las correcciones de Bryan en respuestas correctas, escritas por un modelo «profesor» en el PC.

Entrada: salve-corrections.jsonl (orden «exporta tu mente» en la app): situación, lo que Salve hizo y lo
que Bryan esperaba. Una corrección dice qué estuvo mal, no cuál era la respuesta buena; aquí un modelo
grande la escribe y una puerta la acepta o la descarta.

Salida: corrected.jsonl con {"question", "reply", "expected"}, para prepare_dataset.py --corrections.

El profesor es cualquier servidor compatible con la API de OpenAI en el propio PC (Ollama, llama.cpp
server, LM Studio). Nada sale del ordenador. Sin dependencias.

  python scripts/mind_lora/write_corrections.py --export-dir exports --output corrected.jsonl --model <modelo>

El examen de Salve NO se usa como material: es quien juzga al adaptador. Las correcciones cuya pregunta
coincide con un caso del examen se descartan, para no entrenar con el juez.
"""
import argparse
import difflib
import json
import re
import unicodedata
import urllib.request
from pathlib import Path

EXAM = Path(__file__).resolve().parents[2] / "app/src/main/assets/exam/conversaciones.json"
MAX_REPLY = 1200
TECHNICAL = re.compile(r"memory\.lookup|web\.search|self\.command|device\.\w+|SALVE_COGNITIVE|\"mode\"\s*:|TOOL_|JSON|```", re.I)
META = re.compile(r"\b(la respuesta correcta|respuesta corregida|como salve|aquí tienes|correcci[oó]n:)\b", re.I)
ENGLISH = re.compile(r"\b(the|and|you|your|with|that|this|have|what|about|would)\b", re.I)


def normalize(text):
    text = unicodedata.normalize("NFD", (text or "").lower())
    text = "".join(c for c in text if unicodedata.category(c) != "Mn")
    return re.sub(r"[^a-z0-9 ]+", " ", text).split()


def load_jsonl(path):
    items = []
    if not path.exists():
        return items
    for number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if line.strip():
            try:
                items.append(json.loads(line))
            except json.JSONDecodeError as error:
                raise ValueError(f"{path}:{number}: JSON inválido") from error
    return items


def exam_questions(path=EXAM):
    if not path.exists():
        return set()
    return {" ".join(normalize(case.get("pregunta", ""))) for case in json.loads(path.read_text(encoding="utf-8")).get("casos", [])}


def teacher_messages(correction, narrative=""):
    system = ("Eres Salve, una IA compañera. Hablas en español, en primera persona, con calidez y sin tecnicismos internos. "
              + (narrative or "")).strip()
    did = correction.get("did") or "(no se guardó)"
    user = (f"Bryan te dijo: «{correction['situation']}»\n"
            f"Tú respondiste: «{did}»\n"
            f"Bryan te corrigió: «{correction['expected']}»\n\n"
            "Escribe SOLO la respuesta que debiste dar a su mensaje, cumpliendo su corrección. "
            "No menciones la corrección, no te disculpes por el error anterior y no añadas explicaciones sobre el cambio.")
    return [{"role": "system", "content": system}, {"role": "user", "content": user}]


def acceptable(correction, reply):
    """None si la respuesta sirve como ejemplo; si no, el motivo por el que se descarta."""
    reply = (reply or "").strip()
    if not reply:
        return "vacía"
    if len(reply) > MAX_REPLY:
        return "demasiado larga"
    if TECHNICAL.search(reply):
        return "contiene texto técnico interno"
    if META.search(reply):
        return "habla de la corrección en vez de responder"
    words = normalize(reply)
    if words and len(ENGLISH.findall(reply)) * 5 > len(words):
        return "no está en español"
    did = (correction.get("did") or "").strip()
    if did and difflib.SequenceMatcher(None, did.lower(), reply.lower()).ratio() > 0.85:
        return "repite la respuesta que Bryan corrigió"
    if difflib.SequenceMatcher(None, correction["situation"].lower(), reply.lower()).ratio() > 0.85:
        return "devuelve el mensaje de Bryan"
    return None


def ask(endpoint, model, messages, timeout=180):
    body = json.dumps({"model": model, "messages": messages, "temperature": 0.4, "max_tokens": 500}).encode("utf-8")
    request = urllib.request.Request(endpoint.rstrip("/") + "/chat/completions", data=body,
                                     headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.loads(response.read().decode("utf-8"))["choices"][0]["message"]["content"]


def build(corrections, teacher, narrative="", held_out=frozenset()):
    """teacher(messages) -> texto. Devuelve (ejemplos aceptados, [(situación, motivo de descarte)])."""
    kept, dropped, seen = [], [], set()
    for correction in corrections:
        if correction.get("type") != "correction" or not correction.get("situation") or not correction.get("expected"):
            continue
        key = " ".join(normalize(correction["situation"]))
        if key in seen:
            continue
        seen.add(key)
        if key in held_out:
            dropped.append((correction["situation"], "es un caso de su examen (el juez no se entrena)"))
            continue
        try:
            reply = teacher(teacher_messages(correction, narrative))
        except Exception as error:  # el servidor del profesor puede fallar en un caso; se sigue con el resto
            dropped.append((correction["situation"], f"el profesor falló: {type(error).__name__}"))
            continue
        reason = acceptable(correction, reply)
        if reason:
            dropped.append((correction["situation"], reason))
            continue
        kept.append({"question": correction["situation"], "reply": reply.strip(), "expected": correction["expected"]})
    return kept, dropped


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--export-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--model", required=True, help="Nombre del modelo en el servidor local (p. ej. el que muestra «ollama list»)")
    parser.add_argument("--endpoint", default="http://localhost:11434/v1", help="API compatible con OpenAI en este PC")
    args = parser.parse_args()
    if not re.match(r"^https?://(localhost|127\.0\.0\.1|\[::1\])[:/]", args.endpoint):
        parser.error("El profesor debe estar en este PC (localhost): las correcciones son datos personales.")
    corrections = load_jsonl(args.export_dir / "salve-corrections.jsonl")
    narrative = next((k["text"] for k in load_jsonl(args.export_dir / "salve-knowledge.jsonl") if k.get("type") == "narrative"), "")
    kept, dropped = build(corrections, lambda messages: ask(args.endpoint, args.model, messages), narrative, exam_questions())
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as out:
        for example in kept:
            out.write(json.dumps(example, ensure_ascii=False) + "\n")
    print(f"{len(kept)} respuestas aceptadas de {len(kept) + len(dropped)} correcciones → {args.output}")
    for situation, reason in dropped:
        print(f"  descartada: «{situation[:60]}» ({reason})")
    if kept:
        print("Léelas antes de entrenar: lo que quede en ese archivo es lo que Salve aprenderá a decir.")


if __name__ == "__main__":
    main()
