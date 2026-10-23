"""Short contact crescents, photographed at native resolution with the body as an occluder.

Each cut has one fixed plane. Wrist corrections and recovery motion must not pull the effect into
loops around the character. Geometry is generated offline; the game still draws one sprite layer.
"""
import math
import os

import bpy
from mathutils import Vector

from puppet import CX, CY, body

# Tilt, centre height, radius, starting angle, ending angle, first/last contact frame.
# Angles sweep around the character's forward axis; the overhead uses a vertical plane.
CUTS = {
    "slash0": (1.1, -0.03, 1.2, -105, 100, 3, 5),
    "slash2": (-1.1, -0.03, 1.2, 105, -100, 3, 5),
    "slash3": (None, -0.02, 1.08, 110, -65, 3, 5),
    "cleave": (0.05, -0.25, 1.25, -110, 130, 3, 5),
    "dashslash": (-0.12, -0.40, 1.18, 105, -105, 3, 5),
    "spincleave": (0.0, -0.28, 1.25, -105, 465, 3, 8.5),
    "cast": (0.3, -0.15, 1.12, -100, 100, 0, 3),
    "counter": (-0.4, -0.15, 1.12, 100, -100, 0, 3),
}
BANDS = [(0.3, (236, 250, 255)), (0.75, (120, 200, 255)), (1.35, (40, 110, 230))]
BODY = (0.0, 0.0, 0.0, 1.0)


def linear(srgb):
    return tuple(((c / 255 + 0.055) / 1.055) ** 2.4 if c > 10 else c / 255 / 12.92 for c in srgb) + (1.0,)


def ribbons(action, frame, facing="east"):
    """Three flat colour bands; no effect before contact or during recovery."""
    quads = [[] for _ in BANDS]
    if action == "slash1":
        if 4 <= frame <= 6:
            # A thrust gets a straight narrow streak, never a circular slash.
            centre = Vector((CX, CY, -0.28))
            width = 0.09 * (7 - frame) / 3
            bias = 0.45 if facing == "north" else 0
            quads[frame - 4].append(tuple(centre + body(r + bias * f, f, z) for r, f, z in
                ((0, 0.35, 0), (-width, 1.15, -0.12), (0, 1.65, -0.18), (width, 1.15, -0.12))))
        return quads
    tilt, height, radius, start, end, first, last = CUTS[action]
    if frame <= first or frame >= last + BANDS[-1][0]:
        return quads
    centre = Vector((CX, CY - 0.12, height))
    across = body(0.4, 0, 1).normalized() if tilt is None else body(1, 0, tilt).normalized()
    forward = body(0, 1, 0)
    # Fixed angular spacing gives clean arcs even when the pose changes sharply between frames.
    steps = math.ceil(abs(end - start) / 4)
    for i in range(steps):
        t0, t1 = first + (last - first) * i / steps, first + (last - first) * (i + 1) / steps
        if t1 > frame or frame - t1 >= BANDS[-1][0]:
            continue
        vertices = []
        for t, inset in ((t0, True), (t0, False), (t1, False), (t1, True)):
            age = max(0, frame - t)
            width = 0.34 * max(0, 1 - age / BANDS[-1][0]) ** 0.7
            angle = math.radians(start + (end - start) * (t - first) / (last - first))
            direction = across * math.sin(angle) + forward * math.cos(angle)
            vertices.append(centre + direction * (radius - width if inset else radius))
        band = next(b for b, (oldest, _) in enumerate(BANDS) if frame - t1 < oldest)
        quads[band].append(tuple(vertices))
    return quads


def build(quads, colour, name):
    mesh = bpy.data.meshes.new(name)
    verts = [tuple(v) for quad in quads for v in quad]
    mesh.from_pydata(verts, [], [tuple(range(i, i + 4)) for i in range(0, len(verts), 4)])
    ob = bpy.data.objects.new(name, mesh)
    ob.color = linear(colour)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def render(scene, p, body_meshes, snaps, shots, facing, action):
    shading = scene.display.shading
    shading.color_type = "OBJECT"
    for mesh in body_meshes:
        mesh.color = BODY
    for i, snap in enumerate(snaps):
        p.restore(snap)
        made = [build(quads, colour, f"trail{b}") for b, (quads, (_, colour)) in
                enumerate(zip(ribbons(action, i, facing), BANDS)) if quads]
        scene.render.filepath = os.path.join(shots, f"trail_{facing}_{i:02d}.png")
        bpy.ops.render.render(write_still=True)
        for ob in made:
            mesh = ob.data
            bpy.data.objects.remove(ob)
            bpy.data.meshes.remove(mesh)
    shading.color_type = "TEXTURE"
