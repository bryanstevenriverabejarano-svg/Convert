"""Render calibrated static views and export camera-derived application landmarks."""
import bpy, math, json, pathlib, hashlib, sys, numpy as np
from mathutils import Vector
from bpy_extras.object_utils import world_to_camera_view
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
OUT=ROOT/'outputs'
s=bpy.data.scenes['04_SALVE_MODELO_3D'];bpy.context.window.scene=s;rig=bpy.data.objects['Salve_Rig']
refs=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/work/Convert-salve-publicacion/art/salve/blender/references')
view_specs=[
 ('front',1,0,0,'ORTHO',4),('front_three_quarter',11,34,0,'PERSP',4.8),('right',1,90,0,'ORTHO',4),
 ('rear_right',1,135,0,'PERSP',4.8),('rear',1,180,0,'ORTHO',4),('rear_three_quarter',21,224,0,'PERSP',4.8),
 ('left',1,270,0,'ORTHO',4),('front_opposite',21,329,0,'PERSP',4.8),('front_variant',41,24,0,'PERSP',4.8),
 ('above',1,0,47,'PERSP',2.7),('below',1,0,-22,'PERSP',2.7),('a_front',31,0,0,'ORTHO',4),
 ('a_open_hair',61,0,0,'ORTHO',4),('a_rear',31,180,0,'ORTHO',4),('seated',91,8,8,'PERSP',3.8),
 ('crouched',121,40,8,'PERSP',3.5),('kneeling',151,32,4,'PERSP',3.5),('kneeling_variant',161,64,8,'PERSP',3.5),
 ('leaning',171,0,4,'PERSP',4.5),('leaning_variant',181,34,6,'PERSP',4.2)]

def visible_character_objects():
 for o in s.objects:
  if o.type=='MESH' and not o.hide_render and any(c.name.startswith(('01_Cuerpo','02_Rostro','03_Cabello','04_Tecnologia')) and not c.hide_render for c in o.users_collection):yield o

degenerate_bounds_log={}
def world_points():
 dg=bpy.context.evaluated_depsgraph_get();points=[];collapsed=[]
 for o in visible_character_objects():
  eo=o.evaluated_get(dg);me=eo.to_mesh();coords=[eo.matrix_world@v.co for v in me.vertices];eo.to_mesh_clear()
  if not coords:continue
  # Hidden internal facial pieces have zero object scale. Their evaluated
  # vertices collapse at the rest origin and must not frame a posed body.
  array=np.asarray(coords,dtype=np.float64)
  if float(np.linalg.norm(np.ptp(array,axis=0)))<1e-7:
   collapsed.append({'object':o.name,'collapsed_world_point':list(coords[0])});continue
  points.extend(coords)
 degenerate_bounds_log[str(s.frame_current)]=collapsed
 return points

def support_points():
 dg=bpy.context.evaluated_depsgraph_get();points=[]
 for o in visible_character_objects():
  if not any(c.name.startswith(('01_Cuerpo','04_Tecnologia')) for c in o.users_collection):continue
  eo=o.evaluated_get(dg);me=eo.to_mesh();points.extend(eo.matrix_world@v.co for v in me.vertices);eo.to_mesh_clear()
 assert points,'No support geometry'
 minimum=min(p.z for p in points)
 # Body/boot/glove samples within 3mm of the physical contact plane.
 # Hair and internal facial pieces cannot define the flat 2D ground line.
 contacts=[p for p in points if p.z<=minimum+.003]
 assert minimum>=-.0002,(s.frame_current,'Body penetrates physical floor',minimum)
 return contacts,minimum

