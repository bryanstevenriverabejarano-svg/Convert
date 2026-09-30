"""Read-only final integrity check for the current Salve V02 delivery.

Run after the final model, audits, GUI record, staging and ZIP are regenerated.
Emits one JSON document to stdout and exits 1 on failure. Writes no receipts.
This checks recorded evidence and file integrity, not artistic acceptance,
production quality, runtime masks, or behavior on an Android device.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import math
import re
import sys
import zipfile
from pathlib import Path, PurePosixPath

from PIL import Image


VIEW_IDS = (
    "front", "front_three_quarter", "right", "rear_right", "rear",
    "rear_three_quarter", "left", "front_opposite", "front_variant",
    "above", "below", "a_front", "a_open_hair", "a_rear", "seated",
    "crouched", "kneeling", "kneeling_variant", "leaning", "leaning_variant",
)
ACTIVE_IDS = ("front", "seated", "crouched", "kneeling")
POINT_KEYS = ("leftEye", "rightEye", "mouth", "headPivot", "bodyPivot", "groundAnchor")
HEX_SHA = re.compile(r"^[0-9a-f]{64}$")


class Verification:
    def __init__(self, out: Path, source_integrity: Path, archive: Path, sums: Path):
        self.out = out.resolve()
        self.source_integrity = source_integrity.resolve()
        self.archive = archive.resolve()
        self.sums = sums.resolve()
        self.failures: list[dict] = []
        self.check_count = 0
        self.section_status: dict[str, str] = {}
        self.counts: dict[str, int] = {}
        self.hashes: dict[str, tuple[tuple[int, int], str]] = {}

    def check(self, ok: bool, key: str, **details) -> None:
        self.check_count += 1
        if not ok:
            self.failures.append({"check": key, **details})

    def section(self, name: str, callback) -> None:
        before = len(self.failures)
        try:
            callback()
        except Exception as exc:
            self.check(False, name + ".exception", error=type(exc).__name__, message=str(exc))
        self.section_status[name] = "PASS" if len(self.failures) == before else "FAIL"

    def read_json(self, relative: str):
        return json.loads(self.output_path(relative).read_text(encoding="utf-8"))

    def output_path(self, relative: str) -> Path:
        posix = PurePosixPath(relative)
        if "\\" in relative or posix.is_absolute() or ".." in posix.parts or ":" in relative:
            raise ValueError("Unsafe output-relative path: " + relative)
        path = (self.out / Path(*posix.parts)).resolve()
        if not path.is_relative_to(self.out):
            raise ValueError("Path outside outputs: " + relative)
        return path

    def sha(self, path: Path) -> str:
        path = path.resolve()
        stat = path.stat()
        signature = (stat.st_size, stat.st_mtime_ns)
        cached = self.hashes.get(str(path))
        if cached and cached[0] == signature:
            return cached[1]
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(block)
        after = path.stat()
        if (after.st_size, after.st_mtime_ns) != signature:
            raise RuntimeError("File changed during read: " + str(path))
        value = digest.hexdigest()
        self.hashes[str(path)] = (signature, value)
        return value

    def sha_matches(self, actual: str, expected, key: str) -> None:
        self.check(isinstance(expected, str) and bool(HEX_SHA.fullmatch(expected))
                   and actual == expected, key, actual=actual, expected=expected)

    def indexed_list(self, rows, expected_count: int, key: str) -> dict:
        if not isinstance(rows, list):
            raise TypeError(key + " must be a list")
        self.check(len(rows) == expected_count, key + ".count", actual=len(rows), expected=expected_count)
        result = {row["id"]: row for row in rows}
        self.check(len(result) == len(rows), key + ".unique_ids")
        return result

    def image(self, path: Path, dims: tuple[int, int], key: str, body=False) -> None:
        with Image.open(path) as im:
            im.load()
            self.check(im.format == "PNG", key + ".format", actual=im.format)
            self.check(im.mode == "RGBA", key + ".mode", actual=im.mode)
            self.check(im.size == dims, key + ".dimensions", actual=list(im.size), expected=list(dims))
            if im.mode != "RGBA":
                return
            alpha = im.getchannel("A")
            minimum, maximum = alpha.getextrema()
            self.check(minimum == 0 and maximum > 16, key + ".transparency", alphaRange=[minimum, maximum])
            if body:
                w, h = im.size
                edges = [alpha.crop(box).getextrema()[1] for box in
                         [(0, 0, w, 1), (0, h - 1, w, h), (0, 0, 1, h), (w - 1, 0, w, h)]]
                self.check(max(edges) <= 16, key + ".no_border_alpha_above_16", borderAlphaMax=max(edges))

    def blend(self) -> None:
        audit = self.read_json("Salve_auditoria_blend_v02.json")
        gui = self.read_json("Salve_apertura_GUI_v02.json")
        model = self.output_path("Salve_refinado_v02.blend")
        actual = self.sha(model)
        self.sha_matches(actual, audit["fileSha256Before"], "blend.audit_before")
        self.sha_matches(actual, audit["fileSha256After"], "blend.audit_after")
        self.sha_matches(actual, gui["fileSha256"], "blend.gui_sha")
        self.check(Path(audit["file"]).resolve() == model, "blend.audit_path")
        self.check(Path(gui["file"]).resolve() == model, "blend.gui_path")
        self.check(audit["sourceFileUnchanged"] is True, "blend.audit_read_only")
        self.check(gui["guiOpened"] is True, "blend.gui_opened")
        self.check(audit["scene"] == gui["scene"] == "04_SALVE_MODELO_3D", "blend.scene")
        self.check(audit["frame"] == gui["frame"] == 1, "blend.frame")
        self.check(gui["camera"] == "CAM_front", "blend.gui_camera")
        self.check(audit["productionReady"] is False, "blend.production_not_certified")
        self.check(audit["missingTextureCount"] == 0, "blend.no_missing_textures")
        self.check(audit["usedAtlasImages"] == 3, "blend.three_atlas_images")
        self.check(audit["usedAtlasPackedImagesMatchExternalFiles"] is True, "blend.atlas_embedded_external_match")
        for im in audit["images"]:
            if im["visibleMaterialUsers"] and im["packed"] and im["externalFileExists"]:
                external = Path(im["filepath"])
                actual_image = self.sha(external)
                self.sha_matches(actual_image, im["externalFileSha256"], "blend.texture_external." + im["name"])
                self.sha_matches(actual_image, im["packedFileSha256"], "blend.texture_packed." + im["name"])
                self.check(im["packedBytesMatchExternalFile"] is True, "blend.texture_match." + im["name"])

    def body_views(self) -> None:
        self.manifest = self.indexed_list(self.read_json("vistas_manifest.json"), 20, "body.manifest")
        reviewed = self.read_json("Salve_revision_visual.json")
        self.check(set(self.manifest) == set(VIEW_IDS), "body.catalog_ids")
        self.check(set(reviewed) == set(VIEW_IDS), "body.visual_review_ids")
        self.body_hashes = {}
        for view_id, row in self.manifest.items():
            key = "body." + view_id
            dims = (1374, 1145) if view_id == "kneeling_variant" else (1024, 1536)
            self.check(row["size"] == list(dims), key + ".manifest_dimensions")
            path = self.output_path(row["file"])
            self.image(path, dims, key, body=True)
            actual = self.sha(path)
            self.body_hashes[view_id] = actual
            self.sha_matches(actual, row["render_sha256"], key + ".manifest_sha")
            self.sha_matches(actual, reviewed[view_id]["render_sha256_reviewed"], key + ".reviewed_sha")
            self.check(reviewed[view_id]["reviewed"] is True, key + ".reviewed")
            self.check(reviewed[view_id]["reference_sha256"] == row["reference_sha256"], key + ".reference_sha_consistent")
        self.counts["body_views"] = len(self.manifest)

    def portraits(self) -> None:
        controls = self.indexed_list(self.read_json("expresiones_controles.json"), 21, "portraits.controls")
        review = self.read_json("Salve_revision_expresiones.json")
        states = self.indexed_list(review["states"], 21, "portraits.review_states")
        numeric = self.read_json("Salve_validacion_facial_v02.json")
        self.check(set(controls) == set(states), "portraits.ids_consistent")
        self.check(review["render_count"] == review["visually_reviewed_count"] == 21, "portraits.review_count")
        self.check(numeric["errors"] == [] and numeric["passed_numeric_checks"] is True, "portraits.numeric_checks_passed")
        for expression_id, row in controls.items():
            key = "portraits." + expression_id
            state = states[expression_id]
            path = self.output_path(row["file"])
            self.image(path, (768, 768), key)
            actual = self.sha(path)
            self.sha_matches(actual, row["sha256"], key + ".controls_sha")
            self.sha_matches(actual, state["sha256"], key + ".state_sha")
            self.sha_matches(actual, state["visual_review"]["reviewed_sha256"], key + ".reviewed_sha")
            self.check(state["visual_review"]["visually_reviewed"] is True, key + ".reviewed")
            self.check(row["size"] == state["image_checks"]["dimensions"] == [768, 768], key + ".metadata_dimensions")
            self.check(row["frame"] == state["frame"] and row["file"] == state["file"], key + ".metadata_consistent")
            self.check(state["image_checks"]["checks_passed"] is True, key + ".image_checks_recorded")
        self.counts["portraits"] = len(controls)

    def originals(self) -> None:
        baseline = json.loads(self.source_integrity.read_text(encoding="utf-8"))
        self.sha_matches(self.sha(Path(baseline["blend_source"])), baseline["blend_sha256"], "originals.previous_blend")
        refs = baseline["references"]
        self.check(set(refs) == set(VIEW_IDS) and len(refs) == 20, "originals.reference_count_and_ids")
        for view_id, row in refs.items():
            key = "originals." + view_id
            source = Path(row["source"])
            self.sha_matches(self.sha(source), row["sha256"], key + ".reference_sha")
            self.check(row["original_hash_matches"] is True, key + ".baseline_hash_match")
            with Image.open(source) as im:
                self.check(list(im.size) == row["size"], key + ".dimensions")
            if hasattr(self, "manifest") and view_id in self.manifest:
                self.check(self.manifest[view_id]["reference_sha256"] == row["sha256"], key + ".manifest_reference")
        self.counts["unchanged_reference_files"] = len(refs)

    def facial_cleanup(self) -> None:
        report_path = self.output_path("Salve_limpieza_rostroUV_v02.json")
        if not report_path.exists():
            self.counts["facial_cleanups_applied"] = 0
            return
        report = json.loads(report_path.read_text(encoding="utf-8"))
        applied = report["saved_model"] is True
        self.counts["facial_cleanups_applied"] = int(applied)
        if not applied:
            # Rejection is evidence of an unapplied attempt, not a requirement
            # that its interrupted numerical pass covered all 33 frames/41 views.
            self.check(report["status"] == "rejected_rolled_back", "cleanup.rejected_status")
            self.check(report["saved"] is False and report["candidate_in_memory"] is False,
                       "cleanup.no_saved_candidate")
            self.check(report["original_restored_on_failure"] is True, "cleanup.rollback_recorded")
            self.check(report["numeric_guard_passed"] is False, "cleanup.failed_guard_recorded")
            self.check(report["delivered_pngs_replaced"] is False, "cleanup.renders_unchanged")
            self.sha_matches(self.sha(self.output_path("Salve_refinado_v02.blend")),
                             report["source_model_sha256"], "cleanup.unchanged_model_sha")
            self.check(any(row["passed"] is False for row in report["metrics_at_unique_frames"]),
                       "cleanup.rejection_metric_present")
            self.counts["rejected_cleanup_frames_measured"] = len(report["metrics_at_unique_frames"])
            return
        # These are the principal cleanup keys emitted by the real adapter.
        # No claim of evaluated bit equality is required or made here.
        metrics = report["surface_conservation_metrics_at_unique_frames"]
        delivered = self.read_json("vistas_manifest.json") + self.read_json("expresiones_controles.json")
        expected = {(row["frame"], row["id"], row["camera"], tuple(row["size"])) for row in delivered}
        measured = {(row["frame"], view["id"], view["camera"], tuple(view["size"]))
                    for row in metrics for view in row["views"]}
        self.check(report["unique_frame_count_checked"] == len(metrics) == len({row["frame"] for row in metrics}) == 33,
                   "cleanup.applied_33_frames")
        self.check(report["rendered_image_count_covered"] == sum(len(row["views"]) for row in metrics) == 41
                   and measured == expected, "cleanup.applied_41_delivered_cameras")
        self.check(report["weights_and_drivers_preserved"] is True, "cleanup.input_weights_drivers_preserved")
        self.check(report["uv_check"]["outside_unit_tile"] == 0
                   and report["uv_check"]["zero_area_faces"] == [], "cleanup.revised_uv_numeric_check")
        for row in metrics:
            key = "cleanup.frame." + str(row["frame"])
            self.check(row["passed"] is True and row["matrix_exact"] is True and row["face_layout_exact"] is True,
                       key + ".guard_passed")
            for angle in ("max_corner_normal_angle_deg", "max_face_normal_angle_deg"):
                value = row[angle]
                self.check(math.isfinite(value) and 0 <= value <= 1.0, key + "." + angle, actual=value, limit=1.0)
            for view in row["views"]:
                value = view["maximum_pixel_displacement"]
                self.check(view["passed"] is True and math.isfinite(value) and 0 <= value <= .01,
                           key + ".camera." + view["id"], actual=value, limit=.01)
        self.check(self.output_path("fuentes_reproducibles/face_cleanup_visual_conservation_v02.py").is_file(),
                   "cleanup.adapter_source_included")

    def staging(self) -> None:
        exported = self.read_json("renders_landmarks.json")
        checked = self.read_json("Salve_integracion_v02/landmark_visual_checks.json")
        overlays = self.indexed_list(self.read_json("Salve_integracion_v02/anclajes_revision.json")["views"], 4, "anchors.overlays")
        validation = self.read_json("Salve_paquete_recursos_v02/validation.json")
        validated_rows = self.indexed_list(validation["views"], 20, "staging.validation_views")
        prefix = "Salve_paquete_recursos_v02/candidate/app/src/main/assets/"
        staged_manifest = self.read_json(prefix + "avatar/core/manifest.json")
        staged_views = self.indexed_list(staged_manifest["views"], 20, "staging.manifest")
        staged_landmarks = self.read_json(prefix + "avatar/core/landmarks.json")
        rig_path = self.output_path(prefix + "avatar/core/rig.json")
        rig = json.loads(rig_path.read_text(encoding="utf-8"))
        self.check(set(checked["views"]) == set(overlays) == set(ACTIVE_IDS), "anchors.four_active_ids")
        self.check(exported["coordinateSystem"] == staged_landmarks["coordinateSystem"] == "pixels_top_left", "anchors.coordinate_system")
        self.check(set(exported["views"]) == set(staged_landmarks["views"]) == set(staged_views) == set(validated_rows) == set(VIEW_IDS), "staging.twenty_ids")
        self.check(validation["errors"] == [], "staging.no_errors", errors=validation["errors"])
        for field in ["dimensionsAndTransparencyPass", "landmarksComplete", "frontalRigGenerated", "packageReadyForIntegrationReview", "androidModuleCompileVerified"]:
            self.check(validation[field] is True, "staging." + field)
        self.check(validation["originalsModified"] is False and staged_manifest["approved"] is False, "staging.unpublished_candidate")
        self.check(validation["productionReady"] is False and validation["visualIdentityVerified"] is False, "staging.acceptance_not_inferred")
        for view_id, staged in staged_views.items():
            key = "staging." + view_id
            actual = self.sha(self.output_path(prefix + staged["path"]))
            self.sha_matches(actual, self.body_hashes[view_id], key + ".current_render_sha")
            self.sha_matches(actual, staged["sha256"], key + ".manifest_sha")
            self.sha_matches(actual, validated_rows[view_id]["sha256"], key + ".validation_sha")
            dims = self.manifest[view_id]["size"]
            self.check([staged["width"], staged["height"]] == validated_rows[view_id]["actualDimensions"] == dims, key + ".dimensions")
            self.check(validated_rows[view_id]["pngPass"] is True, key + ".png_pass")
            landmark = exported["views"][view_id]
            self.sha_matches(actual, landmark["sourceSha256"], key + ".landmarks_sha")
            self.check(staged_landmarks["views"][view_id] == landmark, key + ".landmarks_current")
            self.check([landmark["width"], landmark["height"]] == dims, key + ".landmark_dimensions")
            self.check(isinstance(landmark["scaleToStanding"], (int, float)) and math.isfinite(landmark["scaleToStanding"]) and landmark["scaleToStanding"] > 0, key + ".scale_finite_positive")
            for point_name in POINT_KEYS:
                point = landmark[point_name]
                self.check(isinstance(point, list) and len(point) == 2 and all(isinstance(v, (int, float)) and math.isfinite(v) for v in point), key + ".finite_point." + point_name)
            if view_id in ACTIVE_IDS:
                technical = checked["views"][view_id]
                overlay = overlays[view_id]
                for label, record in [("exported", landmark), ("technical_review", technical), ("overlay", overlay)]:
                    self.sha_matches(actual, record["sourceSha256"], key + "." + label + "_sha")
                    self.check(record["visualCheck"] == "passed", key + "." + label + "_visual_check")
                self.check(landmark["occlusionCheck"] == technical["occlusionCheck"] == "passed", key + ".iris_centers_visible")
                overlay_points = {point["name"]: point["point"] for point in overlay["points"]}
                self.check(all(overlay_points[name] == landmark[name] for name in POINT_KEYS), key + ".overlay_points_current")
        self.sha_matches(self.body_hashes["front"], rig["sourceSha256"], "staging.rig_front_sha")
        for point in ("headPivot", "bodyPivot", "leftEye", "rightEye", "mouth"):
            self.check(rig[point] == exported["views"]["front"][point], "staging.rig_point." + point)
        self.check(all(value == exported["views"]["front"]["joints"][name] for name, value in rig["joints"].items()), "staging.rig_joints_current")
        compiled = self.read_json("Salve_integracion_v02/compilation_validation.json")
        self.check(compiled["buildExitCode"] == 0 and compiled["testsPassed"] == 21, "java.compile_and_21_tests")
        self.check(compiled["coreRigTestUsesCandidateAssets"] is True, "java.candidate_rig_tested")
        self.check(sum(int(suite["tests"]) for suite in compiled["suites"]) == 21 and all(int(suite[field]) == 0 for suite in compiled["suites"] for field in ("failures", "errors", "skipped")), "java.suite_counts")
        self.sha_matches(self.sha(rig_path), compiled["coreRigSha256"], "java.tested_rig_sha")
        self.sha_matches(self.body_hashes["front"], compiled["frontPNGSourceSha256"], "java.tested_front_sha")
        tested = validation["candidateTestRun"]
        self.check(tested["testsPassed"] == 21 and tested["coreRigTests"] == 3 and tested["testsForcedToExecute"] is True, "staging.test_evidence")
        self.sha_matches(self.sha(rig_path), tested["coreRigSha256"], "staging.tested_rig_sha")
        self.sha_matches(self.body_hashes["front"], tested["frontPNGSourceSha256"], "staging.tested_front_sha")
        self.counts["active_anchor_reviews"] = len(ACTIVE_IDS)
        self.counts["staged_pngs"] = len(staged_views)
        self.counts["java_tests_passed"] = compiled["testsPassed"]

    def archive_integrity(self) -> None:
        raw_sums = self.sums.read_bytes()
        entries = {}
        for line_number, line in enumerate(raw_sums.decode("utf-8").splitlines(), 1):
            match = re.fullmatch(r"([0-9a-f]{64})  (.+)", line)
            if not match:
                raise ValueError("Malformed SHA256SUMS line " + str(line_number))
            expected, relative = match.groups()
            self.output_path(relative)
            if relative in entries:
                raise ValueError("Duplicate SHA256SUMS entry: " + relative)
            entries[relative] = expected
        self.check(bool(entries), "archive.nonempty_checksum_manifest")
        eligible = {p.relative_to(self.out).as_posix() for p in self.out.rglob("*")
                    if p.is_file() and p.suffix not in (".zip", ".blend1")
                    and p.name != "SHA256SUMS_v02.txt" and not p.name.startswith("Salve_GUI_")}
        self.check(set(entries) == eligible, "archive.disk_coverage", unlistedFiles=sorted(eligible - set(entries)), nonexistentEntries=sorted(set(entries) - eligible))
        with zipfile.ZipFile(self.archive, "r") as archive:
            names = archive.namelist()
            self.check(len(names) == len(set(names)), "archive.unique_members")
            for name in names:
                self.output_path(name)
            self.check(set(names) == set(entries) | {self.sums.name}, "archive.member_coverage", missingMembers=sorted(set(entries) - set(names)), extraMembers=sorted(set(names) - set(entries) - {self.sums.name}))
            bad_member = archive.testzip()
            self.check(bad_member is None, "archive.testzip", corruptMember=bad_member)
            self.check(archive.read(self.sums.name) == raw_sums, "archive.embedded_checksum_manifest_identical")
            for relative, expected in entries.items():
                self.sha_matches(self.sha(self.output_path(relative)), expected, "archive.disk_sha." + relative)
                digest = hashlib.sha256()
                with archive.open(relative, "r") as member:
                    for block in iter(lambda: member.read(1024 * 1024), b""):
                        digest.update(block)
                self.sha_matches(digest.hexdigest(), expected, "archive.member_sha." + relative)
        self.counts["checksum_entries"] = len(entries)
        self.counts["zip_members"] = len(names)

    def run(self) -> int:
        for name, callback in [("blend", self.blend), ("body_views", self.body_views),
                               ("portraits", self.portraits), ("originals", self.originals),
                               ("facial_cleanup_applied_or_rejected", self.facial_cleanup),
                               ("staging_and_java_evidence", self.staging),
                               ("zip_and_checksums", self.archive_integrity)]:
            self.section(name, callback)
        result = {
            "status": "FAIL" if self.failures else "PASS",
            "scope": "Read-only delivery integrity and consistency of recorded technical reviews; does not certify artistic identity, production, runtime masks or device behavior.",
            "outputs": str(self.out), "archive": str(self.archive),
            "sections": self.section_status, "counts": self.counts,
            "checks": self.check_count, "failureCount": len(self.failures),
            "failures": self.failures, "writesPerformed": False,
            "blenderOrAndroidExecuted": False,
        }
        print(json.dumps(result, ensure_ascii=True, indent=2))
        return 1 if self.failures else 0


def main() -> int:
    root = Path(__file__).resolve().parents[1]
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--outputs", type=Path, default=root / "outputs")
    parser.add_argument("--source-integrity", type=Path, default=root / "work/source_integrity.json")
    parser.add_argument("--zip", type=Path, default=None)
    parser.add_argument("--sums", type=Path, default=None)
    args = parser.parse_args()
    verifier = Verification(args.outputs, args.source_integrity,
                            args.zip or args.outputs / "Salve_entrega_v02.zip",
                            args.sums or args.outputs / "SHA256SUMS_v02.txt")
    return verifier.run()


if __name__ == "__main__":
    sys.exit(main())
