"""Read-only FK/IK measurements; temporary settings are restored, no geometry edits.
Run on assembled ready scene. Writes outputs/poses/Salve_diagnostico_IK_v02.json.
"""
import bpy, json, pathlib, math
from mathutils import Vector

def salve_diagnose_ik():
    scene = bpy.context.scene
    rig = bpy.data.objects['Salve_Rig']
    original_frame = scene.frame_current
    visibility = [(o, o.hide_viewport) for o in scene.objects if o.type == 'MESH']
    for o, hidden in visibility:
        o.hide_viewport = True
    def update():
        rig.update_tag()
        bpy.context.view_layer.update()
    def points(a, b):
        axis = b.tail - a.head
        t = (b.head - a.head).dot(axis) / max(axis.length_squared, 1e-10)
        projected = a.head + axis * t
        return {
            'first_head': list(a.head), 'first_tail': list(a.tail),
            'second_head': list(b.head), 'second_tail': list(b.tail),
            'first_length_m': (a.tail-a.head).length,
            'second_length_m': (b.tail-b.head).length,
            'joint_gap_m': (a.tail-b.head).length,
            'joint_radius_from_endpoint_axis_m': (b.head-projected).length,
            'endpoint_distance_from_start_m': axis.length,
            'first_basis_translation': list(a.location),
            'second_basis_translation': list(b.location),
            'first_basis_scale': list(a.scale), 'second_basis_scale': list(b.scale)
        }
    result = {
        'method': 'Bone endpoints in armature pose space, FK0 to IK1, exact same frame; no geometry edits',
        'armature_ik_solver': rig.pose.ik_solver,
        'frames': {}, 'limitations': [
            'This diagnoses static switching, not deformation or motion continuity.',
            'Disconnected rest bones are metadata evidence, not by themselves proof of the cause.'
        ]
    }
    try:
        for frame in (1, 151, 161, 311):
            scene.frame_set(frame)
            for k in rig.keys():
                if k.startswith('IK_'): rig[k] = 0.0
            update()
            checks = {}
            for side in ('L', 'R'):
                for part, first, second in (('foot', 'thigh', 'shin'), ('hand', 'upper_arm', 'forearm')):
                    a = rig.pose.bones[first+'.'+side]
                    b = rig.pose.bones[second+'.'+side]
                    c = next((c for c in b.constraints if c.type == 'IK'), None)
                    if not c: continue
                    prop = 'IK_'+part+'.'+side
                    fk = points(a, b)
                    anchor = a.head.copy(); joint = b.head.copy(); end = b.tail.copy()
                    check = {
                        'rest_first_length_m': a.bone.length,
                        'rest_second_length_m': b.bone.length,
                        'rest_joint_gap_m': (a.bone.tail_local-b.bone.head_local).length,
                        'first_connected': a.bone.use_connect,
                        'second_connected': b.bone.use_connect,
                        'first_inherit_scale': a.bone.inherit_scale,
                        'second_inherit_scale': b.bone.inherit_scale,
                        'first_ik_flags': {k:getattr(a,k) for k in ('lock_ik_x','lock_ik_y','lock_ik_z','use_ik_limit_x','use_ik_limit_y','use_ik_limit_z','ik_stiffness_x','ik_stiffness_y','ik_stiffness_z','ik_stretch')},
                        'second_ik_flags': {k:getattr(b,k) for k in ('lock_ik_x','lock_ik_y','lock_ik_z','use_ik_limit_x','use_ik_limit_y','use_ik_limit_z','ik_stiffness_x','ik_stiffness_y','ik_stiffness_z','ik_stretch')},
                        'constraint': {k:getattr(c,k) for k in ('chain_count','iterations','use_stretch','use_location','use_rotation','pole_angle')},
                        'fk': fk
                    }
                    rig[prop] = 1.0; update()
                    check['ik'] = points(a,b)
                    check['switch_error_m'] = {
                        'chain_start': (a.head-anchor).length,
                        'joint': (b.head-joint).length, 'endpoint': (b.tail-end).length
                    }
                    iterations = c.iterations
                    c.iterations = 256; update()
                    check['iterations_256_error_m'] = {
                        'chain_start': (a.head-anchor).length,
                        'joint': (b.head-joint).length, 'endpoint': (b.tail-end).length
                    }
                    c.iterations = iterations
                    rig[prop] = 0.0; update()
                    checks[part+'.'+side] = check
            result['frames'][str(frame)] = checks
        out = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2/outputs/poses')
        out.mkdir(parents=True, exist_ok=True)
        (out/'Salve_diagnostico_IK_v02.json').write_text(json.dumps(result, indent=2, ensure_ascii=False), encoding='utf8')
        print('SALVE_IK_DIAGNOSTIC_DONE', flush=True)
    finally:
        scene.frame_set(original_frame)
        for o, hidden in visibility: o.hide_viewport = hidden
        update()
    # Identify low objects independently of the contact-weight mask. Single point
    # landmarks and loose unweighted vertices can otherwise contaminate min Z.
    low_objects={}
    for frame in (1,91,121):
        scene.frame_set(frame);update();dg=bpy.context.evaluated_depsgraph_get()
        lows=[]
        for o in _character_meshes(False):
            ev=o.evaluated_get(dg);me=ev.to_mesh(preserve_all_data_layers=True,depsgraph=dg)
            if not me.vertices:ev.to_mesh_clear();continue
            vertex=min(me.vertices,key=lambda v:(ev.matrix_world@v.co).z)
            point=ev.matrix_world@vertex.co
            if point.z<.040:
                names={g.index:g.name for g in o.vertex_groups}
                lows.append({'object':o.name,'minimum_z_m':point.z,'minimum_point_world':list(point),'vertices':len(me.vertices),'polygons':len(me.polygons),'minimum_vertex_weights':{names.get(g.group,str(g.group)):g.weight for g in vertex.groups}})
            ev.to_mesh_clear()
        low_objects[str(frame)]=sorted(lows,key=lambda i:i['minimum_z_m'])
    result['low_mesh_objects']=low_objects
    scene.frame_set(original_frame);update()
    (out/'Salve_diagnostico_IK_v02.json').write_text(json.dumps(result,indent=2,ensure_ascii=False),encoding='utf8')
    print('SALVE_LOW_OBJECT_DIAGNOSTIC_DONE',json.dumps({f:[(i['object'],round(i['minimum_z_m'],6)) for i in items[:4]] for f,items in low_objects.items()}),flush=True)
    return result

if globals().get('SALVE_IK_DIAGNOSTICS_AUTORUN', True):
    salve_ik_diagnosis = salve_diagnose_ik()
