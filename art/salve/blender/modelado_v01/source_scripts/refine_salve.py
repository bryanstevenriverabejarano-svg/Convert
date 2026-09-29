import bpy, math, pathlib, json
from mathutils import Vector
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2');OUT=ROOT/'outputs'
scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];rig.hide_set(False)
BODY=bpy.data.collections['01_Cuerpo_y_traje'];TECH=bpy.data.collections['04_Tecnologia_y_circuitos'];FACE=bpy.data.collections['02_Rostro_y_ojos'];HAIR=bpy.data.collections['03_Cabello']
WHITE=bpy.data.materials['Traje | ceramica perla'];BLACK=bpy.data.materials['Paneles | grafito'];BLUE=bpy.data.materials['Circuitos | azul ionico']
def sphere(name,loc,scale,mat,col):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=32,ring_count=20,location=loc);o=bpy.context.object;o.name=name
    for c in list(o.users_collection):c.objects.unlink(o)
    col.objects.link(o)
    for v in o.data.vertices:v.co.x*=scale[0];v.co.y*=scale[1];v.co.z*=scale[2]
    o.data.materials.append(mat)
    for p in o.data.polygons:p.use_smooth=True
    return o
def bind(o,bone):
    g=o.vertex_groups.new(name=bone);g.add(list(range(len(o.data.vertices))),1,'REPLACE');m=o.modifiers.new('Deformacion','ARMATURE');m.object=rig;o.parent=rig
white_parts=[]
for o in list(BODY.objects):
    if o.type=='MESH' and not o.name.startswith('Cuello'):white_parts.append(o)
for side,s in [('L',1),('R',-1)]:
    white_parts.append(sphere('Union_hombro.'+side,(s*.135,0,1.421),(.052,.045,.053),WHITE,BODY))
    white_parts.append(sphere('Union_muneca.'+side,(s*.342,-.025,.997),(.022,.021,.035),WHITE,BODY))
    white_parts.append(sphere('Bota_tobillera.'+side,(s*.056,.002,.143),(.03,.033,.061),WHITE,BODY))
    white_parts.append(sphere('Bota_union.'+side,(s*.056,-.09,.079),(.034,.081,.037),WHITE,BODY))
    white_parts.append(sphere('Guante_union.'+side,(s*.389,-.031,.917),(.032,.017,.033),WHITE,BODY))
# Real watertight suit mesh replaces intersecting construction surfaces.
bpy.ops.object.select_all(action='DESELECT')
for o in white_parts:
    # Evaluate subdivisions in the rest position, before rebinding the continuous surface.
    for m in list(o.modifiers):
        if m.type=='ARMATURE':o.modifiers.remove(m)
    o.select_set(True)
bpy.context.view_layer.objects.active=white_parts[0]
bpy.ops.object.convert(target='MESH');bpy.ops.object.join();body=bpy.context.object;body.name='Salve_Traje_CONTINUO'
body.parent=None;body.vertex_groups.clear()
rm=body.modifiers.new('Fusion_continua','REMESH');rm.mode='VOXEL';rm.voxel_size=.0037;rm.use_smooth_shade=True
bpy.ops.object.modifier_apply(modifier=rm.name)
sm=body.modifiers.new('Suavizado_superficie','SMOOTH');sm.factor=.7;sm.iterations=4;bpy.ops.object.modifier_apply(modifier=sm.name)
dec=body.modifiers.new('Optimizar_superficie','DECIMATE');dec.ratio=.62;bpy.ops.object.modifier_apply(modifier=dec.name)
body['topologia']='Superficie voxel continua; retopologia de produccion pendiente'
data=json.loads((pathlib.Path('C:/Users/agred/.codex/.chatgpt-projects/g-p-6ab2c01aa8f8819193dceb05d9e78da7/salve_blender')/'skeleton.json').read_text())
segments={b['name']:(Vector(b['head']),Vector(b['tail'])) for b in data if b['deform'] and not b['name'].startswith(('hair_','eye.','jaw'))}
groups={n:body.vertex_groups.new(name=n) for n in segments}
def distance(p,a,b):
    ab=b-a;t=max(0,min(1,(p-a).dot(ab)/ab.length_squared));return (p-(a+ab*t)).length
for v in body.data.vertices:
    p=body.matrix_world@v.co
    allowed=[n for n in segments if not n.startswith(('thumb','index','middle','ring','pinky'))] if p.z>1.00 or abs(p.x)<.30 else list(segments)
    near=sorted((distance(p,*segments[n]),n) for n in allowed)[:4];w=[1/max(.004,d)**5 for d,n in near];total=sum(w)
    for wi,(_,n) in zip(w,near):groups[n].add([v.index],wi/total,'REPLACE')
