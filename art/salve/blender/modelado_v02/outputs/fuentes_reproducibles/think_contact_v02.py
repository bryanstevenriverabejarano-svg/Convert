"""Selective static THINK311 skin/contact diagnosis and repair.
No auto-execution, no save, no edits to Basis, UV, weights or other frames.
Run repair_salve_think_contact() in the existing final assembly process.
"""
import bpy, pathlib, json, math
from mathutils import Vector, Matrix
from mathutils.bvhtree import BVHTree
from bpy_extras.object_utils import world_to_camera_view

_TC_ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
_TC_CONTACT_REST=(-.006,-.035,1.525)

def _tc_eval(obj):
    dg=bpy.context.evaluated_depsgraph_get();eo=obj.evaluated_get(dg)
    me=eo.to_mesh(preserve_all_data_layers=True,depsgraph=dg)
    ps=[eo.matrix_world@v.co for v in me.vertices]
    faces=[tuple(p.vertices) for p in me.polygons]
    eo.to_mesh_clear();return ps,faces

def _tc_tree(obj):
    ps,faces=_tc_eval(obj)
    return BVHTree.FromPolygons(ps,faces,epsilon=.000001)

def _tc_camera_rays():
    """Identify actual visible meshes at observed white fragments; no guesses."""
    s=bpy.context.scene;cam=bpy.data.objects.get('CAM_FACE_THINK')
    if not cam:return {'status':'camera not present'}
    matrices=cam.matrix_world
    resolution=(s.render.resolution_x,s.render.resolution_y)
    try:
        s.render.resolution_x=768;s.render.resolution_y=768
        frame=list(cam.data.view_frame(scene=s))
    finally:s.render.resolution_x,s.render.resolution_y=resolution
    x0=min(v.x for v in frame);x1=max(v.x for v in frame)
    y0=min(v.y for v in frame);y1=max(v.y for v in frame)
    objects=[]
    for ob in s.objects:
        if ob.type!='MESH' or ob.hide_render:continue
        if not any(c.name.startswith(('01_Cuerpo','02_Rostro','03_Cabello','04_Tecnologia')) and not c.hide_render for c in ob.users_collection):continue
        ps,faces=_tc_eval(ob)
        if faces and max((p-ps[0]).length for p in ps)>1e-7:objects.append((ob,BVHTree.FromPolygons(ps,faces)))
    result={}
    # Coordinates refer to the original 768x768 THINK screenshot.
    for px,py in ((366,646),(366,650),(352,746),(355,737),(330,716),(375,673),(273,629),(282,657),(290,688)):
        local=Vector((x0+(x1-x0)*px/768,y0+(y1-y0)*(1-py/768),0))
        if cam.data.type=='ORTHO':origin=matrices@local;direction=matrices.to_quaternion()@Vector((0,0,-1))
        else:
            origin=matrices.translation;local.z=frame[0].z;direction=(matrices.to_quaternion()@local).normalized()
        hits=[]
        for ob,tree in objects:
            p,n,i,d=tree.ray_cast(origin,direction,10)
            if p is not None:hits.append((d,ob,p,i))
        hits.sort(key=lambda v:v[0]);key='%d,%d'%(px,py)
        result[key]=[{'object':ob.name,'distance_camera_m':d,'world':list(p),'polygon':i,'materials':[m.name if m else None for m in ob.data.materials]} for d,ob,p,i in hits[:3]]
    return result

