import bpy, math, pathlib, json, random
from mathutils import Vector
from math import sin, cos, pi, exp, sqrt

ROOT = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
OUT = ROOT/'outputs'
OUT.mkdir(exist_ok=True)
scene = bpy.data.scenes['04_SALVE_MODELO_3D']
bpy.context.window.scene = scene
rig = bpy.data.objects['Salve_Rig']
rig.hide_set(False)
scene.frame_set(31)
old_action = rig.animation_data.action if rig.animation_data else None
if rig.animation_data: rig.animation_data.action = None
for b in rig.pose.bones: b.matrix_basis.identity()
for key in rig.keys():
    if key.startswith('IK_'): rig[key] = 0.0
bpy.context.view_layer.update()
BODY = bpy.data.collections['01_Cuerpo_y_traje']
HAIR = bpy.data.collections['03_Cabello']
TECH = bpy.data.collections['04_Tecnologia_y_circuitos']
WHITE = bpy.data.materials['Traje | ceramica perla']
BLACK = bpy.data.materials['Paneles | grafito']
BLUE = bpy.data.materials['Circuitos | azul ionico']
SILVER = bpy.data.materials['Bordes | titanio']
HWHITE = bpy.data.materials['Cabello | blanco lavanda']
HLINE = bpy.data.materials['Cabello | sombreado de mechon']

def principled(mat): return next(n for n in mat.node_tree.nodes if n.type == 'BSDF_PRINCIPLED')
for mat,color,metal,rough in [(WHITE,(.91,.925,.98,1),.12,.23),(BLACK,(.006,.009,.022,1),.72,.20),(SILVER,(.23,.30,.42,1),.85,.25),(HWHITE,(.83,.85,.97,1),.03,.33),(HLINE,(.43,.47,.67,1),.02,.45)]:
    p=principled(mat);p.inputs['Base Color'].default_value=color;p.inputs['Metallic'].default_value=metal;p.inputs['Roughness'].default_value=rough
    if mat == WHITE: p.inputs['Coat Weight'].default_value=.28;p.inputs['Coat Roughness'].default_value=.2
principled(BLUE).inputs['Emission Strength'].default_value=2.8

# Preserve the voxel construction source in a hidden collection, rather than discard it.
archive = bpy.data.collections.new('90_Base_v01_oculta')
scene.collection.children.link(archive)
source=bpy.data.objects['Salve_Traje_CONTINUO'];source.name='v01_Traje_voxel_fuente'
for c in list(source.users_collection): c.objects.unlink(source)
archive.objects.link(source);source.hide_render=True;source.hide_set(True)
archive.hide_render=True;archive.hide_viewport=True
for ob in list(TECH.objects):
    if not ob.name.startswith(('Auricular','Aro_ionico','Punta_','Suelo')):
        bpy.data.objects.remove(ob,do_unlink=True)

created=[]
def mesh(name,verts,faces,col,mat,sub=1):
    me=bpy.data.meshes.new(name);me.from_pydata(verts,[],faces);me.update()
    ob=bpy.data.objects.new(name,me);col.objects.link(ob);me.materials.append(mat)
    for p in me.polygons:p.use_smooth=True
    if sub:
        m=ob.modifiers.new('Superficie quad editable','SUBSURF');m.levels=sub;m.render_levels=sub
    created.append(ob)
    return ob

def distance(p,a,b):
    ab=b-a;t=max(0,min(1,(p-a).dot(ab)/ab.length_squared));return (p-a-ab*t).length

def bind(ob,names):
    if isinstance(names,str): names=[names]
    groups={name:ob.vertex_groups.new(name=name) for name in names}
    segments={name:(rig.data.bones[name].head_local,rig.data.bones[name].tail_local) for name in names}
    for v in ob.data.vertices:
        p=ob.matrix_world@v.co
        ds=sorted((distance(p,*segments[name]),name) for name in names)[:2]
        if len(ds)==1:groups[ds[0][1]].add([v.index],1,'REPLACE');continue
        ws=[1/max(.009,d)**6 for d,n in ds];total=sum(ws)
        for w,(_,name) in zip(ws,ds):groups[name].add([v.index],w/total,'REPLACE')
    m=ob.modifiers.new('Deformacion | pesos regionales','ARMATURE');m.object=rig;m.use_deform_preserve_volume=True
    ob.parent=rig

