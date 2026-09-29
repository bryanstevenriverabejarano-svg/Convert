import bpy, math, json, pathlib, random
from mathutils import Vector
from math import sin,cos,pi,exp,sqrt
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2')
SRC=pathlib.Path('C:/Users/agred/.codex/.chatgpt-projects/g-p-6ab2c01aa8f8819193dceb05d9e78da7/salve_blender')
OUT=ROOT/'outputs'
random.seed(12)
scene=bpy.data.scenes.new('04_SALVE_MODELO_3D');bpy.context.window.scene=scene
scene.unit_settings.system='METRIC';scene.render.fps=30
cols={}
for name in ['01_Cuerpo_y_traje','02_Rostro_y_ojos','03_Cabello','04_Tecnologia_y_circuitos','05_Rig_y_controles','06_Estudio']:
    c=bpy.data.collections.new(name);scene.collection.children.link(c);cols[name]=c
BODY=cols['01_Cuerpo_y_traje'];FACE=cols['02_Rostro_y_ojos'];HAIR=cols['03_Cabello'];TECH=cols['04_Tecnologia_y_circuitos'];RIG=cols['05_Rig_y_controles'];STUDIO=cols['06_Estudio']
deform=[];rigid=[]
def mat(name,color,metal=0,rough=.4,emission=0):
    m=bpy.data.materials.new(name);m.diffuse_color=(*color,1);m.use_nodes=True
    p=next(n for n in m.node_tree.nodes if n.type=='BSDF_PRINCIPLED')
    p.inputs['Base Color'].default_value=(*color,1);p.inputs['Metallic'].default_value=metal;p.inputs['Roughness'].default_value=rough
    if emission:p.inputs['Emission Color'].default_value=(*color,1);p.inputs['Emission Strength'].default_value=emission
    return m
WHITE=mat('Traje | ceramica perla',(.79,.84,.93),.28,.27)
BLACK=mat('Paneles | grafito',(.009,.017,.034),.65,.26)
BLUE=mat('Circuitos | azul ionico',(.005,.09,1),.35,.22,3)
SILVER=mat('Bordes | titanio',(.26,.34,.48),.82,.22)
SKIN=mat('Rostro | porcelana calida',(.88,.62,.53),.02,.5)
p=next(n for n in SKIN.node_tree.nodes if n.type=='BSDF_PRINCIPLED');p.inputs['Subsurface Weight'].default_value=.085
HWHITE=mat('Cabello | blanco lavanda',(.72,.77,.91),.12,.38)
HLINE=mat('Cabello | sombreado de mechon',(.4,.48,.7),.15,.42)
SCLERA=mat('Ojos | esclerotica',(.91,.95,1),.02,.24)
IRIS=mat('Ojos | iris azul',(.018,.27,.95),.25,.21,.35)
PUPIL=mat('Ojos | pupila',(.002,.006,.022),.05,.18)
LASH=mat('Cejas y pestanas',(.14,.12,.19),0,.5)
LIP=mat('Labios | rosa tenue',(.57,.24,.25),0,.44)
MOUTH=mat('Boca | cavidad',(.047,.009,.019),0,.7)
TEETH=mat('Dientes | marfil',(.9,.86,.8),0,.35)
TONGUE=mat('Lengua',(.53,.12,.18),0,.48)
def mesh(name,verts,faces,col,material,sub=0,skin=False,bone=None):
    d=bpy.data.meshes.new(name);d.from_pydata(verts,[],faces);d.update();o=bpy.data.objects.new(name,d);col.objects.link(o);o.data.materials.append(material)
    for p in d.polygons:p.use_smooth=True
    if sub:
        mod=o.modifiers.new('Suavizado editable','SUBSURF');mod.levels=sub;mod.render_levels=sub
    if skin:deform.append(o)
    if bone:rigid.append((o,bone))
    return o
def move_col(o,col):
    for c in list(o.users_collection):c.objects.unlink(o)
    col.objects.link(o)
def ellipsoid(name,loc,scale,material,col,skin=False,bone=None,segments=32,rings=20):
    bpy.ops.mesh.primitive_uv_sphere_add(segments=segments,ring_count=rings,location=loc)
    o=bpy.context.object;o.name=name;move_col(o,col)
    for v in o.data.vertices:v.co.x*=scale[0];v.co.y*=scale[1];v.co.z*=scale[2]
    o.data.materials.append(material)
    for p in o.data.polygons:p.use_smooth=True
    if skin:deform.append(o)
    if bone:rigid.append((o,bone))
    return o
