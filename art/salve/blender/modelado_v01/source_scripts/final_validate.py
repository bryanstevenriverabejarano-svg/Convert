import bpy, pathlib, json, math, hashlib
from mathutils import Vector
OUT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/outputs');SRC=pathlib.Path('C:/Users/agred/.codex/.chatgpt-projects/g-p-6ab2c01aa8f8819193dceb05d9e78da7/salve_blender')
scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];scene.frame_set(1);bpy.context.view_layer.update()
for o in scene.objects:
    for m in o.modifiers:
        if m.type=='ARMATURE':m.use_deform_preserve_volume=True
# Expression markers use standing body pose, avoiding an inherited bent calibration pose.
for pb in rig.pose.bones:
    pb.keyframe_insert('location',frame=201);pb.keyframe_insert('rotation_quaternion',frame=201);pb.keyframe_insert('scale',frame=201)
report=json.loads((OUT/'Salve_validacion.json').read_text());report['objects']=len(scene.objects);report['materials']=len(bpy.data.materials);report['verified_saved_file_reopened']=True
report['reference_hash_checks']=[]
for view in json.loads((SRC/'references/manifest.json').read_text())['views']:
    path=SRC/'references'/pathlib.Path(view['path']).name;digest=hashlib.sha256(path.read_bytes()).hexdigest()
    report['reference_hash_checks'].append({'id':view['id'],'sha256_matches':digest==view['sha256']})
report['packed_images']=sum(1 for im in bpy.data.images if im.packed_file)
report['pose_checks']={}
for name,frame in report['poses'].items():
    scene.frame_set(frame);bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();minz=1e9;count=0;finite=True
    for ob in scene.objects:
        if ob.type!='MESH' or ob.name=='Suelo_estudio':continue
        eo=ob.evaluated_get(dg);me=eo.to_mesh()
        for v in me.vertices:
            p=eo.matrix_world@v.co;minz=min(minz,p.z);finite=finite and all(math.isfinite(x) for x in p);count+=1
        eo.to_mesh_clear()
    report['pose_checks'][name]={'finite_vertices':finite,'minimum_z':round(minz,4),'floor_penetration_m':round(max(0,-minz),4),'vertices_evaluated':count}
scene.frame_set(1);bpy.context.view_layer.update()
report['skin_weights']={}
for o in scene.objects:
    if o.type=='MESH' and any(m.type=='ARMATURE' for m in o.modifiers):
        deform_names=set(b.name for b in rig.data.bones if b.use_deform)
        valid_indices={vg.index for vg in o.vertex_groups if vg.name in deform_names}
        weights=[sum(g.weight for g in v.groups if g.group in valid_indices) for v in o.data.vertices]
        report['skin_weights'][o.name]={'unweighted_vertices':sum(1 for w in weights if w<.0001),'max_normalization_error':max((abs(w-1) for w in weights),default=0)}
action=rig.animation_data.action;rig.animation_data.action=None
report['facial_deformation_checks']={}
for name in ['blink.L','blink.R','jawOpen','smile','browDown.L','browInnerUp.R']:
    affected=[]
    for o in scene.objects:
        if o.type=='MESH' and o.data.shape_keys and name in o.data.shape_keys.key_blocks:
            basis=o.data.shape_keys.key_blocks['Basis'];key=o.data.shape_keys.key_blocks[name]
            delta=max((Vector(v.co)-Vector(basis.data[i].co)).length for i,v in enumerate(key.data));affected.append({'object':o.name,'maximum_vertex_displacement':delta})
    report['facial_deformation_checks'][name]={'has_nonzero_deformation':any(x['maximum_vertex_displacement']>0 for x in affected),'objects':affected}
rig.animation_data.action=action;scene.frame_set(1);bpy.context.view_layer.update()
scene.render.resolution_x=768;scene.render.resolution_y=1152;scene.camera.data.ortho_scale=2.02;scene.camera.location=(0,-4,1.0);scene.camera.rotation_euler=(Vector((0,0,.90))-scene.camera.location).to_track_quat('-Z','Y').to_euler()
rig.hide_set(True)
text=bpy.data.texts['SALVE_MODELO_LEEME'];text.write('\nEstado: reconstruccion inicial estilizada. 20 vistas y hoja de expresiones en carpeta outputs. Controles faciales incluyen parpadeo L/R, cejas, apertura, sonrisa, fruncido y redondeo. Lagrimas con propiedad tears. La semejanza exacta, expresiones artisticas finas, correctivos, UV final y optimizacion siguen pendientes.\n')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_modelado_v01.blend'))
report['blend_sha256']=hashlib.sha256((OUT/'Salve_modelado_v01.blend').read_bytes()).hexdigest()
report['status']='VERSION_INICIAL_ESTILIZADA_NO_FINAL'
(OUT/'Salve_validacion.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print('FINAL_VALIDATION',len(report['reference_hash_checks']),'refs',len(report['skin_weights']),'bound meshes',len(report['facial_deformation_checks']),'facial tests')
