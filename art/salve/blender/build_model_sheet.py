import json, math, pathlib, hashlib
from PIL import Image
from reportlab.pdfgen import canvas
from reportlab.lib.colors import HexColor, Color, white
from reportlab.lib.utils import ImageReader
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import Paragraph, Table, TableStyle
from reportlab.lib.styles import ParagraphStyle

ROOT=pathlib.Path(__file__).parent
OUT=ROOT/'Salve_hoja_tecnica_modelado.pdf'
W,H=1190.55,841.89
INK=HexColor('#14263D'); MUTED=HexColor('#4A6277'); BLUE=HexColor('#126CE5'); CYAN=HexColor('#10B9D0'); LIGHT=HexColor('#EFF4FA'); RULE=HexColor('#D8E2EC')
font_options=[
    (pathlib.Path('C:/Windows/Fonts/arial.ttf'),pathlib.Path('C:/Windows/Fonts/arialbd.ttf')),
    (pathlib.Path('/usr/share/fonts/truetype/liberation2/LiberationSans-Regular.ttf'),pathlib.Path('/usr/share/fonts/truetype/liberation2/LiberationSans-Bold.ttf')),
    (pathlib.Path('/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf'),pathlib.Path('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf')),
]
regular,bold=next(((a,b) for a,b in font_options if a.exists() and b.exists()),(None,None))
if regular is None:
    raise RuntimeError('Install Arial, Liberation Sans or DejaVu Sans to rebuild the PDF.')
pdfmetrics.registerFont(TTFont('Inter',str(regular)))
pdfmetrics.registerFont(TTFont('InterBold',str(bold)))
c=canvas.Canvas(str(OUT),pagesize=(W,H));c.setTitle('Salve | Hoja técnica de modelado y rigging');c.setAuthor('Proyecto Salve')
manifest=json.loads((ROOT/'references/manifest.json').read_text(encoding='utf-8'))
views={v['id']:v for v in manifest['views']}; page=0
style=ParagraphStyle('body',fontName='Inter',fontSize=11,leading=16,textColor=INK)
small=ParagraphStyle('small',parent=style,fontSize=9,leading=13)
def text(s,x,y,size=11,color=INK,bold=False):
    c.setFillColor(color);c.setFont('InterBold' if bold else 'Inter',size);c.drawString(x,y,s)
def para(s,x,y,w,fs=11):
    st=ParagraphStyle('p',parent=style,fontSize=fs,leading=fs*1.42)
    p=Paragraph(s,st);aw,ah=p.wrap(w,1000);p.drawOn(c,x,y-ah);return y-ah
def start(title,kicker):
    global page
    page+=1;c.setFillColor(white);c.rect(0,0,W,H,fill=1,stroke=0)
    c.setFillColor(BLUE);c.rect(36,H-47,32,5,fill=1,stroke=0)
    text('SALVE / NÚCLEO',79,H-49,10,BLUE,True)
    text(kicker.upper(),W-340,H-49,9,MUTED)
    text(title,36,H-94,27,INK,True)
    c.setStrokeColor(RULE);c.line(36,49,W-36,49)
    text('29 SEP 2026  /  REFERENCIA DE PRODUCCIÓN  /  v01',36,30,9,MUTED)
    text(f'{page:02d}',W-56,30,10,BLUE,True)
def end():c.showPage()
def box(x,y,w,h,fill=LIGHT):
    c.setFillColor(fill);c.roundRect(x,y,w,h,9,fill=1,stroke=0)
def photo(id,x,y,w,h,label=None,bg=True):
    if bg:box(x,y,w,h)
    path=ROOT/'references'/pathlib.Path(views[id]['path']).name
    c.drawImage(ImageReader(str(path)),x+5,y+22,w-10,h-30,preserveAspectRatio=True,anchor='c',mask='auto')
    if label is not None:text(label,x+10,y+8,9,MUTED)
