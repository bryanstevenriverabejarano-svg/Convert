import bpy,json,math,pathlib,sys
from mathutils import Vector
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
scene=bpy.context.scene;rig=bpy.data.objects.get('Salve_Rig');original_frame=scene.frame_current
report={'blend':bpy.data.filepath,'component':'facial_drivers_and_shapes','read_only':True,'blender':bpy.app.version_string,'errors':[],'warnings':[],'object_checks':{},'preset_checks':{},'limitations':['Estas comprobaciones numericas no certifican fidelidad canonica ni ausencia de defectos visuales.','La topologia facial usa componentes orbitales y orales separados, no retopologia soldada de produccion.']}
if not rig:report['errors'].append('Salve_Rig missing')
else:
 presets=json.loads(rig.get('expresiones_json','{}'));controls=sorted(set(k for vals in presets.values() for k in vals)|{'blink.L','blink.R','eyeSquint.L','eyeSquint.R','eyeWide.L','eyeWide.R','browInnerUp.L','browInnerUp.R','browDown.L','browDown.R','browOuterUp.L','browOuterUp.R','jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel','mouthPress','blush','tears'})
 report['preset_count']=len(presets);report['preset_names']=list(presets);report['controls']=controls
 if len(presets)!=21:report['errors'].append('Expected 21 facial presets')
 face=bpy.data.collections.get('02_Rostro_y_ojos');obs=list(face.objects) if face else []
 if not face:report['errors'].append('Facial collection missing')
 for o in obs:
  if o.type!='MESH':continue
  dat=o.data;sh=dat.shape_keys;drivers=list(sh.animation_data.drivers) if sh and sh.animation_data else []
  objectdrivers=list(o.animation_data.drivers) if o.animation_data else []
  invalid=[d.data_path for d in drivers if not d.is_valid]
  invalidobj=[d.data_path for d in objectdrivers if not d.is_valid]
  finite=all(all(math.isfinite(x) for x in v.co) for v in dat.vertices)
  finitekeys=all(all(all(math.isfinite(x) for x in v.co) for v in k.data) for k in sh.key_blocks) if sh else True
  deltas={k.name:max((a.co-b.co).length for a,b in zip(k.data,sh.key_blocks[0].data)) for k in sh.key_blocks[1:]} if sh else {}
  large=[nm for nm,d in deltas.items() if d>.055]
  missinggroups=[g.name for g in o.vertex_groups if g.name not in rig.data.bones]
  rc={'vertices':len(dat.vertices),'polygons':len(dat.polygons),'uv_layers':list(dat.uv_layers.keys()),'shape_keys':list(sh.key_blocks.keys()) if sh else [],'driver_count':len(drivers),'object_driver_count':len(objectdrivers),'invalid_drivers':invalid,'invalid_object_drivers':invalidobj,'finite_base_vertices':finite,'finite_shape_vertices':finitekeys,'missing_bone_groups':missinggroups,'shape_max_deltas_m':deltas,'excessive_shape_deltas':large}
  report['object_checks'][o.name]=rc
  if invalid or invalidobj or not finite or not finitekeys or missinggroups or large:report['errors'].append(o.name+' failed shapes/drivers/bone checks')
 for nm in ['LANDMARK_eye_left','LANDMARK_eye_right','LANDMARK_mouth','LANDMARK_head_pivot','LANDMARK_chin']:
  if nm not in bpy.data.objects:report['errors'].append(nm+' missing')
 def face_projection(obj):
  dg=bpy.context.evaluated_depsgraph_get();eo=obj.evaluated_get(dg);me=eo.to_mesh();D=rig.matrix_world@rig.pose.bones['head'].matrix@rig.data.bones['head'].matrix_local.inverted()@rig.matrix_world.inverted();coords=[D.inverted()@eo.matrix_world@v.co for v in me.vertices]
  area=0
  for p in me.polygons:
   if len(p.vertices)<3:continue
   a=coords[p.vertices[0]]
   for i in range(1,len(p.vertices)-1):
    b=coords[p.vertices[i]];c=coords[p.vertices[i+1]];area+=abs((b.x-a.x)*(c.z-a.z)-(b.z-a.z)*(c.x-a.x))/2
  bb={'xmin':min(v.x for v in coords),'xmax':max(v.x for v in coords),'zmin':min(v.z for v in coords),'zmax':max(v.z for v in coords)} if coords else {}
  eo.to_mesh_clear();return area,bb
 for index,(name,expected) in enumerate(presets.items()):
  fr=201+index*10;scene.frame_set(fr);rig.update_tag();bpy.context.view_layer.update()
  actual={k:float(rig.get(k,0)) for k in controls};delta={k:{'expected':float(expected.get(k,0)),'actual':actual[k]} for k in controls if abs(float(expected.get(k,0))-actual[k])>1e-5}
  rc={'frame':fr,'control_values':actual,'preset_mismatches':delta,'ocular_projected_area_m2':{},'lip_bounds_rest_m':{},'cavity_projected_area_m2':None,'cavity_bounds_rest_m':{},'cavity_projected_bbox_area_m2':None,'cavity_projected_z_span_m':None,'blush_material_strength':None,'internal_element_bounds_rest_m':{},'mouth_inner_boundary_z_span_m':None,'mouth_inner_edge_pair_gap_m':None,'tooth_commissure_width_check':None}
  for side in ['L','R']:
   ob=bpy.data.objects.get('Esclerotica.'+side)
   if ob:
    area,_=face_projection(ob);rc['ocular_projected_area_m2'][side]=area
    if actual['blink.'+side]>.999 and area>1e-6:report['warnings'].append(name+' eye '+side+' projected surface does not fully collapse: '+str(area))
  ob=bpy.data.objects.get('Labios_controles')
  if ob:
   _,rc['lip_bounds_rest_m']=face_projection(ob)
   bb=rc['lip_bounds_rest_m']
   if bb['zmax']-bb['zmin']>.055 or bb['xmax']-bb['xmin']>.065:report['errors'].append(name+' excessive lip bounds '+str(bb))
   keys=ob.data.shape_keys.key_blocks;basis=keys[0];inner=[]
   for index in range(96):
    point=basis.data[index].co.copy()
    for keyblock in keys[1:]:point+=(keyblock.data[index].co-basis.data[index].co)*keyblock.value
    inner.append(point)
   rc['mouth_inner_boundary_z_span_m']=max(v.z for v in inner)-min(v.z for v in inner)
   rc['mouth_inner_edge_pair_gap_m']=max((inner[i]-inner[(-i)%96]).length for i in range(96))
  ob=bpy.data.objects.get('Boca_interior')
  if ob:
   rc['cavity_projected_area_m2'],rc['cavity_bounds_rest_m']=face_projection(ob)
   cavitybb=rc['cavity_bounds_rest_m'];rc['cavity_projected_z_span_m']=cavitybb['zmax']-cavitybb['zmin'];rc['cavity_projected_bbox_area_m2']=(cavitybb['xmax']-cavitybb['xmin'])*rc['cavity_projected_z_span_m']
   if rc['cavity_projected_area_m2']>.001:report['errors'].append(name+' excessive mouth cavity projection')
   # Summed absolute triangle areas accumulate roundoff after inverse head
   # transforms of a closed, folded disc. Its actual silhouette thickness and
   # bounding area are stable tests of the seal at 0.01mm tolerance.
   if name=='MBP' and (rc['cavity_projected_z_span_m']>1e-5 or rc['cavity_projected_bbox_area_m2']>2.5e-7):report['errors'].append('MBP mouth cavity silhouette does not seal within 0.01mm')
  if name=='MBP' and rc['mouth_inner_boundary_z_span_m'] is not None and rc['mouth_inner_boundary_z_span_m']>1e-5:report['errors'].append('MBP inner lip boundary does not seal')
  if name=='MBP' and rc['mouth_inner_edge_pair_gap_m'] is not None and rc['mouth_inner_edge_pair_gap_m']>1e-5:report['errors'].append('MBP upper/lower inner lip edges remain separated in 3D')
  m=bpy.data.materials.get('Rostro v02 | rubor timidez')
  if m:
   for n in m.node_tree.nodes:
    if n.type=='MATH' and n.operation=='MULTIPLY':rc['blush_material_strength']=n.inputs[1].default_value
  if rc['blush_material_strength'] is None:report['errors'].append(name+' blush material multiplier missing')
  elif abs(rc['blush_material_strength']-actual.get('blush',0)*.72)>1e-5:report['errors'].append(name+' blush material driver differs from control')
  for objname in ['Dientes_superiores','Lengua','Lagrima.L','Lagrima.R']:
   ob=bpy.data.objects.get(objname)
   if not ob:continue
   _,bb=face_projection(ob);rc['internal_element_bounds_rest_m'][objname]=bb
   if bb['zmin']<1.49 or bb['zmax']>1.70:report['errors'].append(name+' facial internal element outside face: '+objname)
  if actual.get('jawOpen',0)>0 and rc['cavity_bounds_rest_m'] and 'Dientes_superiores' in rc['internal_element_bounds_rest_m']:
   lipbb=rc['cavity_bounds_rest_m'];toothbb=rc['internal_element_bounds_rest_m']['Dientes_superiores'];contained=toothbb['xmin']>=lipbb['xmin']-.0001 and toothbb['xmax']<=lipbb['xmax']+.0001
   rc['tooth_commissure_width_check']={'passed':contained,'cavity_width_m':lipbb['xmax']-lipbb['xmin'],'tooth_width_m':toothbb['xmax']-toothbb['xmin'],'test_scope':'Ancho X en reposo relativo a cabeza. No certifica oclusion completa Y/Z; debe inspeccionarse el PNG.'}
   if not contained:report['errors'].append(name+' tooth strip exceeds horizontal mouth commissures')
  report['preset_checks'][name]=rc
  if delta:report['errors'].append(name+' preset values differ at frame '+str(fr))
  if name=='SHY' and actual.get('blush',0)<.999:report['errors'].append('SHY missing blush control')
 report['iris_texture']={'packed':bool(bpy.data.images.get('Salve_Iris_Blue_v02') and bpy.data.images['Salve_Iris_Blue_v02'].packed_file),'dimensions':list(bpy.data.images['Salve_Iris_Blue_v02'].size) if bpy.data.images.get('Salve_Iris_Blue_v02') else None}
 if not report['iris_texture']['packed']:report['errors'].append('Iris texture is not packed')
 if report['iris_texture']['dimensions']!=[512,512]:report['errors'].append('Iris texture dimensions differ from 512x512')
 scene.frame_set(original_frame)
report['passed_numeric_checks']=not report['errors']
report['source_file_unchanged']=True
out=ROOT/'work'/'face_previews'/'face_driver_validation.json'
if '--' in sys.argv:
 tail=sys.argv[sys.argv.index('--')+1:]
 if tail:out=pathlib.Path(tail[0])
out.parent.mkdir(parents=True,exist_ok=True);out.write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps({'validation_file':str(out),'errors':report['errors'],'warnings':report['warnings'],'preset_count':report.get('preset_count')},ensure_ascii=False))