def _tc_contact():
    rig=bpy.data.objects['Salve_Rig'];head=bpy.data.objects['Rostro_topologia'];finger=bpy.data.objects['Dedo_index_quad.R']
    tree=_tc_tree(head);ps,faces=_tc_eval(finger)
    headbone=rig.pose.bones['head'];rest=rig.data.bones['head']
    nominal=rig.matrix_world@(headbone.matrix@(rest.matrix_local.inverted()@Vector(_TC_CONTACT_REST)))
    skin,normal,idx,dist=tree.find_nearest(nominal)
    tail=rig.matrix_world@rig.pose.bones['index.03.R'].tail
    # Distal cap/pad samples; nearest lower-chin point determines the target.
    distal=[(i,p) for i,p in enumerate(ps) if (p-tail).length<.033]
    if not distal:raise RuntimeError('No distal index surface samples near tip')
    vi,p=min(distal,key=lambda v:(v[1]-skin).length)
    q,n,fi,d=tree.find_nearest(p)
    gaps=[]
    for i,v in distal:
        hp,hn,hi,hd=tree.find_nearest(v)
        if hp is not None:gaps.append((hd,i,v,hp,hn))
    closest=min(gaps,key=lambda v:v[0]);overlap=tree.overlap(BVHTree.FromPolygons(ps,faces,epsilon=.000001))
    return {'nominal_chin_world':list(nominal),'target_skin_world':list(skin),'target_skin_normal':list(normal),'selected_index_vertex':vi,'selected_index_world':list(p),'selected_to_target_skin_m':(p-skin).length,'distal_minimum_skin_distance_m':closest[0],'closest_index_world':list(closest[2]),'closest_skin_world':list(closest[3]),'closest_skin_normal':list(closest[4]),'closest_signed_skin_distance_m':(closest[2]-closest[3]).dot(closest[4]),'selected_signed_skin_distance_m':(p-q).dot(n),'finger_head_surface_crossing_pairs':len(overlap),'bone_tip_world':list(tail)},p,skin,normal

def _tc_catmull(points,t):
    j=min(len(points)-2,int(t));f=t-j
    a=points[max(0,j-1)];b=points[j];c=points[j+1];d=points[min(len(points)-1,j+2)]
    return .5*((2*b)+(-a+c)*f+(2*a-5*b+4*c-d)*f*f+(-a+3*b-3*c+d)*f*f*f)

def _tc_cage(obj):
    bpy.context.view_layer.update();ps,faces=_tc_eval(obj)
    assert len(ps)==len(obj.data.vertices),(obj.name,len(ps),len(obj.data.vertices))
    return ps

def _tc_static_skin_key_impl(obj,target):
    """Exact local affine inverse of DQ deformation using three tiny probes."""
    mods=[(m,m.show_viewport,m.show_render) for m in obj.modifiers if m.type in ('SUBSURF','SOLIDIFY')]
    for m,_,_ in mods:m.show_viewport=False;m.show_render=False
    if not obj.data.shape_keys:obj.shape_key_add(name='Basis',from_mix=False)
    basis=obj.data.shape_keys.key_blocks[0];key=obj.data.shape_keys.key_blocks.get('THINKSkin_311')
    if key is None:key=obj.shape_key_add(name='THINKSkin_311',from_mix=False)
    try:key.driver_remove('value')
    except TypeError:pass
    for i,v in enumerate(key.data):v.co=basis.data[i].co
    key.value=1;base=_tc_cage(obj);eps=.0001;columns=[]
    probe=obj.shape_key_add(name='__THINK_PROBE__',from_mix=False)
    for axis in range(3):
        off=Vector((0,0,0));off[axis]=eps
        for i,v in enumerate(probe.data):v.co=basis.data[i].co+off
        probe.value=1;obj.update_tag();pos=_tc_cage(obj)
        columns.append([(pos[i]-base[i])/eps for i in range(len(base))]);probe.value=0;obj.update_tag();_tc_cage(obj)
    obj.shape_key_remove(probe)
    maximum=max((target[i]-base[i]).length for i in range(len(base)))
    if maximum>.045:raise RuntimeError('THINK skin displacement exceeds 45mm: '+obj.name+' '+str(maximum))
    for i,v in enumerate(key.data):
        jac=Matrix((columns[0][i],columns[1][i],columns[2][i])).transposed()
        v.co=basis.data[i].co+jac.inverted_safe()@(target[i]-base[i])
    key.driver_add('value').driver.expression='1.0 if frame == 311 else 0.0'
    obj.update_tag();actual=_tc_cage(obj);error=max((actual[i]-target[i]).length for i in range(len(actual)))
    for m,v,r in mods:m.show_viewport=v;m.show_render=r
    bpy.context.view_layer.update()
    assert error<.0002,(obj.name,'skinfit reconstruction error',error)
    return {'object':obj.name,'key':'THINKSkin_311','maximum_world_displacement_m':maximum,'maximum_cage_reconstruction_error_m':error}