def projected_bounds(cam,points):
 inverse=np.asarray(cam.matrix_world.normalized().inverted(),dtype=np.float64)
 xyz=np.asarray(points,dtype=np.float64);local=xyz@inverse[:3,:3].T+inverse[:3,3]
 frame=list(cam.data.view_frame(scene=s))
 if cam.data.type!='ORTHO':
  frame=[v/(-v.z) for v in frame];local[:,:2]/=np.maximum(1e-9,-local[:,2,None])
 xmin=min(v.x for v in frame);xmax=max(v.x for v in frame);ymin=min(v.y for v in frame);ymax=max(v.y for v in frame)
 xx=(local[:,0]-xmin)/(xmax-xmin);yy=(local[:,1]-ymin)/(ymax-ymin)
 check=world_to_camera_view(s,cam,points[0]);assert abs(check.x-xx[0])<1e-5 and abs(check.y-yy[0])<1e-5,'Projection mismatch'
 return (float(xx.min()),float(yy.min()),float(xx.max()),float(yy.max()))

def create_cam(name,angle,elev,mode,dist,points,target_bbox):
 data=bpy.data.cameras.new('CAM_'+name);ob=bpy.data.objects.new('CAM_'+name,data);bpy.data.collections['06_Estudio'].objects.link(ob)
 assert mode in [e.identifier for e in data.bl_rna.properties['type'].enum_items]
 data.type=mode;data.lens=70;data.ortho_scale=1.9;data.clip_start=.01;data.clip_end=100
 lo=Vector(tuple(min(p[i] for p in points) for i in range(3)));hi=Vector(tuple(max(p[i] for p in points) for i in range(3)))
 target=(lo+hi)/2;theta=math.radians(angle);el=math.radians(elev);out=Vector((math.sin(theta)*math.cos(el),-math.cos(theta)*math.cos(el),math.sin(el)))
 q=(-out).to_track_quat('-Z','Y');right=q@Vector((1,0,0));up=q@Vector((0,1,0));ob.rotation_euler=q.to_euler()
 # Reference alpha placement drives framing, with a minimum border for all visible mesh.
 rx0,ry0,rx1,ry1=target_bbox
 rx0=max(.013,rx0);rx1=min(.987,rx1);ry0=max(.013,ry0);ry1=min(.987,ry1)
 wantw=max(.2,rx1-rx0);wanth=max(.2,ry1-ry0);cx=(rx0+rx1)/2;cy=1-(ry0+ry1)/2
 for step in range(5):
  ob.location=target+out*dist;bpy.context.view_layer.update()
  x0,y0,x1,y1=projected_bounds(ob,points);factor=max((x1-x0)/wantw,(y1-y0)/wanth)
  if mode=='ORTHO':data.ortho_scale*=factor
  else:dist*=factor
  ob.location=target+out*dist;bpy.context.view_layer.update()
  x0,y0,x1,y1=projected_bounds(ob,points)
  frame=list(data.view_frame(scene=s))
  if mode!='ORTHO':frame=[v/(-v.z)*dist for v in frame]
  vertical=max(v.y for v in frame)-min(v.y for v in frame)
  horizontal=vertical*s.render.resolution_x/s.render.resolution_y
  target+=right*((x0+x1)/2-cx)*horizontal+up*((y0+y1)/2-cy)*vertical
 ob.location=target+out*dist;bpy.context.view_layer.update()
 return ob

def landmark(nm):
 o=bpy.data.objects.get('LANDMARK_'+nm)
 if o:
  eo=o.evaluated_get(bpy.context.evaluated_depsgraph_get());me=eo.to_mesh();p=eo.matrix_world@me.vertices[0].co;eo.to_mesh_clear();return p
 return rig.matrix_world@rig.pose.bones['head'].head

def pixel(cam,p):
 v=world_to_camera_view(s,cam,p)
 return [round(v.x*s.render.resolution_x,2),round((1-v.y)*s.render.resolution_y,2)]

