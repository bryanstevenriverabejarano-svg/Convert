"""Salve v02 FK poses and optional matched IK, no geometry replacement or save.

Run with exec(compile(open(path, encoding='utf8').read(),path,'exec')).
Exports bake_salve_poses(), apply_salve_pose(), validate_salve_poses().
Pose-space units metres, front -Y, L +X, Z up. Existing skeleton retained.
"""
import bpy, json, math, pathlib
from mathutils import Vector, Matrix, Quaternion

SALVE_OUTPUT = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2/outputs')
SALVE_POSE_FRAMES = {'stand':1,'front_three_quarter':11,'front_opposite':21,'A':31,'front_variant':41,'hair_open':61,'seated':91,'crouched':121,'kneeling':151,'kneeling_variant':161,'leaning':171,'leaning_variant':181,'THINK':311}
SALVE_CONTACT_PLANE = 0.0
_RIG = bpy.data.objects['Salve_Rig']
_SCENE = bpy.context.scene

def _update():
    _RIG.update_tag()
    bpy.context.view_layer.update()

def _reset_fk():
    for b in _RIG.pose.bones:
        b.rotation_mode='QUATERNION'
        b.matrix_basis=Matrix.Identity(4)
    for k in _RIG.keys():
        if k.startswith('IK_'):
            _RIG[k]=0.0
    _update()

def _aim(name, direction, surface_normal=None, rest_normal=(0,-1,0)):
    """Aim a bone in armature space, preserve original roll unless a surface is supplied."""
    b=_RIG.pose.bones[name];r=_RIG.data.bones[name]
    direction=Vector(direction).normalized()
    q=(r.tail_local-r.head_local).normalized().rotation_difference(direction) @ r.matrix_local.to_quaternion()
    if surface_normal is not None:
        local_normal=r.matrix_local.to_quaternion().inverted() @ Vector(rest_normal)
        actual=q@local_normal
        desired=Vector(surface_normal)
        actual-=direction*actual.dot(direction)
        desired-=direction*desired.dot(direction)
        if actual.length>1e-5 and desired.length>1e-5:
            actual.normalize();desired.normalize()
            angle=math.atan2(direction.dot(actual.cross(desired)),actual.dot(desired))
            q=Quaternion(direction,angle)@q
    b.matrix=Matrix.Translation(b.head)@q.to_matrix().to_4x4()
    _update()

def _root_translate(delta):
    b=_RIG.pose.bones['root']
    m=b.matrix.copy();m.translation+=Vector(delta);b.matrix=m
    _update()

def _chain(first,second,target,pole):
    """Two-link analytic FK solve, clamped to reach; returns target residual in metres."""
    start=_RIG.pose.bones[first].head.copy();target=Vector(target);pole=Vector(pole)
    length_a=_RIG.data.bones[first].length;length_b=_RIG.data.bones[second].length
    d=target-start;distance=d.length
    if distance<1e-6:d=Vector((0,-1,0));distance=1e-6
    axis=d.normalized();reach=min(max(distance,abs(length_a-length_b)+.0005),length_a+length_b-.0005)
    along=(length_a*length_a-length_b*length_b+reach*reach)/(2*reach)
    off=math.sqrt(max(0,length_a*length_a-along*along))
    transverse=pole-start-axis*(pole-start).dot(axis)
    if transverse.length<.0001:transverse=axis.cross(Vector((1,0,0)))
    transverse.normalize();joint=start+axis*along+transverse*off
    end=start+axis*reach
    _aim(first,joint-start)
    _aim(second,end-_RIG.pose.bones[second].head)
    return float((_RIG.pose.bones[second].tail-target).length)

def _fingers(side,curl=.0,spread=.0):
    for idx,name in enumerate(('thumb','index','middle','ring','pinky')):
        for j in range(1,4):
            b=_RIG.pose.bones.get(f'{name}.{j:02d}.{side}')
            if not b:continue
            # Local X bends the fingers into the palm; the thumb is independently weaker.
            angle=curl*(.48 if name=='thumb' else (1.0 if j>1 else .65))
            b.rotation_quaternion=Quaternion((1,0,0),angle)
            if j==1 and spread:b.rotation_quaternion=Quaternion((0,0,1),(idx-2)*spread)@b.rotation_quaternion
    _update()

