#!/usr/bin/env python3
"""Compare fixed synthetic retrieval cases; report host costs, never Android measurements.

Run once per process for a meaningful process RSS peak. Downloaded files stay outside git.
E5 reference is canonical XLMRobertaTokenizer(use_fast=False), as in production SentencePiece.
"""
import argparse
import hashlib
import json
import re
import resource
import time
import unicodedata
from pathlib import Path
import numpy as np
import onnxruntime as ort
from transformers import AutoTokenizer

parser = argparse.ArgumentParser()
parser.add_argument('--model-dir', type=Path, required=True)
parser.add_argument('--model', choices=['e5', 'minilm'], required=True)
parser.add_argument('--output', type=Path, required=True)
parser.add_argument('--vectors', type=Path)
parser.add_argument('--onnx-file')
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
rows = json.loads((root / 'docs/evidence/embeddings/retrieval-holdout.json').read_text())['cases']
file = args.onnx_file or ('model_qint8_avx512_vnni.onnx' if args.model == 'e5' else 'model_qint8_arm64.onnx')
tokenizer = AutoTokenizer.from_pretrained(args.model_dir, local_files_only=True, use_fast=args.model != 'e5')
options = ort.SessionOptions()
options.intra_op_num_threads = 2
options.inter_op_num_threads = 1
options.enable_cpu_mem_arena = False
options.enable_mem_pattern = False
before = resource.getrusage(resource.RUSAGE_SELF).ru_maxrss
start = time.perf_counter()
session = ort.InferenceSession(str(args.model_dir / file), options, providers=['CPUExecutionProvider'])
load_seconds = time.perf_counter() - start
samples, timing = [], []

def embed(text, kind):
    start = time.perf_counter()
    prefixed = (kind + ': ' if args.model == 'e5' else '') + text
    inputs = dict(tokenizer(prefixed, truncation=True, max_length=256, return_tensors='np'))
    inputs.setdefault('token_type_ids', np.zeros_like(inputs['input_ids']))
    hidden = session.run(None, {i.name: inputs[i.name].astype(np.int64) for i in session.get_inputs()})[0]
    vector = (hidden * inputs['attention_mask'][..., None]).sum(1) / inputs['attention_mask'].sum(1, keepdims=True)
    vector = (vector / np.linalg.norm(vector, axis=1, keepdims=True))[0]
    timing.append((time.perf_counter() - start) * 1000)
    samples.append({'text': text, 'kind': kind, 'vector': vector.tolist()})
    return vector

documents = np.stack([embed(r['document'], 'passage') for r in rows])
queries = np.stack([embed(r['query'], 'query') for r in rows])
scores = queries @ documents.T
rankings = np.argsort(-scores, axis=1)

def metrics(ids):
    return {'count': len(ids), 'top1_correct': sum(int(rankings[i, 0] == i) for i in ids),
            'recall_at_3': sum(int(i in rankings[i, :3]) for i in ids)}

# Transparent lexical overlap comparator, not a claim to reproduce the complete Room/FTS pipeline.
def words(text):
    return set(re.findall(r'\w+', ''.join(c for c in unicodedata.normalize('NFD', text.lower()) if unicodedata.category(c) != 'Mn')))
lexical = []
for r in rows:
    q = words(r['query'])
    lexical.append(sorted(range(len(rows)), key=lambda i: -len(q & words(rows[i]['document'])) / max(1, len(q | words(rows[i]['document'])))))
report = {'model': args.model, 'runtime': 'onnxruntime ' + ort.__version__, 'device': 'Linux x86_64 host, NOT Samsung S24 Ultra',
          'onnx_file': file, 'sha256': hashlib.file_digest((args.model_dir / file).open('rb'), 'sha256').hexdigest(),
          'tokenizer': type(tokenizer).__name__, 'max_tokens': 256, 'threads': 2,
          'overall': metrics(list(range(len(rows)))),
          'categories': {c: metrics([i for i, r in enumerate(rows) if r['category'] == c]) for c in sorted({r['category'] for r in rows})},
          'lexical_overlap_baseline': {'top1_correct': sum(int(v[0] == i) for i, v in enumerate(lexical)), 'count': len(rows)},
          'load_seconds': load_seconds, 'median_encode_ms': float(np.median(timing)),
          'process_peak_rss_kib': resource.getrusage(resource.RUSAGE_SELF).ru_maxrss, 'peak_before_session_kib': before,
          'failures': [{'id': i, 'query': r['query'], 'expected': i, 'top3': rankings[i, :3].tolist()}
                       for i, r in enumerate(rows) if rankings[i, 0] != i]}
args.output.write_text(json.dumps(report, ensure_ascii=False, indent=2))
if args.vectors:
    args.vectors.write_text(json.dumps(samples, ensure_ascii=False))
print(json.dumps(report, ensure_ascii=False, indent=2))