def _tc_static_skin_key(obj,target):
    states=[(m,m.show_viewport,m.show_render) for m in obj.modifiers]
    try:return _tc_static_skin_key_impl(obj,target)
    finally:
        if obj.data.shape_keys:
            probe=obj.data.shape_keys.key_blocks.get('__THINK_PROBE__')
            if probe:obj.shape_key_remove(probe)
        for m,viewport,render in states:m.show_viewport=viewport;m.show_render=render
        obj.update_tag();bpy.context.view_layer.update()

def _tc_finger_targets(fn):
    rig=bpy.data.objects['Salve_Rig'];obj=bpy.data.objects['Dedo_'+fn+'_quad.R']
    # Preserve row angles/radii from the authored basis, rather than change UVs/topology.
    assert len(obj.data.vertices)==25*12,(obj.name,'unexpected cage layout')
    bones=[rig.pose.bones[fn+'.%02d.R'%i] for i in (1,2,3)]
    points=[rig.matrix_world@bones[0].head]+[rig.matrix_world@b.tail for b in bones]
    restpts=[rig.data.bones[fn+'.01.R'].head_local]+[rig.data.bones[fn+'.%02d.R'%i].tail_local for i in (1,2,3)]
    basis=obj.data.shape_keys.key_blocks[0].data if obj.data.shape_keys else obj.data.vertices
    targets=[]
    for row in range(25):
        t=row/24*3;centre=_tc_catmull(points,t)
        ra=_tc_catmull(restpts,max(0,t-.005));rb=_tc_catmull(restpts,min(3,t+.005));restaxis=(rb-ra).normalized()
        ref=Vector((0,1,0)) if abs(restaxis.y)<=.95 else Vector((1,0,0))
        ru=restaxis.cross(ref).normalized();rv=restaxis.cross(ru).normalized();restcentre=_tc_catmull(restpts,t)
        pa=_tc_catmull(points,max(0,t-.005));pb=_tc_catmull(points,min(3,t+.005));axis=(pb-pa).normalized()
        idx=min(2,int(t));restq=rig.data.bones[fn+'.%02d.R'%(idx+1)].matrix_local.to_quaternion()
        currentq=(rig.matrix_world@bones[idx].matrix).to_quaternion()
        # Transport the source ring basis with the nearest phalanx, then make it perpendicular.
        u=currentq@(restq.inverted()@ru);u-=axis*u.dot(axis);u.normalize();v=axis.cross(u).normalized()
        for k in range(12):
            delta=obj.matrix_world@basis[row*12+k].co-restcentre
            targets.append(centre+u*delta.dot(ru)+v*delta.dot(rv))
    return obj,targets

def _tc_fix_nail(fn):
    obj=bpy.data.objects['Una_'+fn+'.R'];finger=bpy.data.objects['Dedo_'+fn+'_quad.R'];tree=_tc_tree(finger)
    mods=[(m,m.show_viewport,m.show_render) for m in obj.modifiers if m.type in ('SUBSURF','SOLIDIFY')]
    for m,_,_ in mods:m.show_viewport=False;m.show_render=False
    ps=_tc_cage(obj);targets=[]
    for p in ps:
        hit,n,idx,d=tree.find_nearest(p)
        targets.append(hit+n*.00065)
    for m,v,r in mods:m.show_viewport=v;m.show_render=r
    return _tc_static_skin_key(obj,targets)