def _arm_to(side,wrist,pole,hand_direction,normal=None,curl=.0):
    _chain('upper_arm.'+side,'forearm.'+side,wrist,pole)
    _aim('hand.'+side,hand_direction,normal)
    _fingers(side,curl)

def _restore_rotation(name,matrix):
    pb=_RIG.pose.bones[name]
    m=matrix.copy();m.translation=pb.head;pb.matrix=m;_update()

def _seat_minimum():
    dg=bpy.context.evaluated_depsgraph_get();points=[]
    pelvis_y=(_RIG.matrix_world@_RIG.pose.bones['pelvis'].head).y
    for o in _character_meshes(False):
        if not any(c.name.startswith('01_Cuerpo') for c in o.users_collection):continue
        ids={g.index for g in o.vertex_groups if g.name=='pelvis'}
        if not ids:continue
        eo=o.evaluated_get(dg);me=eo.to_mesh(preserve_all_data_layers=True,depsgraph=dg)
        for v in me.vertices:
            if sum(g.weight for g in v.groups if g.group in ids)<.72:continue
            p=eo.matrix_world@v.co
            if p.y>pelvis_y+.012:points.append(p.z)
        eo.to_mesh_clear()
    return min(points) if points else None

def _settle_supports(kind):
    """Correct real support regions individually, preserving anatomical bone lengths."""
    if kind in ('front_three_quarter','front_opposite','front_variant'):
        support='R' if kind=='front_opposite' else 'L';free='L' if support=='R' else 'R'
        regions=_contact_regions();support_min=regions['foot.'+support]['minimum_z_m']
        if support_min is not None:_root_translate((0,0,.001-support_min))
        regions=_contact_regions();minimum=regions['foot.'+free]['minimum_z_m']
        if minimum is not None:
            shin=_RIG.pose.bones['shin.'+free];foot_matrix=_RIG.pose.bones['foot.'+free].matrix.copy()
            _chain('thigh.'+free,'shin.'+free,shin.tail+Vector((0,0,.001-minimum)),shin.head+Vector((0,-.12,.015)))
            _restore_rotation('foot.'+free,foot_matrix)
        return
    if kind not in ('seated','kneeling','kneeling_variant'):return
    for iteration in range(3):
        regions=_contact_regions()
        if kind=='seated':anchor=_seat_minimum()
        else:anchor=min(regions['knee.L']['minimum_z_m'],regions['knee.R']['minimum_z_m'])
        if anchor is not None:_root_translate((0,0,.001-anchor))
        regions=_contact_regions()
        for side in ('L','R'):
            floor=regions['foot.'+side]['minimum_z_m']
            if floor is None:continue
            delta=.001-floor
            foot_matrix=_RIG.pose.bones['foot.'+side].matrix.copy()
            shin=_RIG.pose.bones['shin.'+side]
            target=shin.tail+Vector((0,0,delta))
            if kind=='seated':
                pole=shin.head+Vector((0,-.03,.03))
                _chain('thigh.'+side,'shin.'+side,target,pole)
            else:
                _aim('shin.'+side,target-shin.head)
            _restore_rotation('foot.'+side,foot_matrix)
        support_sides=('L',) if kind=='seated' else (('L','R') if kind=='kneeling_variant' else ())
        for side in support_sides:
            s=1 if side=='L' else -1
            sh=_RIG.pose.bones['upper_arm.'+side].head
            if iteration==0:
                wrist=(sh.x+s*.07,sh.y+(.065 if kind=='seated' else -.04),.023)
            else:
                hand_min=_contact_regions()['hand.'+side]['minimum_z_m']
                wrist=_RIG.pose.bones['hand.'+side].head+Vector((0,0,.001-(hand_min if hand_min is not None else .001)))
            direction=(s*.09,.01,0) if kind=='seated' else (s*.01,-.09,0)
            pole=(sh.x+s*.11,sh.y+.02,sh.z-.23)
            # Rest -Y is dorsal (nails/insert). For floor support dorsal faces up.
            _arm_to(side,wrist,pole,direction,(0,0,1),.0)
            _fingers(side,spread=.04)
        # Fold the resting hand across the supported knee after the hip settles.
        if kind=='seated':
            knee=_RIG.pose.bones['thigh.L'].tail
            _arm_to('R',(knee.x-.12,knee.y+.01,knee.z+.12),(-.21,-.17,knee.z+.19),(.055,-.02,-.072),(0,-1,0),.18)
    _update()

