"""Pose the Rainoray puppet frame by frame and photograph it. One Blender run does every facing.

  blender -b --python tools/sprite3d/animate.py -- puppet.blend out_dir --action run
      [--frames N] [--views south,east,north] [--cell 80] [--height-px 68] [--foot-px 73]

Nothing is keyframed: each frame is posed from scratch and rendered, so there is no animation
data to fight the pose being built. If puppet.blend is missing it is built from --source first.
Frames land in out_dir/frames-<action>-<facing>/<facing>_NN.png; build_sheets.py finishes them.
With --trail, swings also get trail_<facing>_NN.png: the blade's swoosh as a separate layer (trail.py).
"""
import argparse
import math
import os
import sys

import bpy

sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
import attacks  # noqa: E402
import channels  # noqa: E402
import locomotion  # noqa: E402
import puppet  # noqa: E402
import reactions  # noqa: E402
import render_frames  # noqa: E402
import trail  # noqa: E402

# Per-facing presets. Side views yaw the head so the mask shows and trail the blade straight
# behind. Front/back runs keep the hand low and the blade beside the leg, clear of the floor.
# Twist is a side-view-only flourish.
# ahead_bias bends a dead-ahead thrust so the front and back cameras can see it (channels.apply).
SIDE = {"face_turn": -46.0, "sword_yaw": -118.0, "arm_in": -34.0, "arm_back": 38.0, "twist": 1.0,
        "carry_direction": (-0.4, 0.78, -0.55)}
VIEWS = {
    "south": {"face_turn": 0.0, "sword_yaw": -35.0, "arm_in": -30.0, "arm_back": 18.0, "twist": 0.15,
              "carry_direction": (-0.14, 0.78, -0.61),
              "ahead_bias": (0.0, 0.0, -0.7)},
    "east": SIDE,
    "north": {**SIDE, "face_turn": 0.0, "sword_yaw": -92.0, "arm_in": -30.0, "arm_back": 18.0, "twist": 0.15,
              "carry_direction": (-0.14, 0.78, -0.61),
              "ahead_bias": (0.45, 0.0, 0.1)},
    # a real left-facing render, not a mirrored east: he stays right-handed and his cuts keep travelling
    # the way the game's slash arc does
    "west": {**SIDE, "face_turn": 46.0},
}


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    p = argparse.ArgumentParser()
    p.add_argument("puppet")
    p.add_argument("out_dir")
    p.add_argument("--action", required=True)
    p.add_argument("--frames", type=int)
    p.add_argument("--views", default="south,east,north,west")
    p.add_argument("--source", help="static GLB, only needed when puppet.blend has to be built")
    p.add_argument("--cell", type=int, default=80)
    p.add_argument("--height-px", type=float, default=68.0)
    p.add_argument("--foot-px", type=float, default=73.0)
    p.add_argument("--elevation", type=float, default=15.0)
    p.add_argument("--canvas", type=int, default=3,
                   help="photograph a square this many cells wide with the cell centred, so the builder "
                        "can see (and slide back in) a blade that reaches past the cell, and so a trail "
                        "has room around him")
    p.add_argument("--trail", action="store_true", help="also photograph the blade trail of a swing")
    p.add_argument("--armed", action="store_true", help="show the separate sword in memory; never save over the editable source")
    return p.parse_args(argv)


def frame_posers(action, n, view):
    """-> (one poser per frame, feet planted on the rest floor, contact effect present)."""
    if action in locomotion.FRAMES:
        return locomotion.poses(action, n or locomotion.FRAMES[action], view), False, False
    for module in (attacks, reactions):
        if action in module.FRAMES:
            n = n or module.FRAMES[action]
            return module.poses(action, n, view), channels.PLANTED, action in trail.CUTS or action == "slash1"
    known = sorted({**locomotion.FRAMES, **attacks.FRAMES, **reactions.FRAMES})
    raise SystemExit(f"unknown action {action}; known: {known}")


def pixel_mask_dot(scene, arm):
    """A flat decal keeps the existing red dot from falling between native pixels; no raised nose."""
    mask = scene.objects.get("RainorayMask")
    if mask is None or mask.hide_render or scene.objects.get("RainorayMaskDot") is not None:
        return
    # shortcut: anchored to the current mask surface; recompute from UVs if the mask is reshaped.
    mesh = bpy.data.meshes.new("MaskDot")
    vertices = [(puppet.CX + 0.027 * math.cos(i * math.tau / 8), 0.017,
                 0.469 + 0.027 * math.sin(i * math.tau / 8)) for i in range(8)]
    mesh.from_pydata(vertices, [], [tuple(range(8))])
    dot = bpy.data.objects.new("RainorayMaskDot", mesh)
    scene.collection.objects.link(dot)
    material = bpy.data.materials.new("MaskRed")
    material.diffuse_color = trail.linear((224, 48, 64))
    mesh.materials.append(material)
    dot.parent = arm
    dot.vertex_groups.new(name="head").add(list(range(8)), 1, "REPLACE")
    dot.modifiers.new("Armature", "ARMATURE").object = arm


def main():
    a = parse_args()
    if not os.path.exists(a.puppet):
        if not a.source:
            raise SystemExit(a.puppet + " is missing; pass --source model.glb to build it")
        puppet.build(a.source, a.puppet)
    bpy.ops.wm.open_mainfile(filepath=a.puppet)
    scene = bpy.context.scene
    arm = next(o for o in scene.objects if o.type == "ARMATURE")
    pixel_mask_dot(scene, arm)
    sword = scene.objects.get("RainoraySword")
    if a.armed and sword is not None:
        sword.hide_render = False
        sword.hide_set(False)
    body_meshes = [o for o in scene.objects if o.type == "MESH" and not o.hide_render]
    p = puppet.Poser(arm)
    render_frames.setup_render(scene, a.cell, a.canvas)

    for facing in a.views.split(","):
        posers, planted, has_trail = frame_posers(a.action, a.frames, VIEWS[facing])
        p.begin()
        rest_floor = p.lowest_foot()
        snaps, floor = [], 1e9
        for pose in posers:                       # pass 1: pose everything, find this action's floor
            pose(p)
            snaps.append(p.snapshot())
            floor = min(floor, p.lowest_foot())
        if planted:                               # IK feet stand on the idle's floor; a heel raise must not move it
            floor = rest_floor
        render_frames.place_camera(scene, facing, puppet.REST_HEIGHT, floor - puppet.SOLE,
                                   (puppet.CX, puppet.CY), a.cell, a.height_px, a.foot_px, a.elevation, a.canvas)
        shots = os.path.join(a.out_dir, f"frames-{a.action}-{facing}")
        os.makedirs(shots, exist_ok=True)
        for i, snap in enumerate(snaps):          # pass 2: photograph
            p.restore(snap)
            scene.render.filepath = os.path.join(shots, f"{facing}_{i:02d}.png")
            bpy.ops.render.render(write_still=True)
        if a.trail and has_trail and (sword is None or not sword.hide_render):
            trail.render(scene, p, body_meshes, snaps, shots, facing, a.action)
        print(f"ANIMATE ok action={a.action} facing={facing} frames={len(snaps)} floor={floor:.3f}")


if __name__ == "__main__":
    main()
