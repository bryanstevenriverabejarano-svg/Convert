#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
javac -encoding UTF-8 -d "$OUT" \
  "$ROOT/app/src/main/java/salve/core/autonomy/VerifiedStrategyPolicy.java" \
  "$ROOT/app/src/main/java/salve/core/memory/MemoryEvidenceRanker.java" \
  "$ROOT/app/src/main/java/salve/core/memory/MemorySearchQuery.java" \
  "$ROOT/app/src/main/java/salve/core/memory/MemoryQueryTerms.java" \
  "$ROOT/app/src/test/java/salve/core/learning/ResearchKernelChecks.java"
java -cp "$OUT" salve.core.learning.ResearchKernelChecks
