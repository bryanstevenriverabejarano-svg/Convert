import bpy,pathlib,sys
from mathutils import Vector
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
# Facial corrections preserve the atlas, hair correction library and body pose action.
p=ROOT/'work/face_v02_shape_fix.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'))
p=ROOT/'work/fringe_local_polish_v02.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'))
p=ROOT/'work/reload_atlas_repair_v02.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'))
p=ROOT/'work/pose_neutral_frame401_fix.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'))
p=ROOT/'work/hair_targeted_fix_v02.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'))
p=ROOT/'work/ik_diagnostics_v02.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'))
scene=bpy.data.scenes['04_SALVE_MODELO_3D'];bpy.context.window.scene=scene
for name,energy,size,color in [('Principal',350,2.0,(.90,.93,1)),('Relleno',105,2.8,(1,.89,.84)),('Contraluz',230,1.7,(.59,.70,1))]:
 ob=bpy.data.objects.get(name)
 if ob:ob.data.energy=energy;ob.data.size=size;ob.data.color=color
rig=bpy.data.objects['Salve_Rig'];rig.hide_set(False)
for name in ['Controles IK','Deformacion cuerpo','Cabello','Rostro']:
 if not rig.data.collections.get(name):rig.data.collections.new(name)
for bone in rig.data.bones:
 name='Controles IK' if bone.name.startswith('CTRL_') else 'Cabello' if bone.name.startswith('hair_') else 'Rostro' if bone.name.startswith(('head','neck','eye','jaw')) else 'Deformacion cuerpo'
 rig.data.collections[name].assign(bone)
for col in rig.data.collections:col.is_visible=col.name=='Controles IK'
if 'STICK' in [e.identifier for e in rig.data.bl_rna.properties['display_type'].enum_items]:rig.data.display_type='STICK'
readme=bpy.data.texts.get('SALVE_MODELO_LEEME') or bpy.data.texts.new('SALVE_MODELO_LEEME');readme.clear()
readme.write('SALVE V02 - AVANCE EN REVISION\nEscena04: modelo editable, superficies quad regionales, UVAtlas y3 mapas empaquetados, ojos/rostro con controles, biblioteca FK con IK ajustada y correctivos.\nLos originales y escenas fuente se conservan. Coleccion90 oculta: cuerpo voxel fuente.\nNo declarar identidad exacta ni produccion final. Faltan equivalencia artistica, uniones de retopologia, pintura y aprobacion de deformacion. Ver comparaciones e informe.\nPoses en marcadores1-181. Expresiones201-401, THINK311. Propiedades de Salve_Rig animadas; editar/retirar claves para control manual.\nBonecollections: ControlesIK visibles, Deformacion/Cabello/Rostro ocultos y activables.\nCorrectivos capilares porframe son soluciones estaticas, no simulacion ni animacion dinamica.\n')
scene.frame_set(1);scene.render.film_transparent=True;scene.render.image_settings.color_mode='RGBA';scene.render.resolution_x=1024;scene.render.resolution_y=1536
scene['estado']='AVANCE_V02_NO_FINAL | identidad y produccion finales pendientes'
for im in bpy.data.images:
 if im.source=='FILE' and im.has_data:
  try:im.pack()
  except RuntimeError:pass
for area in bpy.context.screen.areas:
 if area.type=='VIEW_3D':
  area.spaces.active.overlay.show_overlays=False
  area.spaces.active.region_3d.view_location=(0,0,.9);area.spaces.active.region_3d.view_distance=2.8
  area.spaces.active.shading.type='MATERIAL'
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'outputs/Salve_refinado_v02.blend'))
print('POLISHED_V02_SAVED',flush=True)
# Validate all twenty-one facial presets in the same loaded process.
saved_argv=sys.argv[:];sys.argv=['validate_face_readonly.py','--',str(ROOT/'outputs/Salve_validacion_facial_v02.json')]
p=ROOT/'work/validate_face_readonly.py';exec(compile(p.read_text(encoding='utf-8'),str(p),'exec'));sys.argv=saved_argv
assert not report['errors'],report['errors']
print('FACIAL_CHECKS_PASS',flush=True)
