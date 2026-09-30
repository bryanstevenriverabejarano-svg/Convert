"""Reset joint correction keys from Basis and account for neutral bone orientation."""
import bpy,math
from math import exp
rig=bpy.data.objects['Salve_Rig']
for side,s in [('L',1),('R',-1)]:
 for stem,name,z,cx,rad,amp,b0,b1 in [
  ('Muslo_y_pierna_quad','Rodilla',.612,s*.071,.066,.16,'thigh','shin'),
  ('Muslo_y_pierna_quad','Cadera',.977,s*.095,.093,.09,'pelvis','thigh'),
  ('Brazo_quad','Codo',1.22,s*.235,.058,.13,'upper_arm','forearm'),
  ('Brazo_quad','Hombro',1.411,s*.14,.066,.08,'chest','upper_arm')]:
  ob=bpy.data.objects[stem+'.'+side];key=ob.data.shape_keys.key_blocks['Correctivo_'+name];basis=ob.data.shape_keys.key_blocks['Basis']
  for v,base in zip(key.data,basis.data):
   v.co=base.co;w=amp*exp(-((base.co.z-z)/rad)**2);v.co.x=cx+(base.co.x-cx)*(1+w);v.co.y=base.co.y*(1+w*.6)
  n0=b0 if b0 in ['pelvis','chest'] else b0+'.'+side;n1=b1+'.'+side
  neutral_angle=rig.data.bones[n0].matrix_local.to_quaternion().rotation_difference(rig.data.bones[n1].matrix_local.to_quaternion()).angle
  d=key.driver_add('value').driver;d.expression='max(0,min(1,abs(bend-'+str(float(neutral_angle))+')/1.8))'
  ob['correctivos_estado']='Correctivos regionales desde Basis, compensados por angulo neutral; revision artistica de pliegues pendiente'
bpy.context.view_layer.update()
print('BODY_CORRECTIVES_REPAIRED',flush=True)
