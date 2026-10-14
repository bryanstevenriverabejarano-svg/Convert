#!/usr/bin/env python3
"""Convierte la exportación de la mente de Salve en ejemplos de conversación para un LoRA.

Entrada: salve-knowledge.jsonl y salve-episodes.jsonl (orden «exporta tu mente» en la app).
Salida: train.jsonl con {"messages": [...]} en formato chat, listo para PEFT/TRL.

Las respuestas se construyen SOLO con lo consolidado (creencias con fuentes y su confianza).
No se inventan datos: una creencia de confianza media se expresa con cautela. Sin dependencias.
"""
import argparse
import json
import random
from pathlib import Path

QUESTIONS = {
    "bryan": [
        "¿Qué sabes de mí?",
        "¿Qué has aprendido de mí últimamente?",
        "Recuérdame algo que sepas sobre mis costumbres.",
    ],
    "salve": [
        "¿Qué has aprendido sobre ti misma?",
        "¿Cómo trabajas mejor?",
    ],
    "mundo": [
        "¿Qué has aprendido recientemente?",
        "Cuéntame algo que hayas consolidado de nuestras conversaciones.",
    ],
}


def load_jsonl(path):
    items = []
    if not path.exists():
        return items
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        line = line.strip()
        if not line:
            continue
        try:
            items.append(json.loads(line))
        except json.JSONDecodeError as error:
            raise ValueError(f"{path}:{line_number}: JSON inválido") from error
    return items


def hedge(text, confidence):
    if confidence >= 0.75:
        return text
    return "Por lo que me has contado, creo que " + text[0].lower() + text[1:] + " Corrígeme si ha cambiado."


def build_examples(knowledge, seed=7):
    rng = random.Random(seed)
    narrative = next((k["text"] for k in knowledge if k.get("type") == "narrative"), None)
    if not narrative:
        raise ValueError("La exportación no contiene la narrativa de Salve")
    system = "Eres Salve. " + narrative
    examples = [{"messages": [
        {"role": "system", "content": system},
        {"role": "user", "content": "¿Quién eres?"},
        {"role": "assistant", "content": narrative},
    ]}]
    for item in knowledge:
        if item.get("type") != "belief" or not item.get("sources"):
            continue
        about = item.get("about", "mundo")
        question = rng.choice(QUESTIONS.get(about, QUESTIONS["mundo"]))
        examples.append({"messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": question},
            {"role": "assistant", "content": hedge(item["text"].rstrip(".") + ".", float(item.get("confidence", 0.6)))},
        ]})
    for item in knowledge:
        if item.get("type") != "thought" or not item.get("sources"):
            continue
        examples.append({"messages": [
            {"role": "system", "content": system},
            {"role": "user", "content": "¿En qué has estado pensando?"},
            {"role": "assistant", "content": "Estuve pensando en esto: " + item["text"]},
        ]})
    return examples


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--export-dir", type=Path, required=True)
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    knowledge = load_jsonl(args.export_dir / "salve-knowledge.jsonl")
    examples = build_examples(knowledge)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    with args.output.open("w", encoding="utf-8") as out:
        for example in examples:
            out.write(json.dumps(example, ensure_ascii=False) + "\n")
    print(f"{len(examples)} ejemplos escritos en {args.output}")


if __name__ == "__main__":
    main()
