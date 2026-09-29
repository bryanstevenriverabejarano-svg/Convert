import bpy, json, math, pathlib
from mathutils import Quaternion

ROOT = pathlib.Path(__file__).resolve().parent
manifest = json.loads((ROOT/'references/manifest.json').read_text(encoding='utf-8'))
bpy.ops.wm.read_factory_settings(use_empty=True)
gallery = bpy.context.scene
gallery.name = '01_REFERENCIAS_20_ORIGINALES'
gallery.unit_settings.system = 'METRIC'
gallery['estado'] = 'Referencias originales 2D. No son la malla 3D.'

def collection(scene, name):
    c=bpy.data.collections.new(name); scene.collection.children.link(c); return c
def label(c,name,text,loc,size=.06):
    d=bpy.data.curves.new(name,'FONT'); d.body=text; d.size=size; d.align_x='CENTER'
    o=bpy.data.objects.new(name,d); c.objects.link(o); o.location=loc; o.rotation_euler=(math.pi/2,0,0)
    return o
images={}
for v in manifest['views']:
    img=bpy.data.images.load(str(ROOT/'references'/pathlib.Path(v['path']).name)); img.pack(); images[v['id']]=img
def ref(c,id,loc,height=1.76,alpha=1):
    o=bpy.data.objects.new('REF_'+id,None); c.objects.link(o); o.empty_display_type='IMAGE';o.data=images[id]
    w,h=o.data.size; o.empty_display_size=height*max(w,h)/h
    o.rotation_euler=(math.pi/2,0,0);o.location=loc;o.color=(1,1,1,alpha)
    o.empty_image_depth='BACK';o.hide_select=True
    o['tipo']='Referencia 2D original; no geometria'; return o
c=collection(gallery,'20 PNG originales - verificados SHA256')
for i,v in enumerate(manifest['views']):
    col=i%5; row=i//5; x=(col-2)*1.5; z=(3-row)*2.05+.95
    ref(c,v['id'],(x,0,z));label(c,'LABEL_'+v['id'],f'{i+1:02d}  {v["label"]}',(x,-.01,z-1),.047)
label(c,'TITLE','SALVE / 20 VISTAS ORIGINALES',(0,0,8.28),.15)
label(c,'STATUS','REFERENCIAS 2D / NUCLEO CANONICO / NO SON UN MODELO 3D',(0,0,8.06),.06)

rig_scene=bpy.data.scenes.new('02_RIG_BASE_PROPUESTA'); rig_scene.unit_settings.system='METRIC'; rig_scene.render.fps=30
bpy.context.window.scene=rig_scene
rc=collection(rig_scene,'Esqueleto inicial - sin malla ni pesos')
bc=collection(rig_scene,'Referencias de calibracion - escala provisional')
ref(bc,'a_front',(0,.14,.88),1.76,.65)
side=ref(bc,'right',(1.55,.14,.88)); rear=ref(bc,'a_rear',(-1.55,.14,.88))
label(bc,'STATUS_RIG','RIG BASE / POSICIONES PROPUESTAS / SIN SKINNING',(0,0,1.92),.05)
label(bc,'SCALE','Altura de trabajo provisional: 1.76 m; ajustar a la escala acordada',(0,0,-.10),.038)
label(bc,'SCENE_HELP','6 poses de comprobacion en marcadores; no son clips terminados',(0,0,-.18),.038)
arm=bpy.data.armatures.new('Salve_Skeleton_Proposal'); ob=bpy.data.objects.new('Salve_Rig_Base_SIN_MALLA',arm); rc.objects.link(ob)
ob.show_in_front=True;arm.display_type='OCTAHEDRAL';bpy.context.view_layer.objects.active=ob;ob.select_set(True)
bpy.ops.object.mode_set(mode='EDIT')
bones={}
def bone(name,head,tail,parent=None,deform=True,connect=False):
    b=arm.edit_bones.new(name);b.head=head;b.tail=tail;b.use_deform=deform
    if parent:b.parent=bones[parent];b.use_connect=connect
    bones[name]=b;return b
