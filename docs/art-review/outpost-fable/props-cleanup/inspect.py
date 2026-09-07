"""Review helpers: zoom one PNG, report edge/alpha/color stats, build original-vs-candidate contact sheets.

Usage:
  python3 inspect.py zoom <png> <out.png> [scale]
  python3 inspect.py stats <png>...
  python3 inspect.py contact <out.png> <orig1> <cand1> [<orig2> <cand2> ...]
"""
import sys
from pathlib import Path
from PIL import Image

BG = (120, 60, 160, 255)  # magenta ground so true alpha is visible


def load(path):
    return Image.open(path).convert("RGBA")


def zoom(path, out, scale=4):
    im = load(path)
    sheet = Image.new("RGBA", (im.width * scale + 16, im.height * scale + 16), BG)
    sheet.alpha_composite(im.resize((im.width * scale, im.height * scale), Image.NEAREST), (8, 8))
    sheet.save(out)


def stats(path):
    im = load(path)
    w, h = im.size
    px = im.load()
    data = list(im.getdata())
    opaque = sum(1 for p in data if p[3] > 0)
    partial = sum(1 for p in data if 0 < p[3] < 255)
    edges = {"top": sum(px[x, 0][3] > 0 for x in range(w)), "bottom": sum(px[x, h - 1][3] > 0 for x in range(w)),
             "left": sum(px[0, y][3] > 0 for y in range(h)), "right": sum(px[w - 1, y][3] > 0 for y in range(h))}
    green = sum(1 for p in data if p[3] > 0 and p[1] > p[0] + 25 and p[1] > p[2] + 25)
    gold = sum(1 for p in data if p[3] > 0 and p[0] > 170 and p[1] > 130 and p[2] < 90)
    hot = sum(1 for p in data if p[3] > 0 and p[0] > 180 and p[1] < 140 and p[2] < 90)
    near_black_opaque = sum(1 for p in data if p[3] > 0 and max(p[:3]) < 12)
    print(f"{path}: {w}x{h} opaque={opaque} ({100 * opaque / (w * h):.1f}%) partial-alpha={partial} "
          f"edges={edges} green={green} gold={gold} hot(red-orange)={hot} near-black={near_black_opaque}")


def contact(out, pairs, scale=4, pad=8):
    rows = []
    for orig, cand in pairs:
        a, b = load(orig), load(cand) if Path(cand).exists() else None
        rows.append((a, b))
    width = max(a.width * scale + (b.width * scale if b else 0) for a, b in rows) + pad * 3
    height = sum(max(a.height, b.height if b else 0) * scale + pad for a, b in rows) + pad
    sheet = Image.new("RGBA", (width, height), BG)
    y = pad
    for a, b in rows:
        row_h = max(a.height, b.height if b else 0) * scale
        up = a.resize((a.width * scale, a.height * scale), Image.NEAREST)
        sheet.alpha_composite(up, (pad, y + row_h - up.height))
        if b:
            upb = b.resize((b.width * scale, b.height * scale), Image.NEAREST)
            sheet.alpha_composite(upb, (pad * 2 + up.width, y + row_h - upb.height))
        y += row_h + pad
    sheet.save(out)


if __name__ == "__main__":
    mode = sys.argv[1]
    if mode == "zoom":
        zoom(sys.argv[2], sys.argv[3], int(sys.argv[4]) if len(sys.argv) > 4 else 4)
    elif mode == "stats":
        for p in sys.argv[2:]:
            stats(p)
    elif mode == "contact":
        rest = sys.argv[3:]
        contact(sys.argv[2], list(zip(rest[0::2], rest[1::2])))
