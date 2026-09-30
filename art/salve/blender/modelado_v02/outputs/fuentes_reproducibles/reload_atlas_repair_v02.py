"""Load repaired files as fresh images; embedded old pixels must not win."""
import bpy,pathlib,hashlib,json
root=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
result=[]
for channel in ('baseColor','roughness','metallic'):
    name='SalveV02_Atlas_'+channel
    path=root/'outputs/texturas'/(name+'.png')
    assert path.is_file(),path
    old=bpy.data.images.get(name)
    assert old is not None,name
    old.name=name+'_antes_reparacion'
    new=bpy.data.images.load(str(path),check_existing=False)
    new.name=name
    new.colorspace_settings.name=old.colorspace_settings.name
    old.user_remap(new)
    new.pack()
    if old.users==0:bpy.data.images.remove(old)
    result.append({'name':name,'dimensions':list(new.size),'packed':bool(new.packed_file),'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
bpy.context.scene['atlas_repair_images_json']=json.dumps(result)
print('REPAIRED_ATLAS_RELOADED_AND_PACKED',json.dumps(result),flush=True)