def _gravity_hair(kind):
    """FK drape: lower chains trail outward and rearward near floor, no physics claim."""
    if kind in ('stand','A','THINK','front_three_quarter','front_opposite','front_variant'):return
    for side,s in (('L',1),('R',-1)):
        for strand in ('front','side','rear'):
            for i in range(1,5):
                name=f'hair_{strand}.{i:02d}.{side}'
                if name not in _RIG.pose.bones:continue
                b=_RIG.pose.bones[name];length=_RIG.data.bones[name].length
                if kind=='hair_open':
                    direction=Vector((s*(.07 if i<3 else .035),.015,-.18))
                else:
                    # Rearward fall keeps the long mass outside bent torsos and folded legs.
                    outward=.045 if strand=='front' else .025
                    rear=.05 if strand=='front' else .065
                    direction=Vector((s*outward,rear,-.18))
                    min_tail=SALVE_CONTACT_PLANE+.035
                    max_drop=max(.005,b.head.z-min_tail)
                    if length*direction.normalized().z < -max_drop:
                        nz=-min(.99,max_drop/max(length,.001))
                        horizontal=math.sqrt(max(.0001,1-nz*nz))
                        xy=Vector((s*.45,1,0)).normalized()*horizontal
                        direction=Vector((xy.x,xy.y,nz))
                _aim(name,direction)

