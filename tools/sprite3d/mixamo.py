"""Mixamo motion on the Rainoray puppet, photographed in the game's sprite camera. Runs inside Blender.

  blender -b --python tools/sprite3d/mixamo.py -- clip.fbx out_dir --frames 1,4,7.5 [--views east]
                                                [--armed] [--loop] [--travel 0.0]

clip.fbx is a Mixamo download ('FBX Binary', 'Without Skin', X Bot or Y Bot). Each puppet bone takes the
world rotation its Mixamo partner has away from the T-pose. Partners are matched on two directions each,
so the A-pose/T-pose difference and bone roll never matter. The legs reach by IK for the Mixamo feet
scaled to his leg length, so a planted foot stays planted. The sword stays rigid in the right fist and the
fist follows Mixamo's grip: the line of its knuckles is the blade. Hair has no Mixamo partner: it swings
on a damped spring that follows his head, with a steady head wind of --travel m/s for runs filmed in place.

--carry keeps the game's own sword carry for runs (arm locked back, blade trailing, locomotion.sword_arm)
on top of the Mixamo body; --upright straightens a clip's forward lean by that many degrees.
Mixamo fighters stand sideways and spin through a cut, which from a fixed sprite camera shows his back and
buries him under his hair. --aim turns the whole clip so his hips face straight ahead on average over the
photographed frames (his right-handed reach sits off his centre line, so aiming the hand turns him away),
and --lock 0..1 takes that share of the hips' turning out of every frame, so he stays facing the camera's way while arms and blade still sweep;
the head is turned the same share back toward straight ahead, so the mask, not his hair, faces the camera.
--in-place drops the clip's travel over the ground (the game carries his mark instead).

Every listed clip frame (fractions allowed) is photographed three times: flat_<facing>_NN.png in the
model's own colours, as build_sheets does; normal_<facing>_NN.png, the camera-space surface direction
smoothed over the mesh; depth_<facing>_NN.png, nearer surfaces brighter. finish.py lights the normals in
hard bands and inks the overlaps the depth shows.
"""
import argparse
import math
import os
import sys

import bpy
import numpy as np
from mathutils import Matrix, Vector

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import animate  # noqa: E402
import locomotion  # noqa: E402
import puppet  # noqa: E402
import render_frames  # noqa: E402

SOURCE = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "../../docs/art-review/"
                                       "sprite3d-run/source/rainoray-editable/Rainoray-Body-and-Separate-Sword.blend"))
PREFIX = "mixamorig:"
UP, RIGHT, FORWARD = Vector((0, 0, 1)), Vector((1, 0, 0)), Vector((0, -1, 0))   # both rigs face -Y, left at +X
# puppet bone -> (Mixamo partner, the Mixamo joint it points at, the second direction that fixes its twist).
# Parents come before children. hand.R is matched on the blade instead, in Retarget.__init__.
PARTNERS = {
    "hips": ("Hips", "Spine", RIGHT), "spine": ("Spine1", "Spine2", RIGHT), "chest": ("Spine2", "Neck", RIGHT),
    "neck": ("Neck", "Head", RIGHT), "head": ("Head", "HeadTop_End", RIGHT),
    "upperarm.L": ("LeftArm", "LeftForeArm", FORWARD), "forearm.L": ("LeftForeArm", "LeftHand", FORWARD),
    "hand.L": ("LeftHand", "LeftHandMiddle1", FORWARD),
    "upperarm.R": ("RightArm", "RightForeArm", FORWARD), "forearm.R": ("RightForeArm", "RightHand", FORWARD),
    "hand.R": ("RightHand", "RightHandMiddle1", None),
}
LEGS = {".L": "Left", ".R": "Right"}
FPS = 30.0                                  # Mixamo exports at 30 fps
HAIR_SPRING, HAIR_DAMPING = 70.0, 11.0      # per s^2 and per s: the strand settles in about a third of a second
HAIR_DRAG = 26.0                            # degrees of swing per m/s of head speed
HAIR_REST, HAIR_LIMIT = 4.0, 30.0           # degrees; past ~35 the thin shell goes edge-on to a side camera
DEPTH_SPAN = 1.6                            # world units of depth mapped onto 0..255 around his centre
SMOOTHING = 4                               # neighbour-averaging passes over the normals: the AI mesh is lumpy
CARRIED = ("upperarm.R", "forearm.R", "hand.R")