def export_landmarks(cam,path,frame,standing_height,standing_alpha_height_px):
 a=pixel(cam,landmark('eye_left'));b=pixel(cam,landmark('eye_right'));left,right=sorted([a,b],key=lambda v:v[0])
 pelvis=rig.matrix_world@rig.pose.bones['pelvis'].head
 contacts,minimum=support_points();support_pixels=[pixel(cam,Vector((p.x,p.y,0))) for p in contacts]
 bottom=max(v[1] for v in support_pixels)
 # Preserve a central horizontal pivot while anchoring Canvas at the lowest
 # projected support; several perspective contacts do not share screen Y.
 ground_pixel=[round((min(v[0] for v in support_pixels)+max(v[0] for v in support_pixels))/2,2),bottom]
 joints={}
 for label,bone,where in [('Shoulder','upper_arm','head'),('Elbow','forearm','head'),('Palm','hand','midpoint'),('Hip','thigh','head'),('Knee','shin','head'),('Ankle','foot','head')]:
  values=[pixel(cam,rig.matrix_world@((rig.pose.bones[bone+'.'+side].head+rig.pose.bones[bone+'.'+side].tail)/2 if where=='midpoint' else getattr(rig.pose.bones[bone+'.'+side],where))) for side in ['L','R']]
  lv,rv=sorted(values,key=lambda v:v[0]);joints['left'+label]=lv;joints['right'+label]=rv
 points=world_points();box=projected_bounds(cam,points);physical_height=max(p.z for p in points)-min(p.z for p in points);alpha_height_px=(box[3]-box[1])*s.render.resolution_y
 return {'sourceSha256':hashlib.sha256(path.read_bytes()).hexdigest(),'frame':frame,'width':s.render.resolution_x,'height':s.render.resolution_y,
  'leftEye':left,'rightEye':right,'mouth':pixel(cam,landmark('mouth')),'headPivot':pixel(cam,landmark('head_pivot')),
  'bodyPivot':pixel(cam,pelvis),'groundAnchor':ground_pixel,'groundAnchorMethod':{'scope':'2D central X of body/boot/glove support span and maximum projected Y at z=0; hair excluded','minimumBodyZ_m':minimum,'contactSamples':len(contacts),'thresholdAboveMinimum_m':.003},'scaleToStanding':round(standing_alpha_height_px/max(1,alpha_height_px)*physical_height/standing_height,5),
  'joints':joints,'semantic':'left/right sorted by screen x, not anatomical Blender .L/.R','visualCheck':'pending','occlusionCheck':'pending'}

