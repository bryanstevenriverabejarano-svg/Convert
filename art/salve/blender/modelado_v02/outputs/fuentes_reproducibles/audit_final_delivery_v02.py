"""Read the delivered blend in background Blender and audit it without saving.

Invoke with Blender --background [blend] --python this_file.py. This wrapper
checks the state read from disk; it never fixes a frame, camera, or resolution.
Only the two JSON reports are written. This file must not run in Blender GUI.
"""
from __future__ import annotations

import hashlib
import json
from pathlib import Path
import runpy
import sys
import traceback

import bpy

ROOT = Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
SOURCE = ROOT / 'outputs/Salve_refinado_v02.blend'
SCENE_NAME = '04_SALVE_MODELO_3D'
AUDIT_SCRIPT = ROOT / 'work/inspect_final_blend.py'
FACE_SCRIPT = ROOT / 'work/validate_face_readonly.py'
AUDIT_JSON = ROOT / 'outputs/Salve_auditoria_blend_v02.json'
FACE_JSON = ROOT / 'outputs/Salve_validacion_facial_v02.json'


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open('rb') as stream:
        for block in iter(lambda: stream.read(1024 * 1024), b''):
            digest.update(block)
    return digest.hexdigest()


def observe(scene) -> dict:
    percentage = scene.render.resolution_percentage
    return {
        'scene': scene.name,
        'activeScene': bpy.context.scene.name,
        'frame': scene.frame_current,
        'camera': scene.camera.name if scene.camera else None,
        'resolution': [scene.render.resolution_x, scene.render.resolution_y],
        'resolutionPercentage': percentage,
        'effectiveResolution': [
            scene.render.resolution_x * percentage // 100,
            scene.render.resolution_y * percentage // 100,
        ],
        'imageFormat': scene.render.image_settings.file_format,
        'colorMode': scene.render.image_settings.color_mode,
        'transparentFilm': scene.render.film_transparent,
    }


def state_errors(stage: str, state: dict) -> list[str]:
    errors = []
    expected = {
        'scene': SCENE_NAME,
        'activeScene': SCENE_NAME,
        'frame': 1,
        'camera': 'CAM_front',
        'resolution': [1024, 1536],
        'resolutionPercentage': 100,
        'effectiveResolution': [1024, 1536],
    }
    for key, value in expected.items():
        if state.get(key) != value:
            errors.append(f'{stage}: {key}={state.get(key)!r}; expected {value!r}')
    return errors


def run_checked(script: Path, arguments: list[str]) -> dict:
    previous_argv = sys.argv[:]
    try:
        # The inspector expects named arguments after --. The facial validator
        # expects its single output path after --. Do not inherit Blender argv.
        sys.argv = [str(script), '--', *arguments]
        return runpy.run_path(str(script), run_name='__main__')
    finally:
        sys.argv = previous_argv


