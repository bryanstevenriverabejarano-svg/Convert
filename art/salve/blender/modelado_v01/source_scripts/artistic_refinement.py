import bpy, math, pathlib
from mathutils import Vector
from math import sin,cos,pi
OUT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/outputs');scene=bpy.context.scene;scene.frame_set(31);rig=bpy.data.objects['Salve_Rig'];rig.hide_set(False)
HAIR=bpy.data.collections['03_Cabello'];FACE=bpy.data.collections['02_Rostro_y_ojos'];hw=bpy.data.materials['Cabello | blanco lavanda'];hl=bpy.data.materials['Cabello | sombreado de mechon']
def bind(o,bone):
    vg=o.vertex_groups.new(name=bone);vg.add(list(range(len(o.data.vertices))),1,'REPLACE');m=o.modifiers.new('Deformacion Salve','ARMATURE');m.object=rig;o.parent=rig
def create(name,vv,ff,col,mat):
    d=bpy.data.meshes.new(name);d.from_pydata(vv,[],ff);d.update();o=bpy.data.objects.new(name,d);col.objects.link(o);o.data.materials.append(mat)
    for p in d.polygons:p.use_smooth=True
    return o
for o in list(HAIR.objects):
    if o.name.startswith('Flequillo'):bpy.data.objects.remove(o,do_unlink=True)
# Curved leaves spread from a parting, with individually pointed tips.
for side,s in [('L',1),('R',-1)]:
    for i,(endx,endz) in enumerate(zip([.009,.025,.041,.054,.068,.078],[1.637,1.653,1.64,1.651,1.63,1.62])):
        points=[Vector((s*(.002+i*.002),-.003,1.751)),Vector((s*(.005+i*.009),-.04,1.731)),Vector((s*(endx*.75),-.065,1.688)),Vector((s*endx,-.065+(i*.001),endz))]
        vv=[];ff=[]
        for j in range(33):
            t=j/32
            p=points[0]*(1-t)**3+points[1]*(3*t*(1-t)**2)+points[2]*(3*t*t*(1-t))+points[3]*t**3
            width=.012*(sin(pi*t)**.65)+.00015
            for k in range(12):
                a=2*pi*k/12;vv.append((p.x+width*cos(a),p.y+(.003*sin(a)*(sin(pi*t)**.4)),p.z))
            if j:
                for k in range(12):a0=(j-1)*12+k;b=(j-1)*12+(k+1)%12;ff.append((a0,b,b+12,a0+12))
        ff.extend([tuple(reversed(range(12))),tuple(32*12+k for k in range(12))])
        o=create('Flequillo_hoja_%02d.'%i+side,vv,ff,HAIR,hw);bind(o,'head')
        m=o.modifiers.new('Suavizado','SUBSURF');m.levels=1;m.render_levels=1
    # Make eye openings and irises conform to the same almond silhouette.
    scl=bpy.data.objects['Esclerotica.'+side];cx=s*.029;cz=1.618
    for v in scl.data.vertices:
        relx=v.co.x-cx;v.co.z=cz+(v.co.z-cz-s*relx*.08)*.8+s*relx*.11
    iris=bpy.data.objects['Iris.'+side]
    for v in iris.data.vertices:v.co.z*=.79;v.co.y*=.45
    pupil=bpy.data.objects['Pupila.'+side]
    for v in pupil.data.vertices:v.co.z*=.74;v.co.y*=.5
    for stem in ['Parpado_superior','Parpado_inferior','Pestana']:
        ob=bpy.data.objects[stem+'.'+side]
        for kb in ob.data.shape_keys.key_blocks:
            for v in kb.data:
                relx=v.co.x-cx;v.co.z=cz+(v.co.z-cz-s*relx*.08)*.8+s*relx*.11
    # Blink shuts the complete opening, not just the upper half.
    cover=bpy.data.objects['Parpado_cierre.'+side]
    for idx,v in enumerate(cover.data.shape_keys.key_blocks['blink.'+side].data):
        j,k=divmod(idx,25);x=v.co.x;edge=cz+.011*math.sqrt(max(0,1-((x-cx)/.024)**2))+s*(x-cx)*.11
        lower=cz-.010*math.sqrt(max(0,1-((x-cx)/.024)**2))+s*(x-cx)*.11
        v.co.z=edge+(lower-edge)*j/8;v.co.y=-.073
# Teeth and tongue retract when the mouth is closed.
for name in ['Dientes_superiores','Lengua']:
    o=bpy.data.objects[name]
    for axis in range(3):
        d=o.driver_add('scale',axis).driver;d.type='SCRIPTED';v=d.variables.new();v.name='jaw';v.targets[0].id=rig;v.targets[0].data_path='["jawOpen"]';d.expression='min(1,jaw*3)'
    o.location.y+=.002
# Tear beads appear only via the tears control; they remain optional in other expressions.
rig['tears']=0.0;rig.id_properties_ui('tears').update(min=0,max=1)
mt=bpy.data.materials.new('Lagrimas | cristal');mt.use_nodes=True;p=next(n for n in mt.node_tree.nodes if n.type=='BSDF_PRINCIPLED');p.inputs['Base Color'].default_value=(.55,.76,1,1);p.inputs['Roughness'].default_value=.1;p.inputs['Transmission Weight'].default_value=.6
for side,s in [('L',1),('R',-1)]:
    bpy.ops.mesh.primitive_uv_sphere_add(segments=24,ring_count=16,location=(s*.046,-.054,1.598));o=bpy.context.object;o.name='Lagrima.'+side
    for c in list(o.users_collection):c.objects.unlink(o)
    FACE.objects.link(o)
    for v in o.data.vertices:v.co.x*=.0025;v.co.y*=.0018;v.co.z*=.009
    o.data.materials.append(mt);bind(o,'head')
    for axis in range(3):
        d=o.driver_add('scale',axis).driver;v=d.variables.new();v.name='tear';v.targets[0].id=rig;v.targets[0].data_path='["tears"]';d.expression='tear'
import json
presets=json.loads(rig['expresiones_json']);presets['CRY']['tears']=1;rig['expresiones_json']=json.dumps(presets)
for index,(name,values) in enumerate(presets.items()):rig['tears']=values.get('tears',0);rig.keyframe_insert(data_path='["tears"]',frame=201+index*10)
(OUT/'expresiones_controles.json').write_text(json.dumps(presets,indent=2),encoding='utf-8')
# Lengthen fingers and soften the fused boot surface.
body=bpy.data.objects['Salve_Traje_CONTINUO']
for v in body.data.vertices:
    if abs(v.co.x)>.34 and .83<v.co.z<.916:
        t=max(0,min(1,(.916-v.co.z)/.032));v.co.z-=.024*t;v.co.x+=math.copysign(.01*t,v.co.x)
vg=body.vertex_groups.new(name='Boot_smoothing')
for v in body.data.vertices:
    if v.co.z<.17:vg.add([v.index],1,'REPLACE')
sm=body.modifiers.new('Botas_suavizado','SMOOTH');sm.vertex_group=vg.name;sm.factor=.8;sm.iterations=18
# Smoothing belongs before armature so it operates on the rest surface.
bpy.context.view_layer.objects.active=body
bpy.ops.object.modifier_move_up(modifier=sm.name)
for o in bpy.data.collections['04_Tecnologia_y_circuitos'].objects:
    if o.name.startswith('Una_'):
        for v in o.data.vertices:v.co.z-=.024;v.co.x+=math.copysign(.01,v.co.x)
scene.frame_set(1);rig.hide_set(True)
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_modelado_v01.blend'))
print('ARTISTIC_REFINEMENT_SAVED')
