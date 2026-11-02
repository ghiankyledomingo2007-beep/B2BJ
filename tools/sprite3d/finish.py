"""Finish mixamo.py photographs as game sprite cells: banded shade, inked overlaps, the outer outline.

  python3 tools/sprite3d/finish.py shots_dir --facing east --frames 8 --out strip.png [--wide]
                                   [--old assets/characters/blade/rainoray_run.png --row 1] [--ms 62]

flat x normal is the trailer's look at sprite size: the model's colours, snapped to the reviewed palette,
times three hard steps of one key light from his upper left (fixed on screen, so every facing is lit the
same way); the shadow step leans cool. depth: where a nearer part crosses a farther one, the farther pixel
of the edge takes a dark shade of its own colour, so an arm across the chest or a leg in front of a leg
reads as a shape of its own. Pale hair and the mask shade in gentler steps, so his silver hair stays silver
instead of turning into a grey hood. A lone pixel whose colour none of its four neighbours share, inside
a patch of one other colour, takes that colour (texture specks at this size are noise, not detail).
The approved red mask dot stays unshaded, then the 1 px outer outline the hand-made sprites have.
With --old, a side-by-side preview GIF (old row | new) comes with it.
"""
import argparse
from pathlib import Path

from PIL import Image

from build_sheets import CANVAS, CELL, MASK_RED, PALETTE, PREVIEW_BG, TRAIL_CELL
from post_pixel import ink_overlaps, outline, palette_from, polish_body

KEY = (-0.55, 0.62, 0.56)                     # camera space (right, up, toward the viewer): upper left, a bit front
BANDS, SHADES = (0.05, 0.45), (0.68, 0.86, 1.0)
PALE_SHADES = (0.84, 0.93, 1.0)               # silver hair and the ivory mask
COOL = (0.92, 0.96, 1.08)                     # the shadow step's tint
PREVIEW_SCALE = 4


def window(wide):
    pad = CELL * (CANVAS - 1) // 2
    grow = (TRAIL_CELL - CELL) // 2 if wide else 0
    return pad - grow, pad - grow, pad + CELL + grow, pad + CELL + grow


def finish(flat, normal, depth, palette, cache, shade=True):
    w, h = flat.size
    flat = ink_overlaps(flat, depth)
    f, nm = flat.load(), normal.load()
    kl = sum(v * v for v in KEY) ** 0.5
    key = [v / kl for v in KEY]
    solid = [[f[x, y][3] >= 128 for y in range(h)] for x in range(w)]
    out = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    o = out.load()
    for y in range(h):
        for x in range(w):
            if not solid[x][y]:
                continue
            r, g, b, _ = f[x, y]
            if (r, g, b) not in cache:
                cache[(r, g, b)] = min(palette, key=lambda c: (c[0] - r) ** 2 + (c[1] - g) ** 2 + (c[2] - b) ** 2)
            base = cache[(r, g, b)]
            if base == MASK_RED:
                o[x, y] = (*base, 255)
                continue
            n = [c / 127.5 - 1 for c in nm[x, y][:3]]
            length = sum(v * v for v in n) ** 0.5 or 1.0
            step = sum(sum(v * k for v, k in zip(n, key)) / length >= edge for edge in BANDS)
            pale = min(base) > 150 and base[2] >= base[0]
            k = (PALE_SHADES if pale else SHADES)[step] if shade else 1.0
            tint = COOL if step == 0 and shade else (1.0, 1.0, 1.0)
            o[x, y] = tuple(min(255, round(c * k * t)) for c, t in zip(base, tint)) + (255,)
    return outline(polish_body(despeckle(out)))


def despeckle(im):
    out = im.copy()
    src, dst = im.load(), out.load()
    w, h = im.size
    for y in range(1, h - 1):
        for x in range(1, w - 1):
            mine = src[x, y]
            if not mine[3] or mine[:3] == MASK_RED:
                continue
            if any(src[i, j] == mine for i, j in ((x + 1, y), (x - 1, y), (x, y + 1), (x, y - 1))):
                continue
            around = [src[i, j] for i in (x - 1, x, x + 1) for j in (y - 1, y, y + 1) if (i, j) != (x, y)]
            best = max(set(around), key=around.count)
            if best[3] and around.count(best) >= 5:
                dst[x, y] = best
    return out


def main():
    a = argparse.ArgumentParser()
    a.add_argument("shots")
    a.add_argument("--facing", default="east")
    a.add_argument("--frames", type=int, required=True)
    a.add_argument("--out", required=True)
    a.add_argument("--wide", action="store_true", help="160 px cells, as build_sheets.WIDE sheets")
    a.add_argument("--old", help="current game sheet, for the side-by-side preview")
    a.add_argument("--row", type=int, default=1, help="row of that sheet to compare (east = 1)")
    a.add_argument("--ms", type=int, default=62)
    a.add_argument("--flat", action="store_true", help="the game's flat colours, inked overlaps only")
    args = a.parse_args()

    shots, box = Path(args.shots), window(args.wide)
    palette, cache = palette_from(PALETTE), {}
    cells = []
    for i in range(args.frames):
        passes = [Image.open(shots / f"{k}_{args.facing}_{i:02d}.png").convert("RGBA").crop(box)
                  for k in ("flat", "normal", "depth")]
        cells.append(finish(*passes, palette, cache, not args.flat))
    size = cells[0].width
    strip = Image.new("RGBA", (size * len(cells), size), (0, 0, 0, 0))
    for i, c in enumerate(cells):
        strip.alpha_composite(c, (i * size, 0))
    strip.save(args.out)
    if args.old:
        old = Image.open(args.old).convert("RGBA")
        frames = []
        for i, c in enumerate(cells):
            before = old.crop((i * size, args.row * size, (i + 1) * size, (args.row + 1) * size))
            pair = Image.new("RGBA", (size * 2 + 8, size), PREVIEW_BG)
            pair.alpha_composite(before, (0, 0))
            pair.alpha_composite(c, (size + 8, 0))
            frames.append(pair.resize((pair.width * PREVIEW_SCALE, pair.height * PREVIEW_SCALE), Image.NEAREST)
                          .convert("P", palette=Image.ADAPTIVE))
        gif = Path(args.out).with_suffix(".gif")
        frames[0].save(gif, save_all=True, append_images=frames[1:], duration=args.ms, loop=0, disposal=2)
        print("preview", gif)
    print("strip", args.out, strip.size)


if __name__ == "__main__":
    main()
