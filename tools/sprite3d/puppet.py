"""The Rainoray puppet: skeleton, skinning and the posing toolkit. Runs inside Blender.

The source is one static AI-made GLB (A-pose, sword baked into the right hand). Everything here is
free and local: joints are placed from measurements of that mesh, weights come from Blender's
automatic bone heat, then sword and hair are re-weighted by position and texture colour.

World frame of the source mesh: the character faces -Y, his right hand is at -X, up is +Z.
`body()` converts (right, forward, up) into that frame so poses can be written the way you think.
"""
import math

import bpy
import numpy as np
from mathutils import Matrix, Vector

CX, CY = 0.271, 0.19                      # body centre line of the source mesh
J = {
    "hips": (CX, CY, -0.40), "spine": (CX, CY, -0.15), "chest": (CX, CY, 0.07),
    "neck": (CX, CY, 0.24), "head": (CX, CY - 0.02, 0.34), "head_top": (CX, CY - 0.02, 0.95),
    "hair": (CX, CY + 0.22, 0.20), "hair_mid": (CX, CY + 0.27, -0.08), "hair_end": (CX, CY + 0.30, -0.35),
    "shoulder.L": (0.509, CY, 0.17), "elbow.L": (0.662, CY - 0.02, -0.05),
    "wrist.L": (0.790, CY - 0.05, -0.20), "hand_end.L": (0.86, CY - 0.06, -0.28),
    "shoulder.R": (0.034, CY, 0.17), "elbow.R": (-0.153, CY - 0.02, -0.05),
    "wrist.R": (-0.290, CY - 0.06, -0.20), "hand_end.R": (-0.36, CY - 0.09, -0.27),
    "thigh.L": (0.407, CY, -0.41), "knee.L": (0.407, CY - 0.01, -0.68),
    "ankle.L": (0.407, CY, -0.865), "toe.L": (0.407, CY - 0.13, -0.93),
    "thigh.R": (0.085, CY, -0.41), "knee.R": (0.085, CY - 0.01, -0.68),
    "ankle.R": (0.085, CY, -0.865), "toe.R": (0.085, CY - 0.13, -0.93),
}
BONES = [  # name, head joint, tail joint, parent. Parents always come before children.
    ("hips", "hips", "spine", None), ("spine", "spine", "chest", "hips"),
    ("chest", "chest", "neck", "spine"), ("neck", "neck", "head", "chest"),
    ("head", "head", "head_top", "neck"), ("hair", "hair", "hair_mid", "head"),
    ("hair_tip", "hair_mid", "hair_end", "hair"),
    ("upperarm.L", "shoulder.L", "elbow.L", "chest"), ("forearm.L", "elbow.L", "wrist.L", "upperarm.L"),
    ("hand.L", "wrist.L", "hand_end.L", "forearm.L"),
    ("upperarm.R", "shoulder.R", "elbow.R", "chest"), ("forearm.R", "elbow.R", "wrist.R", "upperarm.R"),
    ("hand.R", "wrist.R", "hand_end.R", "forearm.R"),
    ("blade", "blade", "blade_tip", "hand.R"),          # joints measured from the sword itself
    ("thigh.L", "thigh.L", "knee.L", "hips"), ("shin.L", "knee.L", "ankle.L", "thigh.L"),
    ("foot.L", "ankle.L", "toe.L", "shin.L"),
    ("thigh.R", "thigh.R", "knee.R", "hips"), ("shin.R", "knee.R", "ankle.R", "thigh.R"),
    ("foot.R", "ankle.R", "toe.R", "shin.R"),
]
REST_HEIGHT = 1.898                       # A-pose height of the source mesh: one scale for every action
SWORD_X = -0.345                          # everything further out than the right fist is sword
HEAD_SCALE = 0.86                         # the AI model's head is bigger than the hand-made sprite's
SLIM_X, SLIM_Y = 0.80, 0.90               # ... and its body wider and a bit deeper
HAIR_STRETCH = 1.7                        # the sprite's hair reaches the waist; the model's stops short
BLADE_SCALE = 0.92                        # the sprite's sword is ~27 px; the model's came out ~29
HAIR_LIGHT = (0.78, 0.86, 0.94, 1.0)      # strand tones for the repainted hanging hair
HAIR_SHADOW = (0.56, 0.66, 0.79, 1.0)
FOOT_JOINTS = ("foot.L", "foot.R")
SOLE = 0.035                              # the mesh sole sits this far under the toe joint
X_AXIS, Y_AXIS, Z_AXIS = Vector((1, 0, 0)), Vector((0, 1, 0)), Vector((0, 0, 1))


def body(right, forward, up):
    """(right, forward, up) as the character feels it -> world vector."""
    return Vector((-right, -forward, up))


def mix(a, b, t):
    return a + (b - a) * t


# ---------------------------------------------------------------- building the rig

