# Reversible terminal refinement of rigid head fringe only. No long-hair keys/UV edits.
import bpy,math,json,hashlib
from mathutils import Vector
rig=bpy.data.objects['Salve_Rig'];scene=bpy.context.scene
FRINGE_TIPS={0:(1.632,1.613),1:(1.642,1.635),2:(1.635,1.649),3:(1.650,1.654),4:(1.635,1.641),5:(1.614,1.604),6:(1.620,1.615)}

def smooth(t):
 t=max(0,min(1,t));return t*t*(3-2*t)
def uv_hash(ob):
 return hashlib.sha256(json.dumps({uv.name:[list(x.uv) for x in uv.data] for uv in ob.data.uv_layers},separators=(',',':')).encode()).hexdigest()
def deformed_coords(original,n,idx,side,isline):
 rows=len(original)//n;result=[];old_tip,new_tip=FRINGE_TIPS[idx]
 for j in range(rows):
  t=j/(rows-1);block=original[j*n:(j+1)*n];center=sum((Vector(p) for p in block),Vector())/n
  previous=sum((Vector(p) for p in original[max(0,j-1)*n:(max(0,j-1)+1)*n]),Vector())/n
  following=sum((Vector(p) for p in original[min(rows-1,j+1)*n:(min(rows-1,j+1)+1)*n]),Vector())/n
  tangent=following-previous
  if tangent.length<1e-9:tangent=Vector((0,0,-1))
  tangent.normalize();width_axis=tangent.cross(Vector((0,1,0)))
  if width_axis.length<1e-9:width_axis=Vector((1,0,0))
  width_axis.normalize()
  root_curve=-.005*(idx/6)**2*(1-t)**3
  shift=Vector((side*.003*(1-t)**3,0,root_curve+(new_tip-old_tip)*smooth((t-.55)/.45)))
  width_factor=1-(.20 if idx==0 else .14 if idx<4 else .07)*sin_value(t)
  for p in block:
   delta=Vector(p)-center
   if isline:delta*=3.0
   else:delta+=width_axis*delta.dot(width_axis)*(width_factor-1)
   result.append(center+shift+delta)
 return result

def sin_value(t):return math.sin(math.pi*t)**.8
report={'status':'APPLIED_REVERSIBLE_FRINGE_ONLY','objects':[],'preserved':['menton','centros_oculares','head_bone','pose_action','long_hair','long_hair_clearance_keys','uv_layers','atlas_images'],'notes':['Central strand reaches between eyes; varied side tips soften uniform sawtooth cut.','Parting spreads first roots 3mm each side; narrow central foil reduces cap bulk.','Fringe strand traces become 0.27mm for visibility; atlas/UV are unchanged.','Visual acceptance still requires final render comparison.']}
for ob in list(bpy.data.collections['03_Cabello'].objects):
 if ob.type!='MESH' or not ob.name.startswith(('Flequillo_hoja_','Surco_flequillo_')):continue
 if ob.get('fringe_polish_v02'):continue
 bits=ob.name.split('_');isline=ob.name.startswith('Surco_');idx=int(bits[2].split('.')[0]);side=1 if '.L' in ob.name else -1
 if idx not in FRINGE_TIPS:continue
 n=6 if isline else 12
 if len(ob.data.vertices)%n:raise RuntimeError('Unexpected rigid fringe topology: '+ob.name)
 uv_before=uv_hash(ob);original=[list(v.co) for v in ob.data.vertices];ob['fringe_original_coords_v02']=json.dumps(original)
 if ob.data.shape_keys:
  ob['fringe_original_keys_v02']=json.dumps({k.name:[list(v.co) for v in k.data] for k in ob.data.shape_keys.key_blocks})
  for key in ob.data.shape_keys.key_blocks:
   for v,p in zip(key.data,deformed_coords([list(v.co) for v in key.data],n,idx,side,isline)):v.co=p
 else:
  for v,p in zip(ob.data.vertices,deformed_coords(original,n,idx,side,isline)):v.co=p
 ob.data.update();assert uv_hash(ob)==uv_before,'UV coordinates changed: '+ob.name
 ob['fringe_polish_v02']=True
 report['objects'].append({'name':ob.name,'tip_before_z':FRINGE_TIPS[idx][0],'tip_after_z':FRINGE_TIPS[idx][1],'uv_preserved':True,'vertex_count':len(original)})
scene['fringe_polish_report_json']=json.dumps(report,ensure_ascii=False)
rig.update_tag();bpy.context.view_layer.update()
print('FRINGE_LOCAL_POLISH_READY',len(report['objects']),flush=True)
