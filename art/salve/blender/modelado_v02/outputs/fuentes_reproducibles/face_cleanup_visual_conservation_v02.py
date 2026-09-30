"""Unsaved face housekeeping candidate with declared visual-conservation bounds.

Reuses strict transactional input/remap/weight/UV checks. Does NOT claim evaluated
bit equality. Limit: <=0.01 px across 41 delivered cameras, and <=1 degree for
corner/face normals at the 33 unique frames. Produces controlled neutral PNGs for
review before any caller saves. Numeric failure restores the original mesh.
"""
import hashlib
import json
import pathlib
import traceback

import bpy
import numpy as np

ROOT = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')
MAX_PIXEL_ERROR = .01
MAX_NORMAL_ANGLE_DEG = 1.0


def rows():
    views = json.loads((ROOT/'outputs/vistas_manifest.json').read_text(encoding='utf-8'))
    expressions = json.loads((ROOT/'outputs/expresiones_controles.json').read_text(encoding='utf-8'))
    assert len(views) == 20 and len(expressions) == 21
    return views + expressions


def matrix_numpy(matrix):
    return np.asarray([tuple(row) for row in matrix], dtype=np.float64)


def capture_sample(obj, scene, frame, delivered):
    scene.frame_set(frame)
    bpy.context.view_layer.update()
    depsgraph = bpy.context.evaluated_depsgraph_get()
    evaluated = obj.evaluated_get(depsgraph)
    mesh = evaluated.to_mesh()
    try:
        vertices = np.empty(len(mesh.vertices)*3, dtype=np.float32)
        mesh.vertices.foreach_get('co', vertices)
        loop_vertices = np.empty(len(mesh.loops), dtype=np.int32)
        mesh.loops.foreach_get('vertex_index', loop_vertices)
        local_positions = vertices.reshape(-1, 3)[loop_vertices].astype(np.float64)
        corner_normals = np.empty(len(mesh.loops)*3, dtype=np.float32)
        mesh.corner_normals.foreach_get('vector', corner_normals)
        face_normals = np.empty(len(mesh.polygons)*3, dtype=np.float32)
        mesh.polygons.foreach_get('normal', face_normals)
        world = matrix_numpy(evaluated.matrix_world)
        normal_transform = np.linalg.inv(world[:3, :3]).T
        face_layout = np.empty((len(mesh.polygons), 4), dtype=np.int32)
        for column, attribute in enumerate(['loop_start','loop_total','material_index','use_smooth']):
            if attribute == 'use_smooth':
                values = np.empty(len(mesh.polygons), dtype=np.bool_)
            else:
                values = np.empty(len(mesh.polygons), dtype=np.int32)
            mesh.polygons.foreach_get(attribute, values)
            face_layout[:, column] = values
        cameras = []
        for row in delivered:
            if row['frame'] != frame:
                continue
            camera = bpy.data.objects[row['camera']].evaluated_get(depsgraph)
            width, height = row['size']
            projection = camera.calc_matrix_camera(
                depsgraph, x=width, y=height,
                scale_x=scene.render.pixel_aspect_x,
                scale_y=scene.render.pixel_aspect_y)
            clip = matrix_numpy(projection) @ matrix_numpy(camera.matrix_world.inverted())
            cameras.append({'id':row['id'], 'camera':row['camera'],
                            'size':[width,height], 'clip':clip})
        return {
            'world':world,
            'positions':local_positions @ world[:3,:3].T + world[:3,3],
            'corner_normals':corner_normals.reshape(-1,3).astype(np.float64) @ normal_transform.T,
            'face_normals':face_normals.reshape(-1,3).astype(np.float64) @ normal_transform.T,
            'face_layout':face_layout, 'cameras':cameras
        }
    finally:
        evaluated.to_mesh_clear()


def angle_max(before, after):
    length_before = np.linalg.norm(before, axis=1)
    length_after = np.linalg.norm(after, axis=1)
    assert np.all(length_before > 0) and np.all(length_after > 0)
    cosine = np.einsum('ij,ij->i', before, after)/(length_before*length_after)
    angles = np.degrees(np.arccos(np.clip(cosine, -1, 1)))
    angles[np.all(before == after, axis=1)] = 0
    return float(np.max(angles))


