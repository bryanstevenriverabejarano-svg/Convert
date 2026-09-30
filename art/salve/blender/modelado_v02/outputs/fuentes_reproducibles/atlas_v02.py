"""Bake a real shared UV atlas on visible body/hair/technology meshes.
Run in Blender, optionally --scene NAME --output DIR --size 2048 --save FILE.
Face collections and hidden sources are excluded. Original material datablocks are preserved.
"""
import argparse, hashlib, json, math, pathlib, sys, time
import bpy

def arguments():
    parser=argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--scene')
    parser.add_argument('--output',required=True)
    parser.add_argument('--size',type=int,choices=(2048,4096),default=2048)
    parser.add_argument('--save')
    parser.add_argument('--threads',type=int,default=2)
    return parser.parse_args(sys.argv[sys.argv.index('--')+1:] if '--' in sys.argv else [])

def node_socket(material,channel):
    outputs=[n for n in material.node_tree.nodes if n.type=='OUTPUT_MATERIAL' and n.is_active_output]
    if not outputs or not outputs[0].inputs['Surface'].is_linked:
        raise RuntimeError('Material has no supported linked surface: '+material.name)
    surface=outputs[0].inputs['Surface'].links[0].from_node
    if surface.type=='BSDF_PRINCIPLED':
        return surface.inputs[{'baseColor':'Base Color','roughness':'Roughness','metallic':'Metallic'}[channel]]
    if surface.type=='EMISSION':
        return surface.inputs['Color'] if channel=='baseColor' else (0.0 if channel=='metallic' else .5)
    raise RuntimeError('Unsupported surface '+surface.type+' on '+material.name)

def scalar_color(value):
    if isinstance(value,(float,int)):return (value,value,value,1.0)
    values=list(value)
    return tuple(values[:3])+((values[3] if len(values)>3 else 1.0),)

