"""Frame-1 diagnostic of the frozen face cleanup; always rolls back, never saves.

Call main() in the GUI with outputs/Salve_refinado_v02.blend loaded. Uses the
unchanged cleanup helper through the UV step, captures evaluated surfaces before
and after, then deliberately raises inside its transaction so it cannot commit.
All equality tests remain exact. Nearest-point distances are diagnostic only.
"""
import array
import gzip
import hashlib
import json
import math
import pathlib
import struct
import traceback

import bpy
from mathutils import Vector
from mathutils.kdtree import KDTree

ROOT = pathlib.Path('C:/Users/agred/Documents/Codex/2026-09-30/new-chat-2')


class DiagnosticRollback(Exception):
    pass


def capture(obj, scene):
    scene.frame_set(1)
    bpy.context.view_layer.update()
    evaluated = obj.evaluated_get(bpy.context.evaluated_depsgraph_get())
    mesh = evaluated.to_mesh()
    try:
        vertices = array.array('f', [0]) * (3 * len(mesh.vertices))
        mesh.vertices.foreach_get('co', vertices)
        indices = array.array('I', [0]) * len(mesh.loops)
        mesh.loops.foreach_get('vertex_index', indices)
        positions = array.array('f', [0]) * (3 * len(mesh.loops))
        for index, vertex_index in enumerate(indices):
            positions[index*3:index*3+3] = vertices[vertex_index*3:vertex_index*3+3]
        normals = array.array('f', [0]) * (3 * len(mesh.loops))
        mesh.corner_normals.foreach_get('vector', normals)
        face_normals = array.array('f', [0]) * (3 * len(mesh.polygons))
        mesh.polygons.foreach_get('normal', face_normals)
        return {
            'matrix': [value for row in evaluated.matrix_world for value in row],
            'vertices': len(mesh.vertices), 'loops': len(mesh.loops),
            'face_count': len(mesh.polygons),
            'positions': positions, 'normals': normals,
            'face_normals': face_normals, 'loop_vertex_indices': indices,
            'faces': [(face.loop_start, face.loop_total, face.material_index,
                       face.use_smooth) for face in mesh.polygons],
            'uv_layers': [layer.name for layer in obj.data.uv_layers],
            'key_values': {key.name: key.value for key in obj.data.shape_keys.key_blocks},
            'mesh_name': obj.data.name
        }
    finally:
        evaluated.to_mesh_clear()


def binary_archive(snapshot, name):
    path = ROOT / 'work' / (name + '.bin.gz')
    header = {key: value for key, value in snapshot.items()
              if key not in ['positions', 'normals', 'face_normals', 'loop_vertex_indices']}
    header['array_order'] = [
        ['positions', 'float32', len(snapshot['positions'])],
        ['normals', 'float32', len(snapshot['normals'])],
        ['face_normals', 'float32', len(snapshot['face_normals'])],
        ['loop_vertex_indices', 'uint32', len(snapshot['loop_vertex_indices'])]
    ]
    header['endianness'] = 'little; Windows host'
    encoded = json.dumps(header, separators=(',', ':')).encode('utf-8')
    with gzip.open(path, 'wb') as file:
        file.write(struct.pack('<I', len(encoded)))
        file.write(encoded)
        for field, _, _ in header['array_order']:
            file.write(snapshot[field].tobytes())
    return {'file': str(path), 'sha256': hashlib.sha256(path.read_bytes()).hexdigest(),
            'format': 'gzip: uint32 JSON-header byte length, UTF-8 JSON header, then arrays in header order'}


