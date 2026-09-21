#!/usr/bin/env python3
"""Run two REAL, local Gemma 4 E2B text checks on Linux CPU.

Dependency: python -m pip install 'litert-lm-api==0.16.1'
Usage: python scripts/verify_local_model.py --model /path/model.litertlm \
    --output /path/result.json

The model must already be downloaded. Its size and SHA-256 must match Salve's
repository catalog; this script never downloads models or invokes cloud APIs.
It uses the official native Python runtime with four CPU threads, a temporary
cache outside the repository, and no tools. Only public text is saved: internal
response channels are discarded and thinking is disabled. Exit status is 1 if
verification, inference, or either answer check fails.

This is a two-prompt smoke check, not an Android/S24 Ultra performance benchmark.
It does not validate GPU/NPU, voice, vision, tool execution, or long conversations.
"""

import argparse
import hashlib
import importlib.metadata
import json
import platform
import re
import sys
import tempfile
import time
from pathlib import Path


RUNTIME_VERSION = "0.16.1"
REPO_ROOT = Path(__file__).resolve().parents[1]
CATALOG_PATH = REPO_ROOT / "app/src/main/assets/config/models.json"
PROMPTS = (
    ("greeting", "Hola, me llamo Bryan. Salúdame por mi nombre en una sola frase."),
    ("arithmetic", "¿Cuánto es 17 por 23? Responde solo con el número."),
)


def verify_model(model_path):
    catalog = json.loads(CATALOG_PATH.read_text(encoding="utf-8"))
    candidates = [item for item in catalog["items"] if item["id"] == "Gemma 4 E2B"]
    if len(candidates) != 1:
        raise ValueError("Catalog must contain exactly one Gemma 4 E2B entry")
    model = candidates[0]
    size = model_path.stat().st_size
    if size != model["sizeBytes"]:
        raise ValueError(f"Model size mismatch: {size} != {model['sizeBytes']}")
    digest = hashlib.sha256()
    with model_path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(4 * 1024 * 1024), b""):
            digest.update(chunk)
    if digest.hexdigest() != model["sha256"].lower():
        raise ValueError("Model SHA-256 mismatch")
    return {
        "id": model["id"],
        "source_url": model["url"],
        "bytes": size,
        "sha256": digest.hexdigest(),
        "verified": True,
    }


def public_text(response):
    """Keep public content only, never reasoning channels or tool payloads."""
    return "".join(
        item["text"]
        for item in response.get("content", [])
        if isinstance(item, dict)
        and item.get("type") == "text"
        and isinstance(item.get("text"), str)
    ).strip()


def run_checks(model_path, report):
    if platform.system() != "Linux":
        raise RuntimeError("This verification script supports Linux CPU only")
    installed = importlib.metadata.version("litert-lm-api")
    if installed != RUNTIME_VERSION:
        raise RuntimeError(f"Expected litert-lm-api {RUNTIME_VERSION}; found {installed}")
    import resource
    import litert_lm

    started = time.monotonic()
    # /tmp is outside the project for this Linux-only runner. Check even when
    # the repository itself happens to live under /tmp.
    with tempfile.TemporaryDirectory(prefix="salve-litert-", dir="/tmp") as cache:
        if Path(cache).resolve().is_relative_to(REPO_ROOT):
            raise RuntimeError("Native cache must be outside the repository")
        with litert_lm.Engine(
            str(model_path),
            backend=litert_lm.Backend.CPU(thread_count=4),
            max_num_tokens=1024,
            cache_dir=cache,
        ) as engine:
            report["load_seconds"] = round(time.monotonic() - started, 3)
            with engine.create_conversation(
                system_message="Responde en español de forma breve y directa.",
                automatic_tool_calling=False,
                thinking_config=litert_lm.ThinkingConfig(
                    enable_thinking=False, thinking_token_budget=0
                ),
                sampler_config=litert_lm.SamplerConfig(
                    top_k=1, temperature=0.0, seed=42
                ),
                max_output_tokens=80,
            ) as conversation:
                for name, prompt in PROMPTS:
                    turn_started = time.monotonic()
                    response = conversation.send_message(prompt, max_output_tokens=80)
                    text = public_text(response)
                    passed = (
                        bool(re.search(r"\bbryan\b", text, re.IGNORECASE))
                        if name == "greeting"
                        else text == "391"
                    )
                    report["checks"].append({
                        "name": name,
                        "prompt": prompt,
                        "public_response": text,
                        "seconds": round(time.monotonic() - turn_started, 3),
                        "passed": passed,
                    })
    report["total_seconds"] = round(time.monotonic() - started, 3)
    report["max_rss_kib"] = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
    report["passed"] = len(report["checks"]) == 2 and all(
        check["passed"] for check in report["checks"]
    )


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--model", required=True, type=Path, help="Existing .litertlm file")
    parser.add_argument("--output", required=True, type=Path, help="Public result JSON")
    args = parser.parse_args()
    model_path = args.model.resolve()
    output_path = args.output.resolve()
    if output_path in (model_path, CATALOG_PATH, Path(__file__).resolve()):
        print("Output must not overwrite the model, catalog, or script", file=sys.stderr)
        return 1
    report = {
        "runtime": f"litert-lm-api {RUNTIME_VERSION}",
        "platform": platform.platform(),
        "backend": "CPU (4 threads)",
        "model_file": model_path.name,
        "context_limit": 1024,
        "thinking_enabled": False,
        "checks": [],
        "passed": False,
        "scope": "Two Linux CPU text smoke checks; Android, voice and vision untested",
    }
    try:
        report["model_verification"] = verify_model(model_path)
        run_checks(model_path, report)
    except Exception as error:
        report["error"] = f"{type(error).__name__}: {error}"
    try:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        output_path.write_text(
            json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8"
        )
    except OSError as error:
        print(f"Cannot write verification result: {error}", file=sys.stderr)
        return 1
    print("PASS" if report["passed"] else "FAIL")
    return 0 if report["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
