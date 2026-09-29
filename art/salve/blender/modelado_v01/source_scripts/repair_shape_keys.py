import bpy,math,pathlib,json
scene=bpy.context.scene;scene.frame_set(1);rig=bpy.data.objects['Salve_Rig']
def basis_copy(ob,key):
    basis=ob.data.shape_keys.key_blocks['Basis']
    for a,b in zip(key.data,basis.data):a.co=b.co.copy()
ob=bpy.data.objects['Labios_controles']
for name in ['jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel']:
    key=ob.data.shape_keys.key_blocks[name];basis_copy(ob,key)
    for v in key.data:
        x,y,z=v.co;dy=z-1.56;sgn=1 if dy>=0 else -1
        if name=='jawOpen':v.co.z+=sgn*.009*(1-(abs(x)/.025)**2)
        elif name=='smile':v.co.x*=1.3;v.co.z+=.006*(abs(x)/.023)**2
        elif name=='frown':v.co.z-=.004*(abs(x)/.023)**2
        elif name=='mouthWide':v.co.x*=1.35
        elif name=='lipPucker':v.co.x*=.57;v.co.y-=.004;v.co.z=1.56+dy*2
        elif name=='lipFunnel':v.co.x*=.75;v.co.z=1.56+dy*3.5;v.co.y-=.002
ob=bpy.data.objects['Boca_interior']
for name in ['jawOpen','mouthWide','lipPucker','lipFunnel']:
    key=ob.data.shape_keys.key_blocks[name];basis_copy(ob,key)
    for v in key.data:
        if name=='jawOpen':v.co.z*=7
        elif name=='mouthWide':v.co.x*=1.35
        elif name=='lipPucker':v.co.x*=.57
        elif name=='lipFunnel':v.co.x*=.75;v.co.z*=3.5
for side,s in [('L',1),('R',-1)]:
    ob=bpy.data.objects['Ceja.'+side]
    for name in ['browInnerUp','browDown','browOuterUp']:
        key=ob.data.shape_keys.key_blocks[name+'.'+side];basis_copy(ob,key)
        for v in key.data:
            inner=math.exp(-((v.co.x-s*.012)/.024)**2)
            v.co.z+=(.009*inner if name=='browInnerUp' else -.007*inner if name=='browDown' else .009*(1-inner))
    ob.data.update()
for ob in [bpy.data.objects['Labios_controles'],bpy.data.objects['Boca_interior']]:ob.data.update()
presets=json.loads(rig['expresiones_json'])
for p in set(k for vals in presets.values() for k in vals):
    rig[p]=float(rig[p]);rig.id_properties_ui(p).update(min=0.0,max=1.0)
rig.update_tag();bpy.context.view_layer.update()
bpy.ops.wm.save_as_mainfile(filepath='C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/outputs/Salve_modelado_v01.blend')
print('INDEPENDENT_SHAPE_KEYS_REPAIRED')
