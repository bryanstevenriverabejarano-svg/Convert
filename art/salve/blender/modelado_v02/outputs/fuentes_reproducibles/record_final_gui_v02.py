"""Record the actual reopened GUI scene. Never saves or edits mesh data."""
from pathlib import Path
import bpy, hashlib, json

ROOT = Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
TARGET = ROOT/'outputs/Salve_refinado_v02.blend'
assert Path(bpy.data.filepath).resolve() == TARGET.resolve()
scene = bpy.data.scenes['04_SALVE_MODELO_3D']
bpy.context.window.scene = scene
assert scene.frame_current == 1
assert scene.camera.name == 'CAM_front'
assert (scene.render.resolution_x, scene.render.resolution_y, scene.render.resolution_percentage) == (1024, 1536, 100)
addon = 'not_running'
try:
    server = getattr(bpy.types, 'blendermcp_server', None)
    if not (server and server.running):
        bpy.ops.blendermcp.start_server()
    server = getattr(bpy.types, 'blendermcp_server', None)
    addon = 'running' if server and server.running else 'start_failed'
except Exception as error:
    addon = 'unavailable: ' + str(error)
record = {
    'file': bpy.data.filepath,
    'fileSha256': hashlib.sha256(TARGET.read_bytes()).hexdigest(),
    'blenderVersion': bpy.app.version_string,
    'scene': scene.name, 'frame': scene.frame_current,
    'camera': scene.camera.name,
    'resolution': [scene.render.resolution_x, scene.render.resolution_y],
    'resolutionPercentage': scene.render.resolution_percentage,
    'objectCount': len(scene.objects),
    'guiOpened': True, 'openedWithoutStartupPython': True,
    'addonStatus': addon, 'mcpTransportVerified': False,
    'mcpTransportWarning': 'Live get_addon_status failed with Incomplete JSON response received after reopening without startup Python. GUI console was used for the record; no addon-version mismatch is inferred.',
    'scope': 'Actual GUI file opening and scene/camera/dimensions. Addon status alone does not verify MCP transport or certify artistic identity or production.',
}
(ROOT/'outputs/Salve_apertura_GUI_v02.json').write_text(json.dumps(record, indent=2, ensure_ascii=False), encoding='utf-8')
for area in bpy.context.screen.areas:
    if area.type == 'CONSOLE':
        area.type = 'VIEW_3D'
    if area.type == 'VIEW_3D':
        area.spaces.active.region_3d.view_perspective = 'CAMERA'
        area.spaces.active.overlay.show_overlays = False
print('SALVE_FINAL_GUI_REOPENED_AND_RECORDED')
