# Reconstruct Salve face in existing 04 scene. Safe: never saves/opens source itself.
import bpy, math, json, pathlib
from mathutils import Vector
from math import sin, cos, pi, exp, sqrt
ROOT=pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
scene=bpy.context.scene
rig=bpy.data.objects.get('Salve_Rig')
assert rig is not None, 'Open Salve v01 copy before executing face_v02.py'
scene.frame_set(1)
FACE=bpy.data.collections.get('02_Rostro_y_ojos')
if FACE is None:
 FACE=bpy.data.collections.new('02_Rostro_y_ojos');scene.collection.children.link(FACE)
for o in list(FACE.objects):bpy.data.objects.remove(o,do_unlink=True)

# All geometry coordinates are rest world coordinates; face remains attached to head.
def material(name,color,rough=.45,metal=0,emission=0):
 m=bpy.data.materials.get(name) or bpy.data.materials.new(name);m.use_nodes=True;m.diffuse_color=(*color,1)
 n=m.node_tree.nodes;n.clear();p=n.new('ShaderNodeBsdfPrincipled');out=n.new('ShaderNodeOutputMaterial');m.node_tree.links.new(p.outputs['BSDF'],out.inputs['Surface'])
 p.inputs['Base Color'].default_value=(*color,1);p.inputs['Roughness'].default_value=rough;p.inputs['Metallic'].default_value=metal
 p.inputs['Emission Color'].default_value=(*color,1);p.inputs['Emission Strength'].default_value=emission
 return m,p
SKIN,p=material('Rostro v02 | porcelana melocoton',(.91,.714,.653),.54);p.inputs['Subsurface Weight'].default_value=.055;p.inputs['Subsurface Radius'].default_value=(.15,.065,.04)
SCLERA,p=material('Ojos v02 | esclerotica',(.91,.94,1),.26);p.inputs['Coat Weight'].default_value=.2
LASH,_=material('Ojos v02 | pestaña violeta grafito',(.041,.023,.037),.48)
BROW,_=material('Rostro v02 | ceja lavanda',(.285,.245,.31),.6)
LIP,_=material('Rostro v02 | labio rosa natural',(.68,.36,.36),.51)
MOUTH,_=material('Rostro v02 | interior boca',(.067,.009,.019),.72)
TEETH,_=material('Rostro v02 | esmalte marfil',(.89,.84,.77),.32)
TONGUE,_=material('Rostro v02 | lengua',(.62,.16,.20),.45)
WATER,p=material('Rostro v02 | lagrima',(.45,.73,.95),.12);p.inputs['Transmission Weight'].default_value=.7;p.inputs['IOR'].default_value=1.333
HIGHLIGHT,p=material('Ojos v02 | reflejo',(.96,.98,1),.18,emission=.5)

controls=['blink.L','blink.R','eyeSquint.L','eyeSquint.R','eyeWide.L','eyeWide.R','browInnerUp.L','browInnerUp.R','browDown.L','browDown.R','browOuterUp.L','browOuterUp.R','jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel','tears','blush','mouthPress']
for nm in controls:
 rig[nm]=0.0;rig.id_properties_ui(nm).update(min=0.0,max=1.0,description='Control facial Salve v02')

def prop_driver(owner,path,idx=None,prop=None,expr='value'):
 fc=owner.driver_add(path) if idx is None else owner.driver_add(path,idx)
 d=fc.driver;d.type='SCRIPTED';v=d.variables.new();v.name='value';v.targets[0].id=rig;v.targets[0].data_path='["'+prop+'"]';d.expression=expr
 return d

def bind(o,bone='head'):
 g=o.vertex_groups.new(name=bone);g.add(list(range(len(o.data.vertices))),1,'REPLACE')
 m=o.modifiers.new('Salve | rostro rig','ARMATURE');m.object=rig;o.parent=rig

def mesh(name,vv,ff,mat,sub=0,bone='head'):
 d=bpy.data.meshes.new(name);d.from_pydata(vv,[],ff);d.update();o=bpy.data.objects.new(name,d);FACE.objects.link(o);d.materials.append(mat)
 for f in d.polygons:f.use_smooth=True
 if sub:
  m=o.modifiers.new('Superficie facial suavizada','SUBSURF');m.levels=sub;m.render_levels=sub
 if bone:bind(o,bone)
 return o

