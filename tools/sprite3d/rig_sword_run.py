"""Local Rainoray rig and 12-pose sword run. Blender 4.5; no services.

blender -b --python tools/sprite3d/rig_sword_run.py -- source.glb output_dir
Coordinates and region cuts are specific to the supplied static Rainoray mesh.
The source is never overwritten. Saves editable .blend, animated .glb and checks.
"""
import json
import math
from pathlib import Path
import sys

import bpy
from mathutils import Matrix, Quaternion, Vector


def main():
    source, output = sys.argv[sys.argv.index('--') + 1:]
    out = Path(output).resolve()
    out.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=str(Path(source).resolve()))
    body = next(o for o in bpy.context.scene.objects if o.type == 'MESH')
    body.name = 'Rainoray_Body'
    bpy.context.view_layer.objects.active = body
    bpy.ops.object.select_all(action='DESELECT')
    body.select_set(True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    # Weld UV-seam duplicates before heat weighting; UV loops remain intact.
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.select_all(action='SELECT')
    bpy.ops.mesh.remove_doubles(threshold=0.0001)
    bpy.ops.mesh.select_all(action='DESELECT')
    bpy.ops.object.mode_set(mode='OBJECT')
    for v in body.data.vertices:
        x, y, z = v.co
        v.select = x < -0.475 or (x < -0.35 and z < -0.30)
    bpy.ops.object.mode_set(mode='EDIT')
    bpy.ops.mesh.separate(type='SELECTED')
    bpy.ops.object.mode_set(mode='OBJECT')
    sword = next(o for o in bpy.context.scene.objects if o.type == 'MESH' and o != body)
    sword.name = 'Rainoray_Sword'
    assert len(sword.data.vertices) > 100, 'Sword cut failed'
    offset = Vector((0.20, 0.20, -0.950325))
    for o in (body, sword):
        for v in o.data.vertices:
            v.co -= offset

    # Replace only near-black texture faces in the back-hair interior region.
    tex = next(n.image for n in body.data.materials[0].node_tree.nodes if n.type == 'TEX_IMAGE')
    pixels = list(tex.pixels)
    hair = bpy.data.materials.new('Back_hair_interior_repair')
    hair.diffuse_color = (0.48, 0.66, 0.80, 1)
    hair.use_nodes = True
    hair.node_tree.nodes.get('Principled BSDF').inputs['Base Color'].default_value = hair.diffuse_color
    body.data.materials.append(hair)
    repaired = 0
    for face in body.data.polygons:
        centre = sum((body.data.vertices[i].co for i in face.vertices), Vector()) / len(face.vertices)
        uv = sum((body.data.uv_layers.active.data[i].uv for i in face.loop_indices), Vector((0, 0))) / len(face.loop_indices)
        px = min(tex.size[0]-1, max(0, int(uv.x*tex.size[0])))
        py = min(tex.size[1]-1, max(0, int(uv.y*tex.size[1])))
        rgb = pixels[4*(py*tex.size[0]+px):4*(py*tex.size[0]+px)+3]
        if centre.y > 0.09 and 0.88 < centre.z < 1.40 and max(rgb) < 0.11:
            face.material_index = 1
            repaired += 1

    arm_data = bpy.data.armatures.new('Rainoray_Skeleton')
    arm = bpy.data.objects.new('Rainoray_Rig', arm_data)
    bpy.context.collection.objects.link(arm)
    bpy.ops.object.select_all(action='DESELECT')
    arm.select_set(True)
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode='EDIT')
    def bone(name, head, tail, parent=None):
        b = arm_data.edit_bones.new(name)
        b.head, b.tail = head, tail
        if parent:
            b.parent = arm_data.edit_bones[parent]
        return b
    bone('pelvis', (0, 0, .53), (0, 0, .76))
    bone('spine', (0, 0, .76), (0, 0, 1.12), 'pelvis')
    bone('head', (0, 0, 1.12), (0, 0, 1.70), 'spine')
    for side, sign in [('R', -1), ('L', 1)]:
        bone('upper_arm.'+side, (sign*.33, 0, 1.11), (sign*.49, -.08, .91), 'spine')
        bone('forearm.'+side, (sign*.49, -.08, .91), (sign*.60, -.16, .74), 'upper_arm.'+side)
        bone('hand.'+side, (sign*.60, -.16, .74), (sign*.63, -.18, .67), 'forearm.'+side)
        bone('thigh.'+side, (sign*.16, -.04, .53), (sign*.16, -.04, .27), 'pelvis')
        bone('shin.'+side, (sign*.16, -.04, .27), (sign*.16, -.04, .10), 'thigh.'+side)
        bone('foot.'+side, (sign*.16, -.04, .10), (sign*.16, -.18, .055), 'shin.'+side)
    bpy.ops.object.mode_set(mode='OBJECT')
    body.select_set(True)
    bpy.ops.object.parent_set(type='ARMATURE_AUTO')
    assert any(m.type == 'ARMATURE' for m in body.modifiers)
    assert all(v.groups for v in body.data.vertices), 'Automatic weights left unweighted vertices'
    # Large stylized head and rear hair must not stretch with shoulder motion.
    for v in body.data.vertices:
        if v.co.z > 1.27 or (v.co.y > .10 and v.co.z > .94):
            for g in body.vertex_groups:
                g.remove([v.index])
            body.vertex_groups['head'].add([v.index], 1, 'REPLACE')

    # Keep boot soles rigid; heat weights otherwise smear them into the shins.
    for v in body.data.vertices:
        if v.co.z < .16:
            for g in body.vertex_groups:
                g.remove([v.index])
            body.vertex_groups['foot.' + ('R' if v.co.x < 0 else 'L')].add([v.index], 1, 'REPLACE')

    bpy.context.view_layer.objects.active = body
    bpy.ops.object.vertex_group_limit_total(limit=4)
    bpy.ops.object.vertex_group_normalize_all(lock_active=False)
    bpy.context.view_layer.objects.active = arm

    bpy.context.view_layer.update()
    sword.parent = arm
    sword.parent_type = 'BONE'
    sword.parent_bone = 'hand.R'
    # Bone parenting uses the bone tail; inverse preserves the original grip.
    sword.matrix_parent_inverse = (arm.matrix_world @ arm.pose.bones['hand.R'].matrix @ Matrix.Translation((0, arm.data.bones['hand.R'].length, 0))).inverted()
    sword.matrix_basis = Matrix.Identity(4)
    assert not sword.modifiers, 'Sword must remain rigid'

    scene = bpy.context.scene
    scene.render.fps = 20
    scene.frame_start, scene.frame_end = 1, 13
    rest = {b.name: b.matrix_local.copy() for b in arm.data.bones}
    lengths = {b.name: b.length for b in arm.data.bones}
    samples = []
    def pose(name, head, direction):
        pb = arm.pose.bones[name]
        pb.rotation_mode = 'QUATERNION'
        q = rest[name].to_quaternion()
        q = (q @ Vector((0, 1, 0))).rotation_difference(Vector(direction).normalized()) @ q
        pb.matrix = Matrix.Translation(head) @ q.to_matrix().to_4x4()
        bpy.context.view_layer.update()
        return Vector(head) + Vector(direction).normalized()*lengths[name]

    for frame in range(1, 14):
        scene.frame_set(frame)
        arm.location = (0, 0, 0)
        phase = (frame-1)*math.tau/12
        lean = math.radians(24 + 1.5*math.cos(2*phase))
        torso = Vector((0, -math.sin(lean), math.cos(lean)))
        root = Vector((0, 0, .48 + .012*math.cos(2*phase)))
        waist = pose('pelvis', root, torso)
        neck = pose('spine', waist, torso)
        pose('head', neck, (0, -math.sin(.25), math.cos(.25)))
        for side, sign in [('R', -1), ('L', 1)]:
            p = phase if side == 'R' else phase+math.pi
            shoulder = root + Vector((sign*.33, -.58*math.sin(lean), .58*math.cos(lean)))
            if side == 'R':
                elbow = pose('upper_arm.R', shoulder, (-.08, .10, -.30))
                wrist = pose('forearm.R', elbow, (.01, .11, -.23))
                # Rotate original blade axis to trail behind the hip, slightly down.
                original_blade = Vector((-.45, -.58, -.27)).normalized()
                blade_direction = Vector((0, 1, -.18)).normalized()
                carry = Quaternion(blade_direction, math.radians(90)) @ original_blade.rotation_difference(blade_direction)
                pb = arm.pose.bones['hand.R']
                pb.matrix = Matrix.Translation(wrist) @ (carry @ rest['hand.R'].to_quaternion()).to_matrix().to_4x4()
                bpy.context.view_layer.update()
            else:
                swing = math.radians(-10 - 40*math.cos(phase))
                elbow = pose('upper_arm.L', shoulder, (.02, math.sin(swing), -math.cos(swing)))
                wrist = pose('forearm.L', elbow, (0, math.sin(swing-1.4), -math.cos(swing-1.4)))
                pose('hand.L', wrist, (0, math.sin(swing-1.4), -math.cos(swing-1.4)))
            # A planted foot moves backward at constant speed; recovery lifts the heel.
            # Solve the two-bone leg instead of independently swinging thigh and shin.
            foot_path = [(-.22,.10),(-.11,.10),(0,.10),(.11,.10),(.21,.14),(.25,.24),
                         (.20,.35),(.05,.40),(-.10,.39),(-.22,.31),(-.28,.21),(-.27,.14)]
            index = ((frame-1) + (0 if side == 'R' else 6)) % 12
            y, z = foot_path[index]
            hip = root+Vector((sign*.16, -.04, 0))
            ankle = Vector((sign*.16, y, z))
            delta = ankle-hip
            upper, lower = lengths['thigh.'+side], lengths['shin.'+side]
            distance = min(delta.length, upper+lower-.001)
            along = delta.normalized()
            ankle = hip + along*distance
            forward = Vector((0,-1,0))
            perpendicular = (forward-along*forward.dot(along)).normalized()
            reach = (upper*upper-lower*lower+distance*distance)/(2*distance)
            knee = hip+along*reach+perpendicular*math.sqrt(max(0,upper*upper-reach*reach))
            pose('thigh.'+side, hip, knee-hip)
            pose('shin.'+side, knee, ankle-knee)
            # Point recovering toes back so the folded boot clears the coat silhouette.
            toe_angle = math.radians([0,0,0,10,25,65,85,60,30,10,0,0][index])
            pose('foot.'+side, ankle, (0,-math.cos(toe_angle),-math.sin(toe_angle)-.32))
        deps = bpy.context.evaluated_depsgraph_get()
        ev = body.evaluated_get(deps)
        shoe_ids = [v.index for v in body.data.vertices if v.co.z < .16]
        low = min((ev.matrix_world @ ev.data.vertices[i].co).z for i in shoe_ids)
        # Tiny correction for mesh soles; do not derive body bob from the lowest foot.
        arm.location.z = max(0, -low)
        arm.keyframe_insert('location', frame=frame)
        for pb in arm.pose.bones:
            pb.keyframe_insert('rotation_quaternion', frame=frame)
            pb.keyframe_insert('location', frame=frame)
        bpy.context.view_layer.update()
        ev = body.evaluated_get(bpy.context.evaluated_depsgraph_get())
        clearance = min((ev.matrix_world @ ev.data.vertices[i].co).z for i in shoe_ids)
        assert clearance > -1e-5, (frame, clearance)
        samples.append({'frame': frame, 'shoe_clearance': clearance, 'root_z': arm.location.z,
                        'right_wrist': list(arm.pose.bones['hand.R'].head-arm.pose.bones['upper_arm.R'].head)})
        arm.location.z = 0

    action = arm.animation_data.action
    action.name = 'Sword_Run_12'
    for fc in action.fcurves:
        for key in fc.keyframe_points:
            key.interpolation = 'LINEAR'
    # Keep rest dimensions available to the renderer across different actions.
    arm['sprite_rest_height'] = 1.8986852
    arm['sprite_centre_x'] = 0.0
    arm['sprite_centre_y'] = 0.0
    scene.frame_set(1)
    bpy.context.view_layer.update()
    def evaluated_vertices():
        deps = bpy.context.evaluated_depsgraph_get()
        return [o.evaluated_get(deps).matrix_world @ v.co for o in (body, sword)
                for v in o.evaluated_get(deps).data.vertices]
    first = evaluated_vertices()
    scene.frame_set(13)
    last = evaluated_vertices()
    loop_error = max((a-b).length for a,b in zip(first,last))
    assert loop_error < 1e-5, f'Loop seam: {loop_error}'
    assert max(s['shoe_clearance'] for s in samples) > .025
    assert max((Vector(s['right_wrist'])-Vector(samples[0]['right_wrist'])).length for s in samples) < 1e-5
    scene.frame_set(1)
    bpy.ops.wm.save_as_mainfile(filepath=str(out/'rainoray-sword-run.blend'))
    bpy.ops.export_scene.gltf(filepath=str(out/'rainoray-sword-run.glb'), export_format='GLB',
                             export_animations=True, export_frame_range=True, export_force_sampling=True,
                             export_extras=True)
    (out/'rig-checks.json').write_text(json.dumps({'source': str(Path(source).resolve()),
        'bones': list(rest), 'body_vertices': len(body.data.vertices), 'sword_vertices': len(sword.data.vertices),
        'repaired_hair_faces': repaired, 'loop_vertex_error': loop_error, 'samples': samples}, indent=2))
    print('RIG_OK', len(rest), 'bones;', repaired, 'hair faces repaired; loop error', loop_error)


if __name__ == '__main__':
    main()