def tube(name,points,radii,material,col=TECH,sides=10,skin=False,bone=None):
    vv=[];ff=[];points=[Vector(p) for p in points]
    for j,p in enumerate(points):
        axis=(points[min(j+1,len(points)-1)]-points[max(j-1,0)]).normalized()
        ref=Vector((0,1,0)) if abs(axis.y)<.9 else Vector((1,0,0))
        u=axis.cross(ref).normalized();v=axis.cross(u).normalized();r=radii[j] if isinstance(radii,list) else radii
        rx,ry=(r,r) if isinstance(r,(int,float)) else r
        for i in range(sides):vv.append(tuple(p+u*cos(2*pi*i/sides)*rx+v*sin(2*pi*i/sides)*ry))
        if j:
            for i in range(sides):a=(j-1)*sides+i;b=(j-1)*sides+(i+1)%sides;ff.append((a,b,b+sides,a+sides))
    ff.extend([tuple(reversed(range(sides))),tuple((len(points)-1)*sides+i for i in range(sides))])
    return mesh(name,vv,ff,col,material,1,skin,bone)
def profile(name,sections,material=WHITE,col=BODY,skin=True,n=48):
    vv=[];ff=[]
    for j,(z,rx,ry,cy) in enumerate(sections):
        for i in range(n):a=2*pi*i/n;vv.append((rx*sin(a),cy-ry*cos(a),z))
        if j:
            for i in range(n):a=(j-1)*n+i;b=(j-1)*n+(i+1)%n;ff.append((a,b,b+n,a+n))
    ff.extend([tuple(reversed(range(n))),tuple((len(sections)-1)*n+i for i in range(n))])
    return mesh(name,vv,ff,col,material,2,skin)