def write_json(path: Path, report: dict) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def main() -> None:
    if not bpy.app.background:
        raise RuntimeError('Final delivery audit is background-only; GUI scene was not touched.')
    for file in (SOURCE, AUDIT_SCRIPT, FACE_SCRIPT):
        if not file.is_file():
            raise FileNotFoundError(file)

    hashes = {'beforeOpen': sha256(SOURCE)}
    # Opening is read-only on disk. Accept both -b SOURCE and -b without a file;
    # in the latter case load the exact authorized output, never a checkpoint.
    loaded = Path(bpy.data.filepath).resolve() if bpy.data.filepath else None
    if loaded != SOURCE.resolve():
        bpy.ops.wm.open_mainfile(filepath=str(SOURCE))
    hashes['afterOpen'] = sha256(SOURCE)
    if Path(bpy.data.filepath).resolve() != SOURCE.resolve():
        raise RuntimeError('Blender did not open the final delivered file.')
    scene = bpy.data.scenes.get(SCENE_NAME)
    if scene is None:
        raise RuntimeError(f'Delivered scene missing: {SCENE_NAME}')
    initial = observe(scene)
    failures = state_errors('initial saved state', initial)
    phases = {}
    inspector = {}
    face = {}

    try:
        run_checked(AUDIT_SCRIPT, ['--scene', SCENE_NAME, '--json', str(AUDIT_JSON)])
        inspector = json.loads(AUDIT_JSON.read_text(encoding='utf-8'))
        phases['blendInspector'] = {'completed': True}
    except (Exception, SystemExit) as exc:
        phases['blendInspector'] = {'completed': False, 'error': repr(exc), 'traceback': traceback.format_exc()}
        failures.append('Blend inspector did not complete: ' + repr(exc))
    hashes['afterBlendInspector'] = sha256(SOURCE)
    after_inspector = observe(scene)
    failures.extend(state_errors('after blend inspector', after_inspector))

    try:
        run_checked(FACE_SCRIPT, [str(FACE_JSON)])
        face = json.loads(FACE_JSON.read_text(encoding='utf-8'))
        phases['facialValidator'] = {'completed': True}
    except (Exception, SystemExit) as exc:
        phases['facialValidator'] = {'completed': False, 'error': repr(exc), 'traceback': traceback.format_exc()}
        failures.append('Facial validator did not complete: ' + repr(exc))
    hashes['afterFacialValidator'] = sha256(SOURCE)
    final = observe(scene)
    failures.extend(state_errors('final runtime state', final))
    unchanged = len(set(hashes.values())) == 1
    if not unchanged:
        failures.append('Final .blend SHA256 changed during the audit.')
    facial_errors = face.get('errors')
    if facial_errors != []:
        failures.append('Facial validation errors are not empty: ' + repr(facial_errors))
    if face.get('passed_numeric_checks') is not True:
        failures.append('Facial validator did not report passed_numeric_checks=True.')

    # Replace the validator's formerly hardcoded source_file_unchanged with a
    # measured result. Preserve its numerical contents and note actual source.
    face.update({
        'blend': str(SOURCE),
        'source_file_sha256_before': hashes['afterBlendInspector'],
        'source_file_sha256_after': hashes['afterFacialValidator'],
        'source_file_sha256_at_wrapper_start': hashes['beforeOpen'],
        'source_file_unchanged': unchanged,
        'source_file_unchanged_method': 'SHA256 of the actual delivered file before/after load, inspector and facial validator',
        'delivery_wrapper_phase': phases['facialValidator'],
        'observed_saved_view_state_initial': initial,
        'observed_runtime_view_state_final': final,
    })
    if not phases['facialValidator']['completed']:
        face['passed_numeric_checks'] = False
        face.setdefault('errors', []).append(phases['facialValidator']['error'])
    write_json(FACE_JSON, face)

    inspector['inspectorPhaseFileHashes'] = {
        'before': inspector.get('fileSha256Before'),
        'after': inspector.get('fileSha256After'),
    }
    inspector.update({
        'file': str(SOURCE),
        'fileSha256Before': hashes['beforeOpen'],
        'fileSha256After': hashes['afterFacialValidator'],
        'sourceFileUnchanged': unchanged,
        'observedSavedViewState': initial,
        'observedAfterInspectorViewState': after_inspector,
        'observedFinalViewState': final,
        'deliveryAudit': {
            'wrapper': str(Path(__file__).resolve()),
            'background': bpy.app.background,
            'sourceHashesByPhase': hashes,
            'phases': phases,
            'facialErrorCount': len(facial_errors) if isinstance(facial_errors, list) else None,
            'facialPassedNumericChecks': face.get('passed_numeric_checks') is True,
            'checksPassed': not failures,
            'errors': failures,
            'modelSaveRequested': False,
            'scope': 'File immutability, saved scene/frame/camera/resolution and existing numeric validators; does not certify production readiness or artistic identity.',
        },
    })
    write_json(AUDIT_JSON, inspector)
    summary = {
        'source': str(SOURCE), 'sourceSHA256Before': hashes['beforeOpen'],
        'sourceSHA256After': hashes['afterFacialValidator'],
        'sourceFileUnchanged': unchanged,
        'initialFrame': initial['frame'], 'finalFrame': final['frame'],
        'initialCamera': initial['camera'], 'finalCamera': final['camera'],
        'initialResolution': initial['effectiveResolution'],
        'finalResolution': final['effectiveResolution'],
        'facialErrors': facial_errors, 'passed': not failures, 'errors': failures,
    }
    print('SALVE_FINAL_DELIVERY_AUDIT ' + json.dumps(summary, ensure_ascii=False), flush=True)
    if failures:
        raise RuntimeError('Final delivery audit failed: ' + '; '.join(failures))


if __name__ == '__main__':
    main()