def basis(primary, secondary):
    """Rotation whose X axis is `primary` and whose XY plane holds `secondary`."""
    x = primary.normalized()
    z = x.cross(secondary).normalized()
    return Matrix((x, z.cross(x), z)).transposed()


class Clip:
    """A Mixamo armature imported into the scene, read in world space."""

    def __init__(self, path):
        before = set(bpy.data.objects)
        bpy.ops.import_scene.fbx(filepath=path)
        new = [o for o in bpy.data.objects if o not in before]
        self.arm = next(o for o in new if o.type == "ARMATURE")
        for o in new:
            o.hide_render = True
        start, end = self.arm.animation_data.action.frame_range
        self.start, self.end = int(round(start)), int(round(end))

    def at(self, frame):
        whole = math.floor(frame)
        bpy.context.scene.frame_set(int(whole), subframe=frame - whole)

    def rotation(self, name):
        return (self.arm.matrix_world @ self.arm.pose.bones[PREFIX + name].matrix).to_3x3().normalized()

    def rest_rotation(self, name):
        return (self.arm.matrix_world @ self.arm.data.bones[PREFIX + name].matrix_local).to_3x3().normalized()

    def head(self, name):
        return self.arm.matrix_world @ self.arm.pose.bones[PREFIX + name].head

    def rest_head(self, name):
        return self.arm.matrix_world @ self.arm.data.bones[PREFIX + name].head_local


