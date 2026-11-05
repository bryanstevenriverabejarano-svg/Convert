#!/usr/bin/env bash
# Prepara la voz neuronal de Salve para empaquetarla en el APK (la usan android.yml y salve-release.yml):
#   - libsherpa-onnx-jni.so (sherpa-onnx 1.13.8, ONNX Runtime enlazado dentro: no choca con el de la app)
#   - tokens.txt y espeak-ng-data (solo el diccionario español) de la voz Piper es_ES-sharvard-medium
# El modelo de la voz (77 MB) NO va en el APK: Salve lo descarga aparte (models_optional.json).
# Todo va fijado por versión y SHA-256.
set -euo pipefail
out="${1:-app/build/generated/neural-voice}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

fetch() { # url sha256 destino
  curl -fsSL --retry 3 -o "$3" "$1"
  echo "$2  $3" | sha256sum -c -
}

fetch https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-v1.13.8-android-static-link-onnxruntime.tar.bz2 \
  7583ca385ae7d981e65468455c2ea2c9f2da383921dccfc5658d0dc19d309e6f "$work/sherpa.tar.bz2"
fetch https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-es_ES-sharvard-medium.tar.bz2 \
  b30a7a83df0518f0ee1c7039506648cade99f1f9b498fc49ed2ced2e2536bb5a "$work/voice.tar.bz2"

mkdir -p "$work/s" "$work/v"
tar xjf "$work/sherpa.tar.bz2" -C "$work/s" ./jniLibs/arm64-v8a/libsherpa-onnx-jni.so
tar xjf "$work/voice.tar.bz2" -C "$work/v"
voice="$work/v/vits-piper-es_ES-sharvard-medium"
echo "87c8ef66eae5473ed0cc0366b3964c736ca6c5f676c979522ea31234e47430b9  $voice/tokens.txt" | sha256sum -c -

rm -rf "$out"
mkdir -p "$out/jniLibs/arm64-v8a" "$out/assets/neural-voice"
cp "$work/s/jniLibs/arm64-v8a/libsherpa-onnx-jni.so" "$out/jniLibs/arm64-v8a/"
cp "$voice/tokens.txt" "$out/assets/neural-voice/"
cp -r "$voice/espeak-ng-data" "$out/assets/neural-voice/"
# Solo español: los demás diccionarios ocupan ~17 MB y espeak solo carga el del idioma de la voz.
find "$out/assets/neural-voice/espeak-ng-data" -maxdepth 1 -name '*_dict' ! -name 'es_dict' -delete
# Lista de archivos: la app los copia de assets a su carpeta (espeak necesita rutas reales).
(cd "$out/assets/neural-voice" && find espeak-ng-data -type f | sort > espeak-files.txt)
du -sh "$out"/*
