"""Reviewable cleanup in the already open Blender; never saves the model."""
import bpy,json
from pathlib import Path
ROOT=Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
assert Path(bpy.data.filepath).resolve()==(ROOT/'outputs/Salve_refinado_v02.blend').resolve()
assert bpy.app.version[:3]==(5,2,2)
assert bpy.context.scene.name=='04_SALVE_MODELO_3D'
assert bpy.context.scene.frame_current==1
preflight={'version':bpy.app.version_string,'file':bpy.data.filepath,
    'scene':bpy.context.scene.name,'frame':bpy.context.scene.frame_current,
    'camera':bpy.context.scene.camera.name,
    'resolution':[bpy.context.scene.render.resolution_x,bpy.context.scene.render.resolution_y]}
(ROOT/'work/cleanup_gui_preflight.json').write_text(json.dumps(preflight,indent=2),encoding='utf-8')
server=getattr(bpy.types,'blendermcp_server',None)
if server:
    print('ADDON_DIRECT_PREFLIGHT',server.get_addon_info())
    print('SCENE_DIRECT_PREFLIGHT',server.get_scene_info())
file=ROOT/'work/face_topology_uv_safe_cleanup_v02.py'
namespace={'__name__':'salve_cleanup_review','__file__':str(file)}
exec(compile(file.read_text(encoding='utf-8'),str(file),'exec'),namespace)
report=namespace['clean_face_topology_uv'](
    report_path=ROOT/'outputs/Salve_limpieza_rostroUV_v02.json',keep_legacy_uv=False)
assert report['weights_and_drivers_preserved']
assert report['removed_isolated_vertices']==256
assert report['uv_check']=={'outside_unit_tile':0,'zero_area_faces':[]}
print('SALVE_GUI_CLEANUP_REVIEW_PASS_NO_SAVE')
