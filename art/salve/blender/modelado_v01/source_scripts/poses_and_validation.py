import bpy, json, math, pathlib, hashlib
from mathutils import Vector,Matrix,Quaternion
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2');OUT=ROOT/'outputs'
scene=bpy.context.scene;rig=bpy.data.objects['Salve_Rig'];rig.hide_set(False)
scene.frame_set(1)
def reset():
    for b in rig.pose.bones:b.matrix_basis=Matrix.Identity(4)
    for key in list(rig.keys()):
        if key.startswith('IK_'):rig[key]=0.0
    bpy.context.view_layer.update()
def aim(name,direction):
    b=rig.pose.bones[name];rest=rig.data.bones[name];a=Vector(direction).normalized()
    q=(rest.tail_local-rest.head_local).normalized().rotation_difference(a)@rest.matrix_local.to_quaternion()
    b.matrix=Matrix.Translation(b.head)@q.to_matrix().to_4x4();bpy.context.view_layer.update()
def pose(kind):
    reset()
    if kind=='A':return
    # Only the root sets global position; limbs keep their anatomical rest lengths.
    if kind=='seated':rig.pose.bones['root'].location.y=-.355
    elif kind=='crouched':rig.pose.bones['root'].location.y=-.255
    elif kind.startswith('kneeling'):rig.pose.bones['root'].location.y=-.40
    elif kind=='leaning_variant':rig.pose.bones['root'].location.y=0
    bpy.context.view_layer.update()
    if kind.startswith('leaning'):
        aim('spine.01',(0,-.045,.15));aim('spine.02',(0,-(.12 if kind.endswith('variant') else .03),.15));aim('chest',(0,-.025,.1));aim('neck',(0,.025,.09));aim('head',(0,.015,.15))
    for side,s in [('L',1),('R',-1)]:
        if kind=='seated':
            aim('thigh.'+side,(s*.17,-.29,-.12));aim('shin.'+side,(-s*.22,-.02,-.31));aim('foot.'+side,(s*.1,-.12,-.02));aim('upper_arm.'+side,(s*.10,-.1,-.21));aim('forearm.'+side,(-s*.035,-.16,-.16))
        elif kind=='crouched':
            aim('thigh.'+side,(s*.1,-.27,-.24));aim('shin.'+side,(-s*.05,.26,-.35));aim('foot.'+side,(0,-.12,-.05));aim('upper_arm.'+side,(s*.04,-.14,-.19));aim('forearm.'+side,(-s*.055,-.08,-.21))
        elif kind.startswith('kneeling'):
            aim('thigh.'+side,(s*.025,-.03,-.37));aim('shin.'+side,(s*.014,.37,-.045));aim('foot.'+side,(0,.10,-.08));aim('upper_arm.'+side,(s*.08,-.06,-.24));aim('forearm.'+side,(-s*.07,-.14,-.19))
            if kind.endswith('variant'):aim('spine.02',(0,-.075,.14));aim('chest',(0,-.055,.08))
        else:
            aim('upper_arm.'+side,(s*.072,0,-.225));aim('forearm.'+side,(s*.055,-.02,-.235))
        aim('hand.'+side,(s*.025,-.008,-.09))
    if kind=='hair_open':
        for side,s in [('L',1),('R',-1)]:
            for strand in ['front','side','rear']:aim('hair_'+strand+'.01.'+side,(s*.08,.01,-.16))
    bpy.context.view_layer.update()
frames={'stand':1,'A':31,'hair_open':61,'seated':91,'crouched':121,'kneeling':151,'kneeling_variant':161,'leaning':171,'leaning_variant':181}
for kind,frame in frames.items():
    saved_action=rig.animation_data.action if rig.animation_data else None
    if rig.animation_data:rig.animation_data.action=None
    scene.frame_set(frame)
    pose(kind)
    if saved_action:rig.animation_data.action=saved_action
    for pb in rig.pose.bones:
        pb.rotation_mode='QUATERNION';pb.keyframe_insert('location',frame=frame);pb.keyframe_insert('rotation_quaternion',frame=frame);pb.keyframe_insert('scale',frame=frame)
    scene.timeline_markers.new('POSE_'+kind,frame=frame)
# No interpolation between unrelated calibration poses.
if rig.animation_data and rig.animation_data.action:
    action=rig.animation_data.action;action.name='Salve | poses y controles faciales'
    try:
        for slot in action.slots:
            for layer in action.layers:
                for strip in layer.strips:
                    bag=strip.channelbag(slot)
                    if bag:
                        for fc in bag.fcurves:
                            for k in fc.keyframe_points:k.interpolation='CONSTANT'
    except Exception:pass

# Rest-pose topology and numerical deformation diagnostics; artistic fit is separate.
scene.frame_set(31);bpy.context.view_layer.update()
report={'blender':bpy.app.version_string,'scene':scene.name,'objects':len(scene.objects),'materials':len(bpy.data.materials),'bones':len(rig.data.bones),'control_bones':[b.name for b in rig.data.bones if b.name.startswith('CTRL_')],'poses':frames,'facial_presets':list(json.loads(rig['expresiones_json'])),'files':{},'pose_checks':{},'limitations':['Fidelidad artistica del rostro, flequillo y paneles pendiente de ajuste fino contra canon.','Skinning por distancia; correctivos articulares y contactos de cabello no certificados.','IK opcional; pole angles no calibrados para todas las poses.','UV inicial por proyeccion; retopologia y atlas finales pendientes.','La propuesta facial no tiene evidencia independiente de aprobacion.']}
body=bpy.data.objects['Salve_Traje_CONTINUO'];report['body_vertices']=len(body.data.vertices);report['body_polygons']=len(body.data.polygons)
for kind,frame in frames.items():
    scene.frame_set(frame);bpy.context.view_layer.update();dg=bpy.context.evaluated_depsgraph_get();coords=[]
    for o in scene.objects:
        if o.type=='MESH' and o.name!='Suelo_estudio':
            eo=o.evaluated_get(dg);me=eo.to_mesh();coords.extend([eo.matrix_world@v.co for v in me.vertices]);eo.to_mesh_clear()
    finite=all(all(math.isfinite(c) for c in v) for v in coords);minz=min(v.z for v in coords)
    report['pose_checks'][kind]={'finite_vertices':finite,'minimum_z':round(minz,4),'vertices_evaluated':len(coords),'floor_penetration_m':round(max(0,-minz),4)}
scene.frame_set(1);rig.hide_set(True)
scene['estado']='VERSION_01_ESTILIZADA | requiere revision artistica y correctivos'
text=bpy.data.texts['SALVE_MODELO_LEEME'];text.write('\nPoses en marcadores 1-181; expresiones 201-391. Rig visible desde coleccion 05. Propiedades IK_foot/hand activan IK (FK por defecto). Revision artistica, retopologia y correctivos pendientes; no declarar equivalencia exacta a las 20 ilustraciones.\n')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_modelado_v01.blend'))
(OUT/'Salve_validacion.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print(json.dumps(report['pose_checks'],indent=2))


