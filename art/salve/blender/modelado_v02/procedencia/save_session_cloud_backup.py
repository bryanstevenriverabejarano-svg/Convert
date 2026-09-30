"""Preserve the current GUI session as a separate file, never overwrite V02."""
from pathlib import Path
import bpy, hashlib, json
ROOT=Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
SOURCE=ROOT/'outputs/Salve_refinado_v02.blend'
TARGET=ROOT/'outputs/Salve_sesion_respaldo_2026-09-30.blend'
assert not bpy.app.background
assert Path(bpy.data.filepath).resolve()==SOURCE.resolve()
assert not TARGET.exists(), 'Never overwrite a previous session backup'
source_sha=hashlib.sha256(SOURCE.read_bytes()).hexdigest()
server=getattr(bpy.types,'blendermcp_server',None)
assert server is not None
addon_info=server.get_addon_info()
scene_info=server.get_scene_info()
scene=bpy.context.scene
record={'source_file':str(SOURCE),'source_sha256_before':source_sha,'source_sha256_after':None,'session_file':str(TARGET),'session_sha256':None,'blender':bpy.app.version_string,'scene':scene.name,'frame':scene.frame_current,'camera':scene.camera.name if scene.camera else None,'objects':len(scene.objects),'resolution':[scene.render.resolution_x,scene.render.resolution_y],'resolution_percentage':scene.render.resolution_percentage,'live_addon_version':addon_info.get('addon_version'),'live_scene_name':scene_info.get('name'),'scope':'Separate snapshot of the open GUI session. Not a new artistically accepted version; original V02 and rendered package remain unchanged.'}
for area in bpy.context.screen.areas:
    if area.type=='CONSOLE':
        area.type='VIEW_3D'
        area.spaces.active.region_3d.view_perspective='CAMERA'
        area.spaces.active.overlay.show_overlays=False
bpy.ops.wm.save_as_mainfile(filepath=str(TARGET),copy=True)
assert Path(bpy.data.filepath).resolve()==SOURCE.resolve()
record['source_sha256_after']=hashlib.sha256(SOURCE.read_bytes()).hexdigest()
assert record['source_sha256_after']==source_sha
record['session_sha256']=hashlib.sha256(TARGET.read_bytes()).hexdigest()
(ROOT/'outputs/Salve_sesion_respaldo_2026-09-30.json').write_text(json.dumps(record,indent=2,ensure_ascii=False),encoding='utf-8')
print('SALVE_OPEN_SESSION_BACKED_UP',record['session_sha256'])