def apply_salve_pose(kind,place_on_floor=True):
    """Place static reference pose; detach any action during authoring before calling."""
    _reset_fk()
    if kind=='A':
        pass
    elif kind in ('front_three_quarter','front_opposite','front_variant'):
        support=-1 if kind=='front_opposite' else 1
        _root_translate((support*.016,0,0))
        _aim('pelvis',(-support*.019,.008,.12));_aim('spine.01',(-support*.014,0,.15));_aim('spine.02',(-support*.008,-.005,.16));_aim('chest',(support*.002,0,.09));_aim('neck',(0,0,.09));_aim('head',(-support*.005,-.006,.16))
        for side,s in (('L',1),('R',-1)):
            if s==support:
                _aim('thigh.'+side,(s*.001,.003,-.38));_aim('shin.'+side,(-s*.006,.022,-.465))
            else:
                _aim('thigh.'+side,(-s*.048,-(.065 if kind=='front_variant' else .048),-.373));_aim('shin.'+side,(s*.012,.065,-.459))
            _aim('foot.'+side,(s*.009,-.12,-.075),(0,0,1),(0,0,1))
            _aim('upper_arm.'+side,(s*.057,-.012,-.226));_aim('forearm.'+side,(s*.032,-.008,-.242));_aim('hand.'+side,(s*.018,-.009,-.092));_fingers(side,spread=.02)
    elif kind=='hair_open':
        for side,s in (('L',1),('R',-1)):
            _aim('upper_arm.'+side,(s*.145,0,-.20))
            _aim('forearm.'+side,(s*.14,-.01,-.23))
            _aim('hand.'+side,(s*.06,-.003,-.085));_fingers(side,spread=.05)
    elif kind in ('seated','crouched','kneeling','kneeling_variant'):
        _root_translate((0,0,{'seated':-.79,'crouched':-.655,'kneeling':-.735,'kneeling_variant':-.65}[kind]))
        if kind=='seated':
            _aim('pelvis',(.018,.016,.12))
            _aim('spine.01',(.06,.025,.135));_aim('spine.02',(-.018,.01,.16));_aim('chest',(.03,-.002,.085));_aim('neck',(-.01,-.01,.09));_aim('head',(-.01,-.008,.16))
            _aim('thigh.R',(-.04,-.255,.28));_aim('shin.R',(.0,-.347,-.31));_aim('foot.R',(-.02,-.12,-.075),(0,0,1),(0,0,1))
            _aim('thigh.L',(.26,-.245,-.10));_aim('shin.L',(-.405,-.225,-.035));_aim('foot.L',(-.12,-.06,-.032),(0,0,1),(0,0,1))
            sh=_RIG.pose.bones['upper_arm.L'].head
            _arm_to('L',(sh.x+.085,sh.y+.12,sh.z-.454),(sh.x+.15,sh.y+.04,sh.z-.23),(.015,-.035,-.08),(0,-1,0),.12)
            knee=_RIG.pose.bones['thigh.L'].tail
            _arm_to('R',(knee.x-.06,knee.y-.04,knee.z+.12),(-.21,-.17,knee.z+.19),(.07,.0,-.06),(0,-1,0),.15)
        elif kind=='crouched':
            _aim('pelvis',(0,.025,.12));_aim('spine.01',(0,-.055,.15));_aim('spine.02',(0,-.045,.16));_aim('chest',(0,-.018,.09));_aim('neck',(0,.014,.09));_aim('head',(0,.01,.16))
            for side,s in (('L',1),('R',-1)):
                _aim('thigh.'+side,(s*.065,-.295,.231));_aim('shin.'+side,(-s*.032,.266,-.382));_aim('foot.'+side,(s*.012,-.12,-.075),(0,0,1),(0,0,1))
                knee=_RIG.pose.bones['thigh.'+side].tail
                _arm_to(side,(-s*.02,knee.y-.015,knee.z+.08),(s*.235,knee.y-.025,knee.z+.13),(-s*.075,-.015,-.035),(0,-.2,1),.32)
        elif kind=='kneeling':
            _aim('pelvis',(0,-.005,.12));_aim('spine.01',(0,-.02,.15));_aim('spine.02',(0,-.006,.16));_aim('chest',(0,0,.09));_aim('neck',(0,.005,.09));_aim('head',(0,-.007,.16))
            for side,s in (('L',1),('R',-1)):
                _aim('thigh.'+side,(s*.033,-.348,-.151));_aim('shin.'+side,(-s*.012,.464,.017));_aim('foot.'+side,(s*.01,.137,-.033),(0,0,-1),(0,0,1))
                knee=_RIG.pose.bones['thigh.'+side].tail
                _arm_to(side,(s*.11,knee.y+.15,knee.z+.105),(s*.19,-.13,knee.z+.30),(-s*.025,-.065,-.06),(0,-1,0),.12)
        elif kind=='kneeling_variant':
            _aim('pelvis',(0,-.035,.12));_aim('spine.01',(0,-.146,.034));_aim('spine.02',(0,-.151,.043));_aim('chest',(0,-.084,.03));_aim('neck',(0,-.060,.07));_aim('head',(0,-.065,.145))
            for side,s in (('L',1),('R',-1)):
                _aim('thigh.'+side,(s*.013,-.28,-.258));_aim('shin.'+side,(-s*.015,.463,-.043));_aim('foot.'+side,(s*.02,.13,-.045),(0,0,-1),(0,0,1))
                sh=_RIG.pose.bones['upper_arm.'+side].head
                _arm_to(side,(s*.14,sh.y-.09,sh.z-.455),(s*.155,sh.y-.02,sh.z-.23),(s*.006,-.05,-.079),(0,-1,0),.10)
    elif kind in ('leaning','leaning_variant'):
        variant=kind.endswith('variant')
        _aim('pelvis',(0,.02,.12))
        for name,le,z in [('spine.01',.112 if variant else .080,.10 if variant else .128),('spine.02',.142 if variant else .105,.077 if variant else .12),('chest',.08 if variant else .06,.04 if variant else .065)]:
            _aim(name,(0,-le,z))
        _aim('neck',(0,-.035,.083));_aim('head',(0,.02,.16))
        for side,s in (('L',1),('R',-1)):
            _aim('thigh.'+side,(s*.007,-.06,-.376));_aim('shin.'+side,(-s*.003,.06,-.461));_aim('foot.'+side,(s*.003,-.12,-.075),(0,0,1),(0,0,1))
            sh=_RIG.pose.bones['upper_arm.'+side].head
            wrist=(s*.095,-.16,sh.z-.435)
            _arm_to(side,wrist,(s*.19,-.28,sh.z-.22),(s*.002,.01,-.095),(0,-1,0),.12)
    elif kind=='THINK':
        # Head stays close to neutral to retain the frontal expression identity.
        _aim('head',(.01,-.007,.16))
        chin=_RIG.pose.bones['head'].matrix @ (_RIG.data.bones['head'].matrix_local.inverted() @ Vector((-.006,-.035,1.525)))
        # Bone-tail and fingertip are separated by the original finger rig; hand points upward.
        wrist=chin+Vector((-.05,-.055,-.105))
        _arm_to('R',wrist,(-.24,-.12,1.25),(.033,.025,.085),(0,1,0),.67)
        _RIG.pose.bones['index.01.R'].rotation_quaternion=Quaternion((1,0,0),.14)
        _RIG.pose.bones['index.02.R'].rotation_quaternion=Quaternion((1,0,0),.18)
        _RIG.pose.bones['index.03.R'].rotation_quaternion=Quaternion((1,0,0),.12)
        _update()
        # Match the distal index pad to the lower chin instead of merely raising the arm.
        hand_matrix=_RIG.pose.bones['hand.R'].matrix.copy()
        tip=_RIG.pose.bones['index.03.R'].tail.copy()
        correction=chin+Vector((0,-.002,-.002))-tip
        wrist=_RIG.pose.bones['hand.R'].head+correction
        _chain('upper_arm.R','forearm.R',wrist,(-.24,-.12,1.25))
        _restore_rotation('hand.R',hand_matrix)
        elbow=_RIG.pose.bones['forearm.R'].head
        _arm_to('L',elbow+Vector((.04,-.035,-.035)),(.21,-.02,1.12),(-.075,.018,.045),(0,0,1),.3)
    else:
        for side,s in (('L',1),('R',-1)):
            _aim('upper_arm.'+side,(s*.063,-.008,-.225));_aim('forearm.'+side,(s*.037,-.01,-.241));_aim('hand.'+side,(s*.027,-.008,-.091));_fingers(side,spread=.02)
    _gravity_hair(kind)
    if place_on_floor:
        _settle_supports(kind)
        coords=_character_vertices(include_hair=False)
        if coords:
            minz=min(v.z for v in coords)
            _root_translate((0,0,SALVE_CONTACT_PLANE+.001-minz))
        _gravity_hair(kind)
    _update()