def repair_salve_think_contact(skin_fit=True):
    bpy.context.window.scene=bpy.data.scenes['04_SALVE_MODELO_3D']
    s=bpy.context.scene;s.frame_set(311);rig=bpy.data.objects['Salve_Rig'];report={'frame':311,'method':'Evaluated chin/index pad surface, selective FK arm translation and frame311 skin keys; UV/rest weights unchanged','limits':['Triangle crossing and sampled surface separation are static checks; no deformation certification.','Skin keys affect frame311 only and require a new visual review.']}
    namespace={'SALVE_POSES_AUTORUN':False};p=_TC_ROOT/'work/poses_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'),namespace)
    out=_TC_ROOT/'outputs/poses/Salve_contacto_pensativa_superficie_v02.json'
    report['before_contact']=_tc_contact()[0];report['fragment_rays_before']=_tc_camera_rays();report['skin_keys']=[]
    report['status']='diagnosed_before_repair';out.write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
    # Vertex rings attached to two independently curled phalanges can bulge into isolated
    # cap silhouettes. Reconstruct a continuous static finger sweep in posed space.
    if skin_fit:
        for fn in ('thumb','index','middle','ring','pinky'):
            obj,target=_tc_finger_targets(fn);report['skin_keys'].append(_tc_static_skin_key(obj,target))
        for fn in ('thumb','index','middle','ring','pinky'):report['skin_keys'].append(_tc_fix_nail(fn))
    report['status']='skin_keys_created_contact_pending';out.write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
    hand_orientation=rig.pose.bones['hand.R'].matrix.copy();original_elbow=rig.pose.bones['forearm.R'].head.copy()
    pole=original_elbow.copy();report['contact_iterations']=[]
    inverse=rig.matrix_world.inverted().to_3x3()
    for step in range(6):
        current,indexpoint,skin,normal=_tc_contact();report['contact_iterations'].append(current)
        # First align with the reference chin region; then settle the true closest
        # distal pad, so a neighbouring curved sample cannot be left penetrating.
        if step==0:delta=skin+normal*.001-indexpoint
        else:delta=Vector(current['closest_skin_world'])+Vector(current['closest_skin_normal'])*.001-Vector(current['closest_index_world'])
        if delta.length<.0002 and current['finger_head_surface_crossing_pairs']==0:break
        if delta.length>.040:raise RuntimeError('THINK hand correction over40mm: '+str(delta.length))
        wrist=rig.pose.bones['hand.R'].head+inverse@delta
        namespace['_chain']('upper_arm.R','forearm.R',wrist,pole)
        namespace['_restore_rotation']('hand.R',hand_orientation)
    # Preserve left-hand support relative to the right elbow in the same frame.
    elbowdelta=rig.pose.bones['forearm.R'].head-original_elbow
    leftorient=rig.pose.bones['hand.L'].matrix.copy();leftpole=rig.pose.bones['forearm.L'].head.copy()
    namespace['_chain']('upper_arm.L','forearm.L',rig.pose.bones['hand.L'].head+elbowdelta,leftpole)
    namespace['_restore_rotation']('hand.L',leftorient)
    report['after_contact']=_tc_contact()[0];report['fragment_rays_after']=_tc_camera_rays()
    after=report['after_contact'];report['status']='surface_measured_checks_pending';out.write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
    assert after['finger_head_surface_crossing_pairs']==0,('THINK index crosses face',after)
    assert .0003<=after['distal_minimum_skin_distance_m']<=.0020,('THINK actual surface distance unresolved',after)
    for side in ('L','R'):
        for bone in ('upper_arm','forearm','hand'):
            pb=rig.pose.bones[bone+'.'+side]
            for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(prop,frame=311)
    action=rig.animation_data.action;rig.animation_data.action=None
    checks=namespace['_snap_ik'](True);rig.animation_data.action=action
    for pb in rig.pose.bones:
        if pb.name.startswith('CTRL_'):
            for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(prop,frame=311)
    for side in ('L','R'):
        for part,bone in (('foot','shin'),('hand','forearm')):
            constraint=next(c for c in rig.pose.bones[bone+'.'+side].constraints if c.type=='IK')
            constraint.keyframe_insert('pole_angle',frame=311)
            prop='IK_'+part+'.'+side;rig[prop]=0;rig.keyframe_insert(data_path='["'+prop+'"]',frame=311)
    namespace['_constant'](action);report['ik_snap_checks']=checks
    report['hair_body_surface_check']=namespace['check_salve_hair_surface_intersections']()
    headtree=_tc_tree(bpy.data.objects['Rostro_topologia']);report['right_hand_head_surface_crossings']={}
    for objname in ['Guante_palmar_quad.R']+['Dedo_'+fn+'_quad.R' for fn in ('thumb','index','middle','ring','pinky')]:
        pairs=headtree.overlap(_tc_tree(bpy.data.objects[objname]))
        if pairs:report['right_hand_head_surface_crossings'][objname]=len(pairs)
    report['body_minimum_z_m']=min(p.z for p in namespace['_character_vertices'](False))
    report['all_minimum_z_m']=min(p.z for p in namespace['_character_vertices'](True))
    assert report['body_minimum_z_m']>=0 and report['all_minimum_z_m']>=0,'Floor penetration THINK'
    contactpath=_TC_ROOT/'outputs/Salve_poses_contactos_v02.json'
    contact=json.loads(contactpath.read_text(encoding='utf8'))
    contact['ik_snap_checks']['THINK']=checks
    contact['poses']['THINK'].update({'hair_body_surface_check':report['hair_body_surface_check'],'minimum_body_z_m':report['body_minimum_z_m'],'minimum_all_z_m':report['all_minimum_z_m'],'contact_regions':namespace['_contact_regions']()})
    # Earlier floor and palm repairs changed rig transforms after the original
    # validation bake. Refresh all recorded coordinates without rebaking any pose.
    for name,frame in namespace['SALVE_POSE_FRAMES'].items():
        s.frame_set(frame);namespace['_update']()
        contact['poses'][name]['bone_points']={n:{'head':[round(x,5) for x in rig.pose.bones[n].head],'tail':[round(x,5) for x in rig.pose.bones[n].tail]} for n in ('pelvis','chest','head','thigh.L','thigh.R','shin.L','shin.R','upper_arm.L','upper_arm.R','forearm.L','forearm.R','hand.L','hand.R')}
    s.frame_set(311);namespace['_update']()
    report['ik_measurement_stage']='After static THINK surface/contact corrective and arm311 adjustment; final saved-frame controllers recalibrated here.'
    contact['think_surface_repair']=report['after_contact'];contactpath.write_text(json.dumps(contact,indent=2,ensure_ascii=False),encoding='utf8')
    ikpath=_TC_ROOT/'outputs/poses/Salve_diagnostico_IK_v02.json'
    if ikpath.exists():
        ik=json.loads(ikpath.read_text(encoding='utf8'))
        ik['latest_snap_checks']=contact['ik_snap_checks']
        ik['latest_snap_measurement_stage']='Frame311 measured after evaluated-finger/chin repair; other frames from saved final support/palm repair. Detailed frames diagnosis remains the earlier snapshot.'
        ikpath.write_text(json.dumps(ik,indent=2,ensure_ascii=False),encoding='utf8')
    report['status']='index_contact_and_floor_checks_passed_visual_review_pending'
    if report['right_hand_head_surface_crossings'] or report['hair_body_surface_check']['objects_with_surface_crossings']:report['status']='index_contact_passed_other_surface_crossings_pending'
    out.write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
    print('THINK_ACTUAL_SURFACE_CONTACT',json.dumps(report['after_contact']),flush=True)
    return report
