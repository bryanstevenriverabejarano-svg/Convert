import bpy, json, math, pathlib,sys
from mathutils import Vector
OUT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/outputs');scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];cam=scene.camera
(OUT/'vistas_20').mkdir(exist_ok=True);(OUT/'expresiones').mkdir(exist_ok=True)
scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG';scene.render.film_transparent=False
views=[('front',1,0,0),('front_three_quarter',1,35,0),('right',1,90,0),('rear_right',1,135,0),('rear',1,180,0),('rear_three_quarter',1,225,0),('left',1,270,0),('front_opposite',1,325,0),('front_variant',1,25,0),('above',1,0,40),('below',1,0,-25),('a_front',31,0,0),('a_open_hair',61,0,0),('a_rear',31,180,0),('seated',91,20,8),('crouched',121,45,8),('kneeling',151,0,0),('kneeling_variant',161,90,8),('leaning',171,0,4),('leaning_variant',181,35,5)]
def bounds():
    dg=bpy.context.evaluated_depsgraph_get();points=[]
    for o in scene.objects:
        if o.type=='MESH' and o.name!='Suelo_estudio':
            eo=o.evaluated_get(dg);points.extend([eo.matrix_world@Vector(v) for v in eo.bound_box])
    lo=Vector(tuple(min(v[i] for v in points) for i in range(3)));hi=Vector(tuple(max(v[i] for v in points) for i in range(3)));return lo,hi
def camera(theta,elevation,target,scale):
    th=math.radians(theta);el=math.radians(elevation);cam.location=target+Vector((4*math.sin(th)*math.cos(el),-4*math.cos(th)*math.cos(el),4*math.sin(el)));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();cam.data.ortho_scale=scale
manifest=[]
for i,(name,frame,angle,elev) in enumerate(views if '--expressions-only' not in sys.argv else []):
    scene.frame_set(frame);bpy.context.view_layer.update();lo,hi=bounds();target=(lo+hi)/2
    scene.render.resolution_x=768;scene.render.resolution_y=1152
    if name=='kneeling_variant':scene.render.resolution_x=960;scene.render.resolution_y=800
    ratio=scene.render.resolution_x/scene.render.resolution_y
    height=hi.z-lo.z;width=max(hi.x-lo.x,hi.y-lo.y)
    scale=(max(height,width/ratio) if ratio<=1 else max(height*ratio,width))*1.18
    if elev:scale*=1.06
    camera(angle,elev,target,scale)
    bpy.data.objects['Suelo_estudio'].hide_render=name=='below'
    scene.render.filepath=str(OUT/'vistas_20'/f'{i+1:02d}_{name}.png');bpy.ops.render.render(write_still=True)
    manifest.append({'index':i+1,'reference_id':name,'frame':frame,'azimuth_deg':angle,'elevation_deg':elev,'ortho_scale':scale,'file':f'vistas_20/{i+1:02d}_{name}.png','status':'reconstruccion estilizada; comparar con original'})
    print('VIEW_DONE',i+1,name,flush=True)
if manifest:(OUT/'vistas_manifest.json').write_text(json.dumps(manifest,indent=2),encoding='utf-8')
bpy.data.objects['Suelo_estudio'].hide_render=False
scene.frame_set(1);bpy.context.view_layer.update()
action=rig.animation_data.action;rig.animation_data.action=None
presets=json.loads(rig['expresiones_json']);keys=sorted(set(k for values in presets.values() for k in values))
scene.render.resolution_x=600;scene.render.resolution_y=600
head=rig.pose.bones['head'];target=Vector((0,-.003,1.635));camera(0,0,target,.255)
for i,(name,values) in enumerate(presets.items()):
    for k in keys:rig[k]=float(values.get(k,0.0))
    rig.update_tag();scene.frame_set(2+i);bpy.context.view_layer.update()
    for object_name,key_name in [('Labios_controles','jawOpen'),('Parpado_superior.L','blink.L')]:
        assert abs(bpy.data.objects[object_name].data.shape_keys.key_blocks[key_name].value-values.get(key_name,0))<.001,(name,key_name)
    scene.render.filepath=str(OUT/'expresiones'/f'{i+1:02d}_{name}.png');bpy.ops.render.render(write_still=True)
    print('EXPRESSION_DONE',i+1,name,flush=True)
print('RENDER_PACKAGE_COMPLETE',flush=True)