def interp(sections,t):
    j=min(len(sections)-2,int(t));f=t-j
    # Catmull-Rom for continuous, controlled silhouettes.
    p0=sections[max(0,j-1)];p1=sections[j];p2=sections[j+1];p3=sections[min(len(sections)-1,j+2)]
    return [.5*((2*b)+(-a+c)*f+(2*a-5*b+4*c-d)*f*f+(-a+3*b-3*c+d)*f*f*f) for a,b,c,d in zip(p0,p1,p2,p3)]

def tube(name,points,radii,mat,col,bones,steps=64,n=24,sub=1):
    rows=[(*p,*r) for p,r in zip(points,radii)]
    vv=[];ff=[]
    for j in range(steps+1):
        t=j/steps*(len(rows)-1);q=interp(rows,t);p=Vector(q[:3])
        qa=interp(rows,max(0,t-.005));qb=interp(rows,min(len(rows)-1,t+.005));axis=Vector(qb[:3])-Vector(qa[:3]);axis.normalize()
        ref=Vector((0,1,0))
        if abs(axis.y)>.95:ref=Vector((1,0,0))
        u=axis.cross(ref).normalized();v=axis.cross(u).normalized()
        rx=max(.0001,q[3]);ry=max(.0001,q[4])
        for k in range(n):vv.append(tuple(p+u*cos(k*2*pi/n)*rx+v*sin(k*2*pi/n)*ry))
        if j:
            for k in range(n):a=(j-1)*n+k;b=(j-1)*n+(k+1)%n;ff.append((a,b,b+n,a+n))
    ff.extend([tuple(reversed(range(n))),tuple(steps*n+k for k in range(n))])
    ob=mesh(name,vv,ff,col,mat,sub);bind(ob,bones)
    return ob

def curve(name,pts,r,mat,bones,col=TECH):
    return tube(name,pts,[(r,r)]*len(pts),mat,col,bones,steps=max(12,len(pts)*7),n=6,sub=1)

sections=[(.9,.060,.055,-.007),(.95,.094,.071,-.001),(1.00,.139,.092,.010),(1.045,.145,.083,.01),(1.10,.119,.068,.0),(1.15,.098,.062,-.001),(1.20,.103,.072,-.005),(1.25,.126,.080,-.009),(1.31,.146,.084,-.004),(1.36,.155,.079,.0),(1.41,.145,.064,.0),(1.448,.118,.048,.0),(1.465,.048,.040,.0)]
def torso_point(z,a):
    j=next((i for i in range(len(sections)-1) if sections[i][0]<=z<=sections[i+1][0]),len(sections)-2)
    t=j+(z-sections[j][0])/(sections[j+1][0]-sections[j][0]);q=interp(sections,t)
    x=q[1]*sin(a);y=q[3]-q[2]*cos(a)
    # The chest volume belongs to the torso surface, with no intersecting spheres.
    front=max(0,cos(a))**3
    y-=.092*exp(-((abs(x)-.072)/.064)**2-((z-1.321)/.061)**2)*front
    y-=.010*exp(-(x/.037)**2-((z-1.12)/.065)**2)*front
    # Smooth twin gluteal volumes are part of the same pelvis shell.
    back=max(0,-cos(a))**3
    y+=.034*exp(-((abs(x)-.07)/.056)**2-((z-.999)/.067)**2)*back
    return Vector((x,y,z))

vv=[];ff=[];N=64;M=80
for j in range(M+1):
    z=.902+(1.465-.902)*j/M
    for k in range(N):
        a=k*2*pi/N
        zz=z+.103*abs(sin(a))**1.6*max(0,1-(z-.902)/.16)**2
        vv.append(tuple(torso_point(zz,a)))
    if j:
        for k in range(N):a=(j-1)*N+k;b=(j-1)*N+(k+1)%N;ff.append((a,b,b+N,a+N))
ff.extend([tuple(reversed(range(N))),tuple(M*N+k for k in range(N))])
torso=mesh('Salve_Traje_CONTINUO',vv,ff,BODY,WHITE,1)
bind(torso,['pelvis','spine.01','spine.02','chest','neck'])
torso['topologia']='Retopologia regional de anillos quad; union anatomica y correctivos finales pendientes'
torso['fuente']='Frontal para identidad; laterales para profundidad'