def _character_meshes(include_hair=True):
    for o in _SCENE.objects:
        if o.type!='MESH' or o.hide_render:continue
        if not any(c.name.startswith(('01_Cuerpo','02_Rostro','03_Cabello','04_Tecnologia')) for c in o.users_collection):continue
        if not include_hair and any(c.name.startswith('03_Cabello') for c in o.users_collection):continue
        yield o

def _character_vertices(include_hair=True):
    dg=bpy.context.evaluated_depsgraph_get();coords=[]
    for o in _character_meshes(include_hair):
        eo=o.evaluated_get(dg);me=eo.to_mesh()
        coords.extend(eo.matrix_world@v.co for v in me.vertices);eo.to_mesh_clear()
    return coords

def _contact_regions():
    """Measure skin vertices weighted to the named deform region, not bounding boxes."""
    dg=bpy.context.evaluated_depsgraph_get();parts={};values={}
    for side in ('L','R'):
        for part in ('foot','knee','hand'):values[part+'.'+side]=[]
    for o in _character_meshes(False):
        if o.name.startswith(('Pest','Ceja','Parpado','Nariz','Boca','Dientes','Lengua','Lagrima')):continue
        groups={g.index:g.name for g in o.vertex_groups}
        assignment={}
        for gid,n in groups.items():
            for side in ('L','R'):
                for part,prefix in (('foot',('foot.','toe.')),('knee',('shin.','thigh.')),('hand',('hand.','index.','middle.','ring.','pinky.'))):
                    if n.endswith('.'+side) and n.startswith(prefix):assignment[gid]=part+'.'+side
        if not assignment:continue
        eo=o.evaluated_get(dg);me=eo.to_mesh(preserve_all_data_layers=True,depsgraph=dg)
        for v in me.vertices:
            regionweights={}
            for g in v.groups:
                region=assignment.get(g.group)
                if region:regionweights[region]=regionweights.get(region,0)+g.weight
            if not regionweights:continue
            p=eo.matrix_world@v.co
            for region,weight in regionweights.items():
                if weight<.72:continue
                if region.startswith('knee') and (p-_RIG.matrix_world@_RIG.pose.bones['shin.'+region[-1]].head).length>.075:continue
                values[region].append(p.z)
        eo.to_mesh_clear()
    for region,points in values.items():
        parts[region]={'minimum_z_m':round(min(points),5) if points else None,'sampled_evaluated_vertices':len(points),'method':'evaluated mesh vertices with at least 0.72 weight for region'}
    return parts