# Principal suit surface; longitudinal loops are retained for further retopology.
torso=profile('Traje_cuerpo_principal',[(.9,.062,.056,-.005),(.945,.095,.077,.004),(.99,.135,.085,.008),(1.035,.139,.082,.009),(1.085,.115,.069,.004),(1.135,.093,.060,0),(1.19,.095,.068,-.001),(1.245,.112,.086,-.008),(1.30,.136,.092,-.016),(1.355,.151,.083,-.003),(1.4,.139,.067,0),(1.44,.10,.047,0),(1.462,.048,.04,0)])
neck=profile('Cuello_y_collar_negro',[(1.415,.046,.041,0),(1.44,.044,.04,0),(1.49,.041,.039,0),(1.523,.04,.037,-.003)],BLACK)
for side,s in [('L',1),('R',-1)]:
    # Anatomical armor follows breast, pelvis and leg contours.
    ellipsoid('Pecho_armadura.'+side,(s*.071,-.075,1.312),(.079,.067,.083),WHITE,BODY,True)
    ellipsoid('Cadera_placa.'+side,(s*.083,.047,.992),(.078,.058,.083),WHITE,BODY,True)
    tube('Pierna_traje.'+side,[(s*.087,0,1.016),(s*.10,0,.947),(s*.095,-.003,.84),(s*.082,-.013,.74),(s*.071,-.026,.637),(s*.071,-.026,.604),(s*.066,-.007,.52),(s*.059,.007,.40),(s*.056,.009,.28),(s*.056,.005,.155)],[(.054,.06),(.072,.069),(.067,.062),(.052,.052),(.043,.043),(.041,.042),(.05,.055),(.047,.052),(.032,.035),(.027,.029)],WHITE,BODY,20,True)
    tube('Brazo_traje.'+side,[(s*.133,0,1.437),(s*.167,-.003,1.38),(s*.20,-.009,1.30),(s*.235,-.015,1.22),(s*.256,-.017,1.181),(s*.29,-.02,1.12),(s*.321,-.023,1.055),(s*.342,-.025,1.0)],[(.046,.042),(.043,.042),(.035,.037),(.027,.03),(.029,.031),(.034,.033),(.023,.024),(.020,.020)],WHITE,BODY,20,True)
    tube('Guante_palmar.'+side,[(s*.341,-.025,1.002),(s*.358,-.029,.967),(s*.378,-.031,.935),(s*.384,-.03,.915)],[(.02,.019),(.029,.016),(.031,.016),(.023,.013)],WHITE,BODY,16,True)
    for fi,fn in enumerate(['thumb','index','middle','ring','pinky']):
        x=.365+(fi-2)*.011;z=.948-abs(fi-2)*.007
        pts=[(s*(x+j*.008),-.035,z-j*.022) for j in range(4)]
        tube('Dedo_'+fn+'.'+side,pts,[.008,.007,.006,.004],WHITE,BODY,10,True)
        ellipsoid('Una_'+fn+'.'+side,(s*(x+.021),-.042,z-.054),(.0045,.002,.009),BLACK,TECH,True)
    # Heel, toe and sole are actual solid meshes.
    ellipsoid('Bota_empeine.'+side,(s*.056,-.069,.109),(.036,.091,.036),WHITE,BODY,True)
    ellipsoid('Bota_puntera.'+side,(s*.056,-.139,.055),(.035,.05,.026),WHITE,BODY,True)
    ellipsoid('Suela.'+side,(s*.056,-.135,.028),(.038,.058,.009),BLACK,TECH,True)
    tube('Tacon.'+side,[(s*.056,.037,.126),(s*.056,.046,.071),(s*.056,.052,.015)],[.016,.01,.009],BLACK,TECH,10,True)
    ellipsoid('Rodillera.'+side,(s*.071,-.058,.61),(.035,.014,.04),WHITE,TECH,True)
    ellipsoid('Codo_placa.'+side,(s*.236,.012,1.22),(.026,.014,.028),SILVER,TECH,True)
    # Black inserts and ion conductors in front, flanks and back.
    paths=[[(s*.121,-.035,1.35),(s*.13,-.066,1.245),(s*.093,-.045,1.14),(s*.129,-.059,1.025)],[(s*.121,-.040,.992),(s*.123,-.05,.89),(s*.095,-.049,.76),(s*.072,-.057,.66)],[(s*.083,-.047,.57),(s*.083,-.046,.48),(s*.065,-.033,.33),(s*.058,-.024,.19)],[(s*.152,-.016,1.41),(s*.19,-.043,1.30),(s*.219,-.035,1.25)],[(s*.26,-.043,1.174),(s*.302,-.047,1.09),(s*.33,-.039,1.025)],[(s*.053,-.133,.10),(s*.056,-.151,.063)],[(s*.127,.052,1.37),(s*.10,.06,1.25),(s*.084,.047,1.14),(s*.12,.068,1.02)]]
    for j,pts in enumerate(paths):
        tube('Panel_negro_%02d.'%j+side,pts,[.010 if j<3 else .006 for _ in pts],BLACK,TECH,8,True)
        shift=-.008 if j<6 else .008
        tube('Circuito_azul_%02d.'%j+side,[(x,y+shift,z) for x,y,z in pts],.0018,BLUE,TECH,6,True)
    # Fine suit seams are narrow titanium threads.
    tube('Costura_pecho.'+side,[(s*.008,-.087,1.385),(s*.018,-.136,1.33),(s*.03,-.131,1.255),(s*.079,-.075,1.21)],.0011,SILVER,TECH,6,True)
    tube('Circuito_collar.'+side,[(s*.026,-.035,1.51),(s*.014,-.046,1.48),(s*.016,-.047,1.44),(0,-.049,1.414)],.0022,BLUE,TECH,6,True)
    tube('Costura_cadera.'+side,[(s*.133,-.023,1.033),(s*.077,-.066,.967),(s*.025,-.06,.91)],.0015,BLUE,TECH,6,True)

def diamond(name,x,y,z,r,bone=None,skin=True):
    vv=[(x,y-.004,z+r),(x+r*.55,y-.004,z),(x,y-.004,z-r),(x-r*.55,y-.004,z),(x,y-.015,z)]
    return mesh(name,vv,[(0,1,4),(1,2,4),(2,3,4),(3,0,4),(3,2,1,0)],TECH,BLUE,0,skin,bone)
for z,y,r in [(1.405,-.073,.021),(1.255,-.102,.022),(1.05,-.078,.013)]:diamond('Emblema_nucleo',0,y,z,r)

