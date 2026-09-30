"""Run after the 41-image batch and optional THINK helper, in one loaded Blender.

Dependencies beside this file or in ROOT/work:
face_mbp_seal_v02.py, validate_face_readonly.py.
Requires existing outputs/expresiones_controles.json (21 rows) and CAM_FACE_*.
"""
import bpy
import hashlib
import json
import pathlib
import sys

ROOT = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
OUT = ROOT / 'outputs'

def source_file(name):
    candidates = []
    if '__file__' in globals():
        candidates.append(pathlib.Path(__file__).resolve().parent / name)
    candidates.append(ROOT / 'work' / name)
    for file in candidates:
        if file.is_file():
            return file
    raise FileNotFoundError(name)

def execute(name):
    file = source_file(name)
    namespace = {'__name__': 'salve_local_review', '__file__': str(file)}
    exec(compile(file.read_text(encoding='utf-8-sig'), str(file), 'exec'), namespace)
    return namespace

def main():
    scene = bpy.data.scenes['04_SALVE_MODELO_3D']
    if bpy.context.window:
        bpy.context.window.scene = scene
    rig = bpy.data.objects['Salve_Rig']
    manifest_path = OUT / 'expresiones_controles.json'
    rows = json.loads(manifest_path.read_text(encoding='utf-8'))
    assert len(rows) == 21, 'First batch must finish before local expression corrections'
    by_name = {row['id']: row for row in rows}
    assert len(by_name) == 21
    scene.frame_set(scene.frame_current)
    rig.update_tag()
    bpy.context.view_layer.update()
    bone_action_before = {
        bone.name: tuple(tuple(v) for v in bone.matrix_basis)
        for bone in rig.pose.bones
    }
    execute('face_mbp_seal_v02.py')
    # The corrective restores the current frame and changes facial custom-property
    # keys only. These evaluated bone transforms must remain exactly untouched.
    for bone in rig.pose.bones:
        assert bone_action_before[bone.name] == tuple(tuple(v) for v in bone.matrix_basis), bone.name + ' pose changed by face helper'
    saved_argv = sys.argv[:]
    try:
        sys.argv = ['validate_face_readonly.py', '--', str(OUT / 'Salve_validacion_facial_v02.json')]
        validation = execute('validate_face_readonly.py')['report']
    finally:
        sys.argv = saved_argv
    assert validation['passed_numeric_checks'], validation['errors']
    presets = json.loads(rig['expresiones_json'])
    affected = json.loads(rig['face_polish_repeat_expressions_json'])
    assert len(affected) == 13, affected
    scene.render.resolution_percentage = 100
    scene.render.resolution_x = 768
    scene.render.resolution_y = 768
    scene.render.image_settings.file_format = 'PNG'
    scene.render.image_settings.color_mode = 'RGBA'
    scene.render.film_transparent = True
    for name in affected:
        row = by_name[name]
        camera = bpy.data.objects['CAM_FACE_' + name]
        assert camera.name == row['camera'], name + ' camera changed'
        scene.frame_set(row['frame'])
        rig.update_tag()
        bpy.context.view_layer.update()
        scene.camera = camera
        file = OUT / row['file']
        scene.render.filepath = str(file)
        bpy.ops.render.render(write_still=True)
        row['values'] = presets[name]
        row['sha256'] = hashlib.sha256(file.read_bytes()).hexdigest()
        row['size'] = [768, 768]
        row['revision'] = 'local_mbp_angry_lashes_tooth_width_contact_polish'
        row['source_model'] = 'Salve_refinado_v02.blend'
        manifest_path.write_text(json.dumps(rows, indent=2, ensure_ascii=False), encoding='utf-8')
        print('EXPRESSION_LOCAL_REVIEW_DONE', name, flush=True)
    scene.frame_set(1)
    scene.camera = bpy.data.objects['CAM_front']
    scene.render.resolution_x = 1024
    scene.render.resolution_y = 1536
    bpy.ops.wm.save_as_mainfile(filepath=str(OUT / 'Salve_refinado_v02.blend'))
    print('LOCAL_EXPRESSION_REVIEW_COMPLETE', affected, flush=True)

if __name__ == '__main__':
    main()
