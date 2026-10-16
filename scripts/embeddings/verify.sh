#!/usr/bin/env bash
# JDK 17+, CMake 3.22+, C++ compiler, curl. Pass additional CMake arguments if necessary.
set -euo pipefail
cd "$(dirname "$0")/../.."
test -f third_party/sentencepiece/src/sentencepiece_processor.h || {
  echo 'Run git submodule update --init third_party/sentencepiece first' >&2; exit 1;
}
validation_dir=${SALVE_EMBEDDING_VALIDATION_DIR:-"$PWD/build/embedding-validation"}
model_dir=${SALVE_E5_MODEL_DIR:-"$validation_dir/model"}
python_bin=${PYTHON:-python3}
mkdir -p "$validation_dir/jars" "$validation_dir/classes"
"$python_bin" scripts/embeddings/prepare.py --output "$model_dir"
"$python_bin" scripts/embeddings/reference.py --model-dir "$model_dir"
"$python_bin" scripts/embeddings/evaluate.py --model-dir "$model_dir" --model e5 \
  --output "$validation_dir/retrieval.json" --vectors "$validation_dir/reference-vectors.json"
"${CMAKE:-cmake}" -S app/src/main/cpp -B "$validation_dir/native" \
  -DSALVE_EMBEDDINGS_ONLY=ON -DCMAKE_BUILD_TYPE=Release "$@"
"${CMAKE:-cmake}" --build "$validation_dir/native" --target salve_sentencepiece --parallel 2
curl --fail --location --silent --show-error https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar -o "$validation_dir/jars/gson.jar"
curl --fail --location --silent --show-error https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime/1.24.2/onnxruntime-1.24.2.jar -o "$validation_dir/jars/onnxruntime.jar"
classpath="$validation_dir/jars/gson.jar:$validation_dir/jars/onnxruntime.jar"
java -m jdk.compiler/com.sun.tools.javac.Main --release 11 -cp "$classpath" -d "$validation_dir/classes" \
  app/src/main/java/salve/core/embeddings/E5Tokenizer.java \
  app/src/main/java/salve/core/embeddings/E5EmbeddingEngine.java \
  app/src/main/java/salve/core/mind/MindEmbedder.java \
  app/src/main/java/salve/core/memory/MemoryVectorStore.java \
  app/src/main/java/salve/core/mind/EmbeddingCalibration.java scripts/embeddings/VerifyE5.java
java -Djava.library.path="$validation_dir/native" -cp "$validation_dir/classes:$classpath" VerifyE5 \
  "$model_dir/sentencepiece.bpe.model" docs/evidence/embeddings/tokenizer-golden.json \
  "$model_dir/model_qint8_avx512_vnni.onnx" "$validation_dir/reference-vectors.json"
