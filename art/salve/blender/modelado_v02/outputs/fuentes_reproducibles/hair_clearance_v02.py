"""Static, editable per-pose hair clearances; evaluated residuals are reported.

Run AFTER body, face and poses. Does not save or overwrite the previous model.
Numerical local Jacobians retain Blender's Preserve Volume armature deformation.
"""
import bpy, math, json, pathlib
from mathutils import Matrix,Vector
from mathutils.bvhtree import BVHTree

_HC_SCENE=bpy.context.scene
_HC_RIG=bpy.data.objects['Salve_Rig']
_HC_OUTPUT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2/outputs')
_HC_BASIS_REPORT={}

def _hc_smoothstep(a,b,v):
    t=max(0,min(1,(v-a)/(b-a)));return t*t*(3-2*t)

def prepare_salve_long_hair_basis():
    """Move long mass behind shoulders; facial locks retain their original placement."""
    global _HC_BASIS_REPORT
    report={}
    for obj in _HC_SCENE.objects:
        if obj.type!='MESH' or obj.hide_render or not obj.name.startswith(('Mechon_largo','Surco_mechon')):continue
        if obj.get('long_mass_behind_shoulders_v02'):continue
        layout=_hc_rows(obj.name,len(obj.data.vertices))
        if not layout:continue
        sides,rows=layout;count=0;maximum=0
        for row in range(rows):
            points=[obj.data.vertices[row*sides+k].co.copy() for k in range(sides)]
            center=sum(points,Vector())/sides
            weight=_hc_smoothstep(1.27,1.35,center.z)*(1-_hc_smoothstep(1.51,1.62,center.z))
            target=.084 if obj.name.startswith('Mechon_largo') else .078
            shift=max(0,target-center.y)*weight
            if shift<1e-6:continue
            count+=sides;maximum=max(maximum,shift)
            for k in range(sides):
                i=row*sides+k
                obj.data.vertices[i].co.y+=shift
                if obj.data.shape_keys:
                    for key in obj.data.shape_keys.key_blocks:key.data[i].co.y+=shift
        obj['rest_coords_v02']=json.dumps([list(v.co) for v in obj.data.vertices])
        obj['long_mass_behind_shoulders_v02']=True
        _hc_update(obj)
        report[obj.name]={'vertices_moved':count,'maximum_y_shift_m':round(maximum,6)}
    _HC_BASIS_REPORT=report
    print('HAIR_BASIS_POSTERIOR',len(report),'objects',flush=True)
    return report

def _hc_update(obj=None):
    if obj:
        obj.data.update();obj.update_tag()
    bpy.context.view_layer.update()

def _hc_base_positions(obj):
    dg=bpy.context.evaluated_depsgraph_get();ev=obj.evaluated_get(dg);me=ev.to_mesh()
    points=[ev.matrix_world@v.co for v in me.vertices];ev.to_mesh_clear()
    return points

def _hc_body_tree():
    dg=bpy.context.evaluated_depsgraph_get();verts=[];faces=[]
    for ob in _HC_SCENE.objects:
        if ob.type!='MESH' or ob.hide_render or not any(c.name.startswith('01_Cuerpo') for c in ob.users_collection):continue
        ev=ob.evaluated_get(dg);me=ev.to_mesh();offset=len(verts)
        verts.extend(ev.matrix_world@v.co for v in me.vertices)
        faces.extend(tuple(offset+i for i in p.vertices) for p in me.polygons)
        ev.to_mesh_clear()
    return BVHTree.FromPolygons(verts,faces,epsilon=.00001)

def _hc_jacobians(obj,basis,key):
    base=_hc_base_positions(obj);columns=[];eps=.001
    probe=obj.shape_key_add(name='__CLEARANCE_PROBE__');probe.value=0
    for axis in range(3):
        offset=Vector((0,0,0));offset[axis]=eps
        for i,p in enumerate(probe.data):p.co=basis.data[i].co+offset
        probe.value=1;_hc_update(obj);posed=_hc_base_positions(obj)
        columns.append([(posed[i]-base[i])/eps for i in range(len(base))])
        probe.value=0;_hc_update(obj)
    obj.shape_key_remove(probe)
    jacobians=[]
    for i in range(len(base)):
        mat=Matrix((columns[0][i],columns[1][i],columns[2][i])).transposed()
        jacobians.append(mat.inverted_safe())
    return jacobians

def _hc_rows(name,count):
    sides=12 if name.startswith(('Mechon_largo','Mechon_rostro')) else 6
    if count%sides:return None
    return sides,count//sides