# Surface patches follow body rings; the inserts are broad, tapered ribbons.
def body_patch(name,zs,angles,widths,side,mat=BLACK,depth=.0014):
    vv=[];ff=[];W=8
    for j,(z,a,w) in enumerate(zip(zs,angles,widths)):
        for k in range(W+1):
            aa=side*(a+(k/W-.5)*w)
            p=torso_point(z,aa);normal=Vector((sin(aa),-cos(aa),0));p+=normal*depth
            vv.append(tuple(p))
        if j:
            for k in range(W):idx=j*(W+1)+k;ff.append((idx-W-1,idx-W,idx+1,idx))
    ob=mesh(name,vv,ff,TECH,mat,2);bind(ob,['pelvis','spine.01','spine.02','chest'])
    sol=ob.modifiers.new('Espesor inserto','SOLIDIFY');sol.thickness=.0008
    return ob

for side,s in [('L',1),('R',-1)]:
    body_patch('Panel_flanco.'+side,[1.422,1.385,1.34,1.285,1.245,1.20,1.16,1.12,1.08,1.04],[1.16,.99,1.10,1.05,.96,.89,.94,1.04,1.20,1.15],[.10,.30,.29,.24,.19,.20,.21,.22,.24,.02],s)
    body_patch('Panel_espalda.'+side,[1.439,1.41,1.36,1.30,1.24,1.18,1.12,1.06],[2.49,2.43,2.20,2.19,2.32,2.42,2.34,2.24],[.25,.27,.19,.16,.14,.12,.14,.01],s)
    pts=[]
    for z,a in zip([1.41,1.36,1.31,1.26,1.20,1.15,1.10,1.05],[1.05,.99,1.06,.96,.82,.87,1.04,1.10]):
        p=torso_point(z,s*a);p+=Vector((s*sin(a),-cos(a),0))*.003;pts.append(tuple(p))
    curve('Circuito_flanco.'+side,pts,.0014,BLUE,['pelvis','spine.01','spine.02','chest'])
    pts=[tuple(torso_point(z,s*a)+Vector((0,-.0015,0))) for z,a in [(1.393,.05),(1.366,.17),(1.337,.22),(1.30,.27),(1.265,.46),(1.25,.7)]]
    curve('Costura_pecho.'+side,pts,.00055,SILVER,['spine.02','chest'])
    pts=[tuple(torso_point(z,s*a)+Vector((0,-.002,0))) for z,a in [(1.05,1.07),(1.014,.95),(.98,.69),(.94,.45),(.907,.3)]]
    curve('Circuito_pelvis.'+side,pts,.0011,BLUE,['pelvis'])

def diamond(name,pos,r,bones):
    x,y,z=pos
    ob=mesh(name,[(x,y,z+r),(x+r*.4,y,z),(x,y,z-r),(x-r*.4,y,z),(x,y-.003,z)],[(0,1,4),(1,2,4),(2,3,4),(3,0,4)],TECH,BLUE,0);bind(ob,bones)
    return ob
for i,(z,r) in enumerate([(1.411,.018),(1.392,.009),(1.25,.017),(1.05,.008)]):
    p=torso_point(z,0);p.y-=.003;diamond('Nucleo_%02d'%i,p,r,['pelvis','spine.01','spine.02','chest'])

def limb_patch(name,points,widths,bones,mat=BLACK):
    # Section in world coords: (x,y,z); stripe width projects transversely in x.
    vv=[];ff=[]
    for j,(p,w) in enumerate(zip(points,widths)):
        x,y,z=p
        for k in range(7):
            f=k/6-.5;vv.append((x+w*f,y-.0015*sqrt(max(0,1-(2*f)**2)),z))
        if j:
            for k in range(6):idx=j*7+k;ff.append((idx-7,idx-6,idx+1,idx))
    ob=mesh(name,vv,ff,TECH,mat,2);bind(ob,bones)
    m=ob.modifiers.new('Espesor panel','SOLIDIFY');m.thickness=.0008
    return ob

