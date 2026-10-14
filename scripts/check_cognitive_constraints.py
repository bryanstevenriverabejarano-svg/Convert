#!/usr/bin/env python3
"""Validate Salve fixtures against JSON Schema AND the pinned native GBNF engine.
Requires jsonschema==4.25.1 and the executable built from scripts/cognitive-grammar.
No model weights, network inference or simulated grammar implementation.
"""
import argparse
import json
import subprocess
import tempfile
from pathlib import Path
from jsonschema import Draft202012Validator


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--validator', type=Path, required=True)
    args = parser.parse_args()
    root = Path(__file__).resolve().parents[1]
    resources = root / 'app/src/main/resources/salve'
    schema = json.loads((resources / 'cognitive_reply.schema.json').read_text())
    Draft202012Validator.check_schema(schema)
    validator = Draft202012Validator(schema)
    cases = json.loads((root / 'app/src/test/resources/cognition/constraint_cases.json').read_text())
    with tempfile.TemporaryDirectory(prefix='salve-grammar-') as temp:
        sample = Path(temp) / 'output.json'
        for case in cases:
            sample.write_text(case['text'], encoding='utf-8')
            try:
                schema_valid = validator.is_valid(json.loads(case['text']))
            except json.JSONDecodeError:
                schema_valid = False
            native = subprocess.run([str(args.validator.resolve()), str(resources / 'cognitive_reply.gbnf'), str(sample)],
                                    capture_output=True, text=True, timeout=10)
            if native.returncode not in (0, 1):
                raise AssertionError(f"{case['name']}: grammar engine failed: {native.stderr}")
            assert schema_valid == case['valid'], f"Schema mismatch: {case['name']}"
            assert (native.returncode == 0) == case['valid'], f"GBNF mismatch: {case['name']}"
    print(f"PASS: {len(cases)} cases validated by JSON Schema and native llama.cpp GBNF")


if __name__ == '__main__':
    main()
