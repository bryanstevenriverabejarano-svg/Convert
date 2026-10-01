"""Transactional cleanup of unused face vertices and a fresh, untextured UV map.

Prepared helper; no save or render. Call clean_face_topology_uv() in a loaded copy.
Every assertion failure restores the original mesh and the user's scene context.
Success retains the original mesh as a fake-user backup. This is housekeeping;
it does not weld the orbital/oral pieces or certify production retopology.
"""
import array
import hashlib
import json
import math
import pathlib
import struct

import bpy
import bmesh

ROOT = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')


def coordinates(block):
    values = array.array('f', [0.0]) * (len(block) * 3)
    block.foreach_get('co', values)
    return values


def driver_state(key):
    result = []
    if key and key.animation_data:
        for fc in key.animation_data.drivers:
            driver = fc.driver
            variables = []
            for variable in driver.variables:
                targets = [(getattr(t.id, 'name_full', None), t.data_path,
                            t.bone_target, t.transform_type, t.transform_space)
                           for t in variable.targets]
                variables.append((variable.name, variable.type, targets))
            result.append((fc.data_path, fc.array_index, fc.mute,
                           driver.type, driver.expression, variables))
    return result


def key_state(mesh):
    key = mesh.shape_keys
    if not key:
        return None
    return {
        'use_relative': key.use_relative,
        'drivers': driver_state(key),
        'blocks': [{
            'name': block.name,
            'value': block.value,
            'relative_key': block.relative_key.name,
            'vertex_group': block.vertex_group,
            'slider_min': block.slider_min,
            'slider_max': block.slider_max,
            'interpolation': block.interpolation,
            'mute': block.mute,
            'coordinates': coordinates(block.data)
        } for block in key.key_blocks]
    }


def weights(obj):
    return [tuple((g.group, g.weight) for g in vertex.groups)
            for vertex in obj.data.vertices]


def other_key_fingerprint(scene, excluded):
    # Streaming hashes keep memory bounded, including pose and hair correctives.
    result = {}
    for obj in scene.objects:
        if obj == excluded or obj.type != 'MESH' or not obj.data.shape_keys:
            continue
        state = key_state(obj.data)
        for block in state['blocks']:
            block['coordinates'] = hashlib.sha256(
                block['coordinates'].tobytes()).hexdigest()
        result[obj.name] = (obj.data.as_pointer(), state,
                           tuple((g.name, g.index, g.lock_weight)
                                 for g in obj.vertex_groups))
    return result


def constant_material_preflight(obj):
    names = []
    for slot in obj.material_slots:
        mat = slot.material
        assert mat and mat.use_nodes, 'Face material is absent or not node based'
        for owner in [mat, mat.node_tree]:
            animation = owner.animation_data
            assert not animation or (not animation.drivers and animation.action is None
                                     and not animation.nla_tracks), \
                'Animated material inputs require a separate UV-dependence review'
        nodes = list(mat.node_tree.nodes)
        assert len(nodes) == 2, 'Material graph changed; UV independence unproven'
        assert sorted(n.type for n in nodes) == ['BSDF_PRINCIPLED', 'OUTPUT_MATERIAL'], \
            'UV/material dependence must be reviewed manually'
        shader = next(n for n in nodes if n.type == 'BSDF_PRINCIPLED')
        output = next(n for n in nodes if n.type == 'OUTPUT_MATERIAL')
        assert not any(socket.is_linked for socket in shader.inputs), \
            'Face Principled has a linked input'
        assert not output.inputs['Displacement'].is_linked, \
            'Material displacement could depend on UV'
        assert output.inputs['Surface'].is_linked and \
            output.inputs['Surface'].links[0].from_node == shader
        for socket in shader.inputs:
            if 'Anisotropic' in socket.name and 'Rotation' not in socket.name:
                assert float(socket.default_value) == 0, \
                    'Anisotropic shading could depend on the active UV tangent'
        names.append(mat.name)
    assert names, 'Face needs a material'
    return names


