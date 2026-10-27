#!/usr/bin/env python3
"""Install Salve's four original G1-G4 turn images in the current 2D avatar.

Usage: python3 scripts/install_salve_turns_g1_g4.py /path/to/G1-G4
The source directory must contain G1.png, G2.png, G3.png and G4.png.
"""
import hashlib
import json
import shutil
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
CELL_W, CELL_H = 448, 672

REPLACEMENTS = {
    "static final int[] RIGHT_PATH = {0, 8, 9, 10, 1, 11, 2}, LEFT_PATH = {0, 12, 13, 14, 15, 7, 6};":
        "static final int[] RIGHT_PATH = {0, 8, 1, 9, 2}, LEFT_PATH = {0, 10, 7, 11, 6};",
    "private static final int[] SPIN = {0, 8, 9, 10, 1, 11, 2, 3, 4, 5, 6, 7, 15, 14, 13, 12};":
        "private static final int[] SPIN = {0, 8, 1, 9, 2, 3, 4, 5, 6, 11, 7, 10};",
    "there(RIGHT_PATH, 6, 2), TO_LEFT = there(LEFT_PATH, 6, 2)":
        "there(RIGHT_PATH, 4, 2), TO_LEFT = there(LEFT_PATH, 4, 2)",
    "there(RIGHT_PATH, 4, 12), LOOK_LEFT = there(LEFT_PATH, 4, 12)":
        "there(RIGHT_PATH, 2, 12), LOOK_LEFT = there(LEFT_PATH, 2, 12)",
    "TURN_STEP = .06f, LOOK_STEP = .08f":
        "TURN_STEP = .10f, LOOK_STEP = .08f",
    "case 8: case 9: case 12: case 13: return 0;\n            case 10: case 11: return 1;\n            case 14: case 15: return 7;":
        "case 8: case 10: return 0;\n            case 9: return 1;\n            case 11: return 7;",
}

def main(source):
    source = Path(source).expanduser().resolve()
    files = [source / ("G%d.png" % i) for i in range(1, 5)]
    for path in files:
        if not path.is_file():
            raise SystemExit("Missing source sprite: " + str(path))
    java = ROOT / "app/src/main/java/salve/avatar/AvatarTurn.java"
    manifest_path = ROOT / "app/src/main/assets/avatar/realistic/manifest.json"
    java_old = java.read_text(encoding="utf-8")
    java_new = java_old
    for before, after in REPLACEMENTS.items():
        if before not in java_new:
            raise SystemExit("AvatarTurn changed: expected code not found; no files modified. " + before[:75])
        java_new = java_new.replace(before, after, 1)

    manifest = json.loads(manifest_path.read_text(encoding="utf-8"))
    if "turn" not in manifest["sheets"]:
        raise SystemExit("Missing turn sheet in manifest; no files modified")
    atlas = Image.new("RGBA", (CELL_W * 4, CELL_H), (0, 0, 0, 0))
    for i, path in enumerate(files):
        with Image.open(path) as original:
            frame = original.convert("RGBA")
            if frame.size != (1024, 1536):
                raise SystemExit("%s must be 1024x1536" % path.name)
            if frame.getchannel("A").getextrema()[0] >= 255:
                raise SystemExit("%s is not transparent" % path.name)
            frame = frame.resize((CELL_W, CELL_H), Image.Resampling.LANCZOS)
            atlas.paste(frame, (i * CELL_W, 0))

    dest_dir = ROOT / "tools/avatar-realistic/turn-frames"
    dest_dir.mkdir(parents=True, exist_ok=True)
    for path in files:
        if path.resolve() != (dest_dir / path.name).resolve():
            shutil.copy2(path, dest_dir / path.name)
    output = manifest_path.parent / "turn.png"
    atlas.save(output, optimize=True)
    manifest["version"] = max(5, int(manifest.get("version", 1)))
    manifest["sheets"]["turn"] = {
        "width": atlas.width,
        "height": atlas.height,
        "sha256": hashlib.sha256(output.read_bytes()).hexdigest(),
        "frames": [[i * CELL_W, 0, (i + 1) * CELL_W, CELL_H] for i in range(4)],
        "frameHeights": [CELL_H] * 4
    }
    manifest_path.write_text(json.dumps(manifest, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    # Keep existing explanatory comments about the legacy 8-frame sequence out of date? No:
    # The authoritative routes above now use the 4 genuine turn drawings instead.
    java.write_text(java_new, encoding="utf-8")
    print("Installed G1/G2 right, G3/G4 left. Keep existing 8 main views and other animations.")

if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit(__doc__)
    main(sys.argv[1])
