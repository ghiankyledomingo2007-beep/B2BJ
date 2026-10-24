"""Apply the approved curved mask and broad coat panels to the refitted native model.

Run in Blender with -- INPUT.blend NEW.blend. Geometry, weights and poses stay intact.
"""
import hashlib
import math
import sys
from pathlib import Path

import bmesh
import bpy

sys.path.insert(0, str(Path(__file__).resolve().parent))
import puppet
import trail


def polish(source, output):
    assert source.is_file() and not output.exists(), "Use an existing source and a fresh output path"
    source_hash = hashlib.sha256(source.read_bytes()).hexdigest()
    bpy.ops.wm.open_mainfile(filepath=str(source))
    scene = bpy.context.scene
    arm, body, mask = (scene.objects[name] for name in ("RainorayRig", "RainorayBody", "RainorayMask"))
    assert arm.get("appearance_rest_height") and not arm.get("coat_panels"), "Apply once to the refitted model"
    geometry = [tuple(v.co) for v in body.data.vertices]
    weights = [[(g.group, g.weight) for g in v.groups] for v in body.data.vertices]
    bones = {b.name: (tuple(b.head_local), tuple(b.tail_local)) for b in arm.data.bones}

    palette = {"navy": (38, 44, 86), "panel": (51, 63, 115), "shadow": (22, 27, 52),
               "seam": (105, 135, 164), "clasp": (205, 175, 112), "ink": (12, 16, 30)}
    materials, slots = {}, {}
    for name, color in palette.items():
        mat = bpy.data.materials.new("Rainoray panel " + name)
        mat.diffuse_color = trail.linear(color)
        materials[name] = mat
        slots[name] = len(body.data.materials)
        body.data.materials.append(mat)

    _, samples = puppet.vertex_colours(body)
    # Spatial panels avoid shading each tiny triangle independently, which produces speckles.
    for poly in body.data.polygons:
        x, y, z = poly.center
        x -= puppet.CX
        r, g, b = samples[list(poly.loop_indices)].mean(axis=0)
        if z < -.68 or z > .23 or poly.material_index in (1, 2) and z > -.36:
            continue
        warm = z > .08 and r > b * 1.08 and r > g * .95
        back_hair = y > .29 and z > -.36 and b > .48 and r > .38
        if warm or back_hair or abs(x) > .48:
            continue
        tone = "navy"
        if abs(x) > .27:  # Sleeves: one broad outside highlight and a dark cuff.
            tone = "panel" if y < .15 else "shadow"
            if abs(x) > .42:
                tone = "shadow"
        elif y < .23:
            opening = .027 + max(0, -.22-z) * .27
            tone = "shadow" if abs(x) < opening else "panel" if x < 0 else "navy"
            if opening < abs(x) < opening + .022:
                tone = "seam"
            if -.24 < z < -.185:
                tone = "shadow"
                if abs(x) < .043 and y < .14:
                    tone = "clasp"
        else:
            tone = "shadow" if abs(x) > .16 else "navy"
            if -.64 < z < -.56 and abs(x) < .22:
                tone = "panel"
        poly.material_index = slots[tone]

    # Remove the previous crest/cheeks/dot but preserve the curved porcelain and its skinning.
    mesh = bmesh.new()
    mesh.from_mesh(mask.data)
    marks = [face for face in mesh.faces if face.material_index >= 2]
    bmesh.ops.delete(mesh, geom=marks, context="FACES")
    mesh.to_mesh(mask.data)
    mesh.free()
    mask.data.update()

    def surface(x, z):
        raw_x = x / 1.025
        raw_z = .3 + (z-.3) / .88
        return .018 + .10 * (raw_x/.205)**2 + .026 * ((raw_z-.535)/.205)**2 - .075

    def patch(name, points, material):
        data = bpy.data.meshes.new(name)
        data.from_pydata([(puppet.CX+x, surface(x,z), z) for x,z in points], [], [tuple(range(len(points)))])
        ob = bpy.data.objects.new(name, data)
        scene.collection.objects.link(ob)
        data.materials.append(material)
        ob.parent = arm
        ob.vertex_groups.new(name="head").add(list(range(len(points))), 1, "REPLACE")
        ob.modifiers.new("Armature", "ARMATURE").object = arm
        return ob

    def stroke(name, points, width):
        left, right = [], []
        for i, (x,z) in enumerate(points):
            a, b = points[max(0,i-1)], points[min(len(points)-1,i+1)]
            dx, dz = b[0]-a[0], b[1]-a[1]
            length = math.hypot(dx,dz)
            nx, nz = -dz/length*width/2, dx/length*width/2
            left.append((x+nx,z+nz))
            right.append((x-nx,z-nz))
        return patch(name, left+right[::-1], materials["ink"])

    marks = []
    for side in (-1,1):
        # Mirrored flowing hooks from the supplied porcelain-mask reference; no forehead crest.
        hook = [(.050,.607),(.086,.609),(.104,.590),(.105,.573),(.084,.548),
                (.054,.520),(.045,.488),(.064,.484),(.063,.463),(.047,.444)]
        marks.append(stroke("Mask long curve", [(side*x,z) for x,z in hook], .022))
        cheek = [(.146,.498),(.162,.507),(.178,.496),(.162,.483),(.170,.470),(.149,.464)]
        marks.append(stroke("Mask cheek curve", [(side*x,z) for x,z in cheek], .020))
    marks.append(stroke("Mask jaw curve", [(-.109,.433),(-.090,.415),(-.055,.406),(0,.402),
                                          (.055,.406),(.090,.415),(.109,.433)], .018))
    red = next(mat for mat in mask.data.materials if mat.name == "Rainoray red")
    marks.append(patch("Mask red dot", [(x,.449+z) for x,z in [(-.023,-.023),(.023,-.023),(.023,.023),(-.023,.023)]], red))
    bpy.ops.object.select_all(action="DESELECT")
    for ob in (mask,*marks):
        ob.hide_set(False)
        ob.select_set(True)
    bpy.context.view_layer.objects.active = mask
    bpy.ops.object.join()
    mask["native_markings"] = True
    mask["reference"] = "mask-reference-20261024.png"
    arm["coat_panels"] = True
    assert geometry == [tuple(v.co) for v in body.data.vertices]
    assert weights == [[(g.group,g.weight) for g in v.groups] for v in body.data.vertices]
    assert bones == {b.name:(tuple(b.head_local),tuple(b.tail_local)) for b in arm.data.bones}
    for bone in arm.pose.bones:
        bone.matrix_basis.identity()
    output.parent.mkdir(parents=True, exist_ok=True)
    bpy.ops.wm.save_as_mainfile(filepath=str(output))
    assert hashlib.sha256(source.read_bytes()).hexdigest() == source_hash
    print("PASS: curved mask and coat panels saved; source, body geometry, weights and bones unchanged")


if __name__ == "__main__":
    args = sys.argv[sys.argv.index("--")+1:]
    assert len(args) == 2, "Specify INPUT.blend and NEW.blend"
    polish(*(Path(arg).resolve() for arg in args))