class Retarget:
    def __init__(self, clip, p):
        self.clip, self.p = clip, p
        p.begin()
        c = clip
        forward = c.rest_head("LeftToeBase") - c.rest_head("LeftFoot")
        forward.z = 0.0
        if forward.dot(FORWARD) < 0.8 * forward.length:
            raise SystemExit("clip does not face -Y after import; expected a Mixamo FBX")
        self.align = {}
        for bone, (partner, toward, second) in PARTNERS.items():
            if bone == "hand.R":       # the knuckle line is the blade; the hand's own line fixes the twist
                theirs = basis(c.rest_head("RightHandIndex1") - c.rest_head("RightHandPinky1"),
                               c.rest_head(toward) - c.rest_head(partner))
                ours = basis(p.direction("blade"), p.direction("hand.R"))
            else:
                theirs = basis(c.rest_head(toward) - c.rest_head(partner), second)
                ours = basis(p.direction(bone), second)
            self.align[bone] = theirs @ ours.transposed()
        self.rest = {partner: c.rest_rotation(partner) for partner, _, _ in PARTNERS.values()}
        for side in LEGS.values():
            self.rest[side + "Foot"] = c.rest_rotation(side + "Foot")
        ours_leg = (p.head("thigh.L") - p.head("foot.L")).length
        theirs_leg = (c.rest_head("LeftUpLeg") - c.rest_head("LeftFoot")).length
        self.scale = ours_leg / theirs_leg     # one scale for root travel and leg reach keeps planted feet planted
        self.foot = {side: p.direction("foot" + side) for side in LEGS}
        self.below = {b.name: [b.name] + [k.name for k in b.children_recursive] for b in p.arm.data.bones}
        self.turn_to, self.mean_yaw, self.lock = 0.0, 0.0, 0.0

    def delta(self, partner):
        return self.clip.rotation(partner) @ self.rest[partner].transposed()

    def facing(self, partner="Hips"):
        """Yaw of a Mixamo bone at the current clip frame, radians, 0 = straight ahead (-Y), positive toward +X."""
        ahead = self.delta(partner) @ FORWARD
        return math.atan2(ahead.x, -ahead.y)

    def aim(self, frames, lock):
        """Set the per-frame turn for --aim/--lock from the photographed span of the clip."""
        ahead = Vector()
        for f in range(int(math.floor(min(frames))), int(math.ceil(max(frames))) + 1):
            self.clip.at(f)
            yaw = self.facing()
            ahead += Vector((math.sin(yaw), -math.cos(yaw), 0.0))       # averaged as directions, not angles
        self.mean_yaw = math.atan2(ahead.x, -ahead.y)
        self.turn_to, self.lock = self.mean_yaw, lock

    def spin(self):
        """The turn --aim/--lock give the current clip frame."""
        wobble = (self.facing() - self.mean_yaw + math.pi) % (2 * math.pi) - math.pi
        return Matrix.Rotation(-(self.turn_to + self.lock * wobble), 3, "Z")

    def state(self, frame):
        """Everything the puppet takes from the clip at `frame`, so two frames can be blended."""
        c = self.clip
        c.at(frame)
        partners = [q for q, _, _ in PARTNERS.values()] + [side + "Foot" for side in LEGS.values()]
        return {"spin": self.spin(), "delta": {q: self.delta(q) for q in partners},
                "hips": c.head("Hips") - c.rest_head("Hips"),
                "legs": {side: tuple(c.head(theirs + n) for n in ("UpLeg", "Leg", "Foot")) for side, theirs in LEGS.items()},
                "stride": 2 * math.pi * (frame - c.start) / (c.end - c.start)}

    @staticmethod
    def blend(a, b, t):
        """A pose t of the way from state a to state b: rotations slerped, positions eased straight."""
        def turn(m, n):
            return m.to_quaternion().slerp(n.to_quaternion(), t).to_matrix()
        return {"spin": turn(a["spin"], b["spin"]), "delta": {q: turn(m, b["delta"][q]) for q, m in a["delta"].items()},
                "hips": a["hips"].lerp(b["hips"], t),
                "legs": {side: tuple(u.lerp(v, t) for u, v in zip(a["legs"][side], b["legs"][side])) for side in LEGS},
                "stride": a["stride"] + (b["stride"] - a["stride"]) * t}

    def pose(self, st, hair, view, carry=False, upright=0.0, in_place=False):
        """Pose the puppet from a state(); hair = (swing back, swing sideways) in degrees."""
        p, spin, delta = self.p, st["spin"], st["delta"]
        p.begin()
        travel = spin @ st["hips"] * self.scale
        if in_place:
            travel.x = travel.y = 0.0
        p.move("hips", travel)
        applied = {name: Matrix.Identity(3) for name in self.below}
        for bone, (partner, _, _) in PARTNERS.items():
            if carry and bone in CARRIED:
                continue
            turn = spin @ delta[partner] @ self.align[bone] @ applied[bone].transposed()
            p._apply(bone, turn.to_4x4())
            for name in self.below[bone]:
                applied[name] = turn @ applied[name]
        p.turn("hips", puppet.X_AXIS, -upright)       # +X leans him forward
        for side, theirs in LEGS.items():
            up, knee, ankle = st["legs"][side]
            p.ik("thigh" + side, "shin" + side, p.head("thigh" + side) + spin @ (ankle - up) * self.scale,
                 spin @ (knee - up))
            p.point("foot" + side, spin @ delta[theirs + "Foot"] @ self.foot[side])
        if carry:
            locomotion.sword_arm(p, view, 1.0, st["stride"], 0.0)
        if self.lock:
            looks = spin @ delta["Head"] @ FORWARD
            p.turn("head", puppet.Z_AXIS, -self.lock * math.degrees(math.atan2(looks.x, -looks.y)))
        p.finish_head(view["face_turn"])
        right = (p.head("upperarm.L") - p.head("upperarm.R")).normalized()
        ahead = right.cross(UP).normalized()
        back, side = hair
        p.turn("hair", right, HAIR_REST + back)        # positive swings the hanging hair backward
        p.turn("hair", ahead, side)                    # positive swings it toward his left
        p.turn("hair_tip", right, 0.6 * (HAIR_REST + back))
        p.turn("hair_tip", ahead, 0.8 * side)


def parse_frame(spec):
    """'33' -> (33.0, 33.0, 0.0); '42>1@0.5' -> halfway from clip frame 42 toward clip frame 1."""
    if ">" not in spec:
        return float(spec), float(spec), 0.0
    a, rest = spec.split(">")
    b, t = rest.split("@")
    return float(a), float(b), float(t)


