"""Local MBP seal, angry preset and attached eyelash corrections; no FACE rebuild."""
import bpy
import json
import math

scene = bpy.context.scene
rig = bpy.data.objects['Salve_Rig']
previous_frame = scene.frame_current
mouth = bpy.data.objects['Labios_controles']
cavity = bpy.data.objects['Boca_interior']
cz = 1.560
for ob in [mouth, cavity]:
    blocks = ob.data.shape_keys.key_blocks
    basis = blocks['Basis']
    pressed = blocks['mouthPress']
    for index, (base, v) in enumerate(zip(basis.data, pressed.data)):
        v.co = base.co
        if ob == mouth:
            t = (index // 96) / 6
            weight = (1 - t) ** 1.5
            v.co.z = cz + (base.co.z - cz) * (1 - weight)
        else:
            v.co.z = cz
    ob.data.update()
presets = json.loads(rig['expresiones_json'])
assert 'MBP' in presets
presets['MBP'] = {'mouthPress': 1.0}
for side in ['L', 'R']:
    presets['ANGRY']['eyeSquint.' + side] = 0.85
    presets['ANGRY']['browOuterUp.' + side] = 0.50
    presets['ANGRY']['browDown.' + side] = 0.95
rig['expresiones_json'] = json.dumps(presets, ensure_ascii=False)
mbp_frame = 201 + list(presets).index('MBP') * 10
scene.frame_set(mbp_frame)
rig['lipPucker'] = 0.0
rig['mouthPress'] = 1.0
for control in ['lipPucker', 'mouthPress']:
    rig.keyframe_insert(data_path='["' + control + '"]', frame=mbp_frame)
rig['face_mbp_seal'] = 'MBP: inner lip boundary and cavity collapse to sealed line; outer facial annulus preserved.'
angry_frame = 201 + list(presets).index('ANGRY') * 10
scene.frame_set(angry_frame)
for prop, value in presets['ANGRY'].items():
    rig[prop] = float(value)
    rig.keyframe_insert(data_path='["' + prop + '"]', frame=angry_frame)
rig['face_angry_preset_polish'] = 'Eye squint0.85 and outer brow0.50 expose angry inclination beyond covered inner brows.'

# The tooth strip must narrow with funnel/pucker instead of spanning lip corners.
teeth = bpy.data.objects['Dientes_superiores']
teeth.driver_remove('scale', 0)
driver = teeth.driver_add('scale', 0).driver
driver.type = 'SCRIPTED'
for variable in list(driver.variables):
    driver.variables.remove(variable)
for variable_name, prop in [('jaw','jawOpen'),('wide','mouthWide'),
                            ('fun','lipFunnel'),('puck','lipPucker')]:
    var = driver.variables.new()
    var.name = variable_name
    var.targets[0].id_type = 'OBJECT'
    var.targets[0].id = rig
    var.targets[0].data_path = '["' + prop + '"]'
driver.expression = 'min(1,jaw*8)*(1+.35*wide)*(1-.28*fun-.38*puck)'
rig['face_teeth_funnel_pucker_fit'] = 'Tooth strip narrows with lipFunnel/pucker; jaw/width behavior preserved.'

profile = [(1.522,.005,.014,-.021),(1.530,.014,.020,-.020),(1.542,.028,.027,-.016),
           (1.558,.042,.037,-.010),(1.577,.055,.044,-.004),(1.597,.063,.050,0),
           (1.618,.066,.055,.003),(1.642,.067,.058,.005),(1.669,.067,.060,.008),
           (1.697,.060,.056,.012),(1.722,.041,.038,.014),(1.742,.005,.009,.015)]
def face_front(x, z):
    dims = profile[0][1:] if z < profile[0][0] else profile[-1][1:]
    for a, b in zip(profile[:-1], profile[1:]):
        if a[0] <= z <= b[0]:
            t = (z - a[0]) / (b[0] - a[0])
            dims = tuple(a[i] * (1 - t) + b[i] * t for i in range(1, 4))
            break
    rx, ry, cy = dims
    y = cy - ry * math.sqrt(max(.001, 1 - (x / rx) ** 2))
    y -= .0062 * math.exp(-(x/.0073)**2-((z-1.600)/.019)**2)
    y -= .0095 * math.exp(-(x/.0080)**2-((z-1.584)/.0062)**2)
    y -= .0014 * math.exp(-(x/.0090)**2-((z-1.567)/.010)**2)
    for sign in [-1, 1]:
        y += .0025 * math.exp(-((x-sign*.029)/.024)**2-((z-1.618)/.014)**2)
        y -= .0018 * math.exp(-((x-sign*.040)/.016)**2-((z-1.593)/.016)**2)
    return y

# Merge the upper/lower inner edges in depth as well as height at full press.
# Otherwise a mouth closed from the front would still retain a tiny Y slit.
for ob in [mouth, cavity]:
    basis = ob.data.shape_keys.key_blocks['Basis']
    pressed = ob.data.shape_keys.key_blocks['mouthPress']
    for index, (base, vertex) in enumerate(zip(basis.data, pressed.data)):
        if ob == mouth:
            t = (index // 96) / 6
            weight = (1 - t) ** 1.5
            center_y = face_front(base.co.x, cz) - (.00015 + .00025 * (1-t)**3)
            vertex.co.y = base.co.y + (center_y - base.co.y) * weight
        else:
            vertex.co.y = face_front(base.co.x, cz) + .0002
    ob.data.update()

scene.frame_set(1)
wing_count = 0
for side, sign in [('L', 1), ('R', -1)]:
    cx, eye_z = sign * .029, 1.618
    for branch in range(3):
        ob = bpy.data.objects['Pestana_ala_%d.' % branch + side]
        if not ob.data.shape_keys:
            ob.shape_key_add(name='Basis', from_mix=False)
        basis = ob.data.shape_keys.key_blocks['Basis']
        for stem in ['blink', 'eyeSquint', 'eyeWide']:
            prop = stem + '.' + side
            block = ob.data.shape_keys.key_blocks.get(prop)
            if block is None:
                block = ob.shape_key_add(name=prop, from_mix=False)
            for original, v in zip(basis.data, block.data):
                v.co = original.co
                slope = sign * (v.co.x - cx) * .15
                if stem == 'blink':
                    u = max(-1, min(1, (v.co.x - cx) / .024))
                    edge = .0095 * math.sqrt(max(0, 1 - u*u))
                    dist = v.co.z - (eye_z + slope)
                    weight = max(0, min(1, 1 - (dist - edge) / .011))
                    v.co.z += -edge * weight + .0018 * (1-u*u) * weight
                    v.co.y = min(v.co.y, face_front(v.co.x, eye_z) - .0018 * weight)
                else:
                    factor = .68 if stem == 'eyeSquint' else 1.35
                    v.co.z = eye_z + (v.co.z - eye_z - slope) * factor + slope
            block.driver_remove('value')
            fc = block.driver_add('value')
            driver = fc.driver
            driver.type = 'SCRIPTED'
            for variable in list(driver.variables):
                driver.variables.remove(variable)
            var = driver.variables.new()
            var.name = 'value'
            var.targets[0].id_type = 'OBJECT'
            var.targets[0].id = rig
            var.targets[0].data_path = '["' + prop + '"]'
            driver.expression = 'value'
        ob.data.update()
        wing_count += 1
assert wing_count == 6
rig['face_eyelash_wings_polish'] = 'Six lash wings follow the same blink/squint/wide transforms as upper lash; explicit Basis.'
affected = [name for name, values in presets.items() if name in ['MBP','ANGRY'] or
            any(values.get(stem+'.'+side, 0) != 0
                for stem in ['blink','eyeSquint','eyeWide'] for side in ['L','R']) or
            (values.get('jawOpen',0)>0 and
             (values.get('lipFunnel',0)>0 or values.get('lipPucker',0)>0))]
rig['face_polish_repeat_expressions_json'] = json.dumps(affected)
scene.frame_set(previous_frame)
rig.update_tag()
bpy.context.view_layer.update()
print('MBP_ANGRY_LASH_LOCAL_CORRECTIVES_READY', affected, flush=True)