def _hc_correct_leaf(obj,tree,frame,clearance=.020,iterations=6):
    info=_hc_rows(obj.name,len(obj.data.vertices))
    if not info:return {'status':'unsupported vertex layout'}
    sides,rows=info
    if not obj.data.shape_keys:obj.shape_key_add(name='Basis')
    basis=obj.data.shape_keys.key_blocks[0]
    name='PoseClearance_%03d'%frame
    key=obj.data.shape_keys.key_blocks.get(name)
    if key is None:key=obj.shape_key_add(name=name)
    try:key.driver_remove('value')
    except TypeError:pass
    for i,p in enumerate(key.data):p.co=basis.data[i].co
    key.value=1
    modifiers=[(m,m.show_viewport,m.show_render) for m in obj.modifiers if m.type=='SUBSURF']
    for m,_,_ in modifiers:m.show_viewport=False;m.show_render=False
    _hc_update(obj)
    invj=_hc_jacobians(obj,basis,key)
    changes=0;max_shift=0
    side=1 if obj.name.endswith('.L') else -1
    strand=obj.get('cabello_cadena','front' if obj.name.startswith('Mechon_rostro') else 'rear')
    preferred=Vector((side*1.0,.22,0)) if strand=='front' else Vector((side*.30,1.0,0))
    preferred.normalize()
    for iteration in range(iterations):
        posed=_hc_base_positions(obj)
        if len(posed)!=len(key.data):break
        offsets=[]
        for row in range(rows):
            pts=posed[row*sides:(row+1)*sides];best=Vector((0,0,0));strongest=0;exit_shift=0
            # Root rows are intentionally anchored under the crown.
            if row>=3:
                for p in pts:
                    nearest=tree.find_nearest(p)
                    if nearest[0] is None:continue
                    hit,normal,_,distance=nearest;separation=(p-hit).dot(normal)
                    if distance<.25 and separation<clearance:
                        if separation<-.001:
                            # Consistent motion prevents rings alternating around a limb.
                            exit_hit=tree.ray_cast(p,preferred,.6)
                            if exit_hit[0] is not None:exit_shift=max(exit_shift,exit_hit[3]+clearance)
                            else:exit_shift=max(exit_shift,distance+clearance)
                        else:
                            disp=(hit+normal*clearance)-p
                            if disp.length>strongest:best=disp;strongest=disp.length
                if exit_shift:best=preferred*exit_shift
                best.z=max(best.z,clearance-min(p.z for p in pts))
                if best.length>.08:best.normalize();best*=.08
            offsets.append(best)
        # Smooth shifts over adjacent rows to retain lock curvature and ring width.
        for _ in range(2):
            offsets=[Vector((0,0,0)) if i<3 else (offsets[max(3,i-1)]*.20+v*.60+offsets[min(rows-1,i+1)]*.20) for i,v in enumerate(offsets)]
        for row in range(3,rows):
            pts=posed[row*sides:(row+1)*sides]
            offsets[row].z=max(offsets[row].z,clearance-min(p.z for p in pts))
            if offsets[row].length>.0002:changes+=1
            for k in range(sides):
                i=row*sides+k;delta=invj[i]@offsets[row]
                total=key.data[i].co-basis.data[i].co+delta
                if total.length>.28:total.normalize();total*=.28
                key.data[i].co=basis.data[i].co+total
                max_shift=max(max_shift,total.length)
        _hc_update(obj)
        if max((d.length for d in offsets),default=0)<.0002:break
    local_offsets=[]
    for row in range(rows):
        local_offsets.append(sum((key.data[row*sides+k].co-basis.data[row*sides+k].co for k in range(sides)),Vector())/sides)
    for m,visible,render in modifiers:m.show_viewport=visible;m.show_render=render
    expression=('1.0 if frame in '+str(tuple([1]+[f for f in range(201,402,10) if f!=311]))+' else 0.0') if frame==1 else f'1.0 if frame == {frame} else 0.0'
    driver=key.driver_add('value').driver;driver.type='SCRIPTED';driver.expression=expression
    obj['clearance_estado']='Correctivos estaticos editables por pose; validar residuales BVH y visuales'
    _hc_update(obj)
    return {'corrected_row_iterations':changes,'maximum_local_shift_m':round(max_shift,5),'key':name,'row_offsets':local_offsets,'rows':rows,'sides':sides}