for side,s in [('L',1),('R',-1)]:
    thigh=['thigh.'+side,'shin.'+side]
    tube('Muslo_y_pierna_quad.'+side,[(s*.086,0,1.035),(s*.098,.003,.965),(s*.102,-.001,.882),(s*.091,-.015,.772),(s*.073,-.026,.65),(s*.071,-.023,.61),(s*.065,-.003,.52),(s*.058,.009,.40),(s*.056,.008,.28),(s*.056,.005,.149)],[(.045,.045),(.077,.073),(.075,.070),(.059,.061),(.038,.041),(.038,.040),(.055,.059),(.050,.055),(.033,.037),(.027,.030)],WHITE,BODY,thigh,steps=72,n=32)
    arm=['upper_arm.'+side,'forearm.'+side,'hand.'+side]
    tube('Brazo_quad.'+side,[(s*.132,0,1.434),(s*.157,-.003,1.390),(s*.198,-.009,1.303),(s*.235,-.015,1.22),(s*.254,-.017,1.182),(s*.286,-.020,1.119),(s*.32,-.023,1.053),(s*.342,-.025,1.0)],[(.047,.042),(.048,.043),(.039,.037),(.026,.029),(.027,.029),(.030,.032),(.021,.023),(.020,.020)],WHITE,BODY,arm,steps=48,n=28)
    tube('Guante_palmar_quad.'+side,[(s*.34,-.025,1.004),(s*.357,-.029,.97),(s*.38,-.033,.935),(s*.392,-.034,.918)],[(.018,.017),(.027,.016),(.031,.014),(.022,.013)],WHITE,BODY,'hand.'+side,steps=20,n=24)
    for fi,fn in enumerate(['thumb','index','middle','ring','pinky']):
        bones=[fn+'.'+str(j).zfill(2)+'.'+side for j in range(1,4)]
        p0=rig.data.bones[bones[0]].head_local.copy();p1=rig.data.bones[bones[0]].tail_local.copy();p2=rig.data.bones[bones[1]].tail_local.copy();p3=rig.data.bones[bones[2]].tail_local.copy()
        # Slightly flatter fingertips and distinct spaces between digits.
        tube('Dedo_'+fn+'_quad.'+side,[tuple(p0),tuple(p1),tuple(p2),tuple(p3)],[(.0065,.006),(.006,.0056),(.0051,.0048),(.0033,.0036)],WHITE,BODY,bones,steps=24,n=12)
        nail=p3*.7+p2*.3;nail.y-=.0045
        tube('Una_'+fn+'.'+side,[tuple(nail+Vector((0,0,.006))),tuple(nail),tuple(nail-Vector((0,0,.005)))],[(.0025,.001),(.003,.001),(.001,.0005)],BLACK,TECH,bones,steps=8,n=8)
    # A continuous boot last with a tapered, elevated heel.
    tube('Bota_quad.'+side,[(s*.056,-.156,.050),(s*.056,-.12,.067),(s*.056,-.079,.085),(s*.056,-.034,.11),(s*.056,.003,.15),(s*.056,.005,.18)],[(.024,.021),(.034,.027),(.036,.035),(.033,.035),(.027,.029),(.027,.03)],WHITE,BODY,['foot.'+side,'shin.'+side],steps=40,n=28)
    tube('Tacon.'+side,[(s*.056,.038,.118),(s*.056,.043,.07),(s*.056,.048,.018)],[(.012,.012),(.008,.009),(.008,.009)],BLACK,TECH,'foot.'+side,steps=20,n=12)
    tube('Suela.'+side,[(s*.056,-.157,.022),(s*.056,-.123,.019),(s*.056,-.079,.028),(s*.056,-.026,.055)],[(.024,.004),(.034,.005),(.033,.004),(.02,.003)],BLACK,TECH,'foot.'+side,steps=32,n=16)
    # Angular inserts follow the actual armored surface and repeat the reference cadence.
    paths=[('Muslo',[(s*.137,-.041,.999),(s*.143,-.051,.965),(s*.137,-.058,.918),(s*.129,-.056,.87),(s*.115,-.052,.81),(s*.094,-.054,.747),(s*.080,-.052,.676)],[.002,.029,.025,.021,.019,.011,.001],thigh),
    ('Espinilla',[(s*.086,-.046,.598),(s*.091,-.050,.553),(s*.088,-.044,.49),(s*.077,-.033,.398),(s*.063,-.027,.301),(s*.060,-.027,.24)],[.001,.022,.025,.018,.010,.001],thigh),
    ('Hombro',[(s*.161,-.031,1.408),(s*.169,-.043,1.377),(s*.188,-.043,1.338),(s*.213,-.038,1.294)],[.001,.019,.017,.001],arm),
    ('Antebrazo',[(s*.26,-.043,1.172),(s*.277,-.05,1.139),(s*.307,-.041,1.08),(s*.33,-.041,1.04)],[.001,.014,.015,.001],arm),
    ('Tobillo',[(s*.067,-.027,.27),(s*.073,-.031,.239),(s*.074,-.031,.207),(s*.06,-.041,.15)],[.001,.015,.013,.001],['shin.'+side,'foot.'+side])]
    for name,pts,ws,bns in paths:
        limb_patch('Panel_'+name+'.'+side,pts,ws,bns)
        ledpts=[(x-s*w*.25,y-.003,z) for (x,y,z),w in zip(pts,ws)]
        curve('Circuito_'+name+'.'+side,ledpts,.0012,BLUE,bns)
    limb_patch('Guante_dorso.'+side,[(s*.354,-.047,.971),(s*.377,-.049,.949),(s*.392,-.048,.928)],[.018,.036,.001],['hand.'+side])
    diamond('Led_guante.'+side,(s*.376,-.052,.951),.007,['hand.'+side])
    curve('Circuito_bota.'+side,[(s*.056,-.153,.04),(s*.056,-.155,.065),(s*.056,-.096,.118),(s*.056,-.033,.153)],.0012,BLUE,['foot.'+side])
    # Rear insert joins collar and shoulder in a coherent symmetric system.
    curve('Circuito_collar_v02.'+side,[(s*.026,-.036,1.536),(s*.02,-.044,1.51),(s*.009,-.045,1.485),(0,-.046,1.462)],.0015,BLUE,['neck'])

