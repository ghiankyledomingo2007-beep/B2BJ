"""Simplify the current mask and hair tones; keep geometry, skinning and the rig intact.

Run inside Blender with -- INPUT.blend NEW.blend.
"""
import hashlib
import sys
from pathlib import Path

import bmesh
import bpy

sys.path.insert(0, str(Path(__file__).resolve().parent))
import puppet


def simplify(source, output):
    assert source.is_file() and not output.exists(), "Use an existing source and a fresh output"
    source_hash = hashlib.sha256(source.read_bytes()).hexdigest()
    bpy.ops.wm.open_mainfile(filepath=str(source))
    scene = bpy.context.scene
    arm, body, mask = (scene.objects[name] for name in ("RainorayRig", "RainorayBody", "RainorayMask"))
    assert arm.get("coat_panels") and not mask.get("simple_markings"), "Apply once to the current model"
    geometry = [tuple(v.co) for v in body.data.vertices]
    weights = [[(g.group, g.weight) for g in v.groups] for v in body.data.vertices]
    bones = {b.name: (tuple(b.head_local), tuple(b.tail_local)) for b in arm.data.bones}

    # Broad locks replace alternating narrow stripes in the hanging hair.
    light, shade = (body.data.materials.find(name) for name in ("HairLight", "HairMid"))
    assert light >= 0 and shade >= 0
    for face in body.data.polygons:
        if face.material_index in (light, shade):
            x, _, z = face.center
            x -= puppet.CX
            face.material_index = shade if x > .10 or (-.06 < x < -.025 and z < .12) else light

    mesh = bmesh.new()
    mesh.from_mesh(mask.data)
    bmesh.ops.delete(mesh, geom=[f for f in mesh.faces if f.material_index >= 2], context="FACES")
    mesh.to_mesh(mask.data)
    mesh.free()
    mask.data.update()
    ink = next(m for m in mask.data.materials if m.name == "Rainoray ink")
    red = next(m for m in mask.data.materials if m.name == "Rainoray red")

    def patch(name, points, material):
        vertices = []
        for x, z in points:
            raw_x, raw_z = x / 1.025, .3 + (z - .3) / .88
            y = .018 + .10 * (raw_x / .205)**2 + .026 * ((raw_z - .535) / .205)**2 - .075
            vertices.append((puppet.CX + x, y, z))
        data = bpy.data.meshes.new(name)
        data.from_pydata(vertices, [], [tuple(range(len(vertices)))])
        data.materials.append(material)
        ob = bpy.data.objects.new(name, data)
        scene.collection.objects.link(ob)
        ob.parent = arm
        ob.vertex_groups.new(name="head").add(list(range(len(vertices))), 1, "REPLACE")
        ob.modifiers.new("Armature", "ARMATURE").object = arm
        return ob

    marks = []
    for side in (-1, 1):
        marks.append(patch("Mask short eye", [(side*x, z) for x, z in
            [(.045, .565), (.105, .570), (.112, .545), (.050, .545)]], ink))
        marks.append(patch("Mask cheek tick", [(side*x, z) for x, z in
            [(.112, .487), (.135, .498), (.145, .477), (.120, .466)]], ink))
    marks.append(patch("Mask red nose", [(x, .505 + z) for x, z in
        [(-.023, -.023), (.023, -.023), (.023, .023), (-.023, .023)]], red))
    bpy.ops.object.select_all(action="DESELECT")
    for ob in (mask, *marks):
        ob.hide_set(False)
        ob.select_set(True)
    bpy.context.view_layer.objects.active = mask
    bpy.ops.object.join()
    mask["native_markings"] = True
    mask["simple_markings"] = True
    mask["reference"] = "Small red nose, short eyes and cheek ticks; no copied mask pattern"
    assert geometry == [tuple(v.co) for v in body.data.vertices]
    assert weights == [[(g.group, g.weight) for g in v.groups] for v in body.data.vertices]
    assert bones == {b.name: (tuple(b.head_local), tuple(b.tail_local)) for b in arm.data.bones}
    for bone in arm.pose.bones:
        bone.matrix_basis.identity()
    output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output))
    assert hashlib.sha256(source.read_bytes()).hexdigest() == source_hash
    print("PASS: simple mask and broad hair locks; source, body geometry, weights and rig preserved")


if __name__ == "__main__":
    args = sys.argv[sys.argv.index("--") + 1:]
    assert len(args) == 2, "Specify INPUT.blend and NEW.blend"
    simplify(*(Path(arg).resolve() for arg in args))
