# SALVE: cambia CLIP y ejecuta este texto en Blender.
import bpy, json
CLIP = "Idle_Breathe"
rig = bpy.data.objects["SALVE_Rig"]
clips = json.loads(rig["salve_clip_map"])
spec = clips[CLIP]
rig.animation_data.action = bpy.data.actions[spec["rig_action"]]
body = bpy.data.objects["Salve_Body"]
body.data.shape_keys.animation_data.action = bpy.data.actions[spec["face_action"]]
scene = bpy.context.scene
scene.frame_start, scene.frame_end = spec["frames"]
scene.frame_set(scene.frame_start)
print("Salve:", CLIP, "| Clips disponibles:", ", ".join(clips))