def compare_samples(before, after, frame):
    matrix_equal = np.array_equal(before['world'], after['world'])
    layout_equal = np.array_equal(before['face_layout'], after['face_layout'])
    assert matrix_equal and layout_equal, 'Matrix or evaluated face layout changed'
    assert before['positions'].shape == after['positions'].shape
    distances = np.linalg.norm(after['positions']-before['positions'], axis=1)
    corner_angle = angle_max(before['corner_normals'], after['corner_normals'])
    face_angle = angle_max(before['face_normals'], after['face_normals'])
    projected = []
    assert len(before['cameras']) == len(after['cameras'])
    for ca, cb in zip(before['cameras'], after['cameras']):
        assert ca['id'] == cb['id'] and ca['camera'] == cb['camera'] and ca['size'] == cb['size']
        assert np.array_equal(ca['clip'], cb['clip']), 'Delivered camera changed'
        clip = ca['clip']
        pa = before['positions'] @ clip[:, :3].T + clip[:, 3]
        pb = after['positions'] @ clip[:, :3].T + clip[:, 3]
        assert np.all(np.abs(pa[:,3]) > 1e-9) and np.all(np.abs(pb[:,3]) > 1e-9), \
            'Projection approaches camera plane'
        xy_before = pa[:,:2]/pa[:,3,None]
        xy_after = pb[:,:2]/pb[:,3,None]
        error = np.linalg.norm((xy_after-xy_before)*np.asarray(ca['size'])[None,:]/2, axis=1)
        maximum = float(np.max(error))
        projected.append({'id':ca['id'], 'camera':ca['camera'], 'size':ca['size'],
                          'maximum_pixel_displacement':maximum,
                          'points_tested':len(error), 'passed':maximum <= MAX_PIXEL_ERROR,
                          'scope':'All evaluated face corners, including occluded/off-frame points; conservative bound.'})
    passed = bool(corner_angle <= MAX_NORMAL_ANGLE_DEG and face_angle <= MAX_NORMAL_ANGLE_DEG
                  and all(view['passed'] for view in projected))
    return {'frame':frame, 'matrix_exact':matrix_equal, 'face_layout_exact':layout_equal,
            'max_world_position_delta_m':float(np.max(distances)),
            'max_corner_normal_angle_deg':corner_angle,
            'max_face_normal_angle_deg':face_angle,
            'max_corner_normal_vector_delta':float(np.max(np.linalg.norm(after['corner_normals']-before['corner_normals'],axis=1))),
            'views':projected, 'passed':passed}


def render_neutral(label, rendered):
    scene = bpy.context.scene
    render = scene.render
    old = {name:getattr(render,name) for name in
           ['resolution_x','resolution_y','resolution_percentage','filepath','film_transparent']}
    image_old = {name:getattr(render.image_settings,name) for name in ['file_format','color_mode','color_depth']}
    old_frame, old_camera = scene.frame_current, scene.camera
    file = ROOT/'work'/('face_cleanup_neutral_'+label+'.png')
    try:
        scene.frame_set(201)
        bpy.context.view_layer.update()
        scene.camera = bpy.data.objects['CAM_FACE_NEUTRAL']
        render.resolution_x = render.resolution_y = 768
        render.resolution_percentage = 100
        render.film_transparent = True
        render.image_settings.file_format = 'PNG'
        render.image_settings.color_mode = 'RGBA'
        render.image_settings.color_depth = '8'
        render.filepath = str(file)
        bpy.ops.render.render(write_still=True)
        rendered[label] = {'file':str(file), 'sha256':hashlib.sha256(file.read_bytes()).hexdigest(),
                           'frame':201, 'camera':'CAM_FACE_NEUTRAL', 'size':[768,768]}
        print('FACE_CLEANUP_CONTROLLED_NEUTRAL_DONE', label, flush=True)
    finally:
        scene.camera = old_camera
        for name,value in old.items():setattr(render,name,value)
        for name,value in image_old.items():setattr(render.image_settings,name,value)
        scene.frame_set(old_frame)
        bpy.context.view_layer.update()


def image_metrics(rendered):
    decoded = []
    for label in ['before','after']:
        image = bpy.data.images.load(rendered[label]['file'], check_existing=False)
        try:
            assert tuple(image.size) == (768,768)
            values = np.empty(768*768*4, dtype=np.float32)
            image.pixels.foreach_get(values)
            decoded.append(values.reshape(768,768,4).copy())
        finally:
            bpy.data.images.remove(image)
    before, after = decoded
    alpha_delta = np.abs(after[:,:,3]-before[:,:,3])*255
    # RGB under zero alpha is not visible; compare premultiplied colour.
    rgb_delta = (after[:,:,:3]*after[:,:,3,None]-before[:,:,:3]*before[:,:,3,None])*255
    return {'comparison_space':'Blender-decoded PNG channels, same colour policy; RGB premultiplied by alpha, scaled to 255.',
            'max_alpha_channel_delta_255':float(np.max(alpha_delta)),
            'max_premultiplied_rgb_channel_delta_255':float(np.max(np.abs(rgb_delta))),
            'rms_premultiplied_rgb_channel_delta_255':float(np.sqrt(np.mean(rgb_delta.astype(np.float64)**2))),
            'pixels_with_alpha_change':int(np.count_nonzero(alpha_delta)),
            'pixels_with_visible_rgb_change':int(np.count_nonzero(np.any(rgb_delta != 0,axis=2))),
            'decoded_pixels_equal':bool(np.array_equal(before,after)),
            'visual_review_required_before_caller_saves':True}


