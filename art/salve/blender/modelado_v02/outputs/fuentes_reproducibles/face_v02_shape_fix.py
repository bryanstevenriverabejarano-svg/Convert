# Correct Blender's implicit mixed-shape capture without editing frozen source.
# Rebuild FACE only; head bones, body, hair, pose action and studio are preserved.
import bpy,pathlib
source_path=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2/work/face_v02.py')
fixed_source=source_path.read_text(encoding='utf-8-sig').replace('.shape_key_add(name=', '.shape_key_add(from_mix=False,name=')
old="if nm=='jawOpen':v.co.z+=(1 if dy>=0 else -1)*.0078*w*max(.15,1-(abs(x)/.019)**2)"
new="if nm=='jawOpen':v.co.z+=.0078*w*sin(2*pi*(idx%96)/96)"
assert old in fixed_source,'Frozen source mouth formula changed; audit before fixing'
fixed_source=fixed_source.replace(old,new)
old="if nm=='jawOpen':v.z=CZ+dy*(1+.0078/.00072)*max(.15,1-(abs(x)/.019)**2)"
new="if nm=='jawOpen':v.z=CZ+dy*(1+.0078/.00072)"
assert old in fixed_source,'Frozen source cavity formula changed; audit before fixing'
fixed_source=fixed_source.replace(old,new)
eye_start=fixed_source.index('def eye_rel(x,z,s):')
eye_end=fixed_source.index('# Mouth ring around a small aperture.')
ocular=fixed_source[eye_start:eye_end]
# Apply opening and tilt consistently to sockets, rims, white, iris and closure keys.
# Restrict replacements to ocular code so the .0062 nasal tip profile is preserved.
ocular=ocular.replace('.0079','.0095').replace('.0062','.0075').replace('.0083','.0098').replace('.105','.15')
iris_old="lim=.0076*sqrt(max(0,1-((x-cx)/.024)**2));z=max(cz-lim+s*(x-cx)*.15,min(cz+lim+s*(x-cx)*.15,z))"
iris_new="almond=sqrt(max(0,1-((x-cx)/.024)**2));lower=cz-.0072*almond+s*(x-cx)*.15;upper=cz+.0092*almond+s*(x-cx)*.15;z=max(lower,min(upper,z))"
assert iris_old in ocular,'Frozen source iris clamp changed; audit before polishing'
ocular=ocular.replace(iris_old,iris_new)
ocular=ocular.replace("(.0003+.00085*(i/(cols-1) if s==1 else 1-i/(cols-1))**.6)*(1 if upper else .35)","(.0003+.00110*(i/(cols-1) if s==1 else 1-i/(cols-1))**.6)*(1 if upper else .25)")
fixed_source=fixed_source[:eye_start]+ocular+fixed_source[eye_end:]
assert '((z-1.584)/.0062)**2' in fixed_source,'Nasal profile unexpectedly changed'
exec(compile(fixed_source,'face_v02_shape_fixed_in_memory','exec'),globals())
rig['face_shape_capture']='Basis explicita from_mix=False; jaw corners sin(angulo)'
rig['face_eye_polish']='Apertura superior9.5mm/inferior7.5mm, inclinacion0.15, iris clip9.2/7.2mm; centros/menton conservados'
bpy.context.scene.frame_set(1)
print('SALVE_FACE_SHAPES_REBUILT_FROM_EXPLICIT_BASIS')
# Keep visibility/deformation scales centered on the facial component.
from mathutils import Vector
CZ=1.560
for name in ['Dientes_superiores','Lengua','Lagrima.L','Lagrima.R']:
 ob=bpy.data.objects.get(name)
 if not ob:continue
 if ob.animation_data:ob.animation_data_clear()
 ob.scale=(1,1,1)
 coords=[v.co.copy() for v in ob.data.vertices]
 center=Vector(tuple((min(v[i] for v in coords)+max(v[i] for v in coords))/2 for i in range(3)))
 for v in ob.data.vertices:v.co-=center
 ob.location=center
 ob['facial_origin_centered']=True
 def drive_centered(path,index,expression,properties):
  fc=ob.driver_add(path,index);d=fc.driver;d.type='SCRIPTED'
  for vn,pn in properties:
   v=d.variables.new();v.name=vn;v.targets[0].id=rig;v.targets[0].data_path='["'+pn+'"]'
  d.expression=expression
 if name=='Dientes_superiores':
  drive_centered('scale',0,'min(1,jaw*8)*(1+.35*wide)',[('jaw','jawOpen'),('wide','mouthWide')])
  for axis in [1,2]:drive_centered('scale',axis,'min(1,jaw*8)',[('jaw','jawOpen')])
  drive_centered('location',2,'1.5608+.0042*jaw',[('jaw','jawOpen')])
 elif name=='Lengua':
  for axis in range(3):drive_centered('scale',axis,'min(1,jaw*2.2)',[('jaw','jawOpen')])
  drive_centered('location',2,'1.5582-.0040*jaw',[('jaw','jawOpen')])
 else:
  for axis in range(3):drive_centered('scale',axis,'tear',[('tear','tears')])
 ob.data.update()
scene.frame_set(1);rig.update_tag();bpy.context.view_layer.update()
print('FACIAL_COMPONENT_ORIGINS_AND_VISEME_TEETH_FIXED')
# Retag fresh object-driver dependencies after animation_data_clear in Blender 5.2.
for name in ['Dientes_superiores','Lengua','Lagrima.L','Lagrima.R']:
 ob=bpy.data.objects.get(name)
 if not ob or not ob.animation_data:continue
 for fc in ob.animation_data.drivers:
  for v in fc.driver.variables:
   v.targets[0].id_type='OBJECT';v.targets[0].id=rig
  fc.driver.expression=fc.driver.expression
scene.frame_set(1);rig.update_tag();bpy.context.view_layer.update()
print('FACIAL_ORIGIN_DRIVERS_DEPENDENCIES_RETAGGED')