def table(rows,x,top,widths,fs=10):
    st=ParagraphStyle('cell',parent=small,fontSize=fs,leading=fs*1.35)
    data=[[Paragraph(str(t),st) for t in row] for row in rows]
    t=Table(data,colWidths=widths,hAlign='LEFT')
    t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),LIGHT),('VALIGN',(0,0),(-1,-1),'TOP'),('LEFTPADDING',(0,0),(-1,-1),9),('RIGHTPADDING',(0,0),(-1,-1),9),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),7),('LINEBELOW',(0,0),(-1,0),1,RULE),('LINEBELOW',(0,1),(-1,-1),.4,RULE)]))
    tw,th=t.wrap(W,H);t.drawOn(c,x,top-th);return top-th
def heading(s,x,y):text(s,x,y,14,BLUE,True);return y-15

start('Identidad y diseño canónico','01 / turnaround')
for id,x,w in [('front',36,275),('right',325,250),('rear',589,275)]:photo(id,x,84,w,635,views[id]['label'])
y=heading('Mantener el diseño',887,699)
y=para('Cabello blanco largo, ojos azules luminosos, traje ajustado blanco, paneles negros y circuitos azul neón. Auriculares negros con aro azul y puntas tecnológicas.',887,y,265)
y=heading('Prioridad de referencia',887,y-32)
y=para('<b>Frontal:</b> identidad, rostro y proporciones.<br/><b>A frontal / A trasera:</b> construcción y simetría.<br/><b>Laterales:</b> profundidad, perfil y tacón.<br/><b>Poses:</b> silueta y contactos.',887,y,265)
y=heading('Decisiones pendientes',887,y-32)
y=para('Las imágenes son ilustraciones independientes: el volumen del cabello, la colocación de paneles y la apertura de brazos pueden variar. Resolver estas diferencias en una única malla; conservar siempre los PNG originales.',887,y,265)
y=heading('Escala de trabajo',887,y-32)
y=para('1,76 m en Blender es una escala provisional para calibrar el esqueleto. No se presenta como una medida canónica del personaje.',887,y,265)
box(887,88,267,99)
para('<b>Estado comprobado</b><br/>PR #110 integrado: reacciones y poses 2D. Esta entrega prepara la referencia y el esqueleto inicial para comenzar el 3D.',901,171,238,10)
end()

for part in range(2):
    start(f'Las 20 vistas originales / {part+1} de 2','02 / archivo visual verificado')
    for j,v in enumerate(manifest['views'][part*10:(part+1)*10]):
        col=j%5;row=j//5;x=36+col*225;y=391 if row==0 else 70
        photo(v['id'],x,y,216,307,f'{part*10+j+1:02d}  {v["label"]}')
    text('20/20 SHA-256 coinciden con manifest.json. Se conserva la proporción original, incluida la variante horizontal de rodillas.',36,720,10,MUTED)
    end()

start('Expresiones faciales y visemas','03 / propuesta visual para esculpir')
c.drawImage(str(ROOT/'design/Salve_expresiones_propuesta.png'),36,68,708,661,preserveAspectRatio=True,anchor='c')
y=heading('Conservar las expresiones del PR',770,711)
y=para('NEUTRAL · WARM · CURIOUS · CONCERNED · SAD · ANGRY · SURPRISED · SHY.<br/><br/>Gestos asociados: LAUGH, CRY y STARTLE. La lámina amplía el repertorio con guiño, mirada pensativa y vocales.',770,y,380)
y=heading('Controles faciales propuestos',770,y-30)
y=para('blink.L/R, squint.L/R, eyeWide.L/R, browInnerUp, browDown.L/R, browOuterUp.L/R, smile.L/R, frown.L/R, jawOpen, mouthWide, lipPucker y lipFunnel.',770,y,380)
y=heading('Lip sync',770,y-30)
y=para('<b>A:</b> apertura vertical.<br/><b>E:</b> abertura media y labios extendidos.<br/><b>I:</b> labios extendidos, apertura pequeña.<br/><b>O:</b> boca redondeada.<br/><b>U:</b> labios juntos hacia delante.<br/>Añadir silencio y cierres M/B/P, contacto F/V y lengua según el sistema de voz. Cinco vocales no cubren todos los fonemas.',770,y,380)
y=heading('Mezcla y prioridades',770,y-30)
y=para('La emoción controla cejas, mejillas y ojos. El audio controla boca y mandíbula. Reducir la sonrisa durante O/U para evitar conflicto. El guiño afecta sólo a un ojo; el parpadeo mantiene contacto de párpados.',770,y,380)
box(770,78,384,71)
para('<b>Propuesta artística generada con ImageGen.</b> No es un render 3D ni prueba de shape keys. El rostro original sigue siendo la referencia principal.',784,136,352,10)
end()