def _hc_follow_lines(leaf,stats,frame):
    if not leaf.name.startswith('Mechon_largo'):return 0
    ident=leaf.name[len('Mechon_largo_'):].split('.')[0];side=leaf.name.rsplit('.',1)[1]
    lines=[o for o in _HC_SCENE.objects if o.type=='MESH' and o.name.startswith('Surco_mechon_'+ident+'_') and o.name.endswith('.'+side)]
    row_offsets=stats.get('row_offsets',[])
    if not row_offsets:return 0
    for obj in lines:
        layout=_hc_rows(obj.name,len(obj.data.vertices))
        if not layout:continue
        sides,rows=layout
        if not obj.data.shape_keys:obj.shape_key_add(name='Basis')
        basis=obj.data.shape_keys.key_blocks[0];name='PoseClearance_%03d'%frame
        key=obj.data.shape_keys.key_blocks.get(name)
        if key is None:key=obj.shape_key_add(name=name)
        try:key.driver_remove('value')
        except TypeError:pass
        for row in range(rows):
            t=row/(rows-1)*(len(row_offsets)-1);j=min(len(row_offsets)-2,int(t));f=t-j
            shift=row_offsets[j]*(1-f)+row_offsets[j+1]*f
            for k in range(sides):
                i=row*sides+k;key.data[i].co=basis.data[i].co+shift
        d=key.driver_add('value').driver;d.type='SCRIPTED'
        d.expression=('1.0 if frame in '+str(tuple([1]+[f for f in range(201,402,10) if f!=311]))+' else 0.0') if frame==1 else f'1.0 if frame == {frame} else 0.0'
        _hc_update(obj)
    return len(lines)

def _hc_residuals(tree):
    dg=bpy.context.evaluated_depsgraph_get();crossings={};mins={};tested=0
    for obj in _HC_SCENE.objects:
        if obj.type!='MESH' or obj.hide_render or not obj.name.startswith(('Mechon_largo','Mechon_rostro')):continue
        ev=obj.evaluated_get(dg);me=ev.to_mesh();points=[ev.matrix_world@v.co for v in me.vertices];faces=[tuple(p.vertices) for p in me.polygons];ev.to_mesh_clear()
        if not faces:continue
        tested+=1;hairtree=BVHTree.FromPolygons(points,faces,epsilon=.00001);overlap=tree.overlap(hairtree)
        if overlap:crossings[obj.name]=len(overlap)
        mins[obj.name]=min(p.z for p in points)
    return {'tested_leaves':tested,'surface_crossings':crossings,'objects_with_crossings':len(crossings),'hair_minimum_z_m':round(min(mins.values()),6),'leaf_minimum_z_m':{n:round(z,6) for n,z in mins.items()},'floor_penetrating_leaves':{n:round(z,6) for n,z in mins.items() if z<0}}

def bake_salve_hair_clearance(frames=None):
    if frames is None:
        frames=json.loads(_HC_RIG['poses_v02_json'])
    leaves=[o for o in _HC_SCENE.objects if o.type=='MESH' and not o.hide_render and o.name.startswith(('Mechon_largo','Mechon_rostro'))]
    report={'method':'Static shape keys, row shifts; armature Preserve Volume numerical Jacobian; suit BVH surface crossings','basis_long_mass_adjustments':_HC_BASIS_REPORT,'limitations':['Surface crossings miss wholly enclosed volumes and exclude intentional hair/scalp contact.','Strand-detail lines follow nearby leaf cage offsets; exact glued alignment still requires visual review.','Static per-frame correction, no physical simulation or animation certification.'],'poses':{}}
    for pose,frame in frames.items():
        _HC_SCENE.frame_set(frame);_hc_update();tree=_hc_body_tree();before=_hc_residuals(tree);stats={}
        for leaf_index,leaf in enumerate(leaves):
            if leaf.name not in before['surface_crossings'] and before['leaf_minimum_z_m'].get(leaf.name,1)>.020:
                stats[leaf.name]={'status':'unchanged; no measured crossing or floor clearance deficit'}
                continue
            s=_hc_correct_leaf(leaf,tree,frame)
            s['detail_lines_followed']=_hc_follow_lines(leaf,s,frame)
            s.pop('row_offsets',None);stats[leaf.name]=s
            if leaf_index%10==0:print('HAIR_CLEARANCE_LEAF',pose,leaf_index+1,len(leaves),leaf.name,flush=True)
        _HC_SCENE.frame_set(frame);_hc_update();after=_hc_residuals(tree)
        report['poses'][pose]={'frame':frame,'before':before,'after':after,'leaves':stats}
        print('HAIR_CLEARANCE_POSE',pose,json.dumps({'before_crossings':before['objects_with_crossings'],'after_crossings':after['objects_with_crossings'],'before_min_z':before['hair_minimum_z_m'],'after_min_z':after['hair_minimum_z_m']}),flush=True)
        _HC_OUTPUT.mkdir(exist_ok=True,parents=True)
        (_HC_OUTPUT/'Salve_cabello_correctivos_v02.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf8')
    _HC_SCENE.frame_set(1);_hc_update()
    return report

if globals().get('SALVE_HAIR_CLEARANCE_AUTORUN',False):
    salve_hair_clearance_report=bake_salve_hair_clearance()

