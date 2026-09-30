from pathlib import Path
from PIL import Image,ImageDraw,ImageFont
import json,hashlib,html,zipfile,shutil,math
ROOT=Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2');OUT=ROOT/'outputs'
REF=Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/work/Convert-salve-publicacion/art/salve/blender/references')
manifest=json.loads((OUT/'vistas_manifest.json').read_text(encoding='utf-8'))
assert len(manifest)==20, 'Twenty renders required before packaging'
expressions=json.loads((OUT/'expresiones_controles.json').read_text(encoding='utf-8'))
assert len(expressions)==21, 'Twenty-one expression portraits required before packaging'
cleanup_path=OUT/'Salve_limpieza_rostroUV_v02.json'
cleanup=json.loads(cleanup_path.read_text(encoding='utf-8')) if cleanup_path.exists() else None
cleanup_bounds=None
if cleanup and cleanup.get('saved_model'):
 metrics=cleanup['surface_conservation_metrics_at_unique_frames']
 expected={(r['frame'],r['id'],r['camera'],tuple(r['size'])) for r in manifest+expressions}
 measured={(r['frame'],v['id'],v['camera'],tuple(v['size'])) for r in metrics for v in r['views']}
 assert cleanup['unique_frame_count_checked']==len(metrics)==33
 assert len({r['frame'] for r in metrics})==33
 assert cleanup['rendered_image_count_covered']==sum(len(r['views']) for r in metrics)==41
 assert expected==measured, 'Cleanup conservation cameras must match the delivered manifest'
 pixels=[v['maximum_pixel_displacement'] for r in metrics for v in r['views']]
 angles=[r[k] for r in metrics for k in ['max_corner_normal_angle_deg','max_face_normal_angle_deg']]
 assert all(math.isfinite(v) and 0<=v<=.01 for v in pixels), 'Cleanup projection bound exceeded'
 assert all(math.isfinite(v) and 0<=v<=1.0 for v in angles), 'Cleanup normal angle bound exceeded'
 assert all(r['passed'] and r['matrix_exact'] and r['face_layout_exact'] and all(v['passed'] for v in r['views']) for r in metrics)
 assert cleanup['weights_and_drivers_preserved'] is True
 cleanup_bounds={'maximum_pixel_displacement':max(pixels),'maximum_normal_angle_deg':max(angles)}