start('Topología y estructura ósea','04 / esqueleto inicial en Blender')
photo('a_front',36,85,420,632,'A frontal original + guía espacial propuesta')
bones=json.loads((ROOT/'skeleton.json').read_text(encoding='utf-8'))
# Same image rectangle as photo(): original height 602, image center x246, y107.
scale=602/1.76; center=246; bottom=107
for b in bones:
    color=HexColor('#9869C5') if b['name'].startswith('hair_') else HexColor('#E79225') if not b['deform'] else CYAN
    c.setStrokeColor(color);c.setFillColor(color);c.setLineWidth(1.1 if b['deform'] else 1.5)
    a,t=b['head'],b['tail'];x1=center+a[0]*scale;y1=bottom+a[2]*scale;x2=center+t[0]*scale;y2=bottom+t[2]*scale
    c.line(x1,y1,x2,y2);c.circle(x1,y1,1.8,fill=1,stroke=0)
text('Azul: cadena principal   /   Ámbar: auxiliares   /   Violeta: cabello',36,70,9,MUTED)
y=heading('Malla de producción: criterios',485,711)
y=para('Construir una superficie corporal continua en A-pose, con loops alrededor de boca, párpados, hombros, codos, caderas y rodillas. Separar ojos, dientes, lengua, placas rígidas, auriculares y mechones. Evitar polos en zonas de máxima flexión. UV coherentes, normales limpias y triangulación controlada para exportar.',485,y,650)
y=heading('Cadena y auxiliares',485,y-28)
y=para('<b>root → pelvis → spine.01 → spine.02 → chest → neck → head</b><br/>clavicle → upper_arm → forearm → hand → dedos<br/>pelvis → thigh → shin → foot → toe<br/>head → mandíbula / ojos / cadenas de cabello',485,y,650)
y=para('El archivo contiene <b>92 huesos</b>, incluidos dedos, ojos y 6 cadenas de cabello con 4 segmentos. Los huesos auxiliares de hombro, codo, cadera y rodilla están colocados como propuesta; sus pesos, controladores y correctivos siguen pendientes.',485,y-14,650)
y=heading('Doble articulación: reparto de deformación',485,y-28)
y=para('Mantener una cadena humanoide principal y repartir torsión/volumen entre huesos secundarios y shape keys correctivas. No duplicar ciegamente las articulaciones. IK/FK de brazos y piernas, pole targets y controles de pies deben añadirse después de reconciliar la anatomía. Dos huesos por zona no garantizan ausencia de deformación.',485,y,650)
y=table([['Zona','Prueba propuesta','Criterio visual'],['Codo / rodilla','0°, 45°, 90°, 135° y máximo diseñado','Sin colapso de volumen ni pliegue invertido.'],['Hombro / cadera','Flexión, separación lateral y torsión combinadas','Sin hundimiento axilar, cruce de placas o pérdida de silueta.'],['Manos / cara','Puño, agarre, guiño y vocales combinadas','Sin intersecciones; párpados y labios cierran.']],485,y-20,[160,225,265],10)
para('En el archivo inicial no existen malla de personaje, skinning, shape keys, IK/FK ni simulación. Las seis poses sólo permiten revisar el esqueleto.',485,y-20,650,10)
end()

start('Poses de acción y contactos','05 / referencias originales')
for i,id in enumerate(['seated','crouched','kneeling','kneeling_variant','leaning','leaning_variant']):
    x=36+(i%3)*374;y=392 if i<3 else 77
    photo(id,x,y,360,299,views[id]['label'])
text('Comprobar centro de masa, manos, pies y colisiones del cabello. No inferir una transición temporal a partir de una imagen fija.',36,718,10,MUTED)
end()

