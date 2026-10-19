#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
java com.sun.tools.javac.Main --release 11 -encoding UTF-8 -d "$OUT" \
  "$ROOT/app/src/main/java/salve/core/ModelResult.java" \
  "$ROOT/app/src/main/java/salve/core/compute/ComputeNode.java" \
  "$ROOT/app/src/main/java/salve/core/compute/ComputeRouter.java" \
  "$ROOT/app/src/main/java/salve/core/compute/NodePairing.java" \
  "$ROOT/app/src/main/java/salve/core/compute/NodeStatusCheck.java" \
  "$ROOT/app/src/test/java/salve/core/compute/ComputeRouterChecks.java" \
  "$ROOT/app/src/test/java/salve/core/compute/NodePairingChecks.java" \
  "$ROOT/app/src/test/java/salve/core/compute/NodeStatusChecks.java"
java -cp "$OUT" salve.core.compute.ComputeRouterChecks
java -cp "$OUT" salve.core.compute.NodePairingChecks
java -cp "$OUT" salve.core.compute.NodeStatusChecks
