"""Validate/stage Salve 2D renders without writing to the Convert repository."""
from __future__ import annotations
import argparse, hashlib, json, math, shutil, sys
from pathlib import Path
from PIL import Image

SOURCE_COMMIT = "18f999ab76e1256485b750af5a215dd84fde3da9"
CATALOG = Path(__file__).parent / "source/app/src/main/assets/avatar/core/manifest.json"
OLD_RIG = Path(__file__).parent / "source/app/src/main/assets/avatar/core/rig.json"
JOINTS = ("leftShoulder", "rightShoulder", "leftElbow", "rightElbow", "leftPalm", "rightPalm", "leftHip", "rightHip", "leftKnee", "rightKnee")

def sha256(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def resolve_render(folder, index, identifier):
    options = [folder / f"{identifier}.png", folder / f"{index:02d}_{identifier}.png"]
    found = [p for p in options if p.is_file()]
    if len(found) != 1:
        raise ValueError(f"{identifier}: expected one named PNG, found {len(found)}")
    return found[0]

def point(value, width, height, name):
    if not isinstance(value, list) or len(value) != 2:
        raise ValueError(f"{name}: expected [x,y]")
    if any(isinstance(v, bool) or not isinstance(v, (int, float)) or not math.isfinite(v) for v in value):
        raise ValueError(f"{name}: invalid coordinate")
    if not (0 <= value[0] < width and 0 <= value[1] < height):
        raise ValueError(f"{name}: point outside image")
    return list(value)

def run(args):
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    metadata = json.loads(args.landmarks.read_text(encoding="utf-8")) if args.landmarks else {}
    if metadata and metadata.get("coordinateSystem") != "pixels_top_left":
        raise ValueError("landmarks must declare coordinateSystem=pixels_top_left")
    views_meta = metadata.get("views", {})
    if not isinstance(views_meta, dict):
        raise ValueError("landmarks.views must be an object indexed by catalog id")
    output = args.out.resolve()
    if output.exists() and any(output.iterdir()):
        raise ValueError("Output directory must be new/empty; prior deliveries are preserved")
    core = output / "candidate/app/src/main/assets/avatar/core"
    core.mkdir(parents=True, exist_ok=True)
    report = {"sourceCommit": SOURCE_COMMIT, "status": "candidate_not_integrated", "originalsModified": False,
              "dimensionsAndTransparencyPass": True, "landmarksComplete": True, "productionReady": False,
              "visualIdentityVerified": False, "views": [], "errors": [], "pending": []}
    staged = {"version": 1, "representation": "2d_multiview", "origin": "blender_static_renders", "defaultView": "front",
              "approved": False, "sourceCommit": SOURCE_COMMIT, "views": []}
    normalized = {"version": 1, "coordinateSystem": "pixels_top_left", "views": {}}
    for index, entry in enumerate(catalog["views"], 1):
        identifier = entry["id"]
        rec = {"id": identifier, "expectedDimensions": [entry["width"], entry["height"]]}
        try:
            source = resolve_render(args.renders, index, identifier)
            with Image.open(source) as check: check.verify()
            with Image.open(source) as image:
                image.load()
                rec.update({"actualDimensions": list(image.size), "mode": image.mode, "format": image.format})
                if image.size != (entry["width"], entry["height"]): raise ValueError("wrong dimensions")
                if image.format != "PNG" or image.mode != "RGBA": raise ValueError("expected RGBA PNG")
                alpha = image.getchannel("A")
                rec["alphaRange"] = list(alpha.getextrema())
                rec["alphaBounds"] = list(alpha.getbbox()) if alpha.getbbox() else None
                if rec["alphaRange"][0] != 0 or rec["alphaRange"][1] == 0: raise ValueError("transparent margin and visible content required")
                w, h = image.size
                borders = (alpha.crop((0,0,w,1)), alpha.crop((0,h-1,w,h)), alpha.crop((0,0,1,h)), alpha.crop((w-1,0,w,h)))
                rec["nonemptyImageBorder"] = any(part.getextrema()[1] > 0 for part in borders)
                if rec["nonemptyImageBorder"]: raise ValueError("visible pixels at image border: possible crop")
            rec["sha256"] = sha256(source)
            rec["pngPass"] = True
            target = core / f"{identifier}.png"
            shutil.copyfile(source, target)
            item = dict(entry)
            item.update({"sha256": rec["sha256"], "referenceSha256": entry["sha256"], "renderSource": source.name})
            staged["views"].append(item)
            landmarks = views_meta.get(identifier)
            if landmarks is not None:
                if not isinstance(landmarks, dict): raise ValueError("landmarks entry must be object")
                if landmarks.get("sourceSha256") != rec["sha256"]: raise ValueError("landmarks hash does not match PNG")
                for key in ("leftEye", "rightEye", "mouth", "headPivot", "bodyPivot", "groundAnchor"):
                    if key in landmarks:
                        if landmarks[key] is None and identifier not in ("front", "seated", "kneeling", "crouched"):
                            # Hidden facial points may be omitted for reference-only side/back views.
                            continue
                        point(landmarks[key], w, h, key)
                for key, value in landmarks.get("joints", {}).items(): point(value, w, h, key)
                if identifier in ("front", "seated", "kneeling", "crouched") and landmarks.get("leftEye") is not None and landmarks.get("rightEye") is not None:
                    left, right = landmarks["leftEye"], landmarks["rightEye"]
                    if math.hypot(left[0]-right[0], left[1]-right[1]) < 2:
                        raise ValueError("invalid eye distance")
                rec["landmarksProvided"] = True
                # Metadata projections must still be checked visually. Do not promote guessed numbers.
                rec["landmarksVisualCheck"] = landmarks.get("visualCheck") == "passed"
                rec["landmarkOcclusionCheck"] = landmarks.get("occlusionCheck", "not_provided")
                if identifier in ("front", "seated", "kneeling", "crouched") and "occlusionCheck" in landmarks and landmarks["occlusionCheck"] != "passed":
                    report["pending"].append(f"{identifier}: eye occlusion check {landmarks['occlusionCheck']}; current Java pose effects require two visible eye anchors")
                    report["landmarksComplete"] = False
                for key in ("leftEye", "rightEye", "mouth", "groundAnchor", "scaleToStanding"):
                    if key not in landmarks and identifier in ("front", "seated", "kneeling", "crouched"):
                        report["pending"].append(f"{identifier}: missing {key}")
                        report["landmarksComplete"] = False
                factor = landmarks.get("scaleToStanding", 1.0)
                if isinstance(factor, bool) or not isinstance(factor, (int,float)) or not math.isfinite(factor) or not .1 <= factor <= 5:
                    raise ValueError("invalid scaleToStanding")
                angle = landmarks.get("mouthRotationDegrees", 0)
                if isinstance(angle, bool) or not isinstance(angle, (int,float)) or not math.isfinite(angle) or not -180 <= angle <= 180:
                    raise ValueError("invalid mouthRotationDegrees")
                face_scale = landmarks.get("faceScale", 1)
                if isinstance(face_scale, bool) or not isinstance(face_scale, (int,float)) or not math.isfinite(face_scale) or not .1 <= face_scale <= 5:
                    raise ValueError("invalid faceScale")
                if identifier in ("front", "seated", "kneeling", "crouched") and not rec["landmarksVisualCheck"]:
                    report["pending"].append(f"{identifier}: landmark visual check pending")
                    report["landmarksComplete"] = False
                normalized["views"][identifier] = {**landmarks, "width": w, "height": h}
            elif identifier in ("front", "seated", "kneeling", "crouched"):
                rec["landmarksProvided"] = False
                report["landmarksComplete"] = False
                report["pending"].append(f"{identifier}: current render landmarks missing; old anchors not copied")
        except (OSError, ValueError, TypeError) as error:
            rec["error"] = str(error)
            if not rec.get("pngPass"): report["dimensionsAndTransparencyPass"] = False
            else: report["landmarksComplete"] = False
            report["errors"].append(f"{identifier}: {error}")
        report["views"].append(rec)
    front = normalized["views"].get("front", {})
    rig_keys = ("headPivot", "bodyPivot", "leftEye", "rightEye", "mouth")
    if all(key in front for key in rig_keys) and all(key in front.get("joints", {}) for key in JOINTS):
        rig = json.loads(OLD_RIG.read_text(encoding="utf-8"))
        # The source's 2D deformation field must be recalibrated when framing/proportions change.
        # Reuse structure only, never old face/joint positions or source hash.
        rig.update({key: front[key] for key in rig_keys})
        rig.update({"sourceSha256": front["sourceSha256"], "joints": {key: front["joints"][key] for key in JOINTS}})
        (core / "rig.json").write_text(json.dumps(rig, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
        report["frontalRigGenerated"] = True
        report["pending"].append("Validate AvatarRig's pixel-based deformation regions and triangle orientation against the new frontal")
    else:
        report["frontalRigGenerated"] = False
        report["landmarksComplete"] = False
        report["pending"].append("front: complete eye/mouth/pivots and all 10 joints needed for rig.json")
    (core / "manifest.json").write_text(json.dumps(staged, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    (core / "landmarks.json").write_text(json.dumps(normalized, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    report["pending"].extend(["Run CoreRigTest against this candidate rig and validate room/overlay behavior on device; runtime patch compilation with original assets is documented separately", "Validate expression/viseme consumption: existing 2D runtime does not consume all new expression PNGs", "Apply/install/publish only after explicit product incorporation instruction"])
    report["packageReadyForIntegrationReview"] = report["dimensionsAndTransparencyPass"] and report["landmarksComplete"] and not report["errors"]
    (output / "validation.json").write_text(json.dumps(report, ensure_ascii=False, indent=2)+"\n", encoding="utf-8")
    print(json.dumps({key:report[key] for key in ("status","dimensionsAndTransparencyPass","landmarksComplete","frontalRigGenerated","packageReadyForIntegrationReview","errors")}, ensure_ascii=False))
    return 0 if report["packageReadyForIntegrationReview"] else 2

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--renders", type=Path, required=True)
    parser.add_argument("--landmarks", type=Path)
    parser.add_argument("--out", type=Path, required=True)
    try: sys.exit(run(parser.parse_args()))
    except (OSError, ValueError, json.JSONDecodeError) as error: parser.exit(1, f"{error}\n")
