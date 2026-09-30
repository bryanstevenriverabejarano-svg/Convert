"""Bounded frame311 FK hand rotation/front-chin fit; save checkpoint only on pass."""
import bpy,pathlib,json,hashlib,struct
from mathutils import Vector,Matrix,Quaternion
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
bpy.context.window.scene=bpy.data.scenes['04_SALVE_MODELO_3D'];scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig']
scene.frame_set(311)
ns={'SALVE_POSES_AUTORUN':False};p=ROOT/'work/poses_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'),ns)
tn={};p=ROOT/'work/think_contact_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'),tn)

def geometry_hash():
    h=hashlib.sha256()
    for ob in sorted((o for o in scene.objects if o.type=='MESH'),key=lambda o:o.name):
        h.update(ob.name.encode());base=ob.data.shape_keys.key_blocks[0].data if ob.data.shape_keys else ob.data.vertices
        for v in base:h.update(struct.pack('3f',*v.co))
        for layer in ob.data.uv_layers:
            h.update(layer.name.encode())
            for uv in layer.data:h.update(struct.pack('2f',*uv.uv))
        for v in ob.data.vertices:
            for g in v.groups:h.update(struct.pack('If',g.group,g.weight))
    return h.hexdigest()

def other_pose_hash():
    result={}
    for name,frame in ns['SALVE_POSE_FRAMES'].items():
        if frame==311:continue
        scene.frame_set(frame);ns['_update']();h=hashlib.sha256()
        for pb in rig.pose.bones:
            for row in pb.matrix:h.update(struct.pack('4f',*row))
        result[name]=h.hexdigest()
    scene.frame_set(311);ns['_update']();return result

hands=[]
for side in ('L','R'):
    for name in ['Guante_palmar_quad.'+side,'Guante_dorso.'+side,'Led_guante.'+side]+['Dedo_'+f+'_quad.'+side for f in ('thumb','index','middle','ring','pinky')]+['Una_'+f+'.'+side for f in ('thumb','index','middle','ring','pinky')]:
        if name in bpy.data.objects:hands.append(bpy.data.objects[name])
headtree=tn['_tc_tree'](bpy.data.objects['Rostro_topologia']);collartree=tn['_tc_tree'](bpy.data.objects['Cuello_y_collar_negro'])

def crossings():
    result={}
    for ob in hands:
        tree=tn['_tc_tree'](ob);head=len(headtree.overlap(tree));neck=len(collartree.overlap(tree))
        if head or neck:result[ob.name]={'head_crossing_pairs':head,'collar_crossing_pairs':neck}
    return result

before_geom=geometry_hash();before_other=other_pose_hash()
baseline={name:rig.pose.bones[name].matrix.copy() for name in ('upper_arm.R','forearm.R','hand.R','upper_arm.L','forearm.L','hand.L')}
report={'frame':311,'source':'work/Salve_think_contact_checkpoint.blend','method':'Measured BVH hand/collar/head; bounded wrist rotation about index pad and front-chin target; only311 FK keys, no Basis/UV/weight edits','before_crossings':crossings(),'before_surface':tn['_tc_contact']()[0],'trials':[]}
pivot=rig.matrix_world.inverted()@Vector(report['before_surface']['closest_index_world']);axis=(rig.matrix_world.inverted().to_3x3()@Vector((1,0,0))).normalized()
action=rig.animation_data.action;rig.animation_data.action=None;pole=baseline['forearm.R'].translation.copy();inverse=rig.matrix_world.inverted().to_3x3();winner=None
for z in (1.535,1.540,1.545):
    tn['_TC_CONTACT_REST']=(-.006,-.046,z)
    for pitch in (-.30,-.45,-.60):
        for name in ('upper_arm.R','forearm.R','hand.R'):rig.pose.bones[name].matrix=baseline[name];ns['_update']()
        target=Matrix.Translation(pivot)@Quaternion(axis,pitch).to_matrix().to_4x4()@Matrix.Translation(-pivot)@baseline['hand.R']
        ns['_chain']('upper_arm.R','forearm.R',target.translation,pole);ns['_restore_rotation']('hand.R',target)
        for step in range(6):
            c,ip,skin,normal=tn['_tc_contact']()
            delta=skin+normal*.001-ip if step==0 else Vector(c['closest_skin_world'])+Vector(c['closest_skin_normal'])*.001-Vector(c['closest_index_world'])
            if step>0 and delta.length<.0001 and c['finger_head_surface_crossing_pairs']==0:break
            assert delta.length<.045,('unsafe hand translation',delta.length)
            ns['_chain']('upper_arm.R','forearm.R',rig.pose.bones['hand.R'].head+inverse@delta,pole);ns['_restore_rotation']('hand.R',target)
        cross=crossings();contact=tn['_tc_contact']()[0]
        trial={'front_chin_rest':list(tn['_TC_CONTACT_REST']),'world_x_pitch_rad':pitch,'crossings':cross,'surface_contact':contact}
        report['trials'].append(trial);print('THINK_FRONT_COLLAR_TRIAL',z,pitch,json.dumps(cross),contact['distal_minimum_skin_distance_m'],flush=True)
        if not cross and .0007<=contact['distal_minimum_skin_distance_m']<=.0013:
            winner={'target':list(tn['_TC_CONTACT_REST']),'pitch':pitch};break
    if winner:break
