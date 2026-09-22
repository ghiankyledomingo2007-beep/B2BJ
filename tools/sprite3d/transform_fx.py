"""The slime <-> human transformation sheets, rebuilt around the puppet's idle pose.

The game swaps the player sprite for these sheets for TransformationAnimation.DURATION (0.64 s, 16 frames,
one row per facing) and then starts the idle at frame 0, so the human end of the morph has to BE idle
frame 0. The slime end keeps the slime's own reviewed drawings: only the frames that showed the old
human are replaced.

  in : slime stretches up (kept) -> the human shape rises out of it as teal glass -> the glass clears
  out: the same, backwards, handing over to the kept slime frames

Pure 2D compositing (Pillow); runs after the idle sheet exists. CombatArtTest needs 16 different frames.
"""
import random
from pathlib import Path

from PIL import Image, ImageFilter

CELL, FRAMES, FOOT_ROW = 80, 16, 73
KEPT_IN = 4                      # transform_in frames 0..3 are pure slime art
KEPT_OUT = 12                    # transform_out frames 12..15 are pure slime art
# human end of the morph, by how far the teal glass has cleared (in) or formed (out)
CLEAR = [0.75, 0.55, 0.40, 0.28, 0.18, 0.10, 0.05, 0.0]
FORM = [0.05, 0.12, 0.22, 0.35, 0.50, 0.68, 0.85]
# (width, height, how much of his glass detail shows) as the column of slime rises into his shape
# The kept slime drawings end (in) / begin (out) as a tall bean about 0.6 of his height, so the column
# starts there instead of dropping to a puddle first.
RISE = [(1.30, 0.62, 0.0), (1.20, 0.74, 0.0), (1.10, 0.88, 0.35), (1.0, 1.0, 0.7)]
SINK = [(1.10, 0.88, 0.35), (1.20, 0.74, 0.0), (1.30, 0.62, 0.0), (1.38, 0.52, 0.0)]
GOO_BANDS = 4                    # few flat tones, like the slime's own shading, not a striped gradient


def cell(sheet, row, frame):
    return sheet.crop((frame * CELL, row * CELL, (frame + 1) * CELL, (row + 1) * CELL))


def teal_ramp(slime_cells):
    """The slime's own body colours, dark to light, so the glass is made of the same stuff.
    Only its teals: the gold star on its belly would turn his face and hair yellow."""
    colours = {p[:3] for c in slime_cells for p in c.getdata()
               if p[3] > 200 and p[1] > p[0] + 20 and p[2] > p[0] + 20}
    if len(colours) < 3:
        raise SystemExit("slime morph source has no teal body colours to build the glass from")
    return sorted(colours, key=lambda c: 0.3 * c[0] + 0.59 * c[1] + 0.11 * c[2])


def glass(sprite, ramp, amount):
    """Tint the sprite toward slime-teal by luminance, so its shading survives as glass."""
    out = sprite.copy()
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            lum = (0.3 * r + 0.59 * g + 0.11 * b) / 255
            tr, tg, tb = ramp[min(int(lum * len(ramp)), len(ramp) - 1)]
            px[x, y] = (round(r + (tr - r) * amount), round(g + (tg - g) * amount),
                        round(b + (tb - b) * amount), a)
    return out