def check_salve_hair_surface_intersections():
    """Triangle surface crossings against body suit, excluding intentional scalp overlap.

    Reports evidence for follow-up, never labels a pose collision-free. A lock wholly
    inside a closed shell may lack a crossing; the eye remains necessary.
    """
    from mathutils.bvhtree import BVHTree
    dg=bpy.context.evaluated_depsgraph_get();verts=[];faces=[]
    for o in _character_meshes(False):
        if not any(c.name.startswith('01_Cuerpo') for c in o.users_collection):continue
        eo=o.evaluated_get(dg);me=eo.to_mesh();offset=len(verts)
        verts.extend(eo.matrix_world@v.co for v in me.vertices)
        faces.extend(tuple(offset+i for i in p.vertices) for p in me.polygons)
        eo.to_mesh_clear()
    if not faces:return {'method':'surface BVH overlap','objects':{},'tested':0,'status':'no body surfaces'}
    body_tree=BVHTree.FromPolygons(verts,faces,epsilon=.00001)
    crossings={};tested=0
    for o in _character_meshes(True):
        if not o.name.startswith(('Mechon_largo','Mechon_rostro')):continue
        eo=o.evaluated_get(dg);me=eo.to_mesh();vv=[eo.matrix_world@v.co for v in me.vertices];ff=[tuple(p.vertices) for p in me.polygons];eo.to_mesh_clear()
        if not ff:continue
        tested+=1;tree=BVHTree.FromPolygons(vv,ff,epsilon=.00001);overlap=body_tree.overlap(tree)
        if overlap:crossings[o.name]=len(overlap)
    return {'method':'evaluated polygon surface BVH overlaps with suit only; excludes scalp/head','objects':crossings,'tested':tested,'objects_with_surface_crossings':len(crossings),'limitation':'Enclosed volumes and touching tolerance require separate visual judgement.'}

def _prepare_ik():
    for side in ('L','R'):
        for part,bone,pole in (('foot','shin','knee_pole'),('hand','forearm','elbow_pole')):
            pb=_RIG.pose.bones[bone+'.'+side]
            constraints=[c for c in pb.constraints if c.type=='IK']
            if not constraints:continue
            c=constraints[0];c.name='IK calibrada | '+part
            c.use_stretch=False;c.chain_count=2;c.iterations=96
            c.target=_RIG;c.subtarget='CTRL_'+part+'.'+side
            c.pole_target=_RIG;c.pole_subtarget='CTRL_'+pole+'.'+side
            for bname in (bone+'.'+side,('thigh' if part=='foot' else 'upper_arm')+'.'+side):
                _RIG.pose.bones[bname].ik_stretch=0
            end=_RIG.pose.bones[part+'.'+side]
            rot=next((r for r in end.constraints if r.name=='Orientacion control IK'),None)
            if rot is None:
                rot=end.constraints.new('COPY_ROTATION');rot.name='Orientacion control IK'
            rot.target=_RIG;rot.subtarget='CTRL_'+part+'.'+side;rot.owner_space='POSE';rot.target_space='POSE'
            try:rot.driver_remove('influence')
            except TypeError:pass
            d=rot.driver_add('influence').driver;d.type='SCRIPTED';v=d.variables.new();v.name='blend';v.targets[0].id=_RIG;v.targets[0].data_path='["IK_'+part+'.'+side+'"]';d.expression='blend'
            _RIG.id_properties_ui('IK_'+part+'.'+side).update(min=0,max=1,description='FK 0 / IK 1. Controles coinciden con cada pose; poles y orientacion calibrados.')