def sword_axis(mesh):
    """Hilt end and tip of the baked sword, from the principal axis of its vertices."""
    mw = mesh.matrix_world
    pts = np.array([tuple(mw @ v.co) for v in mesh.data.vertices
                    if (mw @ v.co).x < SWORD_X and (mw @ v.co).z < 0.0])
    centre = pts.mean(axis=0)
    axis = np.linalg.svd(pts - centre)[2][0]
    along = (pts - centre) @ axis
    ends = [centre + axis * along.min(), centre + axis * along.max()]
    ends.sort(key=lambda e: abs(e[0] - CX))            # hilt is the end nearer the body
    return tuple(ends[0]), tuple(ends[1])


def build_armature(mesh):
    J["blade"], J["blade_tip"] = sword_axis(mesh)
    data = bpy.data.armatures.new("RainorayRig")
    arm = bpy.data.objects.new("RainorayRig", data)
    bpy.context.scene.collection.objects.link(arm)
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.mode_set(mode="EDIT")
    made = {}
    for name, head, tail, parent in BONES:
        b = data.edit_bones.new(name)
        b.head, b.tail = Vector(J[head]), Vector(J[tail])
        if parent:
            b.parent = made[parent]
            b.use_connect = (made[parent].tail - b.head).length < 1e-4
        made[name] = b
    bpy.ops.object.mode_set(mode="OBJECT")
    return arm


def set_only(mesh, vert_index, weights):
    for g in mesh.vertex_groups:
        g.remove([vert_index])
    for name, w in weights.items():
        mesh.vertex_groups[name].add([vert_index], w, "REPLACE")


def vertex_colours(mesh):
    """Average base-colour texel per vertex, so hair (pale blue) can be told from coat (dark blue)."""
    img = next(n.image for m in mesh.data.materials for n in m.node_tree.nodes
               if n.type == "TEX_IMAGE" and n.image)
    w, h = img.size
    px = np.empty(w * h * 4, dtype=np.float32)
    img.pixels.foreach_get(px)
    px = px.reshape(h, w, 4)
    uv = mesh.data.uv_layers.active.data
    total = np.zeros((len(mesh.data.vertices), 3), dtype=np.float64)
    count = np.zeros(len(mesh.data.vertices))
    per_loop = np.zeros((len(mesh.data.loops), 3))
    for loop in mesh.data.loops:
        u, v = uv[loop.index].uv
        per_loop[loop.index] = px[int(v % 1.0 * (h - 1)), int(u % 1.0 * (w - 1)), :3]
        total[loop.vertex_index] += per_loop[loop.index]
        count[loop.vertex_index] += 1
    return total / np.maximum(count, 1)[:, None], per_loop


def is_hair(c):
    return c[2] > 0.55 and c[0] > 0.40 and c[2] - c[0] < 0.35      # pale blue / silver, not coat navy


def is_unpainted(c):
    return max(c) < 0.15


def in_back_hair(w):
    return w.y > CY + 0.10 and -0.45 < w.z < 0.30 and abs(w.x - CX) < 0.34


def paint_hair(mesh, per_loop):
    """The generator left the hanging hair half unpainted (black). Repaint every hanging-hair
    face in two flat strand tones; coat-coloured faces in the same region are left alone."""
    slots = []
    for name, colour in (("HairLight", HAIR_LIGHT), ("HairMid", HAIR_SHADOW)):
        mat = bpy.data.materials.new(name)
        mat.diffuse_color = colour
        mesh.data.materials.append(mat)
        slots.append(len(mesh.data.materials) - 1)
    mw, fixed = mesh.matrix_world, 0
    for poly in mesh.data.polygons:
        centre = mw @ poly.center
        if not in_back_hair(centre):
            continue
        c = per_loop[list(poly.loop_indices)].mean(axis=0)
        is_coat = c[2] > 0.25 and c[0] < 0.30 and c[2] - c[0] > 0.15
        if not is_coat:
            poly.material_index = slots[int(centre.x * 22) % 2]      # vertical strands
            fixed += 1
    return fixed


def skin(mesh, arm):
    bpy.ops.object.select_all(action="DESELECT")
    bpy.context.view_layer.objects.active = mesh
    mesh.select_set(True)
    # glTF import splits vertices at UV seams; weld them so bone heat sees one surface
    bpy.ops.object.mode_set(mode="EDIT")
    bpy.ops.mesh.select_all(action="SELECT")
    bpy.ops.mesh.remove_doubles(threshold=0.0005)
    bpy.ops.object.mode_set(mode="OBJECT")
    arm.select_set(True)
    bpy.context.view_layer.objects.active = arm
    bpy.ops.object.parent_set(type="ARMATURE_AUTO")
    missing = [b[0] for b in BONES if b[0] not in mesh.vertex_groups]
    if missing:
        raise SystemExit("automatic weights failed, no groups for: " + ", ".join(missing))

    mw = mesh.matrix_world
    colour, per_loop = vertex_colours(mesh)
    sword = hair = 0
    for v in mesh.data.vertices:
        w, c = mw @ v.co, colour[v.index]
        if w.x < SWORD_X and w.z < 0.0:                       # rigid blade + hilt
            set_only(mesh, v.index, {"blade": 1.0})
            sword += 1
        elif w.z > 0.30:                                       # skull, mask, top hair
            set_only(mesh, v.index, {"head": 1.0})
            hair += 1
        elif in_back_hair(w) and (is_hair(c) or is_unpainted(c)):   # long back hair only, never the coat
            t = min(max((0.30 - w.z) / 0.70, 0.0), 1.0)       # 0 at the head, 1 at the tips
            if t < 0.5:
                set_only(mesh, v.index, {"head": 1.0 - 2 * t, "hair": 2 * t})
            else:
                set_only(mesh, v.index, {"hair": 2.0 - 2 * t, "hair_tip": 2 * t - 1.0})
            hair += 1
    fixed = paint_hair(mesh, per_loop)
    print(f"PUPPET sword_verts={sword} hair_verts={hair} hair_faces={fixed} verts={len(mesh.data.vertices)}")