def frame_list():
    frames = set()
    for filename, expected in [('vistas_manifest.json', 20),
                               ('expresiones_controles.json', 21)]:
        rows = json.loads((ROOT / 'outputs' / filename).read_text(encoding='utf-8'))
        assert len(rows) == expected
        frames.update(int(row['frame']) for row in rows)
    return sorted(frames)


def surface_digest(obj, scene, frame):
    scene.frame_set(frame)
    bpy.context.view_layer.update()
    evaluated = obj.evaluated_get(bpy.context.evaluated_depsgraph_get())
    mesh = evaluated.to_mesh()
    try:
        digest = hashlib.sha256()
        digest.update(struct.pack('<16d', *(v for row in evaluated.matrix_world for v in row)))
        digest.update(struct.pack('<I', len(mesh.polygons)))
        assert hasattr(mesh, 'corner_normals'), 'Evaluated corner normals unavailable'
        corner_normals = mesh.corner_normals
        assert len(corner_normals) == len(mesh.loops)
        # Loose evaluated vertices are intentionally ignored. Ordered polygon
        # corners, their normals, face normals, smooth flags and material IDs
        # are the visible surface, irrespective of a vertex-index remapping.
        for face in mesh.polygons:
            digest.update(struct.pack('<II?3f', len(face.vertices),
                                      face.material_index, face.use_smooth,
                                      *face.normal))
            for loop_index in face.loop_indices:
                vertex = mesh.vertices[mesh.loops[loop_index].vertex_index]
                normal = corner_normals[loop_index].vector
                digest.update(struct.pack('<6f', *vertex.co, *normal))
        return digest.hexdigest()
    finally:
        evaluated.to_mesh_clear()


def uv_check(mesh, layer):
    outside = sum(not all(0 <= value <= 1 and math.isfinite(value)
                          for value in item.uv) for item in layer.data)
    zero_area = []
    for face in mesh.polygons:
        points = [layer.data[index].uv for index in face.loop_indices]
        area = abs(sum(a.x * b.y - b.x * a.y
                       for a, b in zip(points, points[1:] + points[:1]))) * .5
        if area <= 1e-14:
            zero_area.append(face.index)
    return {'outside_unit_tile': outside, 'zero_area_faces': zero_area}


