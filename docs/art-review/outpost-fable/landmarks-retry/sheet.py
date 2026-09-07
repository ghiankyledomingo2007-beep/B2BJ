"""Contact sheets + stats. Usage:
  python3 sheet.py stats <png>...            -> size, opaque count/%, edge contact, green/gold/red-orange counts
  python3 sheet.py sheet <out.png> <png>...  -> 4x nearest-neighbour row on navy, labelled by filename
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

NAVY = (24, 30, 46, 255)


def stats(path):
    im = Image.open(path).convert("RGBA")
    w, h = im.size
    px = im.load()
    opaque = green = gold = redorange = 0
    edges = [0, 0, 0, 0]  # top bottom left right
    for y in range(h):
        for x in range(w):
            r, g, b, a = px[x, y]
            if a < 128:
                continue
            opaque += 1
            if y == 0: edges[0] += 1
            if y == h - 1: edges[1] += 1
            if x == 0: edges[2] += 1
            if x == w - 1: edges[3] += 1
            if g > 90 and g > r + 25 and g > b + 25: green += 1
            if r > 170 and g > 130 and b < 90: gold += 1
            if r > 170 and g < 120 and b < 80: redorange += 1
    return dict(file=Path(path).name, size=f"{w}x{h}", opaque=opaque, pct=round(100 * opaque / (w * h), 1),
                edges=edges, green=green, gold=gold, redorange=redorange)


def sheet(out, paths, scale=4):
    ims = [Image.open(p).convert("RGBA") for p in paths]
    gap = 8 * scale
    W = sum(im.width * scale for im in ims) + gap * (len(ims) + 1)
    H = max(im.height * scale for im in ims) + gap * 2 + 14
    canvas = Image.new("RGBA", (W, H), NAVY)
    d = ImageDraw.Draw(canvas)
    x = gap
    for p, im in zip(paths, ims):
        big = im.resize((im.width * scale, im.height * scale), Image.NEAREST)
        canvas.alpha_composite(big, (x, gap + 14))
        d.text((x, 2), Path(p).name[:40], fill=(230, 230, 230, 255))
        x += big.width + gap
    canvas.save(out)


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "stats":
        for p in sys.argv[2:]:
            print(stats(p))
    else:
        sheet(sys.argv[2], sys.argv[3:])