def build(glb_path, blend_path):
    """Static GLB -> rigged, skinned puppet saved as a .blend (posed later, never keyframed)."""
    bpy.ops.wm.read_factory_settings(use_empty=True)
    bpy.ops.import_scene.gltf(filepath=glb_path)
    mesh = next(o for o in bpy.context.scene.objects if o.type == "MESH")
    bpy.context.view_layer.objects.active = mesh
    mesh.select_set(True)
    bpy.ops.object.transform_apply(location=True, rotation=True, scale=True)
    arm = build_armature(mesh)
    skin(mesh, arm)
    bpy.ops.wm.save_as_mainfile(filepath=blend_path)


# ---------------------------------------------------------------- posing

class Poser:
    """Poses bones with WORLD-space operations, so bone roll never matters.

    Every operation is pre-multiplied, which keeps each bone's matrix = rotations x slim-scale x rest:
    the body is slimmed once and then only rotated rigidly, so limbs never shear or change length.
    """

    def __init__(self, arm):
        self.arm = arm
        self.bones = arm.pose.bones

    def begin(self):
        """Rest pose with the proportion fixes every action shares."""
        for pb in self.bones:
            pb.rotation_mode = "QUATERNION"
            pb.matrix_basis = Matrix.Identity(4)
        bpy.context.view_layer.update()
        self._apply("hips", Matrix.Scale(SLIM_X, 4, X_AXIS))
        self._apply("hips", Matrix.Scale(SLIM_Y, 4, Y_AXIS))
        self._apply("blade", Matrix.Scale(BLADE_SCALE, 4, self.direction("blade")))

    def finish_head(self, yaw_degrees):
        """Head yaw, head size and hair length: after the torso is posed, before the hair swings."""
        self.turn("head", Z_AXIS, yaw_degrees)
        self._apply("head", Matrix.Scale(HEAD_SCALE, 4))
        hair = self.bones["hair"]
        self._apply("hair", Matrix.Scale(HAIR_STRETCH, 4, (hair.tail - hair.head).normalized()))

    def _apply(self, name, world_matrix, about_head=True):
        pb = self.bones[name]
        if about_head:
            pivot = Matrix.Translation(pb.head)
            world_matrix = pivot @ world_matrix @ pivot.inverted()
        pb.matrix = world_matrix @ pb.matrix
        bpy.context.view_layer.update()

    def turn(self, name, axis, degrees):
        self._apply(name, Matrix.Rotation(math.radians(degrees), 4, axis))

    def move(self, name, offset):
        self._apply(name, Matrix.Translation(offset), about_head=False)

    def head(self, name):
        return self.bones[name].head.copy()

    def tail(self, name):
        return self.bones[name].tail.copy()

    def direction(self, name):
        pb = self.bones[name]
        return (pb.tail - pb.head).normalized()

    def point(self, name, direction):
        """Swing a bone (and its children) so it points along `direction`."""
        q = self.direction(name).rotation_difference(Vector(direction).normalized())
        self._apply(name, q.to_matrix().to_4x4())

    def ik(self, upper, lower, target, hint):
        """Two-bone reach: place the end of `lower` on `target`, bending toward `hint`."""
        root = self.head(upper)
        l1 = (self.tail(upper) - root).length
        l2 = (self.tail(lower) - self.head(lower)).length
        to = Vector(target) - root
        d = min(max(to.length, abs(l1 - l2) + 1e-4), l1 + l2 - 1e-4)
        along = to.normalized()
        x = (l1 * l1 - l2 * l2 + d * d) / (2 * d)
        h = math.sqrt(max(l1 * l1 - x * x, 0.0))
        bend = Vector(hint) - Vector(hint).dot(along) * along
        if bend.length < 1e-5:
            bend = along.orthogonal()
        joint = root + along * x + bend.normalized() * h
        self.point(upper, joint - root)
        self.point(lower, root + along * d - self.head(lower))

    def snapshot(self):
        return {pb.name: pb.matrix_basis.copy() for pb in self.bones}

    def restore(self, snap):
        for pb in self.bones:
            pb.matrix_basis = snap[pb.name]
        bpy.context.view_layer.update()

    def lowest_foot(self):
        return min(self.bones[j].tail.z for j in FOOT_JOINTS)
