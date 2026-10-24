"""Refit the existing character without changing its rig; run in Blender with -- INPUT.blend NEW.blend."""
import hashlib
import math
import sys
from pathlib import Path

import bpy

sys.path.insert(0, str(Path(__file__).resolve().parent))
import puppet
import trail


def refit(source, output):
    assert source.is_file() and not output.exists(), "Use an existing source and a fresh output path"
    original = hashlib.sha256(source.read_bytes()).hexdigest()
    bpy.ops.wm.open_mainfile(filepath=str(source))
    scene = bpy.context.scene
    arm, body, mask = (scene.objects[name] for name in ("RainorayRig", "RainorayBody", "RainorayMask"))
    assert not arm.get("appearance_rest_height"), "Do not apply the refit twice"
    bones = {b.name: (tuple(b.head_local), tuple(b.tail_local)) for b in arm.data.bones}
    weights = [[(g.group, g.weight) for g in v.groups] for v in body.data.vertices]
    feet = [tuple(v.co) for v in body.data.vertices if v.co.z < -.65]

    def material(name, rgb):
        mat = bpy.data.materials.new("Rainoray " + name)
        mat.diffuse_color = trail.linear(rgb)
        return mat

    colors = {"ink": (12,16,40), "coat": (44,51,113), "boot": (37,38,69),
              "ivory": (241,243,244), "edge": (218,224,233), "red": (224,48,64)}
    mats = {name: material(name, color) for name, color in colors.items()}
    for name in ("coat", "boot"):
        body.data.materials.append(mats[name])
    for poly in body.data.polygons:
        if poly.center.z < -.68:
            poly.material_index = len(body.data.materials) - (2 if poly.center.y < .12 and poly.center.z < -.84 else 1)

    mask.data.materials.clear()
    mask.data.materials.append(mats["ivory"])
    mask.data.materials.append(mats["edge"])
    for poly in mask.data.polygons:
        poly.material_index = int(poly.center.x > puppet.CX + .13)
    for v in mask.data.vertices:
        v.co.y -= .065

    def patch(name, points, color):
        mesh = bpy.data.meshes.new(name)
        # Keep the marks ahead of the curved porcelain so shallow views cannot expose the old texture.
        vertices = [(puppet.CX+x, .018+.10*(x/.205)**2+.026*((z-.535)/.205)**2-.105, z) for x,z in points]
        mesh.from_pydata(vertices, [], [tuple(range(len(vertices)))])
        ob = bpy.data.objects.new(name, mesh)
        scene.collection.objects.link(ob)
        mesh.materials.append(mats[color])
        ob.parent = arm
        ob.vertex_groups.new(name="head").add(list(range(len(vertices))), 1, "REPLACE")
        ob.modifiers.new("Armature", "ARMATURE").object = arm
        return ob

    marks = [patch("Mask crest", [(0,.71),(.063,.64),(0,.57),(-.063,.64)], "ink")]
    for side in (-1, 1):
        marks.append(patch("Mask cheek", [(side*x,z) for x,z in
            [(.16,.59),(.12,.60),(.055,.50),(.06,.435),(.10,.47),(.10,.505)]], "ink"))
    marks.append(patch("Mask dot", [(.027*math.cos(i*math.tau/12),.469+.027*math.sin(i*math.tau/12)) for i in range(12)], "red"))
    for ob in (body, mask, *marks):
        for v in ob.data.vertices:
            x, y, z = v.co
            if z > .3:
                v.co.z = .3 + (z-.3) * (.76 if ob == body else .88)
                v.co.x = puppet.CX + (x-puppet.CX) * (1+.025*min(1,(z-.3)/.2))
        ob.data.update()

    # One removable mask object includes its markings; hiding it cannot leave floating marks.
    bpy.ops.object.select_all(action="DESELECT")
    for ob in (mask, *marks):
        ob.hide_set(False)
        ob.select_set(True)
    bpy.context.view_layer.objects.active = mask
    bpy.ops.object.join()
    mask["native_markings"] = True
    assert len(arm.data.bones) == 20 and bones == {b.name: (tuple(b.head_local),tuple(b.tail_local)) for b in arm.data.bones}
    assert weights == [[(g.group,g.weight) for g in v.groups] for v in body.data.vertices]
    assert feet == [tuple(v.co) for v in body.data.vertices if v.co.z < -.65]
    for bone in arm.pose.bones:
        bone.matrix_basis.identity()
    arm["appearance_rest_height"] = max(v.co.z for v in body.data.vertices) - min(v.co.z for v in body.data.vertices)
    # The longest side thrust needs this margin while every action keeps the same pixel scale.
    arm["appearance_frame_height"] = arm["appearance_rest_height"] * 1.045
    output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output))
    assert hashlib.sha256(source.read_bytes()).hexdigest() == original, "Input model changed"
    print("PASS: source, 20 bones, body weights and foot geometry preserved; compact head and integrated mask saved")


if __name__ == "__main__":
    args = sys.argv[sys.argv.index("--")+1:]
    assert len(args) == 2, "Specify INPUT.blend and NEW.blend"
    refit(*(Path(arg).resolve() for arg in args))