def _snap_ik(calibrate=True):
    # Calibration only needs evaluated bones. Suspend mesh evaluation to avoid
    # rebuilding the high-resolution character for every pole-angle candidate.
    visibility=[(o,o.hide_viewport) for o in _character_meshes(True)]
    for obj,_ in visibility:obj.hide_viewport=True
    try:
        return _snap_ik_impl(calibrate)
    finally:
        for obj,hidden in visibility:obj.hide_viewport=hidden
        _update()

def _snap_ik_impl(calibrate=True):
    result={}
    for side in ('L','R'):
        for part,first,second,pole in (('foot','thigh','shin','knee_pole'),('hand','upper_arm','forearm','elbow_pole')):
            a=_RIG.pose.bones[first+'.'+side];b=_RIG.pose.bones[second+'.'+side]
            desired_joint=b.head.copy();desired_end=b.tail.copy();end=_RIG.pose.bones[part+'.'+side]
            desired_orientation=end.matrix.copy()
            _RIG.pose.bones['CTRL_'+part+'.'+side].matrix=desired_orientation
            axis=desired_end-a.head;projection=a.head+axis*max(0,min(1,(desired_joint-a.head).dot(axis)/max(axis.length_squared,1e-8)))
            bent=desired_joint-projection
            if bent.length<.0001:bent=Vector((0,-1,0))
            pole_loc=desired_joint+bent.normalized()*.35
            p=_RIG.pose.bones['CTRL_'+pole+'.'+side];m=p.matrix.copy();m.translation=pole_loc;p.matrix=m
            _update()
            c=next((c for c in b.constraints if c.type=='IK'),None)
            if not c:continue
            prop='IK_'+part+'.'+side
            _RIG[prop]=1.0;_update()
            if calibrate:
                base=c.pole_angle;best=(float('inf'),base)
                for i in range(25):
                    angle=-math.pi+i*(2*math.pi/24);c.pole_angle=angle;_update()
                    error=(b.head-desired_joint).length
                    if error<best[0]:best=(error,angle)
                step=2*math.pi/24
                for _ in range(3):
                    center=best[1]
                    for i in range(-3,4):
                        angle=center+i*step/3;c.pole_angle=angle;_update();error=(b.head-desired_joint).length
                        if error<best[0]:best=(error,angle)
                    step/=3
                c.pole_angle=best[1];_update()
            result[part+'.'+side]={'joint_switch_error_m':round((b.head-desired_joint).length,6),'endpoint_switch_error_m':round((b.tail-desired_end).length,6),'pole_angle_rad':round(c.pole_angle,6),'stretch':False}
            _RIG[prop]=0.0;_update()
    return result

def _constant(action):
    try:
        for slot in action.slots:
            for layer in action.layers:
                for strip in layer.strips:
                    bag=strip.channelbag(slot)
                    if bag:
                        for fc in bag.fcurves:
                            for key in fc.keyframe_points:key.interpolation='CONSTANT'
    except Exception:pass