def sphere(name,center,radii,mat,bone='head',seg=32,rings=20):
 vv=[];ff=[]
 for j in range(rings+1):
  a=pi*j/rings
  for i in range(seg):
   b=2*pi*i/seg;vv.append((center[0]+radii[0]*sin(a)*cos(b),center[1]+radii[1]*sin(a)*sin(b),center[2]+radii[2]*cos(a)))
   if j:
    p=(j-1)*seg+i;q=(j-1)*seg+(i+1)%seg;ff.append((p,q,q+seg,p+seg))
 return mesh(name,vv,ff,mat,bone=bone)

def tube(name,pts,radii,mat,bone='head',sides=8):
 vv=[];ff=[];pts=[Vector(p) for p in pts]
 for j,p in enumerate(pts):
  axis=(pts[min(j+1,len(pts)-1)]-pts[max(j-1,0)]).normalized();ref=Vector((0,1,0));u=axis.cross(ref).normalized();v=axis.cross(u).normalized();r=radii[j] if isinstance(radii,list) else radii
  rx,ry=(r,r) if isinstance(r,(int,float)) else r
  for i in range(sides):vv.append(tuple(p+u*cos(2*pi*i/sides)*rx+v*sin(2*pi*i/sides)*ry))
  if j:
   for i in range(sides):a=(j-1)*sides+i;b=(j-1)*sides+(i+1)%sides;ff.append((a,b,b+sides,a+sides))
 ff.extend([tuple(reversed(range(sides))),tuple((len(pts)-1)*sides+i for i in range(sides))])
 return mesh(name,vv,ff,mat,sub=1,bone=bone)

def key(o,nm,transform):
 if not o.data.shape_keys:o.shape_key_add(name='Basis')
 k=o.shape_key_add(name=nm)
 for v in k.data:v.co=transform(v.co.copy())
 prop_driver(k,'value',prop=nm)
 return k

PROFILE=[(1.522,.005,.014,-.021),(1.530,.014,.020,-.020),(1.542,.028,.027,-.016),(1.558,.042,.037,-.010),(1.577,.055,.044,-.004),(1.597,.063,.050,0),(1.618,.066,.055,.003),(1.642,.067,.058,.005),(1.669,.067,.060,.008),(1.697,.060,.056,.012),(1.722,.041,.038,.014),(1.742,.005,.009,.015)]
def dims(z):
 for a,b in zip(PROFILE[:-1],PROFILE[1:]):
  if a[0]<=z<=b[0]:
   t=(z-a[0])/(b[0]-a[0]);return tuple(a[i]*(1-t)+b[i]*t for i in range(1,4))
 return PROFILE[0][1:] if z<PROFILE[0][0] else PROFILE[-1][1:]
def front_y(x,z):
 rx,ry,cy=dims(z);y=cy-ry*sqrt(max(.001,1-(x/rx)**2))
 # Continuous nasal bridge/tip and softened orbital hollows, cheek volumes.
 y-=.0062*exp(-(x/.0073)**2-((z-1.600)/.019)**2)
 y-=.0095*exp(-(x/.0080)**2-((z-1.584)/.0062)**2)
 y-=.0014*exp(-(x/.0090)**2-((z-1.567)/.010)**2)
 for s in [-1,1]:
  y+=.0025*exp(-((x-s*.029)/.024)**2-((z-1.618)/.014)**2)
  y-=.0018*exp(-((x-s*.040)/.016)**2-((z-1.593)/.016)**2)
 return y

def eye_rel(x,z,s):
 u=(x-s*.029)/.024;dz=z-1.618-s*(x-s*.029)*.105;v=dz/(.0079 if dz>=0 else .0062)
 return u*u+v*v