# Long hair is reconstructed as flattened curved locks, with tapered tips and strand relief.
for ob in list(HAIR.objects):
    if ob.name.startswith(('Mechon_largo','Mechon_rostro','Surco_mechon')):bpy.data.objects.remove(ob,do_unlink=True)
hair_created=[]
random.seed(29)
for side,s in [('L',1),('R',-1)]:
    for i in range(20):
        f=i/19;a=.5+f*2.58;rx=.069*sin(a);ry=.012-.061*cos(a)
        spread=.225+.123*sin(pi*f)**.7
        endz=.745+.035*sin(i*1.1)+.04*f
        yy=.018+.15*f
        xhead=(.074+.018*sin(a))*(1-f**3)+.006*f**3
        xshoulder=(.11+.012*sin(a))*(1-f**3)+.044*f**3
        points=[(s*rx,ry,1.72),(s*xhead,ry+.015,1.61),(s*xshoulder,yy,1.44),(s*(spread*.75),yy+.026,1.23),(s*spread,yy+.065,1.035),(s*(spread+.025),yy+.074,.916),(s*(spread-.025),yy+.033,.833),(s*(spread-.078),yy-.024,endz)]
        phase=.15*sin(i*2.2)
        points=[(x+s*.009*sin(j*.9+i)*j/7,y+.012*sin(j*.7+i)*j/7,z) for j,(x,y,z) in enumerate(points)]
        widths=[.009,.014,.020,.035,.043,.042,.026,.0003]
        depths=[.004,.004,.004,.005,.006,.006,.004,.0003]
        chains=['front','side','rear'];chain=chains[min(2,int(f*3))]
        bones=['head']+['hair_'+chain+'.'+str(j).zfill(2)+'.'+side for j in range(1,5)]
        ob=tube('Mechon_largo_%02d.'%i+side,points,list(zip(widths,depths)),HWHITE,HAIR,bones,steps=64,n=12)
        ob['cabello_cadena']=chain;ob['cabello_lado']=side;hair_created.append(ob)
        # Five subtle strand lines per leaf preserve the editable geometry.
        for k in range(3):
            p=[(x+s*w*(k-1)*.44,y-d*.93,z) for (x,y,z),w,d in zip(points,widths,depths)]
            line=curve('Surco_mechon_%02d_%d.'%(i,k)+side,p,.00009,HLINE,bones,HAIR);hair_created.append(line)
    for i in range(3):
        points=[(s*(.067+i*.005),-.033,1.66),(s*(.07+i*.008),-.058,1.59),(s*(.072+i*.011),-.063,1.515),(s*(.085+i*.015),-.075,1.455),(s*(.106+i*.011),-.084,1.39),(s*(.093+i*.014),-.09,1.35-i*.01)]
        ob=tube('Mechon_rostro_%02d.'%i+side,points,[(.008,.003),(.012,.004),(.013,.004),(.010,.003),(.006,.003),(.0002,.0002)],HWHITE,HAIR,['head','hair_front.01.'+side,'hair_front.02.'+side],steps=40,n=12)
        hair_created.append(ob)