def clean_face_topology_uv(report_path=None, keep_legacy_uv=False):
    expected_source = (ROOT / 'outputs' / 'Salve_refinado_v02.blend').resolve()
    assert pathlib.Path(bpy.data.filepath).resolve() == expected_source, \
        'Load the final outputs model before running this helper'
    source_sha = hashlib.sha256(expected_source.read_bytes()).hexdigest()
    obj = bpy.data.objects['Rostro_topologia']
    scene = bpy.context.scene
    assert obj.mode == 'OBJECT', 'Run from Object mode'
    assert bpy.context.object is None or bpy.context.object.mode == 'OBJECT'
    original = obj.data
    assert original.users == 1, 'Shared face datablock requires manual review'
    assert not getattr(original, 'has_custom_normals', False), \
        'Custom split normals need a separate remapping proof'
    assert not any(edge.use_edge_sharp for edge in original.edges)
    assert all(face.use_smooth for face in original.polygons)
    assert [modifier.type for modifier in obj.modifiers] == ['SUBSURF', 'ARMATURE'], \
        'Unexpected modifier; deletion equivalence must be reviewed'
    materials = constant_material_preflight(obj)
    used_by_faces = {index for face in original.polygons for index in face.vertices}
    used_by_edges = {index for edge in original.edges for index in edge.vertices}
    loose = [vertex.index for vertex in original.vertices
             if vertex.index not in used_by_faces and vertex.index not in used_by_edges]
    assert len(loose) == 256, 'Source changed: expected the audited 256 isolated vertices'
    loose_set = set(loose)
    surviving = [index for index in range(len(original.vertices)) if index not in loose_set]
    remap = {old: new for new, old in enumerate(surviving)}
    before_vertices = coordinates(original.vertices)
    before_weights = weights(obj)
    before_keys = key_state(original)
    assert before_keys and [block['name'] for block in before_keys['blocks']] == \
        ['Basis', 'jawOpen', 'smile']
    before_faces = [(tuple(remap[index] for index in face.vertices),
                     face.material_index, face.use_smooth) for face in original.polygons]
    before_edges = {tuple(sorted(remap[index] for index in edge.vertices))
                    for edge in original.edges}
    current_frame = scene.frame_current
    old_selection = list(bpy.context.selected_objects)
    old_active = bpy.context.view_layer.objects.active
    old_select_mode = tuple(scene.tool_settings.mesh_select_mode)
    old_shape_index = obj.active_shape_key_index
    old_only_key = obj.show_only_shape_key
    original_fake_user = original.use_fake_user
    original_cleanup_properties = {name: obj.get(name) for name in
                                   ['cleanup_backup_mesh', 'cleanup_scope']}
    frames = frame_list()
    try:
        before_surfaces = {frame: surface_digest(obj, scene, frame) for frame in frames}
    finally:
        scene.frame_set(current_frame)
        bpy.context.view_layer.update()
    other_keys = other_key_fingerprint(scene, obj)
    working = original.copy()
    committed = False
    try:
        obj.data = working
        assert key_state(working) == before_keys, 'Mesh copy did not preserve the Key datablock'
        for selected in bpy.context.selected_objects:
            selected.select_set(False)
        obj.select_set(True)
        bpy.context.view_layer.objects.active = obj
        obj.active_shape_key_index = 0
        obj.show_only_shape_key = False
        scene.tool_settings.mesh_select_mode = (True, False, False)
        bpy.ops.object.mode_set(mode='EDIT')
        edit_mesh = bmesh.from_edit_mesh(working)
        edit_mesh.verts.ensure_lookup_table()
        for face in edit_mesh.faces:
            face.select_set(False)
        for edge in edit_mesh.edges:
            edge.select_set(False)
        for vertex in edit_mesh.verts:
            vertex.select_set(vertex.index in loose_set)
        bmesh.update_edit_mesh(working, loop_triangles=False, destructive=False)
        bpy.ops.mesh.delete(type='VERT')
        bpy.ops.object.mode_set(mode='OBJECT')
        assert len(working.vertices) == len(surviving)
        assert len(working.polygons) == len(before_faces)
        assert [(tuple(face.vertices), face.material_index, face.use_smooth)
                for face in working.polygons] == before_faces
        assert {tuple(sorted(edge.vertices)) for edge in working.edges} == before_edges
        after_vertices = coordinates(working.vertices)
        for new_index, old_index in enumerate(surviving):
            assert after_vertices[new_index*3:new_index*3+3] == \
                before_vertices[old_index*3:old_index*3+3], 'Basis coordinate changed'
        assert weights(obj) == [before_weights[index] for index in surviving]
        after_keys = key_state(working)
        assert after_keys['use_relative'] == before_keys['use_relative']
        assert after_keys['drivers'] == before_keys['drivers']
        for before, after in zip(before_keys['blocks'], after_keys['blocks']):
            for key in before:
                if key != 'coordinates':
                    assert before[key] == after[key], 'Shape property changed: ' + key
            for new_index, old_index in enumerate(surviving):
                assert after['coordinates'][new_index*3:new_index*3+3] == \
                    before['coordinates'][old_index*3:old_index*3+3], \
                    'Shape coordinate changed: ' + before['name']

        revised = working.uv_layers.new(name='UV_Rostro_revisada')
        revised_name = revised.name
        working.uv_layers.active = revised
        revised.active_render = True
        bpy.ops.object.mode_set(mode='EDIT')
        bpy.ops.mesh.select_all(action='SELECT')
        bpy.ops.uv.smart_project(angle_limit=math.radians(66), island_margin=.015)
        bpy.ops.object.mode_set(mode='OBJECT')
        revised = working.uv_layers[revised_name]
        revised_check = uv_check(working, revised)
        assert revised_check['outside_unit_tile'] == 0, revised_check
        assert not revised_check['zero_area_faces'], revised_check
        if not keep_legacy_uv:
            # The unchanged original datablock retains its original UV layer.
            # Remove only the obsolete layer on the working copy so the new
            # editable head has no dormant, invalid UV map reported as final.
            old_layer_names = [layer.name for layer in working.uv_layers
                               if layer.name != revised_name]
            for name in old_layer_names:
                working.uv_layers.remove(working.uv_layers[name])
        revised = working.uv_layers[revised_name]
        working.uv_layers.active = revised
        for layer in working.uv_layers:
            layer.active_render = layer == revised
        for frame in frames:
            assert surface_digest(obj, scene, frame) == before_surfaces[frame], \
                'Visible surface/normal changed at frame ' + str(frame)
        scene.frame_set(current_frame)
        bpy.context.view_layer.update()
        assert other_key_fingerprint(scene, obj) == other_keys, \
            'A shape key or its control changed on another object'
        assert constant_material_preflight(obj) == materials
        assert hashlib.sha256(expected_source.read_bytes()).hexdigest() == source_sha, \
            'The source .blend changed on disk during this transaction'
        original.use_fake_user = True
        working.name = 'Rostro_topologia_v02_limpia'
        obj['cleanup_backup_mesh'] = original.name
        obj['cleanup_scope'] = '256 isolated vertices removed; constant skin material; new draft UV only. No production retopology certification.'
        report = {
            'component': 'face_topology_uv_housekeeping',
            'input_blend': str(expected_source), 'input_file_sha256': source_sha,
            'saved_model': False, 'rendered_images_changed': False,
            'original_backup_mesh': original.name,
            'new_mesh': working.name,
            'removed_isolated_vertices': len(loose),
            'surviving_vertices': len(surviving),
            'faces_preserved': len(before_faces),
            'shape_keys_preserved': [block['name'] for block in before_keys['blocks']],
            'other_shape_objects_preserved': len(other_keys),
            'weights_and_drivers_preserved': True,
            'rendered_image_count_covered': 41,
            'unique_frame_count_checked': len(frames),
            'surface_and_normal_bit_hashes_equal_at_unique_frames': frames,
            'uv_layer': revised_name, 'uv_check': revised_check,
            'material_uv_independence': materials,
            'legacy_uv_retained_on_working_mesh': keep_legacy_uv,
            'limits': ['UV Smart Project is a draft unwrap, not a manual seam, overlap or texel-density certification.',
                       'Separate facial components remain; production retopology and artistic fidelity remain pending.']
        }
        if report_path:
            path = pathlib.Path(report_path)
            path.parent.mkdir(parents=True, exist_ok=True)
            path.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
        committed = True
        print('FACE_HOUSEKEEPING_SURFACES_PRESERVED', json.dumps(report), flush=True)
        return report
    finally:
        if obj.mode != 'OBJECT':
            bpy.ops.object.mode_set(mode='OBJECT')
        if not committed:
            obj.data = original
            original.use_fake_user = original_fake_user
            for name, old_value in original_cleanup_properties.items():
                if old_value is None:
                    if name in obj:
                        del obj[name]
                else:
                    obj[name] = old_value
            if working.users == 0:
                bpy.data.meshes.remove(working)
        obj.active_shape_key_index = old_shape_index
        obj.show_only_shape_key = old_only_key
        scene.tool_settings.mesh_select_mode = old_select_mode
        scene.frame_set(current_frame)
        for selected in bpy.context.selected_objects:
            selected.select_set(False)
        for selected in old_selection:
            selected.select_set(True)
        bpy.context.view_layer.objects.active = old_active
        bpy.context.view_layer.update()


def main(cleanup=True, save=False, report_path=None):
    assert cleanup is True, 'This prepared entry point is cleanup only'
    assert save is False, 'Caller must review PASS before saving'
    return clean_face_topology_uv(report_path=report_path, keep_legacy_uv=False)


if __name__ == '__main__':
    main()
