"""Blender side of the 3D-to-pixel pipeline: render an animated GLB to tiny unfiltered frames.

Usage:
  blender -b --python render_frames.py -- model.glb out_dir [--dir east] [--frames 12]
                                          [--cell 80] [--height-px 66] [--elevation 15]

The game stays 2D. This only photographs a rigged puppet with an orthographic camera,
one 80x80 picture per animation pose. post_pixel.py then snaps colours and packs the sheet.
"""
import argparse
import math
import os
import sys

import bpy
from mathutils import Vector

# Screen-right must equal the direction the character travels for east, and so on.
# glTF characters face -Y after import; azimuth is the camera's angle around Z.
AZIMUTH = {"east": 180.0, "south": 270.0, "west": 0.0, "north": 90.0,
           "southeast": 225.0, "northeast": 135.0, "northwest": 45.0, "southwest": 315.0}


def parse_args():
    argv = sys.argv[sys.argv.index("--") + 1:] if "--" in sys.argv else []
    p = argparse.ArgumentParser()
    p.add_argument("glb")
    p.add_argument("out_dir")
    p.add_argument("--dir", default="east", choices=sorted(AZIMUTH))
    p.add_argument("--frames", type=int, default=12)
    p.add_argument("--cell", type=int, default=80)
    p.add_argument("--height-px", type=float, default=66.0)
    p.add_argument("--foot-px", type=float, default=73.0)
    p.add_argument("--elevation", type=float, default=15.0)
    return p.parse_args(argv)


def mesh_bounds(objects, frames):
    """World-space bounds of every mesh over every sampled frame."""
    lo, hi = Vector((1e9,) * 3), Vector((-1e9,) * 3)
    deps = bpy.context.evaluated_depsgraph_get()
    for f in frames:
        bpy.context.scene.frame_set(f)
        deps.update()
        for ob in objects:
            ev = ob.evaluated_get(deps)
            for corner in ev.bound_box:
                w = ev.matrix_world @ Vector(corner)
                lo = Vector(map(min, lo, w))
                hi = Vector(map(max, hi, w))
    return lo, hi


def place_camera(scene, direction, height, ground, centre_xy, cell=80, height_px=66.0, foot_px=73.0,
                 elevation=15.0, canvas=1):
    """Orthographic camera for one facing. `height` world units become `height_px` pixels and
    `ground` lands on pixel row `foot_px`, so every action of a puppet shares one scale and floor.
    canvas=2 photographs a square twice the cell with the cell centred in it: same pixels, plus a
    margin that shows how far a long weapon reaches past the cell instead of silently cutting it."""
    cam = scene.objects.get("sprite_cam")
    if cam is None:
        cam = bpy.data.objects.new("sprite_cam", bpy.data.cameras.new("sprite_cam"))
        scene.collection.objects.link(cam)
    cam.data.type = "ORTHO"
    # ortho_scale is the world width of the frame; cell px / height px converts it
    cam.data.ortho_scale = height * cell / height_px * canvas
    scene.camera = cam
    az, el = math.radians(AZIMUTH[direction]), math.radians(elevation)
    # put the ground line on foot_px: cell centre is cell/2 px above the bottom edge
    world_per_px = height / height_px
    target = Vector((centre_xy[0], centre_xy[1], ground + (foot_px - cell / 2) * world_per_px))
    offset = Vector((math.cos(az) * math.cos(el), math.sin(az) * math.cos(el), math.sin(el)))
    cam.location = target + offset * height * 10
    cam.rotation_euler = (target - cam.location).to_track_quat("-Z", "Y").to_euler()


def setup_render(scene, cell=80, canvas=1):
    scene.render.engine = "BLENDER_WORKBENCH"
    sh = scene.display.shading
    sh.light = "FLAT"
    sh.color_type = "TEXTURE"
    scene.display.render_aa = "OFF"      # no smoothing: every pixel is one decided colour
    scene.render.filter_size = 0.01
    scene.render.film_transparent = True
    scene.render.resolution_x = scene.render.resolution_y = cell * canvas
    scene.render.resolution_percentage = 100
    scene.render.image_settings.file_format = "PNG"
    scene.render.image_settings.color_mode = "RGBA"
    scene.view_settings.view_transform = "Standard"


def main():
    a = parse_args()
    if a.glb.endswith(".blend"):             # a rig saved by rig_rainoray.py
        bpy.ops.wm.open_mainfile(filepath=a.glb)
    else:
        bpy.ops.wm.read_factory_settings(use_empty=True)
        bpy.context.scene.render.fps = 20
        bpy.ops.import_scene.gltf(filepath=a.glb)
    scene = bpy.context.scene
    arm = next((o for o in scene.objects if o.type == "ARMATURE"), None)
    bone_shapes = {b.custom_shape for b in arm.pose.bones} if arm else set()
    meshes = [o for o in scene.objects if o.type == "MESH" and not o.hide_render and o not in bone_shapes]
    if not meshes:
        raise SystemExit("no mesh in " + a.glb)

    start, end = 1, 1
    if arm and arm.animation_data and arm.animation_data.action:
        start, end = (int(round(v)) for v in arm.animation_data.action.frame_range)
    # a loop's last frame repeats its first, so sample [start, end) evenly
    span = max(end - start, 1)
    frames = [start + round(i * span / a.frames) for i in range(a.frames)]

    # Rest-height sets the scale so every action of this puppet shares one pixel size.
    scene.frame_set(start)
    rest_lo, rest_hi = mesh_bounds(meshes, [start])
    height = arm.get("sprite_rest_height", rest_hi.z - rest_lo.z) if arm else rest_hi.z - rest_lo.z
    lo, hi = mesh_bounds(meshes, frames)
    ground = arm.get("sprite_ground", lo.z) if arm else lo.z   # rig may pin the floor to the feet, not the sword tip

    centre_xy = ((rest_lo.x + rest_hi.x) / 2, (rest_lo.y + rest_hi.y) / 2)
    if arm and "sprite_centre_x" in arm:     # rig may pin the body centre so a long sword cannot shift it
        centre_xy = (arm["sprite_centre_x"], arm["sprite_centre_y"])
    place_camera(scene, a.dir, height, ground, centre_xy, a.cell, a.height_px, a.foot_px, a.elevation)
    setup_render(scene, a.cell)

    os.makedirs(a.out_dir, exist_ok=True)
    for i, f in enumerate(frames):
        scene.frame_set(f)
        scene.render.filepath = os.path.join(a.out_dir, f"{a.dir}_{i:02d}.png")
        bpy.ops.render.render(write_still=True)
    print(f"SPRITE3D ok dir={a.dir} frames={len(frames)} action={start}-{end} height={height:.3f}")


if __name__ == "__main__":
    main()
