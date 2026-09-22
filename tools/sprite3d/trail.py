"""Blade trails: the swoosh a fast cut leaves behind the sword. Runs inside Blender (animate.py --trail).

The trail is a SEPARATE layer the game draws over the character sprite, so it can be restyled or switched
off without touching him. It is not drawn: the puppet knows where the blade is at every instant, so the
path the blade sweeps between sheet frames is sampled, skinned into a ribbon from part-way up the blade to
its tip, and photographed by the same camera as the sprite. It therefore sits exactly on the sword in every
facing, flattens correctly under the 3/4 camera (a level cut is a squashed ellipse, an overhead cut a full
arc), and the body hides the part that passes behind him: he is photographed with it, in pure black, and
build_sheets.py turns black back into transparency.

Only fast movement leaves a trail, and it ages over a few frames: white at the blade, then pale blue, then
deep blue, narrowing toward the tip as it fades.
"""
import os

import bpy

SUBSTEPS = 8             # blade samples between two sheet frames; the wind-up-to-contact swing is one frame
MIN_SPEED = 0.30         # world units of tip travel per sheet frame (~11 px); slower moves leave nothing
ROOT = 0.30              # a fresh trail starts this far up the blade...
ROOT_AGED = 0.94         # ...and an old one has narrowed to this: a crescent, fat at the blade, a point at its tail
TAPER = 0.7              # < 1 narrows quickly at first, so even a one-frame swing reads as a crescent, not a band
# (oldest age in sheet frames, colour) - sRGB, the three tones build_sheets.py snaps the photo to
BANDS = [(0.4, (236, 250, 255)), (1.0, (120, 200, 255)), (1.9, (40, 110, 230))]
BODY = (0.0, 0.0, 0.0, 1.0)


def linear(srgb):
    """Blender's object colours are scene-linear; the render comes out in sRGB."""
    return tuple(((c / 255 + 0.055) / 1.055) ** 2.4 if c > 10 else c / 255 / 12.92 for c in srgb) + (1.0,)


def sample(p, pose, frames):
    """-> [(time in sheet frames, blade hilt end, blade tip)] over the whole action."""
    out = []
    for k in range((frames - 1) * SUBSTEPS + 1):
        pose(p, k / SUBSTEPS)
        out.append((k / SUBSTEPS, p.head("blade"), p.tail("blade")))
    return out


def ribbons(samples, frame, window, root=ROOT):
    """-> one quad list per colour band for this sheet frame. Only blade travel inside `window`
    (first, last sheet frame of the strike) counts: a fast wind-up is not a cut."""
    span = BANDS[-1][0]
    quads = [[] for _ in BANDS]

    def start(age):                          # how far up the blade the ribbon begins at this age
        return root + (ROOT_AGED - root) * min(max(age, 0.0) / span, 1.0) ** TAPER

    for (t0, hilt0, tip0), (t1, hilt1, tip1) in zip(samples, samples[1:]):
        age = frame - t1
        if t1 > frame + 1e-6 or age >= span or (tip1 - tip0).length * SUBSTEPS < MIN_SPEED:
            continue
        if t0 < window[0] - 1e-6 or t1 > window[1] + 1e-6:
            continue
        band = next(i for i, (oldest, _) in enumerate(BANDS) if age < oldest)
        quads[band].append((hilt0.lerp(tip0, start(frame - t0)), tip0, tip1, hilt1.lerp(tip1, start(age))))
    return quads


def build(quads, colour, name):
    mesh = bpy.data.meshes.new(name)
    verts = [tuple(v) for quad in quads for v in quad]
    mesh.from_pydata(verts, [], [tuple(range(i, i + 4)) for i in range(0, len(verts), 4)])
    ob = bpy.data.objects.new(name, mesh)
    ob.color = linear(colour)
    bpy.context.scene.collection.objects.link(ob)
    return ob


def render(scene, p, body_mesh, snaps, samples, shots, facing, window, root=ROOT):
    """Photograph every sheet frame's trail, with the posed body in black as an occluder."""
    shading = scene.display.shading
    shading.color_type = "OBJECT"
    body_mesh.color = BODY
    for i, snap in enumerate(snaps):
        p.restore(snap)
        made = [build(quads, colour, f"trail{b}") for b, (quads, (_, colour)) in
                enumerate(zip(ribbons(samples, i, window, root), BANDS)) if quads]
        scene.render.filepath = os.path.join(shots, f"trail_{facing}_{i:02d}.png")
        bpy.ops.render.render(write_still=True)
        for ob in made:
            mesh = ob.data
            bpy.data.objects.remove(ob)
            bpy.data.meshes.remove(mesh)
    shading.color_type = "TEXTURE"