for ob in hair_created:
    ob['rest_coords_v02']=json.dumps([list(v.co) for v in ob.data.vertices])

# Layered, swept fringe follows the crown parting rather than a uniform cut edge.
for ob in list(HAIR.objects):
    if ob.name.startswith('Flequillo'):bpy.data.objects.remove(ob,do_unlink=True)
for side,s in [('L',1),('R',-1)]:
    for i,(ex,ez) in enumerate([(0.004,1.632),(.019,1.642),(.034,1.635),(.047,1.650),(.061,1.635),(.077,1.614),(.085,1.620)]):
        pts=[(s*(.002+i*.003),.008,1.751),(s*(.004+i*.005),-.026,1.735),(s*(ex*.70),-.056,1.692),(s*(ex*.96),-.069,1.66),(s*ex,-.068+i*.001,ez)]
        ws=[.003,.007,.013,.012,.00015];ds=[.002,.003,.003,.0025,.00015]
        ob=tube('Flequillo_hoja_%02d.'%i+side,pts,list(zip(ws,ds)),HWHITE,HAIR,'head',steps=40,n=12)
        for k in range(2):
            curves=[(x+s*w*(k-.5)*.8,y-d*.96,z+.0001) for (x,y,z),w,d in zip(pts,ws,ds)]
            curve('Surco_flequillo_%02d_%d.'%(i,k)+side,curves,.00009,HLINE,'head',HAIR)

# More depth and a visible center in the headset; original rings and housing stay editable.
for side,s in [('L',1),('R',-1)]:
    for j,r in enumerate([.021,.009]):
        pts=[(s*.101,.013+r*cos(k*2*pi/64),1.664+r*sin(k*2*pi/64)) for k in range(65)]
        curve('Auricular_titanio_%d.'%j+side,pts,.0012,SILVER,'head')

# Rear panels and ionic collar detail are visible in the turnaround rather than only front.
for side,s in [('L',1),('R',-1)]:
    pts=[(s*.122,.055,.988),(s*.132,.058,.94),(s*.122,.058,.875),(s*.099,.051,.77),(s*.078,.028,.675)]
    limb_patch('Panel_muslo_posterior.'+side,pts,[.004,.021,.022,.016,.001],['thigh.'+side,'shin.'+side])
    curve('Circuito_muslo_posterior.'+side,[(x,y+.003,z) for x,y,z in pts],.0012,BLUE,['thigh.'+side,'shin.'+side])
    curve('Circuito_nuca.'+side,[(s*.022,.038,1.529),(s*.032,.04,1.492),(s*.018,.043,1.459),(0,.047,1.437)],.0016,BLUE,['neck','chest'])

# Pose-space volume correctives for joints. They supplement regional weights.
for side,s in [('L',1),('R',-1)]:
    for stem,name,z,cx,rad,amp,b0,b1 in [
        ('Muslo_y_pierna_quad','Rodilla',.612,s*.071,.066,.16,'thigh','shin'),
        ('Muslo_y_pierna_quad','Cadera',.977,s*.095,.093,.09,'pelvis','thigh'),
        ('Brazo_quad','Codo',1.22,s*.235,.058,.13,'upper_arm','forearm'),
        ('Brazo_quad','Hombro',1.411,s*.14,.066,.08,'chest','upper_arm')]:
        ob=bpy.data.objects[stem+'.'+side]
        if not ob.data.shape_keys:ob.shape_key_add(name='Basis',from_mix=False)
        key=ob.shape_key_add(name='Correctivo_'+name,from_mix=False)
        for v in key.data:
            w=amp*exp(-((v.co.z-z)/rad)**2)
            v.co.x=cx+(v.co.x-cx)*(1+w)
            v.co.y*=1+w*.6
        d=key.driver_add('value').driver;d.type='SCRIPTED'
        var=d.variables.new();var.name='bend';var.type='ROTATION_DIFF'
        for target,bone in zip(var.targets,[b0,b1]):
            target.id=rig;target.bone_target=bone if bone in ['pelvis','chest'] else bone+'.'+side
        n0=b0 if b0 in ['pelvis','chest'] else b0+'.'+side;n1=b1+'.'+side
        neutral_angle=rig.data.bones[n0].matrix_local.to_quaternion().rotation_difference(rig.data.bones[n1].matrix_local.to_quaternion()).angle
        d.expression='max(0,min(1,abs(bend-'+str(float(neutral_angle))+')/1.8))'
        ob['correctivos_estado']='Correctivos de volumen conducidos por angulo; revisar pliegues extremos'