if winner is None:
    report['status']='bounded_search_failed_no_model_saved';(ROOT/'outputs/poses/Salve_correccion_collar_pensativa_v02.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
    raise RuntimeError('No collision-free front-chin pose in bounded hand-pitch search')

# Preserve the supporting left hand relative to the moved right elbow.
elbowdelta=rig.pose.bones['forearm.R'].head-baseline['forearm.R'].translation
ns['_chain']('upper_arm.L','forearm.L',rig.pose.bones['hand.L'].head+elbowdelta,rig.pose.bones['forearm.L'].head.copy());ns['_restore_rotation']('hand.L',baseline['hand.L'])
rig.animation_data.action=action
for name in baseline:
    pb=rig.pose.bones[name]
    for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(prop,frame=311)
ns['_constant'](action)
fit=tn['repair_salve_think_contact'](skin_fit=False)
report['initial_hair_body_surface_check']=fit['hair_body_surface_check']
report['targeted_hair_311_fixes']={}
if fit['hair_body_surface_check']['objects_with_surface_crossings']:
    hc={'SALVE_HAIR_CLEARANCE_AUTORUN':False};p=ROOT/'work/hair_clearance_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'),hc)
    tree=hc['_hc_body_tree']()
    for name in fit['hair_body_surface_check']['objects']:
        assert name.startswith('Mechon_rostro_'),('unexpected newly crossed long hair',name)
        stats=hc['_hc_correct_leaf'](bpy.data.objects[name],tree,311,clearance=.006,iterations=10)
        stats['detail_lines_followed']=hc['_hc_follow_lines'](bpy.data.objects[name],stats,311)
        stats.pop('row_offsets',None);report['targeted_hair_311_fixes'][name]=stats
    fit['hair_body_surface_check']=ns['check_salve_hair_surface_intersections']()
    fit['all_minimum_z_m']=min(v.z for v in ns['_character_vertices'](True))
after=crossings();report['chosen']=winner;report['after_crossings']=after;report['after_contact']=fit['after_contact'];report['hair_body_surface_check']=fit['hair_body_surface_check'];report['ik_snap_checks']=fit['ik_snap_checks'];report['floor_minimum_m']=fit['all_minimum_z_m']
(ROOT/'outputs/poses/Salve_correccion_collar_pensativa_v02.json').write_text(json.dumps({**report,'status':'final_checks_pending_checkpoint_not_saved'},indent=2,ensure_ascii=False),encoding='utf8')
assert not after,('Hand still crosses collar/head',after)
assert fit['hair_body_surface_check']['objects_with_surface_crossings']==0,'Hair crosses suit after hand correction'
assert fit['all_minimum_z_m']>=0,'Floor penetration'
assert .0007<=fit['after_contact']['distal_minimum_skin_distance_m']<=.0013,'Final index gap'
report['basis_uv_weights_hash_before']=before_geom;report['basis_uv_weights_hash_after']=geometry_hash();assert report['basis_uv_weights_hash_after']==before_geom,'Basis/UV/weights modified'
report['other_fk_pose_hashes_preserved']=other_pose_hash()==before_other;assert report['other_fk_pose_hashes_preserved'],'Other FK poses changed'
contactpath=ROOT/'outputs/Salve_poses_contactos_v02.json';contact=json.loads(contactpath.read_text(encoding='utf8'))
contact['poses']['THINK']['hair_body_surface_check']=fit['hair_body_surface_check'];contact['poses']['THINK']['minimum_all_z_m']=fit['all_minimum_z_m'];contact['think_collar_surface_repair']={'target':winner['target'],'pitch':winner['pitch'],'head_collar_crossings':after,'source_stage':'work/Salve_think_contact_checkpoint.blend; visual rerender and final model save pending'}
contactpath.write_text(json.dumps(contact,indent=2,ensure_ascii=False),encoding='utf8')
surfacepath=ROOT/'outputs/poses/Salve_contacto_pensativa_superficie_v02.json';surface=json.loads(surfacepath.read_text(encoding='utf8'));surface['hair_body_surface_check']=fit['hair_body_surface_check'];surface['right_hand_collar_surface_crossings']=after;surface['status']='head_collar_floor_surface_checks_passed_checkpoint_visual_review_pending';surfacepath.write_text(json.dumps(surface,indent=2,ensure_ascii=False),encoding='utf8')
hairpath=ROOT/'outputs/Salve_cabello_correctivos_v02.json';hair=json.loads(hairpath.read_text(encoding='utf8'));hair['poses']['THINK']['targeted_collar_refinement_311']=report['targeted_hair_311_fixes'];hair['poses']['THINK']['after']=hc['_hc_residuals'](tree) if report['targeted_hair_311_fixes'] else hair['poses']['THINK']['after'];hairpath.write_text(json.dumps(hair,indent=2,ensure_ascii=False),encoding='utf8')
report['status']='surface_checks_passed_checkpoint_saved_visual_PNG_review_pending'
scene.frame_set(1);ns['_update']()
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'work/Salve_think_contact_checkpoint.blend'))
(ROOT/'outputs/poses/Salve_correccion_collar_pensativa_v02.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
print('THINK_COLLAR_CLEAR_CHECKPOINT_SAVED',flush=True)