def canonical_faces(snapshot):
    records = []
    positions, normals = snapshot['positions'], snapshot['normals']
    for face_index, (start, count, material, smooth) in enumerate(snapshot['faces']):
        points = [tuple(positions[(start+i)*3:(start+i)*3+3]) for i in range(count)]
        vectors = [tuple(normals[(start+i)*3:(start+i)*3+3]) for i in range(count)]
        rotation = min(range(count), key=lambda k: tuple(points[k:] + points[:k]))
        geometry = tuple(points[rotation:] + points[:rotation])
        shading = tuple(vectors[rotation:] + vectors[:rotation])
        records.append((geometry, material, smooth, shading,
                        tuple(snapshot['face_normals'][face_index*3:face_index*3+3])))
    records.sort(key=lambda record: record[:3])
    geometry_hash = hashlib.sha256()
    shaded_hash = hashlib.sha256()
    for geometry, material, smooth, shading, face_normal in records:
        prefix = struct.pack('<II?', len(geometry), material, smooth)
        geometry_hash.update(prefix)
        shaded_hash.update(prefix)
        for point in geometry:
            encoded = struct.pack('<3f', *point)
            geometry_hash.update(encoded)
            shaded_hash.update(encoded)
        for vector in shading:
            shaded_hash.update(struct.pack('<3f', *vector))
        shaded_hash.update(struct.pack('<3f', *face_normal))
    return records, geometry_hash.hexdigest(), shaded_hash.hexdigest()


def deltas(before, after, vectors=False):
    assert len(before) == len(after)
    count_nonzero = 0
    max_component = 0.0
    max_distance = 0.0
    max_angle = 0.0
    worst = []
    for index in range(0, len(before), 3):
        a = tuple(before[index:index+3])
        b = tuple(after[index:index+3])
        diff = tuple(y-x for x, y in zip(a, b))
        component = max(abs(value) for value in diff)
        distance = math.sqrt(sum(value*value for value in diff))
        if component:
            count_nonzero += 1
        max_component = max(max_component, component)
        max_distance = max(max_distance, distance)
        if vectors and component:
            length_a = math.sqrt(sum(value*value for value in a))
            length_b = math.sqrt(sum(value*value for value in b))
            if length_a and length_b:
                angle = math.degrees(math.acos(max(-1, min(1,
                    sum(x*y for x, y in zip(a, b))/(length_a*length_b)))))
                max_angle = max(max_angle, angle)
        if component and (len(worst) < 12 or distance > worst[-1]['distance']):
            worst.append({'corner_or_face_index': index//3, 'before': a, 'after': b,
                          'delta': diff, 'distance': distance})
            worst.sort(key=lambda item: item['distance'], reverse=True)
            del worst[12:]
    return {'different_vector_count_exact': count_nonzero,
            'max_abs_component': max_component, 'max_euclidean_distance': max_distance,
            'max_angle_degrees': max_angle if vectors else None, 'worst_examples': worst}


def nearest_points(before, after):
    points_before = sorted(set(tuple(before[index:index+3])
                               for index in range(0, len(before), 3)))
    points_after = sorted(set(tuple(after[index:index+3])
                              for index in range(0, len(after), 3)))
    points_after_set = set(points_after)
    tree = KDTree(len(points_after))
    for index, point in enumerate(points_after):
        tree.insert(Vector(point), index)
    tree.balance()
    maximum = 0.0
    changed = sum(point not in points_after_set for point in points_before)
    example = None
    for point in points_before:
        _, index, distance = tree.find(Vector(point))
        if distance > maximum:
            maximum = distance
            example = {'before': point, 'nearest_after': points_after[index],
                       'distance_m': distance}
    return {'unique_before': len(points_before), 'unique_after': len(points_after),
            'points_without_exact_nearest_match': changed,
            'max_nearest_distance_m': maximum, 'worst_example': example}