bone('root',(0,0,0),(0,0,.12),deform=False)
bone('pelvis',(0,0,.96),(0,0,1.08),'root')
bone('spine.01',(0,0,1.08),(0,0,1.23),'pelvis')
bone('spine.02',(0,0,1.23),(0,0,1.39),'spine.01',connect=True)
bone('chest',(0,0,1.39),(0,0,1.48),'spine.02',connect=True)
bone('neck',(0,0,1.48),(0,0,1.57),'chest',connect=True)
bone('head',(0,0,1.57),(0,0,1.73),'neck',connect=True)
bone('jaw',(0,-.025,1.58),(0,-.11,1.55),'head')
for suffix,s in [('L',1),('R',-1)]:
    def xyz(x,y,z):return (s*x,y,z)
    bone('clavicle.'+suffix,xyz(.03,0,1.44),xyz(.135,0,1.43),'chest')
    bone('upper_arm.'+suffix,xyz(.135,0,1.43),xyz(.235,-.015,1.22),'clavicle.'+suffix,connect=True)
    bone('forearm.'+suffix,xyz(.235,-.015,1.22),xyz(.342,-.025,1.00),'upper_arm.'+suffix,connect=True)
    bone('hand.'+suffix,xyz(.342,-.025,1),xyz(.384,-.03,.915),'forearm.'+suffix,connect=True)
    bone('thigh.'+suffix,xyz(.088,0,.99),xyz(.071,-.026,.61),'pelvis')
    bone('shin.'+suffix,xyz(.071,-.026,.61),xyz(.056,.005,.145),'thigh.'+suffix,connect=True)
    bone('foot.'+suffix,xyz(.056,.005,.145),xyz(.056,-.115,.07),'shin.'+suffix,connect=True)
    bone('toe.'+suffix,xyz(.056,-.115,.07),xyz(.056,-.17,.045),'foot.'+suffix,connect=True)
    bone('eye.'+suffix,xyz(.035,-.086,1.615),xyz(.035,-.125,1.615),'head')
    for name,h,t,p in [
        ('shoulder_aux',(.14,.008,1.44),(.17,.008,1.40),'upper_arm'),
        ('elbow_aux',(.235,.004,1.235),(.245,.004,1.195),'forearm'),
        ('hip_aux',(.085,.01,.995),(.097,.01,.95),'thigh'),
        ('knee_aux',(.071,-.04,.63),(.071,-.04,.59),'shin'),
        ('arm_twist',(.18,.025,1.34),(.22,.015,1.25),'upper_arm'),
        ('forearm_twist',(.27,.018,1.15),(.31,.008,1.06),'forearm')]:
        bone(name+'.'+suffix,xyz(*h),xyz(*t),p+'.'+suffix,deform=False)
    for fi,fn in enumerate(['thumb','index','middle','ring','pinky']):
        x=.365+(fi-2)*.011;z=.948-(abs(fi-2)*.007)
        parent='hand.'+suffix
        for j in range(3):
            h=xyz(x+j*.008,-.035,z-j*.022);t=xyz(x+(j+1)*.008,-.035,z-(j+1)*.022)
            nm=f'{fn}.{j+1:02d}.{suffix}';bone(nm,h,t,parent);parent=nm
    for strand,xx,yy in [('front',.11,-.025),('side',.16,.055),('rear',.08,.12)]:
        parent='head'
        for j in range(4):
            nm=f'hair_{strand}.{j+1:02d}.{suffix}'
            bone(nm,xyz(xx+j*.018,yy,1.62-j*.185),xyz(xx+(j+1)*.018,yy+.015,1.62-(j+1)*.185),parent)
            parent=nm
bpy.ops.object.mode_set(mode='OBJECT')
for b in arm.bones:
    b['estado']='Propuesto; requiere ajuste anatomico, pesos y validacion'
    if 'aux' in b.name or 'twist' in b.name:b.color.palette='THEME03'
    elif 'hair' in b.name:b.color.palette='THEME04'
    else:b.color.palette='THEME05'
