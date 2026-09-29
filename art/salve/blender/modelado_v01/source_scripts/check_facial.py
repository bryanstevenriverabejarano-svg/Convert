import bpy,json
rig=bpy.data.objects['Salve_Rig'];scene=bpy.context.scene;scene.frame_set(1);rig.animation_data.action=None
rig['blink.L']=1;rig['jawOpen']=1;rig.update_tag();scene.frame_set(2);bpy.context.view_layer.update()
for name in ['Parpado_superior.L','Labios_controles','Boca_interior']:
    o=bpy.data.objects[name];sk=o.data.shape_keys
    print(name,[(k.name,k.value) for k in sk.key_blocks])
    print([(f.data_path,f.is_valid,f.driver.expression,[(v.name,v.targets[0].id.name,v.targets[0].data_path) for v in f.driver.variables]) for f in sk.animation_data.drivers])
print('AUTO_EXEC',bpy.app.autoexec_fail,bpy.app.autoexec_fail_message)
ob=bpy.data.objects['Labios_controles']
for key in ob.data.shape_keys.key_blocks:
    print('LIP_BOUNDS',key.name,[(min(v.co[a] for v in key.data),max(v.co[a] for v in key.data)) for a in range(3)])
