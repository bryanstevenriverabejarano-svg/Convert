#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
OUT="$(mktemp -d)"
trap 'rm -rf "$OUT"' EXIT
SOURCE="$ROOT/app/src/main/java/salve/avatar"
javac -encoding UTF-8 -d "$OUT" \
  "$SOURCE/AvatarState.java" "$SOURCE/AvatarMotion.java" \
  "$SOURCE/AvatarMotionProtocol.java" "$SOURCE/MotionPreferenceProfile.java" \
  "$SOURCE/AvatarBodyCue.java" "$SOURCE/AvatarLocomotion.java" \
  "$SOURCE/AvatarStageLayout.java" "$SOURCE/RealisticSpriteAnimation.java" \
  "$ROOT/tools/avatar-realistic/SpriteChecks.java"
java -cp "$OUT" salve.avatar.SpriteChecks
