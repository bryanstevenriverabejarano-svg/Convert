"""Run sequentially after the complete render batch, with no other Blender open."""
from pathlib import Path
import bpy, json, sys

ROOT = Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
OUT = ROOT/'outputs'
scene = bpy.data.scenes['04_SALVE_MODELO_3D']
bpy.context.window.scene = scene
assert len(json.loads((OUT/'vistas_manifest.json').read_text(encoding='utf-8'))) == 20
assert len(json.loads((OUT/'expresiones_controles.json').read_text(encoding='utf-8'))) == 21

def execute(name):
    file = ROOT/'work'/name
    namespace = {'__name__':'salve_static_final_review','__file__':str(file)}
    exec(compile(file.read_text(encoding='utf-8-sig'),str(file),'exec'),namespace)
    return namespace

if '--skip-contact' in sys.argv:
    assert all(bpy.data.objects['Dedo_'+name+'_quad.R'].data.shape_keys.key_blocks.get('THINKSkin_311') for name in ('thumb','index','middle','ring','pinky')),'Contact checkpoint required'
    print('EXISTING_THINK_CONTACT_CHECKPOINT_REUSED',flush=True)
else:
    think = execute('think_contact_v02.py')['repair_salve_think_contact'](skin_fit=True)
    scene.frame_set(1)
    bpy.context.view_layer.update()
    bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'work/Salve_think_contact_checkpoint.blend'),copy=True)
    print('THINK_CONTACT_CHECKPOINT_SAVED',flush=True)
execute('rerender_face_local_v02.py')['main']()
scene.frame_set(1)
scene.camera = bpy.data.objects['CAM_front']
readme = bpy.data.texts['SALVE_MODELO_LEEME']
text = readme.as_string().replace('biblioteca FK con IK ajustada','biblioteca FK con IK opcional; calibracion de polos pendiente')
readme.clear()
readme.write(text+'\nRevision final: contacto indice/menton medido sobre superficies deformadas, cierre MBP, alas de pestanas y ancho de dientes corregidos. Ver informes y PNG. La identidad ilustrativa y la preparacion de produccion siguen pendientes.\n')
for area in bpy.context.screen.areas:
    if area.type == 'VIEW_3D':
        area.spaces.active.region_3d.view_perspective = 'CAMERA'
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_refinado_v02.blend'))
saved_argv=sys.argv[:]
try:
    sys.argv=['validate_face_readonly.py','--',str(OUT/'Salve_validacion_facial_v02.json')]
    validation=execute('validate_face_readonly.py')['report']
    assert validation['passed_numeric_checks'],validation['errors']
finally:
    sys.argv=saved_argv
print('FINAL_STATIC_LOCAL_REVIEW_SAVED',flush=True)