# A closed quad head, shaped to the frontal chin and three-quarter silhouette.
head=profile('Rostro_topologia',[(1.523,.007,.014,-.022),(1.535,.022,.025,-.025),(1.55,.037,.035,-.022),(1.568,.050,.045,-.016),(1.59,.061,.051,-.006),(1.615,.067,.056,0),(1.64,.068,.058,.002),(1.67,.068,.061,.005),(1.70,.061,.057,.01),(1.727,.044,.041,.014),(1.743,.018,.018,.015)],SKIN,FACE,False,64)
rigid.append((head,'head'))
# Small nose bridge and tip, kept in the same facial palette.
ellipsoid('Nariz_puente',(0,-.053,1.594),(.009,.011,.022),SKIN,FACE,bone='head')
ellipsoid('Nariz_punta',(0,-.065,1.583),(.01,.01,.007),SKIN,FACE,bone='head')
for side,s in [('L',1),('R',-1)]:
    ellipsoid('Oreja.'+side,(s*.066,.0,1.608),(.013,.012,.026),SKIN,FACE,bone='head')
    # Exposed eye surface conforms to a stylized almond opening.
    cx=s*.029;cz=1.618
    vv=[];ff=[]
    for j in range(9):
        rr=j/8
        for k in range(48):
            a=2*pi*k/48;x=cx+.024*cos(a)*rr;z=cz+.0105*sin(a)*rr+s*(x-cx)*.08
            y=-.053-.013*(1-rr*rr)
            vv.append((x,y,z))
            if j:
                a0=(j-1)*48+k;b=(j-1)*48+(k+1)%48;ff.append((a0,b,b+48,a0+48))
    mesh('Esclerotica.'+side,vv,ff,FACE,SCLERA,1,bone='head')
    ellipsoid('Iris.'+side,(cx,-.066,cz),(.0098,.0032,.011),IRIS,FACE,bone='eye.'+side)
    ellipsoid('Pupila.'+side,(cx,-.069,cz),(.0038,.0013,.0077),PUPIL,FACE,bone='eye.'+side)
    ellipsoid('Brillo.'+side,(cx-.003,-.0705,cz+.005),(.0024,.0008,.0028),SCLERA,FACE,bone='eye.'+side)
    for upper in [True,False]:
        pts=[]
        for i in range(25):
            a=pi*i/24 if upper else pi+pi*i/24
            x=cx+.024*cos(a);z=cz+.011*sin(a)+s*(x-cx)*.08
            pts.append((x,-.054,z))
        lid=tube(('Parpado_superior.' if upper else 'Parpado_inferior.')+side,pts,.0024 if upper else .0015,SKIN,FACE,8,bone='head')
        lid['facial_part']='lid';lid['side']=side;lid['upper']=upper
        lid.shape_key_add(name='Basis');kb=lid.shape_key_add(name='blink.'+side)
        for v in kb.data:v.co.z=cz+(v.co.x-cx)*s*.08
        if upper:
            lash=tube('Pestana.'+side,[(x,y-.0015,z+.0008) for x,y,z in pts],.0015,LASH,FACE,6,bone='head')
            lash.shape_key_add(name='Basis');k=lash.shape_key_add(name='blink.'+side)
            for v in k.data:v.co.z=cz+(v.co.x-cx)*s*.08
    # An eyelid cover retracts into the head at rest and covers the sclera on blink.
    vv=[];ff=[]
    for j in range(9):
        t=j/8
        for i in range(25):
            x=cx-.024+i*.048/24;edge=cz+.011*sqrt(max(0,1-((x-cx)/.024)**2))+s*(x-cx)*.08
            vv.append((x,-.051,edge+.019*t))
            if j and i:idx=j*25+i;ff.append((idx-26,idx-25,idx,idx-1))
    cover=mesh('Parpado_cierre.'+side,vv,ff,FACE,SKIN,1,bone='head');cover.shape_key_add(name='Basis');key=cover.shape_key_add(name='blink.'+side)
    for idx,v in enumerate(key.data):
        j,i=divmod(idx,25);x=v.co.x;edge=cz+.011*sqrt(max(0,1-((x-cx)/.024)**2))+s*(x-cx)*.08
        v.co.z=edge-(edge-(cz+s*(x-cx)*.08))*j/8;v.co.y=-.068
    brow=tube('Ceja.'+side,[(cx-.022,-.054,1.644),(cx-.01,-.059,1.648),(cx+.007,-.058,1.648),(cx+.023,-.048,1.642)],.0017,HLINE,FACE,6,bone='head')
    brow.shape_key_add(name='Basis')
    for nm in ['browInnerUp','browDown','browOuterUp']:
        k=brow.shape_key_add(name=nm+'.'+side)
        for v in k.data:
            inner=exp(-((v.co.x-s*.012)/.024)**2)
            v.co.z+=(.009*inner if nm=='browInnerUp' else -.007*inner if nm=='browDown' else .009*(1-inner))