start('Catálogo de animaciones y transiciones','06 / clips a producir; tiempos orientativos')
left=[['Postura / desplazamiento','Duración / bucle','Control'],['idle_active','4-8 s / sí','Respiración, peso y parpadeo separados.'],['stand_to_sit / sit_to_stand','1-2 s / no','Altura y borde del asiento con IK.'],['sit_idle / sit_float','4-6 s / sí','Silla/cama con contacto; flotación sin apoyo.'],['stand_to_crouch / crouch_to_stand','0,8-1,5 s / no','Pies anclados y rodillas estables.'],['crouch_idle','3-5 s / sí','Soporte de manos opcional.'],['kneel_enter / idle / exit','1-2 s / sí en idle','Contacto de rodillas y despeje de botas.'],['lie_back_enter / idle / exit','2-3 s / sí en idle','Espalda, cabeza y cabello apoyados.'],['lie_side_enter / idle / exit','2-3 s / sí en idle','Contacto de cadera y hombro.'],['walk_normal / elegant / fast','0,7-1,3 s / sí','Velocidad ligada a longitud de paso.'],['jump_start / air / land','0,3-1 s / aire variable','Aterrizaje antes de volver a idle.'],['float_enter / loop / exit','1-4 s / sí en loop','Raíz libre y física amortiguada.'],['approach / retreat','Variable','Desplazar root hacia/desde cámara.']]
right=[['Gesto / modo','Duración / bucle','Control'],['wave','1,5-2,5 s / no','Mano y dedos; espejo L/R.'],['blow_kiss','1,5-2,5 s / no','Boca + mano + retorno suave.'],['nod / shake','0,7-1,2 s / no','Cabeza sin mover la raíz.'],['arms_cross / uncross','1-1,5 s / no','Evitar torso y cabello.'],['hands_on_hips / release','1-1,5 s / no','Contacto estable de manos.'],['point_screen','1-2 s / no','Objetivo en espacio de pantalla.'],['hologram_type','3-5 s / sí','Dedos, mirada y punto de interés.'],['think_chin','2-4 s / no','Mirada arriba + mano en barbilla.'],['laugh / cry','2-4 s / no','Separar gesto, emoción y voz.'],['startle / recover','0,3 s + 1 s','Prioridad alta, recuperación explícita.'],['dance_pop','8-16 s / sí','Giros, cadera y pasos; contactos.'],['dance_urban','8-16 s / sí','Cadencia fluida; evitar deslizamiento.']]
table(left,36,714,[213,121,216],10);table(right,605,714,[203,123,223],10)
box(36,69,1118,83)
para('<b>Entrega por clip:</b> entrada y salida compatibles, raíz definida, 30 fps como base, eventos de contacto y versión in-place cuando proceda. Los tiempos son una propuesta artística, no mediciones de clips ya hechos.<br/><b>En Blender ahora:</b> 6 poses de calibración en los fotogramas 1, 31, 61, 91, 121 y 151. No son coreografías ni transiciones terminadas.',51,136,1085,10)
end()

def node(label,x,y,w=170,h=47,color=LIGHT):
    box(x,y,w,h,color);p=Paragraph(label,ParagraphStyle('node',parent=style,fontSize=11,leading=14,alignment=1));pw,ph=p.wrap(w-16,h);p.drawOn(c,x+8,y+(h-ph)/2)
def arrow(x1,y1,x2,y2,label=None):
    c.setStrokeColor(BLUE);c.setFillColor(BLUE);c.setLineWidth(1.5);c.line(x1,y1,x2,y2)
    a=math.atan2(y2-y1,x2-x1);p=c.beginPath();p.moveTo(x2,y2);p.lineTo(x2-7*math.cos(a-.45),y2-7*math.sin(a-.45));p.lineTo(x2-7*math.cos(a+.45),y2-7*math.sin(a+.45));p.close();c.drawPath(p,fill=1,stroke=0)
    if label:text(label,(x1+x2)/2-30,(y1+y2)/2+8,9,MUTED)