ob['estado']='Plantilla de esqueleto; no rig terminado. Sin IK/FK, drivers, pesos ni fisica.'
ob['joint_strategy']='Cadena humanoide principal + auxiliares para futura correccion de volumen.'
rig_scene.frame_end=151
poses=[(1,'A_BASE',{}),(31,'SENTADA_PREVIEW',{'thigh.L':(-80,0,0),'thigh.R':(-80,0,0),'shin.L':(85,0,0),'shin.R':(85,0,0)}),(61,'CUCLILLAS_PREVIEW',{'thigh.L':(-95,0,-10),'thigh.R':(-95,0,10),'shin.L':(135,0,0),'shin.R':(135,0,0),'spine.01':(18,0,0)}),(91,'SALUDO_PREVIEW',{'upper_arm.L':(0,0,-115),'forearm.L':(0,0,-65),'hand.L':(0,0,15)}),(121,'FLOTAR_PREVIEW',{'upper_arm.L':(0,0,-35),'upper_arm.R':(0,0,35),'thigh.L':(-25,0,0),'shin.L':(40,0,0),'thigh.R':(-10,0,0),'shin.R':(20,0,0)}),(151,'RECLINAR_PREVIEW',{'root':(-90,0,0),'thigh.L':(-10,0,0),'shin.L':(20,0,0)})]
ob.animation_data_create()
for frame,title,rotations in poses:
    rig_scene.frame_set(frame)
    for pb in ob.pose.bones:
        pb.rotation_mode='XYZ';pb.rotation_euler=tuple(math.radians(a) for a in rotations.get(pb.name,(0,0,0)))
        pb.keyframe_insert('rotation_euler',frame=frame,group=pb.name)
    rig_scene.timeline_markers.new(title,frame=frame)
ob.animation_data.action.name='SEIS_POSES_DE_CALIBRACION_NO_CLIPS_FINALES';ob.animation_data.action.use_fake_user=True
rig_scene.frame_set(1)

face=bpy.data.scenes.new('03_EXPRESIONES_PROPUESTA');fc=collection(face,'Lamina artistica - sin shape keys')
img=bpy.data.images.load(str(ROOT/'design/Salve_expresiones_propuesta.png'));img.pack();images['expression_proposal']=img
ref(fc,'expression_proposal',(0,0,1.4),2.8)
label(fc,'FACE_TITLE','SALVE / EXPRESIONES Y VISEMAS / PROPUESTA',(0,0,3.05),.09)
label(fc,'FACE_STATUS','Lamina generada a partir del rostro original. Aun no existen shape keys 3D.',(0,0,-.20),.05)

txt=bpy.data.texts.new('LEEME_SALVE')
txt.write('SALVE - BASE TECNICA PARA MODELADO\n\n20 originales verificados y empaquetados. Escenas: referencias, rig base y expresiones.\nEl rig es una plantilla espacial de '+str(len(arm.bones))+' huesos, NO un personaje final.\nNo hay malla de personaje, skinning, shape keys, fisica ni exportacion validada.\nLas seis poses son claves de calibracion; no clips de animacion acabados.\n1.76 m es escala provisional. Ajustar profundidad usando vistas y decisiones artisticas.\nEl PDF adjunto define topologia, expresiones, clips y criterios de entrega.\nPR #110 ya integrado contiene reacciones y posturas 2D.\n')
for s in bpy.data.scenes:
    s.world=bpy.data.worlds.new(s.name+'_World');s.world.color=(.07,.09,.12)
for key,img in images.items():
    img.filepath='//design/Salve_expresiones_propuesta.png' if key=='expression_proposal' else '//references/'+key+'.png'
bpy.context.window.scene=rig_scene
for screen in bpy.data.screens:
    for area in screen.areas:
        if area.type=='VIEW_3D':
            sp=area.spaces.active;sp.region_3d.view_rotation=Quaternion((math.sqrt(.5),math.sqrt(.5),0,0));sp.region_3d.view_perspective='ORTHO';sp.region_3d.view_distance=3.3;sp.region_3d.view_location=(0,0,.89)
            sp.overlay.show_floor=False;sp.overlay.show_axis_x=False;sp.overlay.show_axis_y=False
bpy.ops.wm.save_as_mainfile(filepath=str(ROOT/'Salve_base_modelado.blend'))
assert len([o for o in gallery.objects if o.type=='EMPTY'])==20
assert all(i.packed_file for i in images.values())
assert len(rig_scene.timeline_markers)==6
report={'blender':bpy.app.version_string,'reference_images':20,'packed_images':len(images),'bones':len(arm.bones),'calibration_poses':6,'character_meshes':0,'shape_keys':0,'physics':False,'status':'reference_and_skeleton_scaffold'}
(ROOT/'blender_verification.json').write_text(json.dumps(report,indent=2),encoding='utf-8')
bone_data=[{'name':b.name,'head':list(b.head_local),'tail':list(b.tail_local),'parent':b.parent.name if b.parent else None,'deform':b.use_deform} for b in arm.bones]
(ROOT/'skeleton.json').write_text(json.dumps(bone_data,indent=2),encoding='utf-8')
print('SALVE_VERIFIED',json.dumps(report))