def hair_swing(clip, rig, frames, travel, loop):
    """Damped spring on the head's speed through the world, run at the clip's own frame rate -> {frame:
    (swing back, swing sideways)}. A loop runs twice so it starts settled; a one-shot settles on frame one."""
    heads = []
    for f in range(clip.start, clip.end + 1):
        clip.at(f)
        heads.append(rig.spin() @ clip.head("Head") * rig.scale)
    count = len(heads) - 1 if loop else len(heads)      # a Mixamo loop repeats its first frame at the end
    dt = 1 / FPS
    angle, speed = [HAIR_REST, 0.0], [0.0, 0.0]
    record = {}
    steps = [(0, False)] * 30 if not loop else [(i, False) for i in range(count)]
    steps += [(i, True) for i in range(count)]
    for i, keep in steps:
        moved = heads[(i + 1) % len(heads)] - heads[i] if (loop or i + 1 < count) else Vector()
        v = moved / dt + FORWARD * travel if keep or loop else FORWARD * travel
        goal = (HAIR_DRAG * (v.dot(FORWARD) - 0.5 * v.dot(UP)), -HAIR_DRAG * v.dot(RIGHT))
        for k in range(2):
            speed[k] += (HAIR_SPRING * (goal[k] - angle[k]) - HAIR_DAMPING * speed[k]) * dt
            angle[k] += speed[k] * dt
        if keep:
            record[i] = tuple(max(-HAIR_LIMIT, min(HAIR_LIMIT, a)) for a in angle)
    out = {}
    for f in frames:
        lo = min(max(int(math.floor(f)) - clip.start, 0), count - 1)
        hi = (lo + 1) % count if loop else min(lo + 1, count - 1)
        t = f - math.floor(f)
        out[f] = tuple(a * (1 - t) + b * t for a, b in zip(record[lo], record[hi]))
    return out


def edges_of(ob):
    e = np.empty(len(ob.data.edges) * 2, np.int32)
    ob.data.edges.foreach_get("vertices", e)
    return e.reshape(-1, 2)


def paint_surface(meshes, edges, cam):
    """Write the two vertex colours finish.py reads: 'depth' (distance from the camera, nearer = brighter,
    centred on his body) and 'normal' (camera-space surface direction, averaged over neighbouring vertices
    so the AI model's small lumps do not speckle the shade)."""
    deps = bpy.context.evaluated_depsgraph_get()
    to_cam = np.array(cam.matrix_world.inverted())
    centre = (to_cam @ np.array((puppet.CX, puppet.CY, 0.0, 1.0)))[2]
    for ob in meshes:
        ev = ob.evaluated_get(deps)
        n = len(ev.data.vertices)
        world = np.array(ob.matrix_world)
        co = np.empty(n * 3, np.float32)
        ev.data.vertices.foreach_get("co", co)
        co = np.c_[co.reshape(n, 3), np.ones(n)] @ (to_cam @ world).T
        depth = np.clip(0.5 + (co[:, 2] - centre) / DEPTH_SPAN, 0.0, 1.0)      # camera looks down -Z: nearer is larger
        nor = np.empty(n * 3, np.float32)
        ev.data.vertex_normals.foreach_get("vector", nor)
        nor = nor.reshape(n, 3) @ (to_cam[:3, :3] @ np.linalg.inv(world[:3, :3]).T).T
        e = edges[ob.name]
        for _ in range(SMOOTHING):
            total = nor.copy()
            np.add.at(total, e[:, 0], nor[e[:, 1]])
            np.add.at(total, e[:, 1], nor[e[:, 0]])
            nor = total / np.maximum(np.linalg.norm(total, axis=1, keepdims=True), 1e-9)
        for name, rgb in (("depth", np.repeat(depth[:, None], 3, 1)), ("normal", nor * 0.5 + 0.5)):
            rgba = np.c_[rgb, np.ones(n)].astype(np.float32)
            ob.data.color_attributes[name].data.foreach_set("color_srgb", rgba.ravel())
        ob.data.update()