# Mouth is a deformable annular lip mesh with an open cavity and internal elements.
vv=[];ff=[]
for j in range(4):
    rr=1+j*.19
    for i in range(64):
        a=2*pi*i/64;vv.append((.0115*cos(a)*rr,-.0555+j*.0003,1.56+.0014*sin(a)*rr))
        if j:
            k=(j-1)*64+i;kn=(j-1)*64+(i+1)%64;ff.append((k,kn,kn+64,k+64))
mouth=mesh('Labios_controles',vv,ff,FACE,LIP,1,bone='head');mouth.shape_key_add(name='Basis')
for name in ['jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel']:
    key=mouth.shape_key_add(name=name)
    for v in key.data:
        x,y,z=v.co;dy=z-1.56;sign=1 if dy>=0 else -1
        if name=='jawOpen':v.co.z+=sign*.009*(1-(abs(x)/.025)**2)
        elif name=='smile':v.co.x*=1.3;v.co.z+=.006*(abs(x)/.023)**2
        elif name=='frown':v.co.z-=.004*(abs(x)/.023)**2
        elif name=='mouthWide':v.co.x*=1.5
        elif name=='lipPucker':v.co.x*=.57;v.co.y-=.006;v.co.z=1.56+dy*2
        elif name=='lipFunnel':v.co.x*=.7;v.co.z=1.56+dy*5;v.co.y-=.003
cavity=ellipsoid('Boca_interior',(0,-.053,1.56),(.012,.004,.0014),MOUTH,FACE,bone='head')
cavity.shape_key_add(name='Basis');k=cavity.shape_key_add(name='jawOpen')
for v in k.data:v.co.z*=7
for nm,sx in [('mouthWide',1.5),('lipPucker',.57),('lipFunnel',.7)]:
    k=cavity.shape_key_add(name=nm)
    for v in k.data:v.co.x*=sx
ellipsoid('Dientes_superiores',(0,-.052,1.565),(.01,.003,.0025),TEETH,FACE,bone='head')
ellipsoid('Lengua',(0,-.051,1.554),(.009,.003,.003),TONGUE,FACE,bone='jaw')

# Hair is built from solid tapered locks; strand ridges stay editable.
def lock(name,pts,widths,depths,bone='head',skin=False):
    return tube(name,pts,list(zip(widths,depths)),HWHITE,HAIR,12,skin,bone if not skin else None)
# Scalp cap covers only the crown and rear, leaving face and eye openings clear.
vv=[];ff=[]
for j in range(17):
    for i in range(64):
        a=2*pi*i/64;end=1.1+(.75*(1-cos(a))/2);th=.015+end*j/16
        vv.append((.073*sin(th)*sin(a),.013-.063*sin(th)*cos(a),1.658+.092*cos(th)))
        if j:
            k=(j-1)*64+i;kn=(j-1)*64+(i+1)%64;ff.append((k,kn,kn+64,k+64))