def bake_salve_poses(calibrate_ik=True):
    rig_hidden=_RIG.hide_get();_RIG.hide_set(False)
    _prepare_ik()
    action=_RIG.animation_data.action if _RIG.animation_data else None
    report={}
    neutral_basis={}
    neutral_pole={}
    for name,frame in SALVE_POSE_FRAMES.items():
        if _RIG.animation_data:_RIG.animation_data.action=None
        _SCENE.frame_set(frame);apply_salve_pose(name)
        checks=_snap_ik(calibrate_ik)
        if name=='stand':
            neutral_basis={pb.name:pb.matrix_basis.copy() for pb in _RIG.pose.bones}
            neutral_pole={(part,side):next(c for c in _RIG.pose.bones[bone+'.'+side].constraints if c.type=='IK').pole_angle for side in ('L','R') for part,bone in (('foot','shin'),('hand','forearm'))}
        if action:_RIG.animation_data.action=action
        for pb in _RIG.pose.bones:
            pb.keyframe_insert('location',frame=frame);pb.keyframe_insert('rotation_quaternion',frame=frame);pb.keyframe_insert('scale',frame=frame)
        for side in ('L','R'):
            for part,bone in (('foot','shin'),('hand','forearm')):
                prop='IK_'+part+'.'+side;_RIG[prop]=0.0;_RIG.keyframe_insert(data_path='["'+prop+'"]',frame=frame)
                c=next(c for c in _RIG.pose.bones[bone+'.'+side].constraints if c.type=='IK');c.keyframe_insert('pole_angle',frame=frame)
        action=_RIG.animation_data.action
        marker=next((m for m in _SCENE.timeline_markers if m.name=='POSE_'+name),None)
        if marker:marker.frame=frame
        else:_SCENE.timeline_markers.new('POSE_'+name,frame=frame)
        report[name]=checks
        print('SALVE_POSE_BAKED',name,frame,json.dumps(checks),flush=True)
    # Restored neutral pose at facial frames; THINK keeps its hand support pose.
    if _RIG.animation_data:_RIG.animation_data.action=None
    for frame in range(201,402,10):
        if frame==311:continue
        _SCENE.frame_set(frame)
        for pb in _RIG.pose.bones:pb.matrix_basis=neutral_basis[pb.name]
        if action:_RIG.animation_data.action=action
        for pb in _RIG.pose.bones:
            pb.keyframe_insert('location',frame=frame);pb.keyframe_insert('rotation_quaternion',frame=frame);pb.keyframe_insert('scale',frame=frame)
        for side in ('L','R'):
            for part,bone in (('foot','shin'),('hand','forearm')):
                c=next(c for c in _RIG.pose.bones[bone+'.'+side].constraints if c.type=='IK');c.pole_angle=neutral_pole[(part,side)];c.keyframe_insert('pole_angle',frame=frame)
        if _RIG.animation_data:_RIG.animation_data.action=None
    _RIG.animation_data.action=action
    _constant(action)
    _RIG['poses_v02_json']=json.dumps(SALVE_POSE_FRAMES)
    _RIG['rig_estado']='Biblioteca FK estatica y controles IK con snap por pose; deformacion de produccion no certificada'
    _SCENE.frame_set(1);_update();_RIG.hide_set(rig_hidden)
    return report

def validate_salve_poses(ik_report=None,check_hair=True):
    result={'frames':SALVE_POSE_FRAMES,'contact_plane_m':SALVE_CONTACT_PLANE,'poses':{},'ik_snap_checks':ik_report or {},'limitations':['Envelope checks do not establish absence of self-intersection.','Contact samples use evaluated vertex weights; diffuse weights can leave a region without samples.','Hair is manually posed by bone chains; local hair-body collisions still require visual review.','FK poses are static. Rig pole switching is numerically checked per static pose; full motion continuity and skin correctives are not production certified.']}
    for name,frame in SALVE_POSE_FRAMES.items():
        _SCENE.frame_set(frame);_update();body=_character_vertices(False);allcoords=_character_vertices(True)
        finite=all(all(math.isfinite(x) for x in v) for v in allcoords)
        result['poses'][name]={'frame':frame,'finite':finite,'minimum_body_z_m':round(min(v.z for v in body),6),'minimum_all_z_m':round(min(v.z for v in allcoords),6),'floor_penetration_m':round(max(0,-min(v.z for v in allcoords)),6),'contact_regions':_contact_regions(),'bone_points':{n:{'head':[round(x,5) for x in _RIG.pose.bones[n].head],'tail':[round(x,5) for x in _RIG.pose.bones[n].tail]} for n in ('pelvis','chest','head','thigh.L','thigh.R','shin.L','shin.R','hand.L','hand.R')}}
        if check_hair:result['poses'][name]['hair_body_surface_check']=check_salve_hair_surface_intersections()
    _SCENE.frame_set(1);_update()
    return result

if globals().get('SALVE_POSES_AUTORUN', True):
    _salve_ik_report=bake_salve_poses()
    _salve_report=validate_salve_poses(_salve_ik_report)
    SALVE_OUTPUT.mkdir(exist_ok=True,parents=True)
    (SALVE_OUTPUT/'Salve_poses_contactos_v02.json').write_text(json.dumps(_salve_report,ensure_ascii=False,indent=2),encoding='utf8')
    print('SALVE_POSES_V02_COMPLETE',flush=True)