def adapted_source(source):
    source = source.replace('surface_digest', 'surface_sample').replace('before_surfaces','before_samples')
    start = source.index('def surface_sample(')
    end = source.index('\ndef uv_check(', start)
    source = source[:start] + 'def surface_sample(obj, scene, frame):\n    return _capture(obj, scene, frame)\n\n' + source[end:]
    old = r'''        for frame in frames:
            assert surface_sample(obj, scene, frame) == before_samples[frame], \
                'Visible surface/normal changed at frame ' + str(frame)'''
    new = '''        for frame in frames:
            metric = _compare(before_samples[frame], surface_sample(obj, scene, frame), frame)
            _metrics.append(metric)
            assert metric['passed'], 'Visual conservation bound failed: ' + json.dumps(metric)
            print('FACE_VISUAL_METRIC_FRAME_PASS', frame, flush=True)
        assert len(_metrics) == 33 and sum(len(item['views']) for item in _metrics) == 41
        _render('after')
        _image_metrics.update(_compare_images())'''
    assert old in source, 'Frozen transaction guard changed; review the adaptation'
    source = source.replace(old,new)
    needle = '    working = original.copy()'
    assert source.count(needle) == 1
    source = source.replace(needle, "    _render('before')\n"+needle)
    needle = "            'surface_and_normal_bit_hashes_equal_at_unique_frames': frames,"
    assert needle in source
    source = source.replace(needle, "            'surface_conservation_metrics_at_unique_frames': _metrics,\n            'neutral_image_comparison': _image_metrics,")
    source = source.replace("'component': 'face_topology_uv_housekeeping'", "'component': 'face_topology_uv_visual_conservation'")
    source = source.replace('FACE_HOUSEKEEPING_SURFACES_PRESERVED', 'FACE_VISUAL_CONSERVATION_CANDIDATE_READY')
    compile(source,'face_visual_transaction_adapted','exec')
    return source


def main(report_path=None, save=False):
    assert save is False, 'Caller must inspect neutral comparison before saving'
    obj = bpy.data.objects['Rostro_topologia']
    original = obj.data
    original_fake_user = original.use_fake_user
    original_properties = {name:obj.get(name) for name in ['cleanup_backup_mesh','cleanup_scope']}
    subdivision = next(modifier for modifier in obj.modifiers if modifier.type == 'SUBSURF')
    assert subdivision.levels == subdivision.render_levels == 1
    delivered = rows()
    metrics, rendered, image_comparison = [], {}, {}
    file = ROOT/'work/face_topology_uv_safe_cleanup_v02.py'
    source = adapted_source(file.read_text(encoding='utf-8-sig'))
    namespace = {'__name__':'salve_visual_conservation_transaction', '__file__':str(file),
                 '_capture':lambda target,scene,frame:capture_sample(target,scene,frame,delivered),
                 '_compare':compare_samples, '_metrics':metrics,
                 '_render':lambda label:render_neutral(label,rendered),
                 '_compare_images':lambda:image_metrics(rendered), '_image_metrics':image_comparison}
    report = {'evaluation_claim':'Conservation at the delivered image resolution under declared numeric bounds; evaluated bit equality is NOT claimed.',
              'pixel_displacement_limit':MAX_PIXEL_ERROR, 'normal_angle_limit_deg':MAX_NORMAL_ANGLE_DEG,
              'exact_input_invariants':['Base cage surviving coordinates','All surviving shape coordinates/settings/drivers','Vertex weights','Face layout and material IDs','World and camera matrices'],
              'subdivision_viewport_and_render_levels':1,
              'inferred_cause':'Small Subsurf reevaluation differences after Mesh/BMesh rebuilding, strongest near the high-valence lower cap. Inference; no separate causal attribution is claimed.',
              'visual_review_status':'Pending review of controlled before/after PNGs before caller saves.',
              'saved':False, 'numeric_guard_passed':False}
    try:
        exec(compile(source,str(file)+'_visual_metric_adaptation','exec'),namespace)
        cleanup = namespace['clean_face_topology_uv'](report_path=None,keep_legacy_uv=False)
        assert len(metrics) == 33 and sum(len(item['views']) for item in metrics) == 41
        report['cleanup'] = cleanup
        report['numeric_guard_passed'] = True
    except Exception:
        report['error'] = traceback.format_exc()
        # The reused transaction rolls back its own errors. Cover any later
        # wrapper/count failure as well, instead of leaving an unsaved candidate.
        if obj.data != original:
            working = obj.data
            obj.data = original
            original.use_fake_user = original_fake_user
            for name,value in original_properties.items():
                if value is None:
                    if name in obj:del obj[name]
                else:obj[name] = value
            if working.users == 0:bpy.data.meshes.remove(working)
            bpy.context.view_layer.update()
    report['metrics_at_unique_frames'] = metrics
    report['neutral_renders'] = rendered
    report['neutral_image_comparison'] = image_comparison
    report['candidate_in_memory'] = obj.data != original
    report['original_restored_on_failure'] = obj.data == original if not report['numeric_guard_passed'] else None
    report['delivered_pngs_replaced'] = False
    output = pathlib.Path(report_path or ROOT/'work/face_cleanup_visual_conservation_v02.json')
    output.parent.mkdir(parents=True,exist_ok=True)
    output.write_text(json.dumps(report,indent=2,ensure_ascii=False),encoding='utf-8')
    print('FACE_VISUAL_CONSERVATION_REPORT_READY', str(output), report['numeric_guard_passed'], flush=True)
    return report


if __name__ == '__main__':
    main()