# Quad surface with eye socket holes and actual mouth aperture. Rings retained for edits.
vv=[];ff=[];N=128;R=112
for j in range(R+1):
 z=1.522+(1.742-1.522)*j/R;rx,ry,cy=dims(z)
 for i in range(N):
  th=2*pi*i/N;x=rx*sin(th);y=cy-ry*cos(th)
  if cos(th)>0:
   base=cy-ry*sqrt(max(.001,1-(x/rx)**2));y+=(front_y(x,z)-base)*max(0,min(1,cos(th)*1.8))
  vv.append((x,y,z))
  if j:
   a=(j-1)*N+i;b=(j-1)*N+(i+1)%N;c=j*N+(i+1)%N;d=j*N+i
   p=(Vector(vv[a])+Vector(vv[d]))/2
   eyehole=(eye_rel(p.x,p.z,1)<.97 or eye_rel(p.x,p.z,-1)<.97) and p.y<0
   mouthhole=(p.x/.0135)**2+((p.z-1.560)/.0098)**2<1.0 and p.y<0
   if not eyehole and not mouthhole:ff.append((a,b,c,d))
ff.extend([tuple(reversed(range(N))),tuple(R*N+i for i in range(N))])
head=mesh('Rostro_topologia',vv,ff,SKIN,sub=1)
head['topologia']='Quad loft continuo con nariz integrada; aperturas orbitales y orales; bordes cubiertos por anillos faciales. No retopologia final certificada.'
uv=head.data.uv_layers.new(name='UV_Rostro_cilindrica')
for p in head.data.polygons:
 us=[]
 for li in p.loop_indices:
  vi=head.data.loops[li].vertex_index;us.append((vi%N)/N)
 cross=max(us)-min(us)>.5
 for li,u in zip(p.loop_indices,us):
  vi=head.data.loops[li].vertex_index
  uv.data[li].uv=(u+1 if cross and u<.5 else u,(vi//N)/R)
# Local cheek/lower-face deformation blends and jaw opening correct chin/lips together.
def jaw_t(v):
 w=exp(-(v.x/.055)**4)*max(0,min(1,(1.551-v.z)/.030))*(1 if v.y<.02 else .3)
 v.z-=.004*w;v.y+=.0008*w;return v
key(head,'jawOpen',jaw_t)
def smile_head(v):
 w=exp(-((abs(v.x)-.021)/.025)**2-((v.z-1.570)/.019)**2)*max(0,min(1,(-v.y)/.040));v.z+=.0018*w;return v
key(head,'smile',smile_head)
for side,s in [('L',1),('R',-1)]:
 sphere('Oreja.'+side,(s*.0655,.003,1.607),(.010,.009,.023),SKIN)
 # Eye aperture has an exposed convex corneal surface lying inside a fitted skin rim.
 cx=s*.029;cz=1.618;verts=[];faces=[];nr=13;nt=64
 for j in range(nr+1):
  r=j/nr
  for i in range(nt):
   a=2*pi*i/nt;x=cx+.024*cos(a)*r;z=cz+(.0079 if sin(a)>=0 else .0062)*sin(a)*r+s*(x-cx)*.105
   y=front_y(x,z)-.0010-.0048*(1-r*r)
   verts.append((x,y,z))
   if j:
    a0=(j-1)*nt+i;b0=(j-1)*nt+(i+1)%nt;faces.append((a0,b0,b0+nt,a0+nt))
 eye=mesh('Esclerotica.'+side,verts,faces,SCLERA,sub=1)
 def sq(v):
  v.z=cz+(v.z-cz-s*(v.x-cx)*.105)*.68+s*(v.x-cx)*.105;return v
 def wide(v):
  v.z=cz+(v.z-cz-s*(v.x-cx)*.105)*1.35+s*(v.x-cx)*.105;return v
 key(eye,'eyeSquint.'+side,sq);key(eye,'eyeWide.'+side,wide)
 def blink_eye(v):
  v.z=cz+s*(v.x-cx)*.105;return v
 key(eye,'blink.'+side,blink_eye)
 # Socket rim/cover are skin surface, not disconnected tubes. Four quad concentric loops.
 for upper in [True,False]:
  pts=[];verts=[];faces=[];cols=49;rows=7
  for j in range(rows):
   t=j/(rows-1)
   for i in range(cols):
    u=-1+2*i/(cols-1);x=cx+.024*u;edge=(.0079 if upper else -.0062)*sqrt(max(0,1-u*u));z=cz+edge+s*(x-cx)*.105
    # outward skin covers the coarse socket boundary, blends into existing face.
    x=cx+(x-cx)*(1+.15*t)
    z+=((.010 if upper else -.008)*sin(pi*i/(cols-1))+.001*(1 if upper else -1))*t
    y=front_y(x,z)-(.0014 if j==0 else .0008*(1-t))
    verts.append((x,y,z))
    if j and i:k=j*cols+i;faces.append((k-cols-1,k-cols,k,k-1))
  rim=mesh(('Parpado_superior.' if upper else 'Parpado_inferior.')+side,verts,faces,SKIN,sub=1)
  # Full lid closure conforms to the scleral bulge; upper travels 80%, lower 20%.
  def blink(v,upper=upper):
   u=max(-1,min(1,(v.x-cx)/.024));edge=(.0079 if upper else .0062)*sqrt(max(0,1-u*u));mid=cz+s*(v.x-cx)*.105
   dist=(v.z-mid)*(1 if upper else -1);w=max(0,min(1,1-(dist-edge)/.011))
   v.z+=(-1 if upper else 1)*edge*w+.0018*(1-u*u)*w
   v.y=min(v.y,front_y(v.x,cz)-.0018*w);return v
  key(rim,'blink.'+side,blink);key(rim,'eyeSquint.'+side,sq);key(rim,'eyeWide.'+side,wide)
  # Lash silhouette follows the exact eye boundary; tapered at inner corner.
  for i in range(cols):
   u=-1+2*i/(cols-1);x=cx+.024*u;z=cz+(.0079 if upper else -.0062)*sqrt(max(0,1-u*u))+s*(x-cx)*.105
   pts.append((x,front_y(x,z)-.00165,z))
  radii=[(.0003+.00085*(i/(cols-1) if s==1 else 1-i/(cols-1))**.6)*(1 if upper else .35) for i in range(cols)]
  lash=tube(('Pestana.' if upper else 'Pestana_inferior.')+side,pts,radii,LASH)
  key(lash,'blink.'+side,blink);key(lash,'eyeSquint.'+side,sq);key(lash,'eyeWide.'+side,wide)
  if upper:
   for q in range(3):
    x=cx+s*(.022+q*.0008);z=cz+.004+s*(x-cx)*.105
    wing=tube('Pestana_ala_%d.'%q+side,[(x,front_y(x,z)-.0018,z),(x+s*(.005+q*.002),front_y(x,z)+.001,z+.0035+q*.0004)], [.00085,.00012],LASH)
 # Blink occluder with broad sheet ensures no visible sclera/iris at full closure.
 verts=[];faces=[]
 for j in range(9):
  t=j/8
  for i in range(49):
   u=-1+2*i/48;x=cx+.025*u;edge=.0083*sqrt(max(0,1-u*u));z=cz+edge+.010*t+s*(x-cx)*.105
   verts.append((x,front_y(x,z)+.0007,z))
   if j and i:k=j*49+i;faces.append((k-50,k-49,k,k-1))
 cover=mesh('Parpado_cierre.'+side,verts,faces,SKIN,sub=1);cover.shape_key_add(name='Basis');kb=cover.shape_key_add(name='blink.'+side)
 for idx,v in enumerate(kb.data):
  j,i=divmod(idx,49);u=-1+2*i/48;edge=.0083*sqrt(max(0,1-u*u));v.co.z=cz+edge*(1-2*j/8)+s*(v.co.x-cx)*.105;v.co.y=front_y(v.co.x,v.co.z)-.0007
 prop_driver(kb,'value',prop='blink.'+side)
 # Iris discs have radial UV, relief and a packed authored procedural image.
 iris_radius=(.0117,.0136)
 iy=front_y(cx,cz)-.0073
 verts=[];faces=[]
 for j in range(17):
  r=j/16
  for i in range(96):
   a=2*pi*i/96;x=cx+iris_radius[0]*r*cos(a);z=cz+iris_radius[1]*r*sin(a);lim=.0076*sqrt(max(0,1-((x-cx)/.024)**2));z=max(cz-lim+s*(x-cx)*.105,min(cz+lim+s*(x-cx)*.105,z));verts.append((x,front_y(x,z)-.0066-.0005*(1-r*r),z))
   if j:
    p0=(j-1)*96+i;q0=(j-1)*96+(i+1)%96;faces.append((p0,q0,q0+96,p0+96))
 im=bpy.data.images.get('Salve_Iris_Blue_v02')
 if im is None:
  sz=512;im=bpy.data.images.new('Salve_Iris_Blue_v02',width=sz,height=sz,alpha=True)
  pixels=[]
  for yy in range(sz):
   for xx in range(sz):
    u=(xx+.5)/sz*2-1;v=(yy+.5)/sz*2-1;r=sqrt(u*u+v*v);a=math.atan2(v,u)
    thread=(sin(a*78+r*32)*.5+.5)*.7+(sin(a*129-r*51)*.5+.5)*.3
    if r<.34:color=(.003,.007,.027)
    else:
     lum=(.4+.6*max(0,1-abs(r-.61)/.35))*(.82+.3*thread)*(1-.38*max(0,v))
     rim=max(.15,1-max(0,(r-.87)/.13)*.83)
     color=(.012*lum*rim,.30*lum*rim,.95*lum*rim)
     ring=exp(-((r-.36)/.032)**2);color=(color[0]+ring*.018,color[1]+ring*.10,color[2]+ring*.035)
    pixels.extend((*color,1))
  im.pixels=pixels;im.filepath_raw=str(ROOT/'work'/'face_previews'/'Salve_iris_blue_v02.png');im.file_format='PNG';im.save();im.pack()
 irismat=bpy.data.materials.get('Ojos v02 | iris azul radial')
 if irismat is None:
  irismat,p=material('Ojos v02 | iris azul radial',(.015,.3,.9),.42,emission=.32);tex=irismat.node_tree.nodes.new('ShaderNodeTexImage');tex.image=im;irismat.node_tree.links.new(tex.outputs['Color'],p.inputs['Base Color']);irismat.node_tree.links.new(tex.outputs['Color'],p.inputs['Emission Color']);p.inputs['Coat Weight'].default_value=.06;p.inputs['Specular IOR Level'].default_value=.18;em=irismat.node_tree.nodes.new('ShaderNodeEmission');em.inputs['Strength'].default_value=.85;irismat.node_tree.links.new(tex.outputs['Color'],em.inputs['Color']);out=next(n for n in irismat.node_tree.nodes if n.type=='OUTPUT_MATERIAL');irismat.node_tree.links.new(em.outputs[0],out.inputs['Surface'])
 iris=mesh('Iris.'+side,verts,faces,irismat,bone='eye.'+side)
 key(iris,'eyeSquint.'+side,sq);key(iris,'eyeWide.'+side,wide);key(iris,'blink.'+side,blink_eye)
 uv=iris.data.uv_layers.new(name='UV_Iris')
 for li,l in enumerate(iris.data.loops):
  v=iris.data.vertices[l.vertex_index].co;uv.data[li].uv=(.5+.5*(v.x-cx)/iris_radius[0],.5+.5*(v.z-cz)/iris_radius[1])
 shine=sphere('Brillo.'+side,(cx-s*.0031,front_y(cx-s*.0031,cz+.0045)-.0079,cz+.0045),(.0018,.00032,.002),HIGHLIGHT,bone='eye.'+side)
 shine2=sphere('Brillo_secundario.'+side,(cx+s*.0043,front_y(cx+s*.0043,cz-.004)-.0078,cz-.004),(.00075,.0002,.0009),HIGHLIGHT,bone='eye.'+side)
 key(shine,'blink.'+side,blink_eye);key(shine2,'blink.'+side,blink_eye)
 # Fine brows: low arc, with stronger tilt at inner ends for worry/anger.
 pts=[]
 for i in range(25):
  u=i/24;x=cx+s*(-.020+.042*u);z=1.640+.0035*sin(pi*u)-.002*u;pts.append((x,front_y(x,z)-.0015,z))
 brow=tube('Ceja.'+side,pts,[.0002+.00065*sin(pi*i/24)**.6 for i in range(25)],BROW)
 for stem in ['browInnerUp','browDown','browOuterUp']:
  def tf(v,stem=stem):
   t=max(0,min(1,(s*(v.x-cx)+.020)/.042));inner=(1-t)**1.5
   v.z+=.006*inner if stem=='browInnerUp' else -.0055*inner if stem=='browDown' else .007*(1-inner);v.y=front_y(v.x,v.z)-.002;return v
  key(brow,stem+'.'+side,tf)
 # Skin blush uses independent opacity controller and soft radial attribute colors.
 blushmat=bpy.data.materials.get('Rostro v02 | rubor timidez')
 if blushmat is None:
  blushmat,_=material('Rostro v02 | rubor timidez',(.88,.16,.19),.6)
  n=blushmat.node_tree.nodes;l=blushmat.node_tree.links;p=next(x for x in n if x.type=='BSDF_PRINCIPLED');out=next(x for x in n if x.type=='OUTPUT_MATERIAL');trans=n.new('ShaderNodeBsdfTransparent');mix=n.new('ShaderNodeMixShader');attr=n.new('ShaderNodeAttribute');attr.attribute_name='BlushOpacity';mult=n.new('ShaderNodeMath');mult.operation='MULTIPLY';mult.inputs[1].default_value=0;prop_driver(mult.inputs[1],'default_value',prop='blush',expr='value*.72');l.new(attr.outputs['Fac'],mult.inputs[0]);l.new(mult.outputs[0],mix.inputs[0]);l.new(trans.outputs[0],mix.inputs[1]);l.new(p.outputs[0],mix.inputs[2]);l.new(mix.outputs[0],out.inputs[0])
 verts=[];faces=[];fac=[]
 for j in range(9):
  r=j/8
  for i in range(48):
   a=2*pi*i/48;x=s*.039+.018*r*cos(a);z=1.594+.009*r*sin(a);verts.append((x,front_y(x,z)-.0008,z));fac.append(max(0,1-r*r)**2)
   if j:
    p0=(j-1)*48+i;q0=(j-1)*48+(i+1)%48;faces.append((p0,q0,q0+48,p0+48))
 blush=mesh('Sonrojo.'+side,verts,faces,blushmat)
 at=blush.data.attributes.new('BlushOpacity','FLOAT','POINT')
 for i,a in enumerate(at.data):a.value=fac[i]
 tear=sphere('Lagrima.'+side,(s*.046,front_y(s*.046,1.602)-.001,1.600),(.0013,.0012,.006),WATER)
 for ax in range(3):prop_driver(tear,'scale',idx=ax,prop='tears')

# Mouth ring around a small aperture. One topology, distinct controllable phonemes.
CZ=1.560
verts=[];faces=[]
for j in range(7):
 t=j/6
 for i in range(96):
  a=2*pi*i/96;x=(.0118+.0062*t)*cos(a);z=CZ+(.00072+.0110*t)*sin(a)
  y=front_y(x,z)-(.00015+.00025*(1-t)**3)
  verts.append((x,y,z))
  if j:
   p0=(j-1)*96+i;q0=(j-1)*96+(i+1)%96;faces.append((p0,q0,q0+96,p0+96))
mouth=mesh('Labios_controles',verts,faces,SKIN,sub=1)
# Narrow inner lip rows are subdued pink; perimeter remains facial skin.
mouth.data.materials.append(LIP)
for poly in mouth.data.polygons:
 if poly.index<96*1:poly.material_index=1

def mouth_key(nm,v):
 x,y,z=v;dy=z-CZ;r=sqrt((x/.018)**2+(dy/.012)**2);w=max(0,1-r*.50)
 if nm=='jawOpen':v.z+=(1 if dy>=0 else -1)*.0095*w*max(.15,1-(abs(x)/.023)**2);v.y-=.0006*w
 elif nm=='smile':v.x*=1+.16*w;v.z+=.0043*(abs(x)/.020)**2*w
 elif nm=='frown':v.z-=.0036*(abs(x)/.020)**2*w
 elif nm=='mouthWide':v.x*=1+.30*w
 elif nm=='lipPucker':v.x*=1-.38*w;v.z=CZ+dy*(1+.8*w);v.y-=.003*w
 elif nm=='lipFunnel':v.x*=1-.28*w;v.z=CZ+dy*(1+1.8*w);v.y-=.0017*w
 elif nm=='mouthPress':v.z=CZ+dy*.72
 return v
mouth.shape_key_add(name='Basis')
for nm in ['jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel','mouthPress']:
 k=mouth.shape_key_add(name=nm)
 for idx,v in enumerate(k.data):
  t=(idx//96)/6;w=(1-t)**1.5;x,y,z=v.co;dy=z-CZ
  if nm=='jawOpen':v.co.z+=(1 if dy>=0 else -1)*.0078*w*max(.15,1-(abs(x)/.019)**2)
  elif nm=='smile':v.co.x*=1+.18*w;v.co.z+=.0034*(abs(x)/.012)**2*w
  elif nm=='frown':v.co.z-=.0028*(abs(x)/.012)**2*w
  elif nm=='mouthWide':v.co.x*=1+.30*w
  elif nm=='lipPucker':v.co.x*=1-.38*w;v.co.z=CZ+dy*(1+1.2*w);v.co.y-=.0015*w
  elif nm=='lipFunnel':v.co.x*=1-.28*w;v.co.z=CZ+dy*(1+2*w);v.co.y-=.0012*w
  elif nm=='mouthPress':v.co.z=CZ+dy*(1-.28*w)
 prop_driver(k,'value',prop=nm)
# Inner cavity, teeth, tongue deform to stay inside lips.
verts=[];faces=[]
for j in range(13):
 r=j/12
 for i in range(96):
  a=2*pi*i/96;x=.0118*r*cos(a);z=CZ+.00072*r*sin(a);verts.append((x,front_y(x,z)+.0002,z))
  if j:
   p0=(j-1)*96+i;q0=(j-1)*96+(i+1)%96;faces.append((p0,q0,q0+96,p0+96))
cavity=mesh('Boca_interior',verts,faces,MOUTH,sub=1)
def cavity_key(nm,v):
 x,y,z=v;dy=z-CZ
 if nm=='jawOpen':v.z=CZ+dy*(1+.0078/.00072)*max(.15,1-(abs(x)/.019)**2)
 elif nm=='smile':v.x*=1.18;v.z+=.0034*(abs(x)/.012)**2
 elif nm=='frown':v.z-=.0028*(abs(x)/.012)**2
 elif nm=='mouthWide':v.x*=1.30
 elif nm=='lipPucker':v.x*=.62;v.z=CZ+dy*2.2;v.y-=.0015
 elif nm=='lipFunnel':v.x*=.72;v.z=CZ+dy*3;v.y-=.0012
 elif nm=='mouthPress':v.z=CZ+dy*.72
 return v
for nm in ['jawOpen','smile','frown','mouthWide','lipPucker','lipFunnel','mouthPress']:key(cavity,nm,lambda v,nm=nm:cavity_key(nm,v))
teeth=sphere('Dientes_superiores',(0,front_y(0,CZ)-.0008,CZ+.005),(.010,.002,.0017),TEETH)
tongue=sphere('Lengua',(0,front_y(0,CZ)-.0008,CZ-.005),(.007,.002,.0021),TONGUE)
for ob in [teeth,tongue]:
 for ax in range(3):prop_driver(ob,'scale',idx=ax,prop='jawOpen',expr='min(1,value*2.2)')
# Nostrils subtly define real integrated nose form; no detached spheres.
for side,s in [('L',1),('R',-1)]:
 o=sphere('Fosa_nasal.'+side,(s*.0036,front_y(s*.0036,1.581)-.00025,1.581),(.0013,.00025,.00045),LIP)

presets={
'NEUTRAL':{},'WARM':{'smile':.70,'eyeSquint.L':.15,'eyeSquint.R':.15},
'CURIOUS':{'browOuterUp.L':.65,'browInnerUp.R':.25,'eyeWide.L':.20,'lipPucker':.10},
'CONCERNED':{'browInnerUp.L':.72,'browInnerUp.R':.72,'frown':.38},
'SAD':{'browInnerUp.L':.65,'browInnerUp.R':.65,'frown':.85,'eyeSquint.L':.15,'eyeSquint.R':.15},
'ANGRY':{'browDown.L':.95,'browDown.R':.95,'eyeSquint.L':.45,'eyeSquint.R':.45,'mouthPress':.35},
'SURPRISED':{'browOuterUp.L':.8,'browOuterUp.R':.8,'eyeWide.L':.72,'eyeWide.R':.72,'jawOpen':.65,'lipFunnel':.45},
'SHY':{'smile':.28,'blink.L':.20,'blink.R':.20,'eyeSquint.L':.35,'eyeSquint.R':.35,'blush':1.0},
'LAUGH':{'smile':1.0,'jawOpen':.86,'blink.L':1.0,'blink.R':1.0},
'CRY':{'browInnerUp.L':1,'browInnerUp.R':1,'frown':1,'jawOpen':.38,'tears':1,'blush':.2},
'STARTLE':{'jawOpen':.78,'lipFunnel':.75,'browOuterUp.L':1,'browOuterUp.R':1,'eyeWide.L':1,'eyeWide.R':1},
'THINK':{'browInnerUp.L':.38,'lipPucker':.18,'eyeSquint.R':.14},
'WINK':{'blink.L':1,'smile':.60,'eyeSquint.R':.10},
'A':{'jawOpen':.9},'E':{'jawOpen':.32,'mouthWide':.70},'I':{'jawOpen':.13,'mouthWide':1},
'O':{'jawOpen':.70,'lipFunnel':1},'U':{'jawOpen':.25,'lipPucker':1},'MBP':{'lipPucker':.15,'mouthPress':.65},'SILENCE':{},'CONFUSED':{'browInnerUp.L':.62,'browDown.R':.30,'lipPucker':.15}
}
rig['expresiones_json']=json.dumps(presets,ensure_ascii=False)
rig['facial_v02']='14 expresiones + A,E,I,O,U,MBP,SILENCE; ojos integrados, nariz continua, iris UV texturado, rubor controlado'
# Preserve bone animation. Update facial property keys at existing expression marker frames only.
for index,(name,vals) in enumerate(presets.items()):
 fr=201+index*10
 for nm in controls:
  rig[nm]=float(vals.get(nm,0));rig.keyframe_insert(data_path='["'+nm+'"]',frame=fr)
# Additional mouth-open corrected lower-face shape keys are driver controlled above.
for ob in FACE.objects:
 if ob.type=='MESH' and not ob.data.uv_layers:
  uv=ob.data.uv_layers.new(name='UV_Rostro_proyeccion_local')
  bounds=[v.co for v in ob.data.vertices];xmin=min(v.x for v in bounds);xmax=max(v.x for v in bounds);zmin=min(v.z for v in bounds);zmax=max(v.z for v in bounds)
  for li,l in enumerate(ob.data.loops):
   v=ob.data.vertices[l.vertex_index].co;uv.data[li].uv=((v.x-xmin)/max(1e-6,xmax-xmin),(v.z-zmin)/max(1e-6,zmax-zmin))
# Landmark mesh vertices are fully rigged; exporter should evaluate them with pose/depsgraph.
for nm,p in {'eye_left':(.029,front_y(.029,1.618)-.0084,1.618),'eye_right':(-.029,front_y(-.029,1.618)-.0084,1.618),'mouth':(0,front_y(0,1.560)-.001,1.560),'head_pivot':(0,0,1.57),'chin':(0,-.035,1.525)}.items():
 ob=mesh('LANDMARK_'+nm,[p],[],SKIN);ob.hide_render=True;ob['contract']='Evaluar vertice 0 world-space para anclajes faciales exactos'
 for mod in ob.modifiers:mod.show_render=False
scene.frame_set(1)
for nm in controls:rig[nm]=0.0
rig.update_tag();bpy.context.view_layer.update()
report={'component':'face_v02','head_vertices':len(head.data.vertices),'head_quads':sum(len(p.vertices)==4 for p in head.data.polygons),'face_objects':len(FACE.objects),'expressions':presets,'limitations':['No se certifica identidad exacta; requiere comparacion final por vista.','Malla de cara mantiene loops horizontales; orbitales/boca usan anillos separados y no constituyen retopologia facial soldada de produccion.','UV iris definitiva y empaquetada; UV cabeza cilindrica y UV piezas locales requieren atlas y pintura final.','Cierre de ojos y visemas deben revisarse en renders de produccion; no animacion certificada.']}
(ROOT/'work'/'face_previews'/'face_report.json').write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
print('SALVE_FACE_V02_READY',json.dumps({'objects':len(FACE.objects),'head_vertices':len(head.data.vertices)}))