def render_expressions():
 presets=json.loads(rig['expresiones_json']);expr=[]
 for i,(name,vals) in enumerate(presets.items()):
  frame=201+i*10;s.frame_set(frame);rig.update_tag();bpy.context.view_layer.update()
  s.render.resolution_x=768;s.render.resolution_y=768
  target=(landmark('chin')+Vector((0,0,.105)))
  data=bpy.data.cameras.new('CAM_FACE_'+name);cam=bpy.data.objects.new('CAM_FACE_'+name,data);bpy.data.collections['06_Estudio'].objects.link(cam)
  data.type='ORTHO';data.ortho_scale=.32;cam.location=target+Vector((0,-4,0));cam.rotation_euler=(target-cam.location).to_track_quat('-Z','Y').to_euler();s.camera=cam
  path=OUT/'expresiones'/f'{i+1:02d}_{name}.png';s.render.filepath=str(path);bpy.ops.render.render(write_still=True)
  expr.append({'id':name,'frame':frame,'file':'expresiones/'+path.name,'values':vals,'camera':cam.name,'size':[768,768],'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
  (OUT/'expresiones_controles.json').write_text(json.dumps(expr,indent=2,ensure_ascii=False),encoding='utf-8')
  print('EXPRESSION_DONE',i+1,name,flush=True)
 return expr

def main():
 (OUT/'vistas_20').mkdir(exist_ok=True);(OUT/'expresiones').mkdir(exist_ok=True)
 s.render.resolution_percentage=100;s.render.image_settings.file_format='PNG';s.render.image_settings.color_mode='RGBA';s.render.film_transparent=True
 for o in s.objects:
  if o.name=='Suelo_estudio':o.hide_render=True
 try:s.eevee.taa_render_samples=64
 except:pass
 refboxes=json.loads((ROOT/'work/reference_metrics.json').read_text(encoding='utf-8'))
 indices=set()
 if '--indices' in sys.argv:indices=set(int(x) for x in sys.argv[sys.argv.index('--indices')+1].split(','))
 # Facial proof appears first so coverage, closure and visemes can be reviewed
 # while the twenty full-body views are being rendered, without duplicate renders.
 if not indices:render_expressions()
 records=json.loads((OUT/'vistas_manifest.json').read_text(encoding='utf-8')) if indices else []
 landmarks=json.loads((OUT/'renders_landmarks.json').read_text(encoding='utf-8')) if indices else {'coordinateSystem':'pixels_top_left','views':{}}
 s.frame_set(1);bpy.context.view_layer.update()
 points=world_points();standing_height=max(p.z for p in points)-min(p.z for p in points);standing_alpha_height_px=1490.0
 if indices and bpy.data.objects.get('CAM_front'):
  bb=projected_bounds(bpy.data.objects['CAM_front'],points);standing_alpha_height_px=(bb[3]-bb[1])*1536
 for i,(name,frame,angle,elev,mode,dist) in enumerate(view_specs,1):
  if indices and i not in indices:continue
  s.frame_set(frame);bpy.context.view_layer.update();points=world_points()
  size=(1374,1145) if name=='kneeling_variant' else (1024,1536)
  s.render.resolution_x,s.render.resolution_y=size
  bbox=refboxes[name]['alpha_bbox_normalized']
  cam=bpy.data.objects.get('CAM_'+name)
  if cam and '--refresh-cameras' in sys.argv:
   data=cam.data;bpy.data.objects.remove(cam,do_unlink=True)
   if not data.users:bpy.data.cameras.remove(data)
   cam=None
  cam=cam or create_cam(name,angle,elev,mode,dist,points,bbox);s.camera=cam
  path=OUT/'vistas_20'/f'{i:02d}_{name}.png';s.render.filepath=str(path);bpy.ops.render.render(write_still=True)
  if name=='front':standing_alpha_height_px=(projected_bounds(cam,points)[3]-projected_bounds(cam,points)[1])*s.render.resolution_y
  landmarks['views'][name]=export_landmarks(cam,path,frame,standing_height,standing_alpha_height_px)
  coords=projected_bounds(cam,points)
  records=[r for r in records if r['id']!=name]
  records.append({'index':i,'id':name,'frame':frame,'file':'vistas_20/'+path.name,'camera':cam.name,'projection':mode,'azimuth_deg':angle,'elevation_deg':elev,'size':size,'mesh_bbox_normalized_bottom_left':coords,'reference_sha256':refboxes[name]['sha256'],'render_sha256':landmarks['views'][name]['sourceSha256'],'visual_acceptance':'pending'})
  records.sort(key=lambda r:r['index'])
  (OUT/'vistas_manifest.json').write_text(json.dumps(records,indent=2,ensure_ascii=False),encoding='utf-8')
  (OUT/'renders_landmarks.json').write_text(json.dumps(landmarks,indent=2,ensure_ascii=False),encoding='utf-8')
  print('VIEW_DONE',i,name,flush=True)
 # Refresh support semantics for all preserved images, without rerendering
 # unaffected neutral/body views. Their image hashes remain unchanged.
 if indices:
  for row in records:
   s.frame_set(row['frame']);bpy.context.view_layer.update();s.render.resolution_x,s.render.resolution_y=row['size']
   landmarks['views'][row['id']]=export_landmarks(bpy.data.objects[row['camera']],OUT/row['file'],row['frame'],standing_height,standing_alpha_height_px)
  (OUT/'renders_landmarks.json').write_text(json.dumps(landmarks,indent=2,ensure_ascii=False),encoding='utf-8')
 (OUT/'poses/Salve_exclusion_envolvente_camara_v02.json').write_text(json.dumps({'reason':'Zero-size evaluated meshes cannot define camera or character bounds','frames':degenerate_bounds_log},indent=2,ensure_ascii=False),encoding='utf-8')
 s.frame_set(1);s.camera=bpy.data.objects['CAM_front'];s.render.resolution_x=1024;s.render.resolution_y=1536
 bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_refinado_v02.blend'))
 print('RENDER_V02_COMPLETE',flush=True)

if __name__=='__main__':main()
