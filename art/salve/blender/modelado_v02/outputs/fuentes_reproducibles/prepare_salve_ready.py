import bpy, pathlib, json
WORK=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2/work')
OUT=WORK.parent/'outputs'
bpy.context.window.scene=bpy.data.scenes['04_SALVE_MODELO_3D']
print('READY_START',flush=True)
fix=WORK/'body_correctives_fix.py'
if fix.exists():exec(compile(fix.read_text(encoding='utf8'),str(fix),'exec'))
SALVE_HAIR_CLEARANCE_AUTORUN=False
path=WORK/'hair_clearance_v02.py';exec(compile(path.read_text(encoding='utf8'),str(path),'exec'))
prepare_salve_long_hair_basis()
SALVE_POSES_AUTORUN=False
path=WORK/'poses_v02.py';exec(compile(path.read_text(encoding='utf8'),str(path),'exec'))
pose_ik=bake_salve_poses()
print('READY_POSES_BAKED',flush=True)
bpy.ops.wm.save_as_mainfile(filepath=str(WORK/'Salve_poses_ready.blend'))
hair_result=bake_salve_hair_clearance()
print('READY_HAIR_BAKED',flush=True)
pose_result=validate_salve_poses(pose_ik)
OUT.mkdir(exist_ok=True,parents=True)
(OUT/'Salve_poses_contactos_v02.json').write_text(json.dumps(pose_result,indent=2,ensure_ascii=False),encoding='utf8')
scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];scene.frame_set(1);bpy.context.view_layer.update()
rig.hide_set(True)
readme=bpy.data.texts.get('SALVE_MODELO_LEEME') or bpy.data.texts.new('SALVE_MODELO_LEEME')
readme.write('\nV02: poses FK estatica con apoyos evaluados; IK opcional y orientacion de manos/pies calibradas por pose. Frames 1 stand,11 frontal3/4,21 frontalopuesta,31 A,41 variante,61 A cabello abierto,91 sentada,121 cuclillas,151 rodillas,161 variante rodillas,171 inclinada,181 varianteinclinada,311 pensativa con indice en barbilla. PoseClearance_* son correctivos editables de cabello estaticos por frame; expresiones y desplazamientos siguen mezclables. Consultar informes numericos de contactos/BVH; esto no certifica ausencia de penetraciones, equivalencia exacta, continuidad de animacion ni retopologia completa de produccion.\n')
bpy.ops.wm.save_as_mainfile(filepath=str(WORK/'Salve_model_ready.blend'))
print('READY_SAVED',str(WORK/'Salve_model_ready.blend'),flush=True)
