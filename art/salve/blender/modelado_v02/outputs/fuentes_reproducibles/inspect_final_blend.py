"""Read-only Blender audit. Does not save or modify the opened blend file."""
import argparse, array, collections, hashlib, json, math, pathlib, sys
import bpy

def digest(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--scene');parser.add_argument('--json',required=True)
    args=parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])
    scene=bpy.data.scenes[args.scene] if args.scene else bpy.context.scene
    if bpy.context.window:bpy.context.window.scene=scene
    source=pathlib.Path(bpy.data.filepath)
    report={'file':str(source),'fileSha256Before':digest(source),'scene':scene.name,'frame':scene.frame_current,
            'productionReady':False,'meshes':[],'armatures':[],'images':[],
            'checksNotCertified':['UV overlap/seam quality and texel density','Anatomical deformation quality and production retopology','Self-intersections/hair-body penetrations in all poses','IK pole calibration/contact accuracy','Artistic identity against all 20 references']}
    for obj in scene.objects:
        if obj.type=='ARMATURE':
            report['armatures'].append({'name':obj.name,'bones':len(obj.data.bones),'constraints':sum(len(b.constraints) for b in obj.pose.bones),
                                      'ikConstraints':sum(c.type=='IK' for b in obj.pose.bones for c in b.constraints)})
        if obj.type!='MESH' or obj.hide_render:continue
        mesh=obj.data
        if any(word in obj.name.lower() for word in ('source','landmark','referencia')):continue
        use=[0]*len(mesh.edges)
        for loop in mesh.loops:use[loop.edge_index]+=1
        valence=[0]*len(mesh.vertices)
        for edge in mesh.edges:
            for index in edge.vertices:valence[index]+=1
        uv=[]
        for layer in mesh.uv_layers:
            coords=[component for loop in layer.data for component in loop.uv]
            degenerate=0
            for poly in mesh.polygons:
                values=[layer.data[i].uv for i in poly.loop_indices]
                area=sum(values[i][0]*values[(i+1)%len(values)][1]-values[(i+1)%len(values)][0]*values[i][1] for i in range(len(values)))
                if abs(area)<1e-12:degenerate+=1
            uv.append({'name':layer.name,'loops':len(layer.data),'renderActive':layer.active_render,
                       'nonfiniteCoordinates':sum(not math.isfinite(v) for v in coords),
                       'outsideUnitTile':sum(v<-.0001 or v>1.0001 for v in coords if math.isfinite(v)),
                       'uvMin':min(coords) if coords else None,'uvMax':max(coords) if coords else None,
                       'zeroAreaFaces':degenerate})
        armatures=[modifier.object for modifier in obj.modifiers if modifier.type=='ARMATURE' and modifier.object]
        weights=None
        if armatures:
            bones={bone.name for armature in armatures for bone in armature.data.bones}
            bone_groups={group.index for group in obj.vertex_groups if group.name in bones}
            sums=[sum(group.weight for group in vertex.groups if group.group in bone_groups) for vertex in mesh.vertices]
            weights={'rigs':[rig.name for rig in armatures],'verticesWithNoBoneInfluence':sum(value<1e-7 for value in sums),
                     'verticesOutsideNormalizedTolerance':sum(abs(value-1)>.02 for value in sums),
                     'minBoneWeightSum':min(sums) if sums else None,'maxBoneWeightSum':max(sums) if sums else None}
        nonfinite=sum(any(not math.isfinite(value) for value in vertex.co) for vertex in mesh.vertices)
        shapes=mesh.shape_keys
        report['meshes'].append({'name':obj.name,'collections':[c.name for c in obj.users_collection],
            'vertices':len(mesh.vertices),'edges':len(mesh.edges),'faces':len(mesh.polygons),
            'triangles':sum(len(p.vertices)==3 for p in mesh.polygons),'quads':sum(len(p.vertices)==4 for p in mesh.polygons),
            'ngons':sum(len(p.vertices)>4 for p in mesh.polygons),'boundaryEdges':sum(value==1 for value in use),
            'nonmanifoldEdgesExcludingBoundary':sum(value==0 or value>2 for value in use),
            'looseVertices':sum(value==0 for value in valence),'nonfiniteVertices':nonfinite,
            'vertexValenceHistogram':dict(collections.Counter(valence)),
            'uvLayers':uv,'weights':weights,'materialSlots':[slot.material.name if slot.material else None for slot in obj.material_slots],
            'shapeKeys':[key.name for key in shapes.key_blocks] if shapes else [],
            'shapeDrivers':len(shapes.animation_data.drivers) if shapes and shapes.animation_data else 0,
            'modifiers':[{'name':m.name,'type':m.type,'viewport':m.show_viewport,'render':m.show_render} for m in obj.modifiers]})
    visible_materials={slot.material for obj in scene.objects if obj.type=='MESH' and not obj.hide_render
                       and not any(word in obj.name.lower() for word in ('source','landmark','referencia'))
                       for slot in obj.material_slots if slot.material and slot.material.use_nodes}
    for image in bpy.data.images:
        path=bpy.path.abspath(image.filepath,library=image.library) if image.filepath else ''
        packed=bool(image.packed_file) or bool(len(image.packed_files))
        external=image.source in {'FILE','TILED','SEQUENCE','MOVIE'} and not packed
        exists=pathlib.Path(path).exists() if path and '<UDIM>' not in path else None
        packed_hash=hashlib.sha256(bytes(image.packed_file.data)).hexdigest() if image.packed_file else None
        file_hash=digest(pathlib.Path(path)) if exists is True and pathlib.Path(path).is_file() else None
        users=[material.name for material in visible_materials
               if any(node.type=='TEX_IMAGE' and node.image==image for node in material.node_tree.nodes)]
        report['images'].append({'name':image.name,'source':image.source,'size':list(image.size),'colorSpace':image.colorspace_settings.name,
                                'filepath':path,'packed':packed,'externalFileExists':exists,'missingExternalFile':external and exists is False,
                                'packedFileSha256':packed_hash,'externalFileSha256':file_hash,
                                'packedBytesMatchExternalFile':packed_hash==file_hash if packed_hash and file_hash else None,
                                'visibleMaterialUsers':users})
    report['missingTextureCount']=sum(bool(image['missingExternalFile']) for image in report['images'])
    report['meshNonfiniteVertexCount']=sum(mesh['nonfiniteVertices'] for mesh in report['meshes'])
    report['riggedVerticesWithNoBoneInfluence']=sum(mesh['weights']['verticesWithNoBoneInfluence'] for mesh in report['meshes'] if mesh['weights'])
    report['meshesWithoutUV']=sum(not mesh['uvLayers'] for mesh in report['meshes'])
    atlas_images=[image for image in report['images'] if 'SalveV02_Atlas_' in image['name'] and image['visibleMaterialUsers']]
    report['usedAtlasImages']=len(atlas_images)
    report['usedAtlasPackedImagesMatchExternalFiles']=len(atlas_images)==3 and all(image['packedBytesMatchExternalFile'] is True for image in atlas_images)
    # A global minimum can be a boot or hand. Measure the declared posterior
    # pelvis region independently in the delivered seated frame.
    rig=next((obj for obj in scene.objects if obj.type=='ARMATURE' and 'pelvis' in obj.pose.bones),None)
    if rig:
        saved_frame=scene.frame_current
        try:
            scene.frame_set(91);bpy.context.view_layer.update()
            depsgraph=bpy.context.evaluated_depsgraph_get()
            pelvis_y=(rig.matrix_world@rig.pose.bones['pelvis'].head).y
            points=[];per_object=[]
            for obj in scene.objects:
                if obj.type!='MESH' or obj.hide_render or not any(c.name.startswith('01_Cuerpo') for c in obj.users_collection):continue
                groups={group.index for group in obj.vertex_groups if group.name=='pelvis'}
                if not groups:continue
                evaluated=obj.evaluated_get(depsgraph)
                mesh=evaluated.to_mesh(preserve_all_data_layers=True,depsgraph=depsgraph)
                local=[]
                try:
                    for vertex in mesh.vertices:
                        if sum(group.weight for group in vertex.groups if group.group in groups)<.72:continue
                        coordinate=evaluated.matrix_world@vertex.co
                        if coordinate.y>pelvis_y+.012:local.append(coordinate.z)
                finally:evaluated.to_mesh_clear()
                points.extend(local)
                if local:per_object.append({'name':obj.name,'samples':len(local),'minimumZ_m':min(local)})
            report['seatedPelvisContactCheck']={'frame':91,'criterion':'pelvis weight >= .72; world Y > pelvis head Y + .012 m',
                    'pelvisHeadWorldY_m':pelvis_y,'samples':len(points),'minimumZ_m':min(points) if points else None,
                    'floorPlaneZ_m':0,'targetClearance_m':.001,'objects':per_object,
                    'scope':'posterior weighted vertex-region minimum; does not certify all pelvis contact surface'}
        finally:scene.frame_set(saved_frame);bpy.context.view_layer.update()
    report['fileSha256After']=digest(source);report['sourceFileUnchanged']=report['fileSha256Before']==report['fileSha256After']
    target=pathlib.Path(args.json);target.parent.mkdir(parents=True,exist_ok=True)
    target.write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print('SALVE_READONLY_AUDIT '+json.dumps({key:report[key] for key in ('scene','missingTextureCount','meshNonfiniteVertexCount','riggedVerticesWithNoBoneInfluence','meshesWithoutUV','sourceFileUnchanged')}))

if __name__=='__main__':main()