mod=body.modifiers.new('Skinning | 4 influencias','ARMATURE');mod.object=rig;body.parent=rig

# Refine the face; the mouth sat behind the original chin surface.
mouth=bpy.data.objects['Labios_controles']
for kb in mouth.data.shape_keys.key_blocks:
    for v in kb.data:v.co.y-=.009
cav=bpy.data.objects['Boca_interior'];cav.location.y-=.009
for name in ['Dientes_superiores','Lengua']:bpy.data.objects[name].location.y-=.01
for name in ['Nariz_punta','Nariz_puente']:
    o=bpy.data.objects[name]
    for v in o.data.vertices:v.co.x*=.65;v.co.z*=.8;v.co.y*=.65
for side in ['L','R']:
    for stem in ['Iris','Pupila','Brillo']:
        o=bpy.data.objects[stem+'.'+side]
        for v in o.data.vertices:v.co.x*=1.22;v.co.z*=1.15
    for stem in ['Ceja','Pestana']:
        o=bpy.data.objects[stem+'.'+side]
        if o.data.shape_keys:
            for kb in o.data.shape_keys.key_blocks:
                for v in kb.data:v.co.y-=.006
    # Extend neck to chin, removing the visible black gap.
collar=bpy.data.objects['Cuello_y_collar_negro']
for v in collar.data.vertices:
    if v.co.z>1.49:v.co.z+=.021

# Break up the straight fringe with asymmetric tapered locks.
for o in HAIR.objects:
    if o.name.startswith('Flequillo'):
        for v in o.data.vertices:
            w=max(0,min(1,(1.72-v.co.z)/.075))
            v.co.z-=.014*w*math.exp(-(v.co.x/.018)**2)
            v.co.x+=.009*w*math.sin(v.co.x*45)
            v.co.y-=.005*w
    elif o.name.startswith('Mechon_largo'):
        idx=int(o.name.split('_')[-1].split('.')[0]);s=1 if o.name.endswith('L') else -1
        for v in o.data.vertices:
            t=max(0,min(1,(1.6-v.co.z)/.7))
            v.co.x+=s*.014*math.sin(t*math.pi*2+idx*.7)*t
            v.co.z-=.03*t*math.sin(idx*.63)
            v.co.y+=.008*math.sin(t*5+idx)

# Add broad fitted technological panels rather than isolated decorative lines.
for side,s in [('L',1),('R',-1)]:
    for name,loc,scale,bone in [('Hombro_inserto',(s*.154,-.031,1.395),(.016,.007,.038),'upper_arm.'+side),('Muslo_inserto',(s*.111,-.055,.899),(.014,.006,.069),'thigh.'+side),('Tobillo_inserto',(s*.075,-.02,.24),(.009,.008,.045),'shin.'+side),('Guante_inserto',(s*.377,-.049,.948),(.022,.005,.018),'hand.'+side)]:
        o=sphere(name+'.'+side,loc,scale,BLACK,TECH);bind(o,bone)
    o=sphere('Led_hombro.'+side,(s*.155,-.039,1.403),(.003,.001,.024),BLUE,TECH);bind(o,'upper_arm.'+side)

# UVs are available for future painting; present appearance uses editable procedural materials.
for o in scene.objects:
    if o.type=='MESH' and not o.data.uv_layers:
        uv=o.data.uv_layers.new(name='UV_modelado')
        xs=[v.co.x for v in o.data.vertices];zs=[v.co.z for v in o.data.vertices]
        if not xs:continue
        xmin,xmax=min(xs),max(xs);zmin,zmax=min(zs),max(zs)
        for loop in o.data.loops:
            v=o.data.vertices[loop.vertex_index].co
            uv.data[loop.index].uv=((v.x-xmin)/max(.00001,xmax-xmin),(v.z-zmin)/max(.00001,zmax-zmin))
        o['UV_estado']='Proyeccion inicial; desplegado de produccion pendiente'
for a in bpy.context.screen.areas:
    if a.type=='VIEW_3D':a.spaces.active.overlay.show_overlays=False
rig.hide_set(True)
bpy.ops.object.select_all(action='DESELECT');body.select_set(True);bpy.context.view_layer.objects.active=body
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_modelado_v01.blend'))
print('REFINED',len(body.data.vertices),'continuous suit vertices')