mesh('Cabello_casquete',vv,ff,HAIR,HWHITE,1,bone='head')
for s,side in [(1,'L'),(-1,'R')]:
    # Forehead bangs sweep aside and terminate above the eyes.
    for i in range(5):
        frac=i/4
        pts=[(s*(.003+.009*i),-.01,1.748),(s*(.005+.011*i),-.047,1.714),(s*(.006+.012*i),-.063,1.685),(s*(.01+.012*i),-.066,1.66),(s*(.018+.013*i),-.058,1.645+.006*frac)]
        lock('Flequillo_%02d.'%i+side,pts,[.009,.016,.015,.01,.0005],[.005,.005,.004,.003,.0005])
    # Long sweeping side and rear locks; roots match the six template chains.
    for i in range(12):
        a=.52+(i/11)*2.55;rootx=.066*sin(a);rooty=.013-.057*cos(a)
        xx=.10+.014*(i%6);yy=.01+.013*i
        pts=[(s*rootx,rooty,1.715),(s*.076,rooty+.012,1.62),(s*.095,yy,1.45),(s*(xx+.025),yy+.025,1.24),(s*(xx+.075),yy+.049,1.06),(s*(xx+.105),yy+.052,.96),(s*(xx+.08),yy+.025,.87),(s*(xx+.025),yy-.01,.845)]
        lock('Mechon_largo_%02d.'%i+side,pts,[.013,.022,.024,.03,.034,.035,.023,.0006],[.009,.014,.016,.019,.023,.023,.014,.0006],skin=True)
        tube('Surco_mechon_%02d.'%i+side,[(x,y-.014,z) for x,y,z in pts],.00065,HLINE,HAIR,5,True)
    for i in range(2):
        pts=[(s*(.060+i*.012),-.028,1.66),(s*(.07+i*.014),-.046,1.58),(s*(.075+i*.018),-.058,1.50),(s*(.105+i*.018),-.07,1.41),(s*(.093+i*.016),-.092,1.34)]
        lock('Mechon_rostro_%02d.'%i+side,pts,[.013,.018,.018,.012,.0005],[.006,.008,.007,.005,.0005],skin=True)

# Headphones, blue concentric rings and swept technological tips.
for side,s in [('L',1),('R',-1)]:
    ellipsoid('Auricular_carcasa.'+side,(s*.078,.013,1.664),(.018,.039,.052),BLACK,TECH,bone='head')
    for j,r in enumerate([.034,.026,.012]):
        pts=[(s*(.095+j*.001),.013+r*cos(2*pi*k/64),1.664+r*sin(2*pi*k/64)) for k in range(65)]
        tube('Aro_ionico_%d.'%j+side,pts,.0018 if j<2 else .0012,BLUE,TECH,6,bone='head')
    ellipsoid('Auricular_disco.'+side,(s*.096,.013,1.664),(.003,.019,.025),BLACK,TECH,bone='head')
    diamond('Auricular_emblema.'+side,s*.099,-.01,1.668,.009,'head',False)
    tube('Punta_tecnologica.'+side,[(s*.073,.019,1.69),(s*.078,.014,1.717),(s*.070,.017,1.752)],[(.015,.01),(.013,.009),(.0007,.0007)],BLACK,TECH,8,bone='head')
    tube('Punta_luz.'+side,[(s*.084,.004,1.70),(s*.087,.002,1.721),(s*.074,.009,1.746)],.0012,BLUE,TECH,6,bone='head')

# Preserve the original reference rig; create a separate production control rig.
data=json.loads((SRC/'skeleton.json').read_text());arm=bpy.data.armatures.new('Salve_Esqueleto');rig=bpy.data.objects.new('Salve_Rig',arm);RIG.objects.link(rig);rig.show_in_front=True
bpy.context.view_layer.objects.active=rig;rig.select_set(True);bpy.ops.object.mode_set(mode='EDIT')
for b in data:
    eb=arm.edit_bones.new(b['name']);eb.head=b['head'];eb.tail=b['tail'];eb.use_deform=b['deform']
for b in data:
    if b['parent']:arm.edit_bones[b['name']].parent=arm.edit_bones[b['parent']]
for side,s in [('L',1),('R',-1)]:
    for part,loc in [('foot', (s*.056,.005,.145)),('hand',(s*.342,-.025,1.0)),('knee_pole',(s*.071,-.45,.61)),('elbow_pole',(s*.235,-.35,1.22))]:
        eb=arm.edit_bones.new('CTRL_'+part+'.'+side);eb.head=loc;eb.tail=Vector(loc)+Vector((0,0,.065));eb.use_deform=False;eb.parent=arm.edit_bones['root']