def squash(sprite, wide, tall):
    """Scale about the point he stands on, so the shape grows out of the slime instead of floating."""
    box = sprite.getbbox()
    body = sprite.crop(box)
    size = (max(1, round(body.width * wide)), max(1, round(body.height * tall)))
    body = body.resize(size, Image.NEAREST)
    out = Image.new("RGBA", sprite.size, (0, 0, 0, 0))
    centre = (box[0] + box[2]) // 2
    out.alpha_composite(body, (max(0, min(CELL - size[0], centre - size[0] // 2)), max(0, box[3] - size[1])))
    return out


def goo(sprite, ramp, wide, tall, detail):
    """A rising column of slime in his outline. Opening the mask melts away the sword and fingers, and
    the body is shaded like the slime (light crown, darker base, dark rim). `detail` lets his glass
    features show through as the column reaches his height."""
    shape = squash(sprite, wide, tall)
    mask = shape.getchannel("A").point(lambda a: 255 if a else 0)
    mask = mask.filter(ImageFilter.MinFilter(5)).filter(ImageFilter.MaxFilter(5))
    box = mask.getbbox()
    out = Image.new("RGBA", sprite.size, (0, 0, 0, 0))
    if not box:
        return shape
    px, src, inside = out.load(), shape.load(), mask.load()
    inner = ramp[1:-1] or ramp
    body = [inner[round(i * (len(inner) - 1) / (GOO_BANDS - 1))] for i in range(GOO_BANDS)]
    for y in range(box[1], box[3]):
        tone = body[min(int((box[3] - 1 - y) / max(box[3] - box[1], 1) * len(body)), len(body) - 1)]
        for x in range(box[0], box[2]):
            if not inside[x, y]:
                continue
            rim = any(not (0 <= x + dx < CELL and 0 <= y + dy < CELL and inside[x + dx, y + dy])
                      for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)))
            colour = ramp[0] if rim else tone
            if detail and src[x, y][3] and not rim:
                colour = tuple(round(c + (s - c) * detail) for c, s in zip(colour, src[x, y][:3]))
            px[x, y] = (*colour, 255)
    return out


def sparkle(sprite, ramp, seed, age):
    """A few motes of slime drifting up off him; they also keep neighbouring frames different."""
    out = sprite.copy()
    box = sprite.getbbox()
    rng = random.Random(seed)
    for _ in range(7):
        x = min(max(rng.randint(box[0], box[2] - 1), 2), CELL - 3)
        y = rng.randint(box[1], box[3] - 1) - 3 * age - rng.randint(0, 6)
        if 2 <= y < CELL - 2 and age < 7 and not sprite.getpixel((x, y))[3]:
            out.putpixel((x, y), (*ramp[-1 - (age // 2) % 3], 255))
    return out


def build(idle_sheet, slime_in, slime_out, out_dir):
    idle = Image.open(idle_sheet).convert("RGBA")
    old_in, old_out = Image.open(slime_in).convert("RGBA"), Image.open(slime_out).convert("RGBA")
    rows = idle.height // CELL
    sheet_in = Image.new("RGBA", (CELL * FRAMES, CELL * rows), (0, 0, 0, 0))
    sheet_out = Image.new("RGBA", (CELL * FRAMES, CELL * rows), (0, 0, 0, 0))
    for row in range(rows):
        human = cell(idle, row, 0)
        ramp = teal_ramp([cell(old_in, row, i) for i in range(KEPT_IN)])
        full = glass(human, ramp, 1.0)

        frames = [cell(old_in, row, i) for i in range(KEPT_IN)]
        frames += [goo(full, ramp, w, t, d) for w, t, d in RISE]
        frames += [sparkle(glass(human, ramp, k), ramp, row * 31, i) for i, k in enumerate(CLEAR)]
        frames[-1] = human                                   # the morph ends ON idle frame 0
        for i, f in enumerate(frames):
            sheet_in.alpha_composite(f, (i * CELL, row * CELL))

        frames = [sparkle(glass(human, ramp, k), ramp, row * 17, 6 - i) for i, k in enumerate(FORM)]
        frames += [full] + [goo(full, ramp, w, t, d) for w, t, d in SINK]
        frames += [cell(old_out, row, i) for i in range(KEPT_OUT, FRAMES)]
        for i, f in enumerate(frames):
            sheet_out.alpha_composite(f, (i * CELL, row * CELL))
    out_dir = Path(out_dir)
    sheet_in.save(out_dir / "transform_in.png")
    sheet_out.save(out_dir / "transform_out.png")
    return sheet_in, sheet_out