start('Estados conectados a la conversación','07 / contrato de integración propuesto')
node('Entrada de texto / voz',36,655,200);node('Validar sesión y turno',307,655,215);node('Respuesta + indicaciones visuales',588,655,256);node('Despachar sólo eventos vigentes',910,655,244)
for a,b in [(236,307),(522,588),(844,910)]:arrow(a,678,b,678)
text('CONVERSACIÓN',36,610,11,BLUE,True)
for title,x in [('Idle',36),('Escuchando',262),('Pensando',488),('Hablando',714),('Idle',940)]:node(title,x,545,180)
for a,b,label in [(216,262,'voz'),(442,488,'entrada'),(668,714,'audio'),(894,940,'fin')]:arrow(a,568,b,568,label)
text('CUERPO: ESTADO INDEPENDIENTE',36,496,11,BLUE,True)
for title,x in [('De pie / caminar',36),('Transición de entrada',309),('Sentada / cuclillas / tumbada',582),('Transición de salida',855)]:node(title,x,418,248,56)
for a,b in [(284,309),(557,582),(830,855)]:arrow(a,447,b,447)
arrow(975,418,975,391);arrow(975,391,160,391);arrow(160,391,160,418)
text('Orden explícita: sentarse / agacharse / tumbarse / levantarse. Hablar no restablece la postura.',36,365,11,MUTED)
text('CAPAS SUPERPUESTAS',36,326,11,BLUE,True)
node('Gesto: saludo, asentir, señalar, pensar',36,249,338,53)
node('Emoción: ojos, cejas y mejillas',412,249,338,53)
node('Audio: visemas + mandíbula',789,249,365,53)
table([['Evento','Respuesta visual','Regla'],['Respuesta cálida / duda / preocupación','WARM / CURIOUS / CONCERNED','Expresión mezclable; conservar postura.'],['Risa / sorpresa / timidez','LAUGH + WARM / STARTLE + SURPRISED / SHY','Susto transitorio con retorno seguro.'],['Fin de audio / interrupción / cambio de sesión','Cerrar boca y limpiar eventos expirados','Sin volver a ponerse de pie por terminar de hablar.']],36,219,[337,385,396],10)
end()

start('Materiales, física y entrega a motores','08 / especificación de producción')
y=heading('Materiales y presupuesto inicial',36,711)
y=para('Propuesta inicial para medir en el dispositivo destino: LOD0 de 30-50 mil triángulos, LOD1 de 15-25 mil y LOD2 de 6-12 mil. Hasta 4 influencias por vértice y 3-5 materiales principales. Son objetivos de trabajo, no garantías de rendimiento ni cifras del archivo actual.',36,y,530)
y=table([['Superficie','Tratamiento'],['Traje blanco','Base blanca con variación de rugosidad; separar costuras y placas.'],['Paneles / auriculares','Negro con rugosidad controlada y detalles rígidos.'],['Circuitos / ojos','Emisión azul en mapa propio; preservar detalle con bloom moderado.'],['Cabello blanco','Mechones o cards con normals coherentes; limitar capas transparentes.'],['Rostro','Albedo suave, párpados y labios móviles; sonrojo por máscara.']],36,y-22,[158,374],10)
y=heading('Cabello y accesorios',36,y-27)
para('6 cadenas de cabello de 4 huesos están propuestas en la plantilla. Configurar spring bones o física en el motor, con colisionadores de cabeza, hombros y torso. Sujetar la raíz del pelo; amortiguar cambios al teletransportar, sentarse o recostarse. Los auriculares siguen rígidamente a la cabeza. Sólo las partes flexibles del traje necesitan dinámica.',36,y,530)
y=heading('Exportación verificable',612,711)
y=para('<b>Blender:</b> aplicar escala de forma controlada antes del skinning; nombres estables, bind pose documentada y acciones separadas. Hornee restricciones que el formato de destino no reproduzca.<br/><br/><b>Unity:</b> asignar el Avatar humanoide y comprobar huesos; importar blendshapes y validar sus pesos.<br/><br/><b>Unreal:</b> probar skeletal mesh, skeleton, animaciones y morph targets con el importador FBX.<br/><br/><b>glTF/GLB:</b> entrega de intercambio con armature, morph targets y animaciones compatibles; no asumir que física, drivers o restricciones se transfieren tal cual.',612,y,540)
y=heading('Pruebas antes de declarar terminado',612,y-29)
y=para('1. Silueta frontal, lateral y trasera contra el canon.<br/>2. Flexión máxima combinada: piel, placas y cabello.<br/>3. Visemas + emoción + parpadeo simultáneos.<br/>4. Sentarse y seguir hablando sin saltar a de pie.<br/>5. Ciclos de caminar/bailar sin deslizamiento.<br/>6. Reimportación y comparación con Blender.<br/>7. FPS, memoria y temperatura medidos en S24 Ultra.',612,y,540)
box(612,86,540,96)
para('<b>Esta entrega no certifica estas pruebas de producción.</b><br/>Se han comprobado los originales, el empaquetado de imágenes, las escenas, los huesos y la apertura del archivo. La validación de deformación y rendimiento exige construir la malla final.',627,166,508,10)
end()