# Project technological inserts onto the actual evaluated armor to prevent buried panels.
from mathutils.bvhtree import BVHTree
dg=bpy.context.evaluated_depsgraph_get()
surface_map={}
for side in ['L','R']:
    for stem in ['Muslo_y_pierna_quad','Brazo_quad','Bota_quad','Guante_palmar_quad']:
        ob=bpy.data.objects[stem+'.'+side];eo=ob.evaluated_get(dg);me=eo.to_mesh()
        surface_map[stem+'.'+side]=BVHTree.FromPolygons([eo.matrix_world@v.co for v in me.vertices],[list(p.vertices) for p in me.polygons],all_triangles=False)
        eo.to_mesh_clear()
for ob in created:
    if ob.users_collection[0]!=TECH or not ob.name.endswith(('.L','.R')):continue
    side=ob.name[-1];target=None
    if any(k in ob.name for k in ['Muslo','Espinilla','Tobillo','muslo_posterior']):target='Muslo_y_pierna_quad.'+side
    elif any(k in ob.name for k in ['Hombro','Antebrazo']):target='Brazo_quad.'+side
    elif any(k in ob.name for k in ['Guante','guante']):target='Guante_palmar_quad.'+side
    if target:
        bvh=surface_map[target]
        for v in ob.data.vertices:
            p=v.co;rear='posterior' in ob.name
            hit,normal,index,dist=bvh.ray_cast(Vector((p.x,1 if rear else -1,p.z)),Vector((0,-1 if rear else 1,0)),2)
            if hit:v.co.y=hit.y+(.0018 if rear else -.0018)

# Closed breast armor seams are fitted to the same chest surface.
for side,s in [('L',1),('R',-1)]:
    pts=[]
    for j in range(65):
        a=j*2*pi/64;x=s*(.072+.067*cos(a));z=1.325+.072*sin(a)
        sec=interp(sections,next((i+(z-sections[i][0])/(sections[i+1][0]-sections[i][0]) for i in range(len(sections)-1) if sections[i][0]<=z<=sections[i+1][0]),8))
        angle=math.asin(max(-.999,min(.999,x/sec[1])))
        p=torso_point(z,angle);p.y-=.0013;pts.append(tuple(p))
    curve('Costura_coraza.'+side,pts,.00055,SILVER,['spine.02','chest'])

# Real UV islands. Each component is independently unwrapped; an atlas is generated later.
for ob in created:
    if ob.type!='MESH':continue
    bpy.ops.object.select_all(action='DESELECT');ob.hide_set(False);ob.select_set(True);bpy.context.view_layer.objects.active=ob
    bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.uv.smart_project(angle_limit=math.radians(66),island_margin=.02)
    bpy.ops.object.mode_set(mode='OBJECT')
    ob['UV_estado']='Islas desplegadas smart_project; revisar densidad/pintura de produccion'

if old_action:rig.animation_data.action=old_action
scene.frame_set(1)
scene.render.film_transparent=True;scene.render.image_settings.file_format='PNG';scene.render.image_settings.color_mode='RGBA'
scene.render.resolution_x=1024;scene.render.resolution_y=1536;scene.render.resolution_percentage=100
ground=bpy.data.objects.get('Suelo_estudio')
if ground:ground.hide_render=True
scene['estado']='v02 refinamiento en revision; no aceptacion artistica ni produccion certificada'
print('BODY_HAIR_DONE',len(created),len(hair_created),flush=True)
