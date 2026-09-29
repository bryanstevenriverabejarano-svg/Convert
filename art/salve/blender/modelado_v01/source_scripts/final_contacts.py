import bpy,json,pathlib
OUT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/outputs');scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];r=json.loads((OUT/'Salve_validacion.json').read_text())
for name,frame in r['poses'].items():
    scene.frame_set(frame);bpy.context.view_layer.update();root=rig.pose.bones['root'];root.location.y+=.002-r['pose_checks'][name]['minimum_z'];root.keyframe_insert('location',frame=frame)
for side in ['L','R']:
    o=bpy.data.objects['Pestana.'+side];k=o.data.shape_keys.key_blocks['blink.'+side]
    for v in k.data:v.co.y=-.075
    o.data.update()
scene.frame_set(1);bpy.context.view_layer.update()
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_modelado_v01.blend'))
print('CONTACTS_AND_CLOSED_LASHES_SAVED')
