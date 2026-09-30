"""One targeted clearance refinement for Mechon_largo_07.R in pose161.
Runs inside final assembly; no full pose bake, no base or UV edits.
It preserves the previous result when crossing count worsens or shift grows >60mm.
"""
import bpy, pathlib, json
from mathutils import Vector
from mathutils.bvhtree import BVHTree

_TF_WORK = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2/work')

def _tf_cross_rows(obj, tree):
    modifiers = [(m,m.show_viewport,m.show_render) for m in obj.modifiers if m.type=='SUBSURF']
    for m,_,_ in modifiers: m.show_viewport=False; m.show_render=False
    _hc_update(obj)
    points = _hc_base_positions(obj)
    layout = _hc_rows(obj.name,len(points))
    centers = []
    if layout:
        sides,rows = layout
        centers = [sum(points[r*sides:(r+1)*sides],Vector())/sides for r in range(rows)]
    for m,v,r in modifiers: m.show_viewport=v; m.show_render=r
    _hc_update(obj)
    dg=bpy.context.evaluated_depsgraph_get();ev=obj.evaluated_get(dg);me=ev.to_mesh()
    pts=[ev.matrix_world@v.co for v in me.vertices]
    faces=[tuple(p.vertices) for p in me.polygons]
    ev.to_mesh_clear()
    ht=BVHTree.FromPolygons(pts,faces,epsilon=.00001)
    overlaps=tree.overlap(ht)
    byrow={};allcenters=[]
    for body_face,hair_face in overlaps:
        centroid=sum((pts[i] for i in faces[hair_face]),Vector())/len(faces[hair_face])
        allcenters.append(centroid)
        row=min(range(len(centers)),key=lambda i:(centers[i]-centroid).length_squared) if centers else -1
        item=byrow.setdefault(str(row),{'row':row,'pairs':0,'points_world':[]})
        item['pairs']+=1
        if len(item['points_world'])<6:item['points_world'].append([round(x,6) for x in centroid])
    return {'object':obj.name,'overlap_pairs':len(overlaps),'row_assignment_method':'nearest posed base-cage row center to each overlapping subdivided polygon centroid','rows_with_crossings':byrow,
            'crossing_bbox_world':{'min':[min(p[i] for p in allcenters) for i in range(3)],'max':[max(p[i] for p in allcenters) for i in range(3)]} if allcenters else None}

def salve_targeted_hair_fix():
    scene=bpy.context.scene;original_frame=scene.frame_current
    scene.frame_set(161);bpy.context.view_layer.update()
    obj=bpy.data.objects['Mechon_largo_07.R'];keyname='PoseClearance_161'
    lines=[o for o in scene.objects if o.type=='MESH' and o.name.startswith('Surco_mechon_07_') and o.name.endswith('.R')]
    snapshot={o.name:[p.co.copy() for p in o.data.shape_keys.key_blocks[keyname].data] for o in [obj]+lines if o.data.shape_keys and o.data.shape_keys.key_blocks.get(keyname)}
    tree=_hc_body_tree();before=_hc_residuals(tree);locations_before=_tf_cross_rows(obj,tree)
    basis=obj.data.shape_keys.key_blocks[0]
    previous_max=max((p.co-basis.data[i].co).length for i,p in enumerate(obj.data.shape_keys.key_blocks[keyname].data))
    outcome={'frame':161,'object':obj.name,'iterations':16,'before':before,'crossing_rows_before':locations_before,'maximum_previous_local_shift_m':previous_max}
    if obj.name not in before['surface_crossings']:
        outcome['status']='unchanged; no measured surface crossing'
    else:
        stats=_hc_correct_leaf(obj,tree,161,iterations=16)
        stats['detail_lines_followed']=_hc_follow_lines(obj,stats,161)
        stats.pop('row_offsets',None)
        after=_hc_residuals(tree)
        accepted=(after['surface_crossings'].get(obj.name,0)<=before['surface_crossings'].get(obj.name,0)
                  and stats['maximum_local_shift_m']<=previous_max+.060)
        if not accepted:
            for name,coords in snapshot.items():
                ob=bpy.data.objects[name];kb=ob.data.shape_keys.key_blocks[keyname]
                for p,co in zip(kb.data,coords):p.co=co
                _hc_update(ob)
            after=_hc_residuals(tree)
            outcome['status']='previous key restored; attempt worsened crossing count or grew more than 60mm'
        else:
            outcome['status']='targeted key retained; visual review still required'
        outcome['attempt_stats']=stats;outcome['after']=after
        outcome['crossing_rows_after']=_tf_cross_rows(obj,tree)
    if 'after' not in outcome:outcome['after']=before;outcome['crossing_rows_after']=locations_before
    # Refresh only frame161 contact evidence; the FK library itself is untouched.
    body=_character_vertices(False);allcoords=_character_vertices(True)
    contact={'minimum_body_z_m':round(min(p.z for p in body),6),
             'minimum_all_z_m':round(min(p.z for p in allcoords),6),
             'floor_penetration_m':round(max(0,-min(p.z for p in allcoords)),6),
             'contact_regions':_contact_regions(),'hair_body_surface_check':check_salve_hair_surface_intersections()}
    outcome['contact_evidence_161']=contact
    out=_TF_WORK.parent/'outputs';(out/'poses').mkdir(exist_ok=True,parents=True)
    (out/'poses/Salve_cabello_correccion_dirigida_v02.json').write_text(json.dumps(outcome,ensure_ascii=False,indent=2),encoding='utf8')
    reportpath=out/'Salve_cabello_correctivos_v02.json'
    if reportpath.exists():
        report=json.loads(reportpath.read_text(encoding='utf8'))
        report['poses']['kneeling_variant']['after']=outcome['after']
        report['poses']['kneeling_variant']['targeted_refinement']=outcome
        reportpath.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    contactpath=out/'Salve_poses_contactos_v02.json'
    if contactpath.exists():
        report=json.loads(contactpath.read_text(encoding='utf8'))
        report['poses']['kneeling_variant'].update(contact)
        contactpath.write_text(json.dumps(report,ensure_ascii=False,indent=2),encoding='utf8')
    scene.frame_set(original_frame);_hc_update()
    print('SALVE_TARGETED_HAIR_DONE',outcome['status'],outcome['after']['surface_crossings'].get(obj.name,0),'pairs',flush=True)
    return outcome

if globals().get('SALVE_TARGETED_HAIR_AUTORUN',True):
    SALVE_HAIR_CLEARANCE_AUTORUN=False
    p=_TF_WORK/'hair_clearance_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'))
    SALVE_POSES_AUTORUN=False
    p=_TF_WORK/'poses_v02.py';exec(compile(p.read_text(encoding='utf8'),str(p),'exec'))
    salve_targeted_hair_result=salve_targeted_hair_fix()