def show_colour(meshes, name):
    for ob in meshes:
        colours = ob.data.color_attributes
        colours.active_color = colours[name]
        colours.render_color_index = colours.find(name)


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    a = argparse.ArgumentParser()
    a.add_argument("clip")
    a.add_argument("out_dir")
    a.add_argument("--frames", required=True,
                   help="clip frames to photograph, comma separated; 42>1@0.5 blends halfway from 42 toward 1")
    a.add_argument("--views", default="east")
    a.add_argument("--armed", action="store_true")
    a.add_argument("--loop", action="store_true", help="the clip is a cycle (its last frame repeats the first)")
    a.add_argument("--travel", type=float, default=0.0, help="m/s of head wind, for clips filmed in place")
    a.add_argument("--carry", action="store_true", help="the game's run carry for the sword arm")
    a.add_argument("--upright", type=float, default=0.0, help="degrees taken off the clip's forward lean")
    a.add_argument("--aim", action="store_true", help="turn the clip so the swing lands straight ahead")
    a.add_argument("--lock", type=float, default=0.0, help="share of the hips' turning to take out, 0..1")
    a.add_argument("--in-place", action="store_true", help="drop the clip's travel over the ground")
    a.add_argument("--cell", type=int, default=80)
    a.add_argument("--height-px", type=float, default=68.0)
    a.add_argument("--foot-px", type=float, default=73.0)
    a.add_argument("--elevation", type=float, default=15.0)
    a.add_argument("--canvas", type=int, default=3)
    return a.parse_args(argv)


def main():
    a = parse_args()
    bpy.ops.wm.open_mainfile(filepath=SOURCE)
    scene = bpy.context.scene
    arm = next(o for o in scene.objects if o.type == "ARMATURE")
    animate.pixel_mask_dot(scene, arm)
    sword = scene.objects.get("RainoraySword")
    if sword is not None:
        sword.hide_render = not a.armed
        sword.hide_set(not a.armed)
    meshes = [o for o in scene.objects if o.type == "MESH" and not o.hide_render]
    for ob in meshes:
        for name in ("depth", "normal"):
            ob.data.color_attributes.new(name, "FLOAT_COLOR", "POINT")
    edges = {ob.name: edges_of(ob) for ob in meshes}
    clip = Clip(a.clip)
    p = puppet.Poser(arm)
    rig = Retarget(clip, p)
    shots = [parse_frame(f) for f in a.frames.split(",")]
    frames = sorted({f for a_, b_, _ in shots for f in (a_, b_)})
    if a.aim or a.lock:
        rig.aim([f for a_, b_, t in shots for f in ((a_,) if t == 0 else (a_, b_))], a.lock)
        rig.turn_to = rig.turn_to if a.aim else 0.0
        print(f"MIXAMO aim turn={math.degrees(rig.turn_to):.0f} mean_hips={math.degrees(rig.mean_yaw):.0f} lock={a.lock}")
    hair = hair_swing(clip, rig, frames, a.travel, a.loop)
    p.begin()
    floor = p.lowest_foot() - puppet.SOLE
    render_frames.setup_render(scene, a.cell, a.canvas)
    shading = scene.display.shading
    os.makedirs(a.out_dir, exist_ok=True)
    for facing in a.views.split(","):
        view = animate.VIEWS[facing]
        render_frames.place_camera(scene, facing, arm.get("appearance_frame_height", puppet.REST_HEIGHT), floor,
                                   (puppet.CX, puppet.CY), a.cell, a.height_px, a.foot_px, a.elevation, a.canvas)
        bpy.context.view_layer.update()
        for i, (f, g, t) in enumerate(shots):
            st = rig.state(f) if t == 0 else rig.blend(rig.state(f), rig.state(g), t)
            swing = tuple(u + (v - u) * t for u, v in zip(hair[f], hair[g]))
            rig.pose(st, swing, view, a.carry, a.upright, a.in_place)
            paint_surface(meshes, edges, scene.camera)
            for kind, colour in (("flat", "TEXTURE"), ("normal", "VERTEX"), ("depth", "VERTEX")):
                if colour == "VERTEX":
                    show_colour(meshes, kind)
                shading.light, shading.color_type = "FLAT", colour
                scene.render.filepath = os.path.join(a.out_dir, f"{kind}_{facing}_{i:02d}.png")
                bpy.ops.render.render(write_still=True)
        print(f"MIXAMO ok clip={os.path.basename(a.clip)} facing={facing} frames={len(shots)} scale={rig.scale:.3f}")


if __name__ == "__main__":
    main()
