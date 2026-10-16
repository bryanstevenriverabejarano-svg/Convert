#!/usr/bin/env python3
"""Regenerate/check golden IDs with the canonical upstream tokenizer, never Salve code."""
import argparse
import json
from pathlib import Path
from transformers import AutoTokenizer
from prepare import FILES, REVISION, digest

parser = argparse.ArgumentParser()
parser.add_argument('--model-dir', type=Path, required=True)
parser.add_argument('--update', action='store_true', help='Explicitly regenerate expected IDs after reviewing a contract change')
args = parser.parse_args()
model = args.model_dir / 'sentencepiece.bpe.model'
assert digest(model) == FILES[model.name][1], 'Wrong tokenizer artifact'
fixture = Path(__file__).resolve().parents[2] / 'docs/evidence/embeddings/tokenizer-golden.json'
golden = json.loads(fixture.read_text())
assert golden['revision'] == REVISION
tokenizer = AutoTokenizer.from_pretrained(args.model_dir, local_files_only=True, use_fast=False)
for i, row in enumerate(golden['cases']):
    ids = tokenizer(row['text'], max_length=row['max_tokens'], truncation=True)['input_ids']
    if args.update:
        row['ids'] = ids
    else:
        assert ids == row['ids'], f'Upstream changed at case {i}'
if args.update:
    fixture.write_text(json.dumps(golden, ensure_ascii=False, indent=2) + '\n')
print(f"Canonical Hugging Face reference: {len(golden['cases'])} cases verified")