font_path='C:/Windows/Fonts/arial.ttf'
def font(size):return ImageFont.truetype(font_path,size)
BG=(26,36,53,255);LIGHT=(242,246,255,255);MUTED=(174,192,218,255)
comparisons=OUT/'comparaciones';comparisons.mkdir(exist_ok=True)
issues={
'front':'Persisten diferencias de mandibula, ojos, labios, volumen de pecho, manos, flequillo y diseño preciso de paneles.',
'front_three_quarter':'Contrapposto propio y camara35 grados aproximan la vista; contorno facial y caida del cabello difieren del original.',
'right':'El perfil de nariz y labios, la forma de botas/tacon y la masa posterior del cabello requieren ajuste artistico.',
'rear_right':'Las uniones de armadura y volumen de cadera quedan aproximados; la laminacion de cabello simplifica las hebras de la ilustracion.',
'rear':'Se cubre la parte posterior del craneo; persisten diferencias de nuca, gluteos, espalda y patron tecnologico.',
'rear_three_quarter':'La ilustracion tiene asimetria y giro de cuello particulares; el mismo rig conserva una geometria coherente pero no reproduce cada detalle.',
'left':'Perfil, profundidad del pecho, puntera/tacon y terminacion de mechones aun no son equivalentes al canon.',
'front_opposite':'Pelvis y piernas adoptan contrapposto opuesto; expresion, silueta de manos y detalles de traje pendientes de equivalencia.',
'front_variant':'Pose individual de piernas y pelvis; pendiente afinar balance de peso, expresion y caida precisa de cabello.',
'above':'Se usa perspectiva elevada; focal y orientacion se infieren de la ilustracion, sin calibracion fotografica exacta.',
'below':'Se usa perspectiva inferior; deformacion de perspectiva, piernas y proporciones aparentes no coinciden exactamente.',
'a_front':'Postura A y simetria comprobables en malla; identidad facial, cortes de paneles y volumen del pelo siguen simplificados.',
'a_open_hair':'Brazos y cadenas capilares abiertos; la referencia varia cantidad y apertura del pelo que este modelo reconcilia de forma aproximada.',
'a_rear':'Postura A posterior; se requiere refinar gluteos, hombros, paneles posteriores y terminaciones del cabello.',
'seated':'Pose asimetrica, pierna plegada y apoyos medidos; orientacion precisa de tobillos, dedos y pelo respecto a la ilustracion sigue en revision.',
'crouched':'Flexion compacta de caderas/rodillas; volumen en flexion, colocacion de brazos y silueta extrema pendientes de revision anatomica.',
'kneeling':'Muslos plegados y rodillas/pies sobre plano de apoyo; manos, dedos y compresion anatomica requieren refinamiento contra la referencia.',
'kneeling_variant':'Se resuelven cuatro apoyos medidos; curva de columna, manos planas, orientacion de cabeza y cabello requieren contraste fino.',
'leaning':'Columna y pelvis inclinan el cuerpo; sigue pendiente igualar gesto, acercamiento de rodillas, manos y cabello.',
'leaning_variant':'Inclinacion mas profunda individual; se requiere corregir silueta artistica, deformacion de hombros/caderas y disposicion de manos.'}
visual_path=OUT/'Salve_revision_visual.json'
if visual_path.exists():visual=json.loads(visual_path.read_text(encoding='utf-8'))
else:visual={row['id']:{'reviewed':False,'identity_accepted':False,'differences':issues[row['id']]} for row in manifest}
png_checks=[]
thumb_pairs=[]
render_thumbs=[]
for row in manifest:
 id=row['id'];path=OUT/row['file'];render=Image.open(path);render.load();ref=Image.open(REF/(id+'.png')).convert('RGBA')
 expected=(1374,1145) if id=='kneeling_variant' else (1024,1536)
 assert render.size==expected,(id,render.size)
 assert render.mode=='RGBA',(id,render.mode)
 alpha=render.getchannel('A');bbox=alpha.point(lambda x:255 if x>16 else 0).getbbox()
 edge=any(alpha.crop(r).getextrema()[1]>16 for r in [(0,0,render.width,1),(0,render.height-1,render.width,render.height),(0,0,1,render.height),(render.width-1,0,render.width,render.height)])
 png_checks.append({'id':id,'size':render.size,'rgba':True,'transparent_pixels':alpha.getextrema()[0]==0,'alpha_bbox':bbox,'alpha_on_border':edge,'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
 # Full-resolution side-by-side comparison, keeping the original canvas geometry.
 pair=Image.new('RGBA',(ref.width*2,ref.height+74),BG);pair.alpha_composite(ref,(0,50));pair.alpha_composite(render,(ref.width,50));d=ImageDraw.Draw(pair)
 d.text((20,10),'ORIGINAL / '+id,font=font(26),fill=LIGHT);d.text((ref.width+20,10),'BLENDER V02 / EN REVISION',font=font(26),fill=LIGHT)
 d.text((20,ref.height+54),'Comparacion sobre fondo comun. Archivos fuente intactos; identidad y produccion pendientes.',font=font(15),fill=MUTED)
 pair.convert('RGB').save(comparisons/(id+'.png'))
 canvas=Image.new('RGBA',(500,400),BG);thumb=pair.copy();thumb.thumbnail((490,356),Image.Resampling.LANCZOS);canvas.alpha_composite(thumb,((500-thumb.width)//2,4));ImageDraw.Draw(canvas).text((12,370),str(row['index']).zfill(2)+' '+id,font=font(18),fill=LIGHT);thumb_pairs.append(canvas)
 rt=Image.new('RGBA',(240,372),BG);ri=render.copy();ri.thumbnail((234,342),Image.Resampling.LANCZOS);rt.alpha_composite(ri,((240-ri.width)//2,4));ImageDraw.Draw(rt).text((8,349),id,font=font(15),fill=LIGHT);render_thumbs.append(rt)

sheet=Image.new('RGBA',(2000,2100),BG);d=ImageDraw.Draw(sheet);d.text((25,20),'SALVE / ORIGINAL Y REFINAMIENTO V02',font=font(33),fill=LIGHT);d.text((25,63),'20 vistas. La comparacion muestra diferencias artisticas pendientes.',font=font(23),fill=MUTED)
for i,im in enumerate(thumb_pairs):sheet.alpha_composite(im,((i%4)*500,100+(i//4)*400))
sheet.convert('RGB').save(OUT/'Salve_comparacion_20_v02.png')
sheet=Image.new('RGBA',(1200,1588),BG);ImageDraw.Draw(sheet).text((20,15),'SALVE / 20 RENDERS V02',font=font(28),fill=LIGHT)
for i,im in enumerate(render_thumbs):sheet.alpha_composite(im,((i%5)*240,80+(i//5)*372))
sheet.convert('RGB').save(OUT/'Salve_20_vistas_v02.png')
exprsheet=Image.new('RGBA',(1792,960),BG);ImageDraw.Draw(exprsheet).text((20,14),'SALVE / EXPRESIONES Y VISEMAS V02',font=font(28),fill=LIGHT)
for i,row in enumerate(expressions):
 im=Image.open(OUT/row['file']).convert('RGBA');tile=Image.new('RGBA',(256,292),BG);im.thumbnail((248,248),Image.Resampling.LANCZOS);tile.alpha_composite(im,((256-im.width)//2,2));ImageDraw.Draw(tile).text((12,260),row['id'],font=font(19),fill=LIGHT);exprsheet.alpha_composite(tile,((i%7)*256,70+(i//7)*292))
exprsheet.convert('RGB').save(OUT/'Salve_hoja_expresiones_v02.png')

integrity=json.loads((ROOT/'work/source_integrity.json').read_text(encoding='utf-8'))
integrity['original_blend_unchanged']=hashlib.sha256(Path(integrity['blend_source']).read_bytes()).hexdigest()==integrity['blend_sha256']
integrity['all_original_references_unchanged']=all(hashlib.sha256(Path(v['source']).read_bytes()).hexdigest()==v['sha256'] for v in integrity['references'].values())
portrait_checks=[]
for row in expressions:
 p=OUT/row['file'];im=Image.open(p);im.load()
 assert im.size==(768,768) and im.mode=='RGBA',(row['id'],im.size,im.mode)
 sha=hashlib.sha256(p.read_bytes()).hexdigest()
 assert sha==row['sha256'], ('Stale portrait manifest',row['id'])
 portrait_checks.append({'id':row['id'],'size':im.size,'rgba':True,'transparent_pixels':im.getchannel('A').getextrema()[0]==0,'sha256':sha})
for row in manifest:
 assert row['render_sha256']==next(p['sha256'] for p in png_checks if p['id']==row['id']), ('Stale body manifest',row['id'])
 assert visual[row['id']]['reviewed'] and visual[row['id']]['render_sha256_reviewed']==row['render_sha256'], ('Stale body review',row['id'])
validation={'status':'AVANCE_V02_NO_FINAL','definition_of_done_met':False,'blender_version':'5.2.2 LTS','png_checks':png_checks,'portrait_checks':portrait_checks,'originals_preserved':{'blend':integrity['original_blend_unchanged'],'references':integrity['all_original_references_unchanged']},'all_required_png_sizes':True,'all_rgba':True,'all_have_transparency':all(r['transparent_pixels'] for r in png_checks),'no_content_on_canvas_border':not any(r['alpha_on_border'] for r in png_checks),'visual_review':visual,'production_certified':False}
for name in ['Salve_auditoria_blend_v02.json','Salve_apertura_GUI_v02.json','Salve_limpieza_rostroUV_v02.json','Salve_validacion_facial_v02.json','Salve_revision_anatomia_v02.json','Salve_poses_contactos_v02.json','Salve_cabello_correctivos_v02.json','Salve_revision_expresiones.json','poses/Salve_revision_poses_v02.json','poses/Salve_revision_pensativa_v02.json','poses/Salve_correccion_collar_pensativa_v02.json','poses/Salve_diagnostico_IK_v02.json','poses/Salve_correccion_apoyos_postrostro_v02.json','poses/Salve_orientacion_palmas_v02.json','texturas/atlas_validation.json','texturas/atlas_repair_validation.json','Salve_integracion_v02/compilation_validation.json']:
 p=OUT/name
 if p.exists():validation[name]=json.loads(p.read_text(encoding='utf-8'))
(OUT/'Salve_validacion_v02.json').write_text(json.dumps(validation,indent=2,ensure_ascii=False),encoding='utf-8')
(OUT/'Salve_revision_visual.json').write_text(json.dumps(visual,indent=2,ensure_ascii=False),encoding='utf-8')

discrepancies=[
 ('Identidad','La frontal fija rostro y ojos. El modelo conserva coherencia tridimensional, pero ojos, nariz, labios y mandibula siguen sin equivalencia ilustrativa.'),
 ('Cabello','Las vistas y posturas muestran aperturas y solapamientos diferentes. Se adopta una masa larga con cadenas y correctivos estaticos por pose. La profundidad se infiere: una diferencia aparente entre vistas no demuestra por si sola una contradiccion.'),
 ('Traje','Se usa un sistema simetrico basado en frontal, perfiles y posteriores, con paneles y circuitos editables. El trazado actual es una aproximacion: no se atribuye su diferencia a una contradiccion de los originales sin evidencia.'),
 ('Perspectiva','Focales y poses de camara se infieren visualmente. No hay datos fotograficos para una calibracion exacta.'),
 ('Escala','Se conserva la escala provisional del esqueleto previo. No se declara altura canonica.'),
 ('Produccion','Mallas regionales en su mayoria quad, con piezas separadas, UV desplegadas y atlas real. No equivalen a retopologia de produccion verificada. Faltan uniones anatomicas, loops de deformacion revisados, densidad UV suficiente para detalle, pintura final y validacion de pesos/correctivos extremos.')]
body=''.join('<section><h2>'+html.escape(r['id'])+'</h2><img loading="lazy" src="comparaciones/'+r['id']+'.png"><p>'+html.escape(visual[r['id']]['differences'])+'</p><p>Revision visual: '+('realizada' if visual[r['id']]['reviewed'] else 'pendiente')+'; identidad final: pendiente.</p></section>' for r in manifest)
disc=''.join('<tr><th>'+html.escape(a)+'</th><td>'+html.escape(b)+'</td></tr>' for a,b in discrepancies)
htmltext='''<!doctype html><html lang="es"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Salve · revisión V02</title><style>body{background:#111b2a;color:#edf3ff;font:17px/1.6 system-ui;max-width:1300px;margin:auto;padding:32px}h1{font-size:36px}h2{margin-top:32px}.status{padding:20px;background:#25374e;border-left:5px solid #79b6ff}img{max-width:100%;display:block;border-radius:12px}section{background:#192638;padding:24px;margin:24px 0;border-radius:16px}a{color:#8fc7ff}table{border-collapse:collapse}th,td{border-bottom:1px solid #3d4e65;padding:14px;text-align:left}th{width:160px}nav a{margin-right:20px}</style><h1>Salve · refinamiento V02</h1><p>30 de septiembre de 2026 · Blender 5.2.2 LTS</p><p class="status"><b>Avance verificable. El encargo completo sigue pendiente.</b><br>La similitud con las ilustraciones y la preparacion de produccion no quedan certificadas. Las diferencias se muestran en cada comparacion.</p><nav><a href="Salve_refinado_v02.blend">Modelo editable</a><a href="Salve_validacion_v02.json">Verificaciones</a><a href="Salve_integracion_v02/Informe_integracion_Salve.html">Contrato e integracion</a></nav><h2>Entrega y cambios</h2><p>20 PNG transparentes con nombres y tamaños del catalogo. Modelo con cuerpo regional quad, ojos/rostro reconstruidos, mechones largos, paneles ajustados a superficie, materiales y atlas, biblioteca de poses, correctivos de volumen y controles faciales con sonrojo y lagrimas. Incluye gesto pensativo y visemas. Las fuentes y la version previa permanecen intactas.</p><h2>Contradicciones resueltas y limites</h2><table>'''+disc+'''</table><h2>Revisión de las 20 vistas</h2>'''+body+'''<h2>Expresiones</h2><img src="Salve_hoja_expresiones_v02.png"><p>Controles mezclables en Salve_Rig; claves por fotograma y marcadores FACE. La revision de expresiones, cierre ocular y visemas se registra por separado. El atlas no convierte esta revision en acabado final.</p><h2>Integracion</h2><p>Paquete de staging y patch Java revisables. Anclajes nuevos ligados al SHA de cada PNG y a su proyeccion de camara. La compilacion Android y la aceptacion visual en producto siguen pendientes. No se publicaron ni sustituyeron recursos aprobados.</p></html>'''
htmltext=htmltext.replace('La compilacion Android y la aceptacion visual en producto siguen pendientes.','El modulo Android compila en una copia del proyecto y supero 21 pruebas dirigidas. El informe registra la prueba del rig con los anclajes de los renders nuevos. La prueba visual en dispositivo y la aprobacion en producto siguen pendientes.')
htmltext=htmltext.replace('Contradicciones resueltas y limites','Interpretaciones tridimensionales y limites')
htmltext=htmltext.replace('<h2>Expresiones</h2><img', '<h2>Expresiones</h2><p><a href="Salve_comparacion_expresiones_v02.png">Comparacion con la propuesta secundaria de expresiones</a> · <a href="Salve_revision_expresiones.json">Revision de los 21 PNG actuales</a>. La frontal original sigue siendo la autoridad de identidad.</p><img')
delivery_status=[
 ('Modelo editable','Entregado como V02 con colecciones, materiales, rig y claves. La fidelidad ilustrativa sigue pendiente.'),
 ('Veinte renders','Renderizados y comparados individualmente; tamaños, transparencia, margenes y hashes comprobados. Hay diferencias de identidad, postura y silueta documentadas.'),
 ('Expresiones','21 presets controlables y retratos de verificacion. Cierres, visemas, sonrojo y contacto pensativo se comprueban por separado; su acabado artistico sigue pendiente.'),
 ('Poses y contactos','Biblioteca estatica y mediciones de apoyos entregadas. Pendientes pose exacta de manos/rodillas/cabeza, apoyo completo de tacones y calidad de deformacion.'),
 ('Preparacion de produccion','Parcial: UV y atlas reales, pesos y correctivos inspeccionados. No aprobadas las uniones de retopologia, densidad para detalle, deformaciones extremas, polos IK ni todo el cabello.'),
 ('Paquete Salve','Candidato local con catalogo, anclajes, patch Java, compilacion y pruebas. Incorporacion definitiva, revision visual en dispositivo y mascaras pendientes.'),
 ('Comparacion e informe','20 comparaciones y revision por zona, expresion y pose, con comprobaciones y diferencias explicitas.')]
delivery_html='<h2>Estado de los entregables solicitados</h2><table>'+''.join('<tr><th>'+html.escape(a)+'</th><td>'+html.escape(b)+'</td></tr>' for a,b in delivery_status)+'</table><p><a href="Guia_controles_Salve_v02.html">Guia para continuar editando poses, gestos y materiales en Blender</a>.</p>'
htmltext=htmltext.replace('<h2>Entrega y cambios</h2>',delivery_html+'<h2>Entrega y cambios</h2>')
checks = [
 ('Archivos y fuentes','Se conservan el modelo anterior y las 20 referencias; hashes comprobados. La auditoria del .blend comprueba apertura, texturas empaquetadas y datos finitos.'),
 ('Lienzos','20 renders RGBA: 19 de 1024 × 1536 y kneeling_variant de 1374 × 1145. Transparencia y margenes comprobados por canal alfa; comparacion visual registrada para cada ID.'),
 ('Apoyos','Se midieron vertices deformados contra z=0 y se corrigieron pelvis, rodillas, pies y orientacion de palmas. El minimo de una region no acredita apoyo de toda su superficie: el tacon conserva una separacion que debe revisarse.'),
 ('Cabello','La prueba de cruces de superficies del cabello contra el traje da cero en las poses entregadas. Excluye cabeza/cuero cabelludo y no detecta necesariamente piezas encerradas dentro de un volumen. La revision visual complementa esa prueba, sin certificar ausencia total de penetraciones.'),
 ('IK y deformacion','Los controles permiten continuar editando. El snap coloca los extremos, pero al activar IK quedan diferencias de plano articular: rodilla de la variante arrodillada ≈25 mm en el diagnostico. El gesto pensativo tiene una medicion nueva tras corregir contacto/collar, registrada en el manual. La entrega estatica usa la pose FK; el ajuste de polos IK sigue pendiente.'),
 ('UV y materiales','Atlas de color, rugosidad y metalicidad de 2048², iris de 512², archivos externos y copias empaquetadas. Se reparo la rasterizacion de islas capilares pequeñas y se verificaron los renders. El atlas de colores constantes no aporta el detalle pintado de las ilustraciones ni certifica densidad UV de produccion.'),
 ('Expresiones','21 presets mezclables, con sonrojo, parpadeo, lagrimas y visemas. La validacion numerica y la revision por imagen se entregan separadas de la aceptacion artistica.'),
 ('Producto','Catalogo y Java consultados en GitHub; patch, recursos de staging y anclajes ligados al SHA de cada render. La compilacion y pruebas se realizan en una copia. Faltan prueba visual en dispositivo y aprobacion de producto.')
]
technical = '<h2>Comprobaciones y alcance</h2><table>' + ''.join('<tr><th>'+html.escape(a)+'</th><td>'+html.escape(b)+'</td></tr>' for a,b in checks) + '</table><p><a href="poses/Salve_limites_IK_v02.md">Limites de IK</a> · <a href="Salve_validacion_facial_v02.json">Validacion facial</a> · <a href="Salve_poses_contactos_v02.json">Mediciones de poses</a></p>'
if cleanup_bounds:
 technical+=('<p>La limpieza facial elimino '+str(cleanup['removed_isolated_vertices'])+' vertices aislados y sustituyo el UV cilindrico defectuoso por un desplegado revisado. '
             'El guard comprobo conservacion visual cuantificada en 33 fotogramas y las 41 camaras entregadas: desplazamiento proyectado maximo '
             +format(cleanup_bounds['maximum_pixel_displacement'],'.6g')+' px (limite 0.01 px) y cambio angular maximo de normales '
             +format(cleanup_bounds['maximum_normal_angle_deg'],'.6g')+' grados (limite 1 grado). '
             'Esto no afirma igualdad bit a bit de superficies evaluadas o normales. Las coordenadas supervivientes de la jaula y shapes, los pesos y drivers tienen comprobaciones de entrada separadas. '
             'Se generaron dos renders neutrales de control antes y despues; su comparacion se registra en el informe. '
             'La malla original queda como respaldo interno. El nuevo desplegado sigue pendiente de revision manual de costuras, solapamientos y densidad. '
             '<a href="Salve_limpieza_rostroUV_v02.json">Registro de limpieza y conservacion visual</a>.</p>')
elif cleanup and cleanup.get('status')=='rejected_rolled_back':
 assert cleanup['saved_model'] is False and cleanup['saved'] is False
 assert cleanup['numeric_guard_passed'] is False and cleanup['candidate_in_memory'] is False
 assert cleanup['original_restored_on_failure'] is True and cleanup['delivered_pngs_replaced'] is False
 assert hashlib.sha256((OUT/'Salve_refinado_v02.blend').read_bytes()).hexdigest()==cleanup['source_model_sha256']
 failed=[(r['frame'],v['id'],v['maximum_pixel_displacement']) for r in cleanup['metrics_at_unique_frames'] for v in r['views'] if not v['passed']]
 assert failed, 'Rejected cleanup must retain its failed metric'
 failure_frame,failure_id,failure_pixels=failed[0]
 technical+=('<p>La limpieza facial se intento con un guard de conservacion visual cuantificada (limites 0.01 px de proyeccion y 1 grado de normales). '
             'Se rechazo en '+html.escape(failure_id)+' (fotograma '+str(failure_frame)+'): '+format(failure_pixels,'.9g')+' px supera 0.01 px. '
             'Se restauro la malla original y no se guardo el candidato ni se sustituyeron PNG. El rostro entregado permanece intacto respecto al modelo auditado; '
             'siguen pendientes los 256 vertices aislados y las UV faciales defectuosas. La medicion se detuvo tras el fallo y no acredita PASS en los 33 fotogramas o las 41 camaras. '
             'Solo se genero el control neutral anterior; no existe una comparacion visual completa antes/despues. '
             'El helper se incluye como fuente de un intento rechazado, no como paso aplicado. '
             '<a href="Salve_limpieza_rostroUV_v02.json">Registro del intento rechazado y rollback</a>.</p>')
htmltext=htmltext.replace('<h2>Revisión de las 20 vistas</h2>',technical+'<h2>Revisión de las 20 vistas</h2>')
anatomy_path=OUT/'Salve_revision_anatomia_v02.json'
if anatomy_path.exists():
 anatomy=json.loads(anatomy_path.read_text(encoding='utf-8'))
 anatomy_html='<h2>Revision por zona</h2><table>'+''.join('<tr><th>'+html.escape(row['region'])+'</th><td>'+html.escape(row['observation'])+'</td></tr>' for row in anatomy['regions'])+'</table>'
 htmltext=htmltext.replace('<h2>Expresiones</h2>',anatomy_html+'<h2>Expresiones</h2>')
interpretation_path=OUT/'Salve_integracion_v02/Interpretacion_referencias_Salve.json'
if interpretation_path.exists():
 htmltext=htmltext.replace('</table><h2>Comprobaciones y alcance</h2>','</table><p><a href="Salve_integracion_v02/Interpretacion_referencias_Salve.json">Interpretaciones verificadas de auriculares y cabello, con las vistas utilizadas y las inferencias identificadas.</a></p><h2>Comprobaciones y alcance</h2>',1)
(OUT/'Salve_informe_v02.html').write_text(htmltext,encoding='utf-8')
readme='''SALVE V02 - AVANCE EN REVISION
30 de septiembre de 2026. Blender 5.2.2 LTS.

La definicion de completado NO se cumple todavia. No se declara identidad exacta ni preparacion final de produccion.

ABRIR
Salve_refinado_v02.blend. Escena 04_SALVE_MODELO_3D.
Las escenas 01-03 y la coleccion 90_Base_v01_oculta conservan bases de trabajo.
La entrega se guarda en una ruta nueva; originales intactos.

CONTROLES
Salve_Rig tiene propiedades faciales mezclables y controles IK opcionales con snap por pose.
Fotogramas: stand1, trescuartos11, opuesto21, A31, variante41, cabelloabierto61, sentada91, cuclillas121, rodillas151, variante161, inclinada171, variante181.
Expresiones desde201 cada10 fotogramas; THINK311 y CONFUSED401.
Las propiedades estan animadas: editar o retirar sus claves si se quiere cambiar manualmente el control.
Los correctivos capilares son estaticos por fotograma, no simulacion ni animacion dinamica.

ARCHIVOS
vistas_20/: 19 PNG 1024x1536 y kneeling_variant1374x1145, RGBA con transparencia.
expresiones/: PNG faciales y visemas.
texturas/: atlas baseColor/roughness/metallic y validacion. Iris512 empaquetado.
comparaciones/: referencia y render para cada ID.
Salve_informe_v02.html y Salve_validacion_v02.json: resultados y diferencias.
Salve_integracion_v02/: contrato Java, patch y fuentes de revision.
Salve_paquete_recursos_v02/: staging de recursos; incorporacion al producto no realizada.

PENDIENTES
Fidelidad canonica de rostro, peinado, anatomia, botas, paneles y poses; revisar todas las diferencias indicadas.
Retopologia facial y uniones anatomicas soldadas, densidad UV, pintura final y deformaciones extremas.
Validacion completa de intersecciones y contactos, especialmente dentro de volumen cerrado.
Prueba Android real, validacion runtime de anclajes y aprobacion artistica antes de sustitucion de recursos.
'''
readme=readme.replace('Prueba Android real, validacion runtime de anclajes y aprobacion artistica antes de sustitucion de recursos.','Compilacion Android y 21 pruebas dirigidas: superadas en copia del proyecto. Prueba visual en dispositivo y aprobacion artistica pendientes antes de sustitucion de recursos.')
if cleanup_bounds:
 readme+=('\nLIMPIEZA FACIAL Y CONSERVACION VISUAL\n'
          'Se eliminaron '+str(cleanup['removed_isolated_vertices'])+' vertices aislados y se revisaron las UV faciales. '
          'El guard mide conservacion visual en 33 fotogramas y 41 camaras: maximo '+format(cleanup_bounds['maximum_pixel_displacement'],'.6g')
          +' px (limite 0.01 px) y '+format(cleanup_bounds['maximum_normal_angle_deg'],'.6g')+' grados de normales (limite 1 grado). '
          'No se declara igualdad bit a bit de superficies evaluadas ni normales. Dos renders neutrales de control se comparan por separado. '
          'Las comprobaciones de jaula/shapes, pesos y drivers son invariantes de entrada. UV manuales, densidad y produccion siguen pendientes.\n')
elif cleanup and cleanup.get('status')=='rejected_rolled_back':
 readme+=('\nLIMPIEZA FACIAL RECHAZADA, NO APLICADA\n'
          'El guard de conservacion visual fallo en '+failure_id+' (fotograma '+str(failure_frame)+'): '+format(failure_pixels,'.9g')+' px supera el limite 0.01 px. '
          'Se restauro la malla original; el rostro entregado sigue intacto y conserva 256 vertices aislados y UV pendientes. '
          'No se guardo el candidato ni se sustituyeron renders. La medicion parcial no acredita los 33 fotogramas ni las 41 camaras; solo existe control neutral anterior. '
          'face_cleanup_visual_conservation_v02.py es la fuente del intento rechazado, no un paso aplicado.\n')
(OUT/'LEEME_Salve_v02.txt').write_text(readme,encoding='utf-8')

sources=OUT/'fuentes_reproducibles';sources.mkdir(exist_ok=True)
for name in ['body_hair_v02.py','body_correctives_fix.py','face_v02.py','face_v02_shape_fix.py','fringe_local_polish_v02.py','pose_neutral_frame401_fix.py','poses_v02.py','hair_clearance_v02.py','hair_targeted_fix_v02.py','prepare_salve_ready.py','final_polish_v02.py','floor_contacts_postface_v02.py','palm_contacts_v02.py','reload_atlas_repair_v02.py','render_v02.py','atlas_v02.py','repair_atlas_constant_v02.py','inspect_final_blend.py','validate_face_readonly.py','ik_diagnostics_v02.py','atlas_uv_faces_v02.json','face_mbp_seal_v02.py','rerender_face_local_v02.py','think_contact_v02.py','think_collar_fix_v02.py','finish_static_review_v02.py','face_topology_uv_safe_cleanup_v02.py','face_cleanup_visual_conservation_v02.py','run_face_cleanup_gui_v02.py']:
 p=ROOT/'work'/name
 if p.exists():shutil.copy2(p,sources/name)
for name in ['audit_final_delivery_v02.py','record_final_gui_v02.py','face_cleanup_diagnostic_v02.py']:
 p=ROOT/'work'/name
 if p.exists():shutil.copy2(p,sources/name)
files=[p for p in OUT.rglob('*') if p.is_file() and p.suffix not in ['.zip','.blend1'] and p.name!='SHA256SUMS_v02.txt' and not p.name.startswith('Salve_GUI_')]
sums='\n'.join(hashlib.sha256(p.read_bytes()).hexdigest()+'  '+p.relative_to(OUT).as_posix() for p in sorted(files))+'\n';(OUT/'SHA256SUMS_v02.txt').write_text(sums,encoding='utf-8')
with zipfile.ZipFile(OUT/'Salve_entrega_v02.zip','w',zipfile.ZIP_DEFLATED,compresslevel=6) as z:
 for p in files+[OUT/'SHA256SUMS_v02.txt']:z.write(p,p.relative_to(OUT).as_posix())
print(json.dumps({'renders':len(png_checks),'expressions':len(expressions),'rgba_transparent':validation['all_have_transparency'],'borders_clear':validation['no_content_on_canvas_border'],'originals_preserved':validation['originals_preserved'],'zip':str(OUT/'Salve_entrega_v02.zip')},ensure_ascii=False))