def comparisons(before, after):
    records_before, geometry_before, shaded_before = canonical_faces(before)
    records_after, geometry_after, shaded_after = canonical_faces(after)
    report = {
        'counts_before': {key: before[key] for key in ['vertices', 'loops', 'face_count']},
        'counts_after': {key: after[key] for key in ['vertices', 'loops', 'face_count']},
        'matrix_world_equal_exact': before['matrix'] == after['matrix'],
        'matrix_world_float64_bytes_equal': struct.pack('<16d', *before['matrix']) == struct.pack('<16d', *after['matrix']),
        'matrix_world_max_delta': max(abs(a-b) for a, b in zip(before['matrix'], after['matrix'])),
        'positions_ordered_equal_exact': before['positions'] == after['positions'],
        'corner_normals_ordered_equal_exact': before['normals'] == after['normals'],
        'face_normals_ordered_equal_exact': before['face_normals'] == after['face_normals'],
        'ordered_position_float32_bytes_equal': before['positions'].tobytes() == after['positions'].tobytes(),
        'ordered_corner_normal_float32_bytes_equal': before['normals'].tobytes() == after['normals'].tobytes(),
        'ordered_face_normal_float32_bytes_equal': before['face_normals'].tobytes() == after['face_normals'].tobytes(),
        'ordered_position_hash_before': hashlib.sha256(before['positions'].tobytes()).hexdigest(),
        'ordered_position_hash_after': hashlib.sha256(after['positions'].tobytes()).hexdigest(),
        'ordered_corner_normal_hash_before': hashlib.sha256(before['normals'].tobytes()).hexdigest(),
        'ordered_corner_normal_hash_after': hashlib.sha256(after['normals'].tobytes()).hexdigest(),
        'canonical_position_hash_before': geometry_before,
        'canonical_position_hash_after': geometry_after,
        'canonical_positions_equal_exact': geometry_before == geometry_after,
        'canonical_shaded_hash_before': shaded_before,
        'canonical_shaded_hash_after': shaded_after,
        'canonical_shading_equal_exact': shaded_before == shaded_after,
        'key_values_before': before['key_values'], 'key_values_after': after['key_values'],
        'face_layout_equal_exact': before['faces'] == after['faces'],
        'limits': ['No tolerance is accepted or changed by this diagnostic.',
                   'KD distances identify possible order versus numeric changes; they do not authorize cleanup.',
                   'This diagnostic covers frame 1 only, not all 41 images.']
    }
    if len(before['positions']) == len(after['positions']):
        report['position_deltas_in_current_corner_order_m'] = deltas(before['positions'], after['positions'])
        report['normal_deltas_in_current_corner_order'] = deltas(before['normals'], after['normals'], True)
        report['face_normal_deltas_in_current_face_order'] = deltas(before['face_normals'], after['face_normals'], True)
    report['nearest_points_before_to_after'] = nearest_points(before['positions'], after['positions'])
    report['nearest_points_after_to_before'] = nearest_points(after['positions'], before['positions'])
    if geometry_before == geometry_after:
        a = array.array('f', (value for record in records_before for normal in record[3] for value in normal))
        b = array.array('f', (value for record in records_after for normal in record[3] for value in normal))
        report['normal_deltas_after_exact_face_canonicalization'] = deltas(a, b, True)
    return report


