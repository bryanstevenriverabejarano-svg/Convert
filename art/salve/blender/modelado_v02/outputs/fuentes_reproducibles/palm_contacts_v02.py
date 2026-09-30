"""Correct dorsal orientation of floor-supported hands in poses91/161 only.
Rest nails/dorsal inserts are -Y; aim this normal upward, palm surface downward.
Preserves skeleton rest data, root support, UVs and geometry basis.
"""
import bpy,pathlib,json
from mathutils import Vector
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
bpy.context.window.scene=bpy.data.scenes['04_SALVE_MODELO_3D']
SALVE_POSES_AUTORUN=False
p=ROOT/'work/poses_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'))
scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];events={}

def hand_dorsal(side):
    rest=rig.data.bones['hand.'+side].matrix_local.to_quaternion()
    current=rig.pose.bones['hand.'+side].matrix.to_quaternion()
    return list(current@(rest.inverted()@Vector((0,-1,0))))

def key_bone(name,frame):
    pb=rig.pose.bones[name]
    for prop in ('location','rotation_quaternion','scale'):pb.keyframe_insert(prop,frame=frame)

original_report=json.loads((ROOT/'outputs/Salve_poses_contactos_v02.json').read_text(encoding='utf8'))
ik_report=original_report['ik_snap_checks']
for name,sides in (('seated',('L',)),('kneeling_variant',('L','R'))):
    frame=SALVE_POSE_FRAMES[name];scene.frame_set(frame);_update();evs={}
    for side in sides:
        before=hand_dorsal(side);hand=rig.pose.bones['hand.'+side]
        direction=hand.tail-hand.head
        _aim('hand.'+side,direction,(0,0,1))
        _fingers(side,curl=0,spread=.04)
        orientation=rig.pose.bones['hand.'+side].matrix.copy()
        arm=rig.pose.bones['forearm.'+side]
        pole=arm.head.copy()
        for iteration in range(4):
            minimum=_contact_regions()['hand.'+side]['minimum_z_m']
            if abs(minimum-.001)<.0001:break
            wrist=rig.pose.bones['hand.'+side].head+Vector((0,0,.001-minimum))
            _chain('upper_arm.'+side,'forearm.'+side,wrist,pole)
            _restore_rotation('hand.'+side,orientation)
        after=hand_dorsal(side);regions=_contact_regions()
        assert after[2]>.98,(name,side,after)
        assert 0<=regions['hand.'+side]['minimum_z_m']<.003,(name,side,regions)
        evs[side]={'dorsal_normal_before':before,'dorsal_normal_after':after,'hand_minimum_z_m':regions['hand.'+side]['minimum_z_m'],'wrist_world':list(rig.matrix_world@rig.pose.bones['hand.'+side].head)}
        for bone in ('upper_arm','forearm','hand'):key_bone(bone+'.'+side,frame)
        for finger in ('thumb','index','middle','ring','pinky'):
            for i in range(1,4):key_bone(f'{finger}.{i:02d}.{side}',frame)
    action=rig.animation_data.action;rig.animation_data.action=None
    checks=_snap_ik(True);rig.animation_data.action=action
    for bone in rig.pose.bones:
        if bone.name.startswith('CTRL_'):key_bone(bone.name,frame)
    for side in ('L','R'):
        for part,bone in (('foot','shin'),('hand','forearm')):
            c=next(c for c in rig.pose.bones[bone+'.'+side].constraints if c.type=='IK')
            c.keyframe_insert('pole_angle',frame=frame)
            prop='IK_'+part+'.'+side;rig[prop]=0;rig.keyframe_insert(data_path='["'+prop+'"]',frame=frame)
    ik_report[name]=checks
    body=_character_vertices(False);allcoords=_character_vertices(True)
    assert min(p.z for p in body)>=-.00001 and min(p.z for p in allcoords)>=-.00001,(name,'floor penetration')
    original_report['poses'][name].update({'minimum_body_z_m':round(min(p.z for p in body),6),'minimum_all_z_m':round(min(p.z for p in allcoords),6),'floor_penetration_m':round(max(0,-min(p.z for p in allcoords)),6),'contact_regions':_contact_regions(),'hair_body_surface_check':check_salve_hair_surface_intersections()})
    evs['body_minimum_z_m']=min(p.z for p in body);evs['all_minimum_z_m']=min(p.z for p in allcoords)
    if name=='seated':evs['posterior_pelvis_minimum_z_m']=_seat_minimum()
    events[name]=evs
    print('PALMS_UP_CONTACT',name,json.dumps(evs),flush=True)
_constant(action)
original_report['palm_orientation_repairs']=events
(ROOT/'outputs/Salve_poses_contactos_v02.json').write_text(json.dumps(original_report,indent=2,ensure_ascii=False),encoding='utf8')
(ROOT/'outputs/poses/Salve_orientacion_palmas_v02.json').write_text(json.dumps(events,indent=2,ensure_ascii=False),encoding='utf8')
hairpath=ROOT/'outputs/Salve_cabello_correctivos_v02.json';hair=json.loads(hairpath.read_text(encoding='utf8'))
for name in events:
    check=original_report['poses'][name]['hair_body_surface_check'];after=hair['poses'][name]['after']
    after['surface_crossings']=check['objects'];after['objects_with_crossings']=check['objects_with_surface_crossings']
hairpath.write_text(json.dumps(hair,indent=2,ensure_ascii=False),encoding='utf8')
scene.frame_set(1);_update()
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'outputs/Salve_refinado_v02.blend'))
print('PALM_CONTACTS_SAVED',flush=True)