bpy.ops.object.mode_set(mode='OBJECT');rig.select_set(False)
rig['escala_provisional_m']=1.76;rig['fuente']='Convert PR111 / 922495162c8fd14f8a084ca7c9d6713be8a677d9'
for pb in rig.pose.bones:pb.rotation_mode='XYZ'
for side in ['L','R']:
    for bone,target,count,pole in [('shin','foot',2,'knee_pole'),('forearm','hand',2,'elbow_pole')]:
        c=rig.pose.bones[bone+'.'+side].constraints.new('IK');c.name='IK opcional';c.target=rig;c.subtarget='CTRL_'+target+'.'+side;c.chain_count=count;c.pole_target=rig;c.pole_subtarget='CTRL_'+pole+'.'+side;c.influence=0
        prop='IK_'+target+'.'+side;rig[prop]=0.0;rig.id_properties_ui(prop).update(min=0,max=1,description='0 FK; 1 IK. Ajustar pole angle para cada pose.')
        drv=c.driver_add('influence').driver;drv.type='SCRIPTED';v=drv.variables.new();v.name='blend';v.targets[0].id=rig;v.targets[0].data_path='["'+prop+'"]';drv.expression='blend'
def attach(o,bone):
    vg=o.vertex_groups.new(name=bone);vg.add(list(range(len(o.data.vertices))),1,'REPLACE')
    mod=o.modifiers.new('Deformacion Salve','ARMATURE');mod.object=rig;o.parent=rig
for o,b in rigid:attach(o,b)
segments={b['name']:(Vector(b['head']),Vector(b['tail'])) for b in data if b['deform']}
def segment_dist(p,a,b):
    ab=b-a;t=max(0,min(1,(p-a).dot(ab)/ab.length_squared));return (p-(a+ab*t)).length
for o in deform:
    ishair=o.users_collection[0]==HAIR
    if ishair:allowed=[n for n in segments if n.startswith('hair_')]+['head']
    elif 'Dedo_' in o.name:
        fn=o.name.split('_')[1].split('.')[0];side=o.name[-1];allowed=[n for n in segments if n.startswith(fn+'.') and n.endswith(side)]+['hand.'+side]
    elif 'Una_' in o.name:
        fn=o.name.split('_')[1].split('.')[0];side=o.name[-1];allowed=[n for n in segments if n.startswith(fn+'.') and n.endswith(side)]
    else:allowed=[n for n in segments if not n.startswith(('hair_','eye.','jaw','thumb','index','middle','ring','pinky'))]
    groups={n:o.vertex_groups.new(name=n) for n in allowed}
    for vtx in o.data.vertices:
        p=o.matrix_world@vtx.co;nearest=sorted([(segment_dist(p,*segments[n]),n) for n in allowed])[:3]
        ws=[1/max(.003,d)**4 for d,n in nearest];tot=sum(ws)
        for w,(_,n) in zip(ws,nearest):groups[n].add([vtx.index],w/tot,'REPLACE')
    mod=o.modifiers.new('Skinning | 3 influences','ARMATURE');mod.object=rig;o.parent=rig

facials=['blink.L','blink.R','browInnerUp.L','browInnerUp.R','browDown.L','browDown.R','browOuterUp.L','browOuterUp.R','jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel']
for name in facials:rig[name]=0.0;rig.id_properties_ui(name).update(min=0,max=1,description='Control facial mezclable')
for o in FACE.objects:
    if o.type=='MESH' and o.data.shape_keys:
        for kb in list(o.data.shape_keys.key_blocks)[1:]:
            if kb.name not in facials:continue
            d=kb.driver_add('value').driver;d.type='SCRIPTED';v=d.variables.new();v.name='control';v.targets[0].id=rig;v.targets[0].data_path='["'+kb.name+'"]';d.expression='control'
presets={'NEUTRAL':{},'WARM':{'smile':.7},'CURIOUS':{'browOuterUp.L':.7,'browInnerUp.R':.3},'CONCERNED':{'browInnerUp.L':.7,'browInnerUp.R':.7,'frown':.35},'SAD':{'browInnerUp.L':.6,'browInnerUp.R':.6,'frown':.8},'ANGRY':{'browDown.L':.9,'browDown.R':.9},'SURPRISED':{'browOuterUp.L':.8,'browOuterUp.R':.8,'jawOpen':.65,'lipFunnel':.7},'SHY':{'smile':.3,'blink.L':.25,'blink.R':.25},'LAUGH':{'smile':1,'jawOpen':.9,'blink.L':1,'blink.R':1},'CRY':{'browInnerUp.L':1,'browInnerUp.R':1,'frown':1,'jawOpen':.4},'STARTLE':{'jawOpen':.5,'lipFunnel':.9,'browOuterUp.L':1,'browOuterUp.R':1},'THINK':{'browInnerUp.L':.4,'lipPucker':.2},'WINK':{'blink.L':1,'smile':.6},'A':{'jawOpen':.9},'E':{'jawOpen':.35,'mouthWide':.65},'I':{'jawOpen':.15,'mouthWide':1},'O':{'jawOpen':.65,'lipFunnel':1},'U':{'jawOpen':.2,'lipPucker':1},'MBP':{'lipPucker':.2},'SILENCE':{}}
(OUT/'expresiones_controles.json').write_text(json.dumps(presets,indent=2),encoding='utf-8')
rig['expresiones_json']=json.dumps(presets)