def main(report_path=None, capture_phases=False):
    output = pathlib.Path(report_path or ROOT/'work/face_cleanup_diagnostic_v02.json')
    obj = bpy.data.objects['Rostro_topologia']
    original = obj.data
    subdivision = next(modifier for modifier in obj.modifiers if modifier.type == 'SUBSURF')
    assert subdivision.levels == subdivision.render_levels == 1, \
        'Viewport/render subdivision must match this delivery at level 1'
    initial_frame = bpy.context.scene.frame_current
    input_file = pathlib.Path(bpy.data.filepath)
    input_sha = hashlib.sha256(input_file.read_bytes()).hexdigest()
    source = ROOT/'work/face_topology_uv_safe_cleanup_v02.py'
    namespace = {'__name__': 'salve_cleanup_diagnostic_reuse', '__file__': str(source)}
    exec(compile(source.read_text(encoding='utf-8-sig'), str(source), 'exec'), namespace)
    digest = namespace['surface_digest']
    snapshots = []
    phases = {}
    report = {'scope': 'frame_1_exact_face_cleanup_diagnostic', 'saved': False,
              'input_file': str(input_file), 'input_sha256_before': input_sha,
              'cleanup_source_sha256': hashlib.sha256(source.read_bytes()).hexdigest()}
    report['subdivision_viewport_and_render_levels'] = 1
    if capture_phases:
        original_key_state = namespace['key_state']
        original_weights = namespace['weights']

        def key_state_with_copy_capture(mesh):
            result = original_key_state(mesh)
            if mesh == obj.data and mesh != original and \
                    len(mesh.vertices) == len(original.vertices) and 'after_copy' not in phases:
                phases['after_copy'] = capture(obj, bpy.context.scene)
                print('FACE_PHASE_AFTER_COPY_CAPTURED', flush=True)
            return result

        def weights_with_delete_capture(target):
            result = original_weights(target)
            if target == obj and target.data != original and \
                    len(target.data.vertices) == len(original.vertices)-256 and 'after_delete' not in phases:
                phases['after_delete'] = capture(obj, bpy.context.scene)
                print('FACE_PHASE_AFTER_DELETE_BEFORE_UV_CAPTURED', flush=True)
            return result

        namespace['key_state'] = key_state_with_copy_capture
        namespace['weights'] = weights_with_delete_capture

    def diagnostic_digest(target, scene, frame):
        assert frame == 1
        actual_digest = digest(target, scene, frame)
        snapshots.append(capture(target, scene))
        if len(snapshots) == 1:
            report['ordered_surface_digest_before'] = actual_digest
            print('FACE_DIAGNOSTIC_BASELINE_CAPTURED', flush=True)
            return actual_digest
        report['ordered_surface_digest_after'] = actual_digest
        print('FACE_DIAGNOSTIC_AFTER_CAPTURED_ALWAYS_ROLLING_BACK', flush=True)
        raise DiagnosticRollback('Diagnostic completed; cleanup intentionally uncommitted')

    namespace['frame_list'] = lambda: [1]
    namespace['surface_digest'] = diagnostic_digest
    try:
        namespace['clean_face_topology_uv'](report_path=None, keep_legacy_uv=False)
        raise AssertionError('Diagnostic hook failed to force rollback')
    except DiagnosticRollback as error:
        report['transaction_stop'] = str(error)
    except Exception:
        report['error'] = traceback.format_exc()
    finally:
        report['original_mesh_restored'] = obj.data == original
        report['frame_restored'] = bpy.context.scene.frame_current == initial_frame
        report['input_sha256_after'] = hashlib.sha256(input_file.read_bytes()).hexdigest()
        report['input_file_unchanged'] = report['input_sha256_after'] == input_sha
    if len(snapshots) == 2:
        report['comparison'] = comparisons(*snapshots)
        report['capture_archives'] = [binary_archive(snapshot, 'face_cleanup_frame1_' + label)
                                      for snapshot, label in zip(snapshots, ['before', 'after'])]
        if capture_phases and 'after_copy' in phases and 'after_delete' in phases:
            report['phase_comparisons'] = {
                'original_to_copy_before_bmesh': comparisons(snapshots[0], phases['after_copy']),
                'copy_to_delete_before_uv': comparisons(phases['after_copy'], phases['after_delete']),
                'delete_to_uv': comparisons(phases['after_delete'], snapshots[1])
            }
            report['capture_archives'].extend(binary_archive(snapshot, 'face_cleanup_frame1_' + label)
                                              for label, snapshot in phases.items())
    else:
        report['capture_count'] = len(snapshots)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(report, indent=2, ensure_ascii=False), encoding='utf-8')
    assert report['original_mesh_restored'] and report['frame_restored'] and report['input_file_unchanged'], \
        'Diagnostic rollback or disk invariants failed; see ' + str(output)
    print('FACE_CLEANUP_DIAGNOSTIC_ROLLBACK_COMPLETE', str(output), flush=True)
    return report


if __name__ == '__main__':
    main()
