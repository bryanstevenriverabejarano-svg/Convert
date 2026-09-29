from pathlib import Path
from PIL import Image,ImageOps,ImageDraw,ImageFont
import json,hashlib,zipfile
OUT=Path('C:/Users/agred/Documents/Codex/2026-09-29/new-chat-2/outputs');SRC=Path('C:/Users/agred/.codex/.chatgpt-projects/g-p-6ab2c01aa8f8819193dceb05d9e78da7/salve_blender')
font=ImageFont.truetype('C:/Windows/Fonts/arial.ttf',20);bold=ImageFont.truetype('C:/Windows/Fonts/arialbd.ttf',29)
def sheet(paths,filename,title,cols,cellw,cellh):
    rows=(len(paths)+cols-1)//cols;canvas=Image.new('RGB',(cols*cellw,rows*cellh+100),'#132037');d=ImageDraw.Draw(canvas);d.text((25,22),title,font=bold,fill='white');d.text((25,60),'VERSION 01 / RECONSTRUCCION ESTILIZADA / REQUIERE REVISION ARTISTICA',font=font,fill='#A7C5EB')
    for i,p in enumerate(paths):
        im=Image.open(p);im.verify();im=Image.open(p).convert('RGB');thumb=ImageOps.contain(im,(cellw-14,cellh-45));x=(i%cols)*cellw;y=(i//cols)*cellh+100
        canvas.paste(thumb,(x+(cellw-thumb.width)//2,y));d.text((x+9,y+cellh-35),p.stem.replace('_',' '),font=font,fill='white')
    canvas.save(OUT/filename)
views=sorted((OUT/'vistas_20').glob('*.png'));expr=sorted((OUT/'expresiones').glob('*.png'))
assert len(views)==20 and len(expr)==20
sheet(views,'Salve_20_vistas.png','SALVE / 20 VISTAS DE LA MALLA',5,320,500)
sheet(expr,'Salve_hoja_expresiones.png','SALVE / EXPRESIONES Y VISEMAS',5,350,395)
originals=json.loads((SRC/'references/manifest.json').read_text())['views'];comp=Image.new('RGB',(2000,1460),'#132037');d=ImageDraw.Draw(comp);d.text((24,20),'SALVE / REFERENCIA ORIGINAL Y RECONSTRUCCION V01',font=bold,fill='white');d.text((24,65),'Izquierda: original. Derecha: malla 3D. Las diferencias visibles siguen pendientes.',font=font,fill='#A7C5EB')
for i,(v,render) in enumerate(zip(originals,views)):
    x=(i%5)*400;y=(i//5)*340+100
    for j,path in enumerate([SRC/'references'/Path(v['path']).name,render]):
        im=Image.open(path).convert('RGBA');thumb=ImageOps.contain(im,(190,295));bg=Image.new('RGB',thumb.size,'#263346');bg.paste(thumb,mask=thumb.getchannel('A'));comp.paste(bg,(x+j*200+(200-thumb.width)//2,y))
    d.text((x+8,y+307),f'{i+1:02d} {v["id"]}',font=font,fill='white')
comp.save(OUT/'Salve_comparacion_20.png')
r=json.loads((OUT/'Salve_validacion.json').read_text());r['renders']={'views':len(views),'expressions':len(expr),'all_pngs_decoded':True};r['artistic_acceptance']='NO: divergencias de rostro, anatomia, peinado, paneles y poses frente al canon';(OUT/'Salve_validacion.json').write_text(json.dumps(r,indent=2,ensure_ascii=False),encoding='utf-8')
print('SHEETS_COMPLETE',len(views),len(expr))
