"""Repair only real floor support after facial-origin fix; no full pose bake.
Frame91 shifts pelvis/hand/feet down together and raises the folded knee via
the two-bone FK solve while keeping ankle endpoint and sole orientation.
Frames121/151/161 use root translation only. UV/materials/base mesh untouched.
"""
import bpy, pathlib, json
from mathutils import Vector
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
bpy.context.window.scene=bpy.data.scenes['04_SALVE_MODELO_3D']
SALVE_POSES_AUTORUN=False
p=ROOT/'work/poses_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'))
scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];events={}
original_report=json.loads((ROOT/'outputs/Salve_poses_contactos_v02.json').read_text(encoding='utf8'))

def key_bone(name,frame):
    pb=rig.pose.bones[name]
    for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(prop,frame=frame)

def physical_minimum(include_hair=False):
    return min(p.z for p in _character_vertices(include_hair))

# Inspect first, then alter. Head and fingers are unaffected relative to torso.
scene.frame_set(91);_update()
seat_before=_seat_minimum();contacts_before=_contact_regions();body_before=physical_minimum()
assert seat_before is not None and 0<seat_before<.030,seat_before
delta=.001-seat_before
_root_translate((0,0,delta))
ankle=rig.pose.bones['shin.L'].tail.copy()
foot_orientation=rig.pose.bones['foot.L'].matrix.copy()
pole=rig.pose.bones['shin.L'].head.copy()
iterations=[]
for step in range(12):
    regions=_contact_regions();deficit=.001-regions['knee.L']['minimum_z_m']
    if deficit<.00025:break
    pole.z+=max(.004,deficit*1.5)
    _chain('thigh.L','shin.L',ankle,pole)
    _restore_rotation('foot.L',foot_orientation)
    iterations.append({'step':step,'knee_minimum_before_m':regions['knee.L']['minimum_z_m'],'pole':list(pole)})
contacts_after=_contact_regions();seat_after=_seat_minimum();body_after=physical_minimum()
assert body_after>=-.00001,('seated physical floor penetration',body_after)
assert abs(seat_after-.001)<.001,('seated pelvis support',seat_after)
assert contacts_after['hand.L']['minimum_z_m']<.003,contacts_after
assert all(contacts_after['foot.'+s]['minimum_z_m']<.003 for s in ('L','R')),contacts_after
for name in ('root','thigh.L','shin.L','foot.L'):key_bone(name,91)
# Snap controls in only this changed pose. Keep the original action library.
action=rig.animation_data.action
rig.animation_data.action=None
seat_ik=_snap_ik(True)
rig.animation_data.action=action
for name in [b.name for b in rig.pose.bones if b.name.startswith('CTRL_')]:key_bone(name,91)
for side in ('L','R'):
    for part,bone in (('foot','shin'),('hand','forearm')):
        c=next(c for c in rig.pose.bones[bone+'.'+side].constraints if c.type=='IK')
        c.keyframe_insert('pole_angle',frame=91)
        prop='IK_'+part+'.'+side;rig[prop]=0;rig.keyframe_insert(data_path='["'+prop+'"]',frame=91)
events['seated']={'frame':91,'root_delta_m':delta,'posterior_pelvis_before_m':seat_before,'posterior_pelvis_after_m':seat_after,'body_before_m':body_before,'body_after_m':body_after,'contacts_before':contacts_before,'contacts_after':contacts_after,'knee_lift_iterations':iterations,'ankle_endpoint_target':list(ankle),'ankle_endpoint_error_m':(rig.pose.bones['shin.L'].tail-ankle).length,'ik_snap':seat_ik}
print('POSTFACE_SEATED_REPAIRED',json.dumps(events['seated']),flush=True)

for name in ('crouched','kneeling','kneeling_variant'):
    frame=SALVE_POSE_FRAMES[name];scene.frame_set(frame);_update()
    body_before=physical_minimum(False);all_before=physical_minimum(True);contacts_before=_contact_regions()
    delta=.001-body_before
    assert abs(delta)<.050,(name,delta)
    _root_translate((0,0,delta));key_bone('root',frame)
    body_after=physical_minimum(False);all_after=physical_minimum(True);contacts_after=_contact_regions()
    assert body_after>=-.00001 and all_after>=-.00001,(name,body_after,all_after)
    events[name]={'frame':frame,'root_delta_m':delta,'body_before_m':body_before,'all_before_m':all_before,'body_after_m':body_after,'all_after_m':all_after,'contacts_before':contacts_before,'contacts_after':contacts_after,'method':'uniform root translation; body, hair and controls move together'}
    print('POSTFACE_ROOT_REPAIRED',name,json.dumps(events[name]),flush=True)
_constant(action)
ik_report=original_report.get('ik_snap_checks',{});ik_report['seated']=seat_ik
report=validate_salve_poses(ik_report)
scene.frame_set(91);_update()
report['poses']['seated']['posterior_pelvis_minimum_z_m']=round(_seat_minimum(),6)
report['postface_floor_repairs']=events
assert all(p['minimum_body_z_m']>=-.00001 and p['minimum_all_z_m']>=-.00001 for p in report['poses'].values()),'postface negative body/hair floor'
(ROOT/'outputs/Salve_poses_contactos_v02.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
(ROOT/'outputs/poses/Salve_correccion_apoyos_postrostro_v02.json').write_text(json.dumps(events,indent=2,ensure_ascii=False),encoding='utf8')
# Uniform root motion translates every hair vertex; crossing counts are measured
# again above. Preserve the original before evidence and append the exact shift.
hairpath=ROOT/'outputs/Salve_cabello_correctivos_v02.json'
hair=json.loads(hairpath.read_text(encoding='utf8'))
for name,event in events.items():
    p=hair['poses'][name];after=p['after'];dz=event['root_delta_m']
    after['hair_minimum_z_m']=round(after['hair_minimum_z_m']+dz,6)
    after['leaf_minimum_z_m']={n:round(z+dz,6) for n,z in after['leaf_minimum_z_m'].items()}
    after['floor_penetrating_leaves']={n:z for n,z in after['leaf_minimum_z_m'].items() if z<0}
    chk=report['poses'][name]['hair_body_surface_check']
    after['surface_crossings']=chk['objects'];after['objects_with_crossings']=chk['objects_with_surface_crossings']
    p['postface_floor_root_delta_m']=dz
hairpath.write_text(json.dumps(hair,indent=2,ensure_ascii=False),encoding='utf8')
scene.frame_set(1);_update()
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'outputs/Salve_refinado_v02.blend'))
print('POSTFACE_FLOOR_REPAIRS_SAVED',flush=True)