# Rig-friendly rest A pose plus independent keyed facial library.
for index,(name,values) in enumerate(presets.items()):
    frame=201+index*10
    for p in facials:rig[p]=values.get(p,0);rig.keyframe_insert(data_path='["'+p+'"]',frame=frame)
    scene.timeline_markers.new('FACE_'+name,frame=frame)
for p in facials:rig[p]=0
scene.frame_end=401;scene.frame_set(1)

world=bpy.data.worlds.new('Estudio | ambiente');world.use_nodes=True;scene.world=world
next(n for n in world.node_tree.nodes if n.type=='BACKGROUND').inputs[0].default_value=(.12,.16,.25,1)
next(n for n in world.node_tree.nodes if n.type=='BACKGROUND').inputs[1].default_value=.45
def area(name,loc,power,size,color):
    d=bpy.data.lights.new(name,'AREA');d.energy=power;d.shape='DISK';d.size=size;d.color=color;o=bpy.data.objects.new(name,d);STUDIO.objects.link(o);o.location=loc;o.rotation_euler=(Vector((0,0,1.1))-o.location).to_track_quat('-Z','Y').to_euler()
area('Principal',(2,-3,3.5),220,3,(.85,.91,1));area('Relleno',(-2,-2,2),130,2.5,(1,.84,.78));area('Contraluz',(0,2,2.5),290,2,(.30,.50,1))
ground=mat('Estudio | azul noche',(.025,.04,.069),.1,.48)
bpy.ops.mesh.primitive_plane_add(size=200,location=(0,0,-.008));o=bpy.context.object;o.name='Suelo_estudio';move_col(o,STUDIO);o.data.materials.append(ground)
camd=bpy.data.cameras.new('Salve_cam');cam=bpy.data.objects.new('Salve_cam',camd);STUDIO.objects.link(cam);scene.camera=cam;camd.type='ORTHO';camd.ortho_scale=2.04
cam.location=(0,-4,1.08);cam.rotation_euler=(Vector((0,0,.9))-cam.location).to_track_quat('-Z','Y').to_euler()
scene.render.engine='BLENDER_EEVEE';scene.render.resolution_x=768;scene.render.resolution_y=1152;scene.render.resolution_percentage=100;scene.render.image_settings.file_format='PNG'
scene.render.film_transparent=False
try:scene.view_settings.view_transform='AgX'
except TypeError:pass
for area_ui in bpy.context.screen.areas:
    if area_ui.type=='VIEW_3D':
        area_ui.spaces.active.region_3d.view_distance=2.9;area_ui.spaces.active.region_3d.view_location=(0,0,.94)
        area_ui.spaces.active.region_3d.view_rotation=cam.rotation_euler.to_quaternion();area_ui.spaces.active.shading.type='MATERIAL'
rig.hide_set(True)
readme=bpy.data.texts.new('SALVE_MODELO_LEEME');readme.write('Salve 3D v01. Reconstruccion parametrica estilizada de las referencias PR111. La escala de 1.76 m es provisional. Originales preservados en escenas 01-03 y en Salve_base_respaldo.blend. Colecciones editables, materiales procedurales, controles faciales en propiedades de Salve_Rig, poses y expresiones en marcadores. Fidelidad de rostro y detalles requiere revision artistica. No certificado para motor ni rendimiento movil.\n')
bpy.ops.wm.save_as_mainfile(filepath=str(OUT/'Salve_modelado_v01.blend'))
print('CREATED',len(scene.objects),'objects',sum(len(o.data.vertices) for o in scene.objects if o.type=='MESH'),'vertices')
