"""Apply after current one-shot assembly: include CONFUSED 401 in neutral hair keys.
Does not affect face expression properties. Inherited FK pose is already neutral.
"""
import bpy
count = 0
neutral_frames = tuple([1] + [f for f in range(201, 402, 10) if f != 311])
for obj in bpy.data.objects:
    if obj.type != 'MESH' or not obj.data.shape_keys:
        continue
    keys = obj.data.shape_keys
    key = keys.key_blocks.get('PoseClearance_001')
    if not key:
        continue
    driver = key.driver_add('value').driver
    driver.type = 'SCRIPTED'
    driver.expression = '1.0 if frame in ' + str(neutral_frames) + ' else 0.0'
    count += 1
print('POSE_CLEARANCE_CONFUSED_401_FIX', count, flush=True)
