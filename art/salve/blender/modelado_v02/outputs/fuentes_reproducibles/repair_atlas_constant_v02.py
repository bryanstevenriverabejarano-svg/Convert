"""Rasterize declared constant material values on exact UVAtlas faces.
No blend, UV, material or source geometry is modified. Padding uses the nearest
occupied source pixel, never overwrites another island's original coverage.
"""
import argparse,hashlib,json,math,shutil,time
from pathlib import Path
import numpy as np
from PIL import Image,ImageDraw

CHANNELS=('baseColor','roughness','metallic')
def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()
def srgb(value):return 12.92*value if value<=.0031308 else 1.055*value**(1/2.4)-.055
def byte(value):return round(max(0,min(1,value))*255)

def main():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--uv',required=True,type=Path)
    parser.add_argument('--output',required=True,type=Path)
    parser.add_argument('--backup',required=True,type=Path)
    parser.add_argument('--supersampling',default=4,type=int,choices=(2,4,8))
    parser.add_argument('--margin',default=8,type=int)
    args=parser.parse_args();started=time.time()
    data=json.loads(args.uv.read_text(encoding='utf-8'))
    size=data['size'];ss=args.supersampling
    if size not in (2048,4096) or not 0<=args.margin<=32:raise ValueError('invalid size/margin')
    materials=data['materials'];names=list(materials)
    if len(names)>254:raise ValueError('too many material classes')
    ids={name:index+1 for index,name in enumerate(names)}
    palette={channel:np.zeros((len(names)+1,4),dtype=np.uint8) for channel in CHANNELS}
    for name,values in materials.items():
        index=ids[name]
        for channel in CHANNELS:
            source=values[channel]
            values3=source[:3] if channel=='baseColor' else [source]*3
            if len(values3)!=3 or any(not math.isfinite(v) or not 0<=v<=1 for v in values3):
                raise ValueError('invalid constant '+channel+' on '+name)
            rgb=[byte(srgb(v)) if channel=='baseColor' else byte(v) for v in values3]
            palette[channel][index]=rgb+[255]
    args.backup.mkdir(parents=True,exist_ok=False)
    records=[]
    for channel in CHANNELS:
        path=args.output/f'SalveV02_Atlas_{channel}.png'
        if not path.is_file():raise ValueError('missing initial atlas '+str(path))
        target=args.backup/path.name;shutil.copyfile(path,target)
        records.append({'channel':channel,'path':str(path.resolve()),'backup':str(target.resolve()),
                        'beforeSha256':digest(path),'colorSpace':'sRGB' if channel=='baseColor' else 'Non-Color'})
    hi_size=size*ss
    image=Image.new('L',(hi_size,hi_size));draw=ImageDraw.Draw(image)
    faces=0;subpixel=0;zeroarea=0;object_records=[]
    for obj in data['objects']:
        label=ids[obj['source_material']];thin=0
        for polygon in obj['faces']:
            if len(polygon)<3:raise ValueError('face with fewer than 3 UV loops')
            if any(len(point)!=2 or any(not math.isfinite(v) or v<-.0001 or v>1.0001 for v in point) for point in polygon):
                raise ValueError('nonfinite/outside unit tile UV in '+obj['name'])
            points=[(max(0,min(hi_size-1,u*hi_size)),max(0,min(hi_size-1,(1-v)*hi_size))) for u,v in polygon]
            twice=sum(polygon[i][0]*polygon[(i+1)%len(polygon)][1]-polygon[(i+1)%len(polygon)][0]*polygon[i][1] for i in range(len(polygon)))
            area=abs(twice)*size*size/2
            if area<1:subpixel+=1;thin+=1
            if area<1e-9:zeroarea+=1
            # Pillow includes polygon edges and supplies at least one high-resolution
            # sample even for a tapered/degenerate UV face. No black edge antialiasing.
            draw.polygon(points,fill=label)
            if area*ss*ss<1:draw.line(points+[points[0]],fill=label,width=1)
            faces+=1
        object_records.append({'name':obj['name'],'sourceMaterial':obj['source_material'],
                              'faces':len(obj['faces']),'subpixelFaces':thin})
    hi=np.asarray(image)
    owner=np.zeros((size,size),dtype=np.uint8)
    best=np.zeros((size,size),dtype=np.uint8)
    mixed=np.zeros((size,size),dtype=np.uint8)
    # Select the dominant source material in each supersampled pixel. Fully color
    # the pixel so subpixel white triangles cannot be mixed with a black clear value.
    for index in range(1,len(names)+1):
        count=(hi==index).reshape(size,ss,size,ss).sum(axis=(1,3),dtype=np.uint8)
        mixed+=(count>0)
        win=count>best;owner[win]=index;best[win]=count[win]
    del hi,image,draw,count,win
    original=owner.copy();padded=owner.copy()
    offsets=sorted((x*x+y*y,y,x) for y in range(-args.margin,args.margin+1)
                   for x in range(-args.margin,args.margin+1) if 0<x*x+y*y<=args.margin*args.margin)
    for _,dy,dx in offsets:
        sx0=max(0,-dx);sx1=size-max(0,dx);sy0=max(0,-dy);sy1=size-max(0,dy)
        src=original[sy0:sy1,sx0:sx1]
        dst=padded[sy0+dy:sy1+dy,sx0+dx:sx1+dx]
        take=(dst==0)&(src!=0);dst[take]=src[take]
    occupied=original!=0
    assert np.array_equal(padded[occupied],original[occupied])
    report={'status':'constant_atlas_rasterized_gui_check_pending','productionReady':False,
            'sourceUV':str(args.uv.resolve()),'sourceUVSha256':digest(args.uv),'size':size,
            'objectGroups':len(data['objects']),'faces':faces,'subpixelFaces':subpixel,
            'zeroAreaUVFaces':zeroarea,'supersampling':ss,'dilationRadiusPixels':args.margin,
            'originalCoveredPixels':int(np.count_nonzero(original)),
            'coveredAndDilatedPixels':int(np.count_nonzero(padded)),
            'mixedMaterialPixelsAtRasterBoundaries':int(np.count_nonzero(mixed>1)),
            'originalCoverageOverwrittenByPadding':0,'islandPaddingPolicy':'nearest source-pixel ownership; existing occupied pixels immutable; no iterative growth',
            'sourceGeometryUVOrMaterialsModified':False,'maps':records,'materials':[],
            'objects':object_records,
            'method':'UV polygon ownership raster at 4x (configured supersampling), dominant full-color texels, sRGB encoding for linear baseColor, linear bytes for scalar maps',
            'verifiedVisualCause':False,'visualCheck':'pending',
            'pending':['Reload and explicitly repack all three images in final blend',
                       'Verify previous fringe-tip defect in GUI and render',
                       'Review UV density/seams and production deformation separately']}
    for name,index in ids.items():
        report['materials'].append({'name':name,'coveredPixels':int(np.count_nonzero(original==index)),
                                    'coveredAndPaddedPixels':int(np.count_nonzero(padded==index)),
                                    'baseColorSRGB8':palette['baseColor'][index,:3].tolist(),
                                    'roughness8':int(palette['roughness'][index,0]),'metallic8':int(palette['metallic'][index,0])})
    for record in records:
        channel=record['channel'];path=Path(record['path'])
        pixels=palette[channel][padded]
        with Image.open(record['backup']) as initial:
            old=np.asarray(initial.convert('RGBA'))
        record['pixelsChanged']=int(np.count_nonzero(np.any(old!=pixels,axis=2)))
        if channel=='baseColor':
            hair_ids=[ids[name] for name in names if 'Cabello' in name]
            hair=np.isin(original,hair_ids)
            record['initialNearBlackPixelsInsideHairCoverage']=int(np.count_nonzero(hair&np.all(old[:,:,:3]<8,axis=2)))
        temporary=args.backup/f'repaired_{path.name}'
        Image.fromarray(pixels,'RGBA').save(temporary)
        with Image.open(temporary) as check:
            check.load();assert check.size==(size,size) and check.mode=='RGBA'
        shutil.copyfile(temporary,path)
        record['afterSha256']=digest(path);record['dimensions']=[size,size]
    report['elapsedSeconds']=round(time.time()-started,2)
    (args.output/'atlas_repair_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps({key:report[key] for key in ('status','faces','subpixelFaces','zeroAreaUVFaces','originalCoveredPixels','coveredAndDilatedPixels','originalCoverageOverwrittenByPadding','elapsedSeconds')},ensure_ascii=False))

if __name__=='__main__':main()
