#!/usr/bin/env python3
"""Download the exact E5 reference outside the APK; reject changed/truncated weights."""
import argparse
import hashlib
from pathlib import Path
import urllib.request

REVISION = '614241f622f53c4eeff9890bdc4f31cfecc418b3'
FILES = {
    'sentencepiece.bpe.model': (5069051, 'cfc8146abe2a0488e9e2a0c56de7952f7c11ab059eca145a0a727afce0db2865'),
    'onnx/model_qint8_avx512_vnni.onnx': (118346824, 'dd476dd0c2514e9b9be83aeb3853fac0763e0bdf4a71645407587d77c48a2d88'),
    'tokenizer_config.json': None, 'special_tokens_map.json': None, 'config.json': None,
}

def digest(path):
    with path.open('rb') as stream:
        return hashlib.file_digest(stream, 'sha256').hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--output', type=Path, required=True)
    args = parser.parse_args()
    args.output.mkdir(parents=True, exist_ok=True)
    for remote, expected in FILES.items():
        target = args.output / Path(remote).name
        if not (target.is_file() and (expected is None or
                (target.stat().st_size == expected[0] and digest(target) == expected[1]))):
            pending = target.with_suffix(target.suffix + '.partial')
            urllib.request.urlretrieve(f'https://huggingface.co/intfloat/multilingual-e5-small/resolve/{REVISION}/{remote}', pending)
            if expected and (pending.stat().st_size != expected[0] or digest(pending) != expected[1]):
                raise ValueError(f'Artifact integrity failed: {remote}')
            pending.replace(target)
        print(f'{target.name}: {target.stat().st_size} bytes')

if __name__ == '__main__':
    main()