start('Estado de entrega y fuentes','09 / trazabilidad')
table([['Componente','Entregado ahora','Pendiente'],['Referencias','20 PNG originales, manifest y hashes verificados.','Resolver variaciones entre vistas en una sola geometría.'],['Hoja visual','Turnaround, poses originales y propuesta de 16 rostros/vocales.','Ajustar expresiones durante el esculpido para conservar identidad.'],['Blender','3 escenas, 21 imágenes empaquetadas, 92 huesos, 6 poses de calibración.','Malla 3D, UV, materiales definitivos y skinning.'],['Rig / rostro','Ubicación inicial de huesos y especificación de controles.','IK/FK, drivers, shape keys y correctivos de articulaciones.'],['Animación / física','Catálogo, capas de estado y criterios de aceptación.','Clips acabados, spring bones y pruebas de colisión.'],['Integración','Compatibilidad conceptual con las emociones del PR #110.','Exportación al motor e integración real del personaje 3D.']],36,714,[181,467,470],11)
y=heading('Cómo continuar en Blender',36,398)
y=para('Abra Salve_base_modelado.blend. Cambie entre las escenas <b>01_REFERENCIAS_20_ORIGINALES</b>, <b>02_RIG_BASE_PROPUESTA</b> y <b>03_EXPRESIONES_PROPUESTA</b>. La escena de rig abre en A-pose. Los marcadores de la línea de tiempo identifican las seis poses de calibración. El bloque de texto LEEME_SALVE describe el alcance.',36,y,1118)
y=heading('Fuentes y archivos de referencia',36,y-30)
sources=[('PR #110: reacciones y posturas 2D','https://github.com/bryanstevenriverabejarano-svg/Convert/pull/110'),('Manifiesto de las 20 vistas, revisión fijada 1ccd26e','https://github.com/bryanstevenriverabejarano-svg/Convert/blob/1ccd26e276925b95ec6f5becd7d53070e1c5498e/app/src/main/assets/avatar/core/manifest.json'),('Blender: glTF 2.0, deformación y animaciones','https://docs.blender.org/manual/en/5.1/addons/import_export/scene_gltf2.html'),('Unity: Humanoid Avatar','https://docs.unity.com/en-us/engine/6000.3/manual/animation-section/animation-mecanim/avatar-creationand-setup'),('Unity: Work with blend shapes','https://docs.unity.com/en-us/engine/6000.6/manual/animation-section/animation-mecanim/animation-clips/animation-editor-guide/blend-shapes'),('Unreal Engine: FBX Morph Target Pipeline','https://dev.epicgames.com/documentation/unreal-engine/fbx-morph-target-pipeline-in-unreal-engine')]
for title,url in sources:
    text(title,36,y-16,10,BLUE);c.linkURL(url,(36,y-19,900,y-5),relative=0);y-=26
para('La lámina facial usa ImageGen integrado y la frontal original como referencia de identidad. El prompt se conserva en design/expresiones_prompt.txt. Las recomendaciones de presupuesto, nomenclatura, tiempos y mezcla son propuestas específicas de este documento. Los originales se conservan sin modificaciones.',36,101,1118,10)
end();c.save()
print('PDF_CREATED',str(OUT.resolve()),'pages',page)