def main():
    args=arguments();started=time.time()
    if args.scene:
        scene=bpy.data.scenes[args.scene]
        if bpy.context.window:bpy.context.window.scene=scene
    else:scene=bpy.context.scene
    out=pathlib.Path(args.output).resolve();out.mkdir(parents=True,exist_ok=True)
    selected=[]
    for obj in scene.objects:
        if obj.type!='MESH' or obj.hide_render or obj.hide_viewport or not obj.visible_get():continue
        names=[c.name.lower() for c in obj.users_collection]
        if not any((name.startswith('01_') and ('cuerpo' in name or 'body' in name)) or (name.startswith('03_') and ('cabello' in name or 'hair' in name)) or (name.startswith('04_') and ('tecno' in name or 'tech' in name)) for name in names):continue
        if obj.get('sourceobj') or any(word in obj.name.lower() for word in ('source','fuente','referencia','landmark')):continue
        selected.append(obj)
    if not selected:raise RuntimeError('No visible body/hair/technology meshes matched selected scene')
    bpy.ops.object.mode_set(mode='OBJECT') if bpy.context.object and bpy.context.object.mode!='OBJECT' else None
    selection_before=list(bpy.context.selected_objects);active_before=bpy.context.view_layer.objects.active
    engine_before=scene.render.engine;samples_before=scene.cycles.samples;device_before=scene.cycles.device
    render_before={'threads_mode':scene.render.threads_mode,'threads':scene.render.threads}
    bake_before={key:getattr(scene.render.bake,key) for key in ('use_selected_to_active','use_clear','margin')}
    report={'scene':scene.name,'productionReady':False,'uvLayer':'UVAtlas','resolution':args.size,'objects':[],
            'maps':[],'materialCopies':[],'pending':['Manual UV seam and texel-density review','Atlas comparison against original shader appearance','Deformation and final production-topology validation']}
    bake_scene=None;bake_objects=[];bake_meshes=[]
    try:
        # Keep prior object data when it is also used by excluded geometry.
        materials={}
        for obj in selected:
            if obj.data.users>1:obj.data=obj.data.copy()
            report['objects'].append({'name':obj.name,'vertices':len(obj.data.vertices),'polygons':len(obj.data.polygons),'collections':[c.name for c in obj.users_collection]})
            for slot in obj.material_slots:
                original=slot.material
                if original is None:raise RuntimeError('Missing material on '+obj.name)
                if not original.use_nodes:raise RuntimeError('Non-node material not supported: '+original.name)
                if original.name not in materials:
                    original.use_fake_user=True
                    copy=original.copy();copy.name='AtlasV02_'+original.name
                    copy['salve_original_material']=original.name
                    for channel in ('baseColor','roughness','metallic'):node_socket(copy,channel)
                    materials[original.name]=(original,copy)
                    report['materialCopies'].append({'original':original.name,'atlas':copy.name})
                slot.material=materials[original.name][1]
            mesh=obj.data
            original_uv=next((uv for uv in mesh.uv_layers if uv.active_render),mesh.uv_layers.active)
            if original_uv:
                old_points=[tuple(point.uv) for point in original_uv.data]
                source_uv=mesh.uv_layers.get('UVBakeSource') or mesh.uv_layers.new(name='UVBakeSource')
                for point,value in zip(source_uv.data,old_points):point.uv=value
            layer=mesh.uv_layers.get('UVAtlas') or mesh.uv_layers.new(name='UVAtlas')
            mesh.uv_layers.active=layer;layer.active_render=True
        for original,material in materials.values():
            tree=material.node_tree
            textures=[node for node in tree.nodes if node.type=='TEX_IMAGE' and not node.inputs['Vector'].is_linked]
            if textures:
                source_uv=tree.nodes.new('ShaderNodeUVMap');source_uv.name='AtlasBake_SourceUV';source_uv.uv_map='UVBakeSource'
                for tex in textures:tree.links.new(source_uv.outputs['UV'],tex.inputs['Vector'])
        print('SALVE_ATLAS: material copies prepared, projecting UVAtlas',flush=True)
        bpy.ops.object.select_all(action='DESELECT')
        for obj in selected:obj.select_set(True)
        bpy.context.view_layer.objects.active=selected[0]
        bpy.ops.object.mode_set(mode='EDIT');bpy.ops.mesh.select_all(action='SELECT')
        bpy.ops.uv.smart_project(angle_limit=1.05,island_margin=.002,area_weight=.8,correct_aspect=True,scale_to_bounds=False)
        print('SALVE_ATLAS: smart projection complete; packing islands',flush=True)
        properties=bpy.ops.uv.pack_islands.get_rna_type().properties
        kwargs={'rotate':True,'scale':True,'margin':8/args.size}
        if 'margin_method' in properties:
            enums=[item.identifier for item in properties['margin_method'].enum_items]
            if 'FRACTION' in enums:kwargs['margin_method']='FRACTION'
        if 'rotate_method' in properties:
            enums=[item.identifier for item in properties['rotate_method'].enum_items]
            if 'CARDINAL' in enums:kwargs['rotate_method']='CARDINAL'
        if 'shape_method' in properties:
            enums=[item.identifier for item in properties['shape_method'].enum_items]
            if 'AABB' in enums:kwargs['shape_method']='AABB'
        if 'udim_source' in properties:
            enums=[item.identifier for item in properties['udim_source'].enum_items]
            if 'ACTIVE_UDIM' in enums:kwargs['udim_source']='ACTIVE_UDIM'
        kwargs={key:value for key,value in kwargs.items() if key in properties}
        bpy.ops.uv.pack_islands(**kwargs)
        bpy.ops.object.mode_set(mode='OBJECT')
        report['packParameters']=kwargs
        print('SALVE_ATLAS: UVAtlas packed for '+str(len(selected))+' objects',flush=True)
        for obj in selected:
            uvs=obj.data.uv_layers['UVAtlas']
            coords=[component for point in uvs.data for component in point.uv]
            if any(not math.isfinite(value) or value<-.0001 or value>1.0001 for value in coords):
                raise RuntimeError('UVAtlas coordinate outside unit tile on '+obj.name)
        # Bake base meshes in isolation. Armature/shape-driver evaluation of the production
        # scene is unnecessary for an atlas and can expand the dependency graph substantially.
        bake_scene=bpy.data.scenes.new('__SALVE_ATLAS_STATIC_BAKE__')
        for obj in selected:
            static=obj.copy();static.data=obj.data.copy();bake_meshes.append(static.data)
            static.animation_data_clear();static.shape_key_clear();static.modifiers.clear()
            for constraint in list(static.constraints):static.constraints.remove(constraint)
            world=obj.matrix_world.copy();static.parent=None;static.matrix_world=world
            static.hide_render=False;static.hide_viewport=False
            bake_scene.collection.objects.link(static);bake_objects.append(static)
        if bpy.context.window:bpy.context.window.scene=bake_scene
        for obj in bake_objects:obj.hide_set(False);obj.select_set(True)
        bpy.context.view_layer.objects.active=bake_objects[0]
        bake_scene.render.engine='CYCLES';bake_scene.cycles.samples=1;bake_scene.cycles.device='CPU'
        bake_scene.render.threads_mode='FIXED';bake_scene.render.threads=max(1,min(16,args.threads))
        bake_scene.render.bake.use_selected_to_active=False;bake_scene.render.bake.use_clear=True;bake_scene.render.bake.margin=8
        report['bakeGeometry']='isolated static base meshes; modifiers and shape drivers excluded from temporary bake copies'
        images={}
        for channel in ('baseColor','roughness','metallic'):
            image=bpy.data.images.new('SalveV02_Atlas_'+channel,width=args.size,height=args.size,alpha=True,float_buffer=False)
            image.colorspace_settings.name='sRGB' if channel=='baseColor' else 'Non-Color'
            image.file_format='PNG';image.filepath_raw=str(out/('SalveV02_Atlas_'+channel+'.png'))
            images[channel]=image
            temporary=[]
            try:
                for original,material in materials.values():
                    tree=material.node_tree
                    output=next(n for n in tree.nodes if n.type=='OUTPUT_MATERIAL' and n.is_active_output)
                    links=[(link.from_socket,link.to_socket) for link in list(output.inputs['Surface'].links)]
                    socket=node_socket(material,channel)
                    emit=tree.nodes.new('ShaderNodeEmission');emit.name='__SALVE_ATLAS_BAKE_EMIT__'
                    emit.inputs['Strength'].default_value=1
                    if isinstance(socket,(float,int)):emit.inputs['Color'].default_value=scalar_color(socket)
                    elif socket.is_linked:tree.links.new(socket.links[0].from_socket,emit.inputs['Color'])
                    else:emit.inputs['Color'].default_value=scalar_color(socket.default_value)
                    target=tree.nodes.new('ShaderNodeTexImage');target.name='__SALVE_ATLAS_BAKE_TARGET__';target.image=image
                    for node in tree.nodes:node.select=False
                    target.select=True;tree.nodes.active=target
                    tree.links.new(emit.outputs['Emission'],output.inputs['Surface'])
                    temporary.append((tree,output,links,emit,target))
                print('SALVE_ATLAS: baking '+channel,flush=True)
                bpy.ops.object.bake(type='EMIT')
                image.save();image.pack()
                path=pathlib.Path(image.filepath_raw)
                if not path.is_file() or path.stat().st_size==0:raise RuntimeError('Atlas image was not saved')
                report['maps'].append({'channel':channel,'path':str(path),'dimensions':list(image.size),
                                       'colorSpace':image.colorspace_settings.name,'packed':bool(image.packed_file),
                                       'sha256':hashlib.sha256(path.read_bytes()).hexdigest()})
            finally:
                for tree,output,links,emit,target in temporary:
                    tree.nodes.remove(emit);tree.nodes.remove(target)
                    for source,destination in links:tree.links.new(source,destination)
        # Work on material copies. Original node graphs are retained, including facial/iris users.
        for original,material in materials.values():
            tree=material.node_tree
            uv=tree.nodes.new('ShaderNodeUVMap');uv.name='textureAtlas_UV';uv.label='UVAtlas';uv.uv_map='UVAtlas'
            for channel in ('baseColor','roughness','metallic'):
                tex=tree.nodes.new('ShaderNodeTexImage');tex.name='textureAtlas_'+channel;tex.label='Salve atlas '+channel;tex.image=images[channel]
                tree.links.new(uv.outputs['UV'],tex.inputs['Vector'])
                socket=node_socket(material,channel)
                if not isinstance(socket,(float,int)):tree.links.new(tex.outputs['Color'],socket)
            material['salve_atlas_uv']='UVAtlas'
        report['status']='atlas_baked_and_linked_material_copies'
    except Exception as error:
        report['status']='failed';report['error']=str(error)
        raise
    finally:
        if bpy.context.object and bpy.context.object.mode!='OBJECT':bpy.ops.object.mode_set(mode='OBJECT')
        if bpy.context.window:bpy.context.window.scene=scene
        for obj in bake_objects:bpy.data.objects.remove(obj,do_unlink=True)
        for mesh in bake_meshes:
            if mesh.users==0:bpy.data.meshes.remove(mesh)
        if bake_scene:bpy.data.scenes.remove(bake_scene)
        scene.render.engine=engine_before;scene.cycles.samples=samples_before;scene.cycles.device=device_before
        for key,value in render_before.items():setattr(scene.render,key,value)
        for key,value in bake_before.items():setattr(scene.render.bake,key,value)
        bpy.ops.object.select_all(action='DESELECT')
        for obj in selection_before:
            if obj.name in bpy.context.view_layer.objects:obj.select_set(True)
        if active_before and active_before.name in bpy.context.view_layer.objects:bpy.context.view_layer.objects.active=active_before
        report['elapsedSeconds']=round(time.time()-started,2)
        report['originalMaterialsPreserved']=True
        (out/'atlas_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    if args.save:
        bpy.ops.wm.save_as_mainfile(filepath=str(pathlib.Path(args.save).resolve()))
        report['savedBlend']=str(pathlib.Path(args.save).resolve())
        (out/'atlas_validation.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    summary={key:report[key] for key in ('status','scene','uvLayer','resolution','maps','elapsedSeconds','originalMaterialsPreserved')}
    summary['objectCount']=len(report['objects']);summary['savedBlend']=report.get('savedBlend')
    print('SALVE_ATLAS_RESULT '+json.dumps(summary,ensure_ascii=False))

if __name__=='__main__':main()
