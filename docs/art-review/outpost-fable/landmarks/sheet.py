"""Review helper: 4x nearest-neighbour contact sheet + transparency stats.
Usage: python3 sheet.py <out.png> <label=path> [label=path ...]
Each image is drawn at 4x on a dark navy ground; a 2x Slime idle frame (48x48 cell) is appended for scale.
Prints size, opaque %, and the near-black share of opaque pixels for every image.
"""
import sys
from pathlib import Path
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parents[4]
SLIME = ROOT / "assets/characters/slime/slime_idle.png"
GROUND = (28, 34, 46, 255)


def stats(img):
    px = list(img.convert("RGBA").getdata())
    opaque = sum(1 for (_, _, _, a) in px if a > 8)
    dark = sum(1 for (r, g, b, a) in px if a > 8 and r + g + b < 60)
    return opaque / len(px), (dark / opaque if opaque else 0)


def main():
    out = Path(sys.argv[1])
    items = [arg.split("=", 1) for arg in sys.argv[2:]]
    cells = []
    for label, path in items:
        img = Image.open(path).convert("RGBA")
        frac, dark = stats(img)
        print(f"{label}: {img.width}x{img.height} opaque={frac:.0%} near-black-of-opaque={dark:.0%}")
        big = img.resize((img.width * 4, img.height * 4), Image.NEAREST)
        cells.append((f"{label} {img.width}x{img.height} 4x", big))
    slime = Image.open(SLIME).convert("RGBA").crop((0, 0, 48, 48))
    cells.append(("slime 2x", slime.resize((96, 96), Image.NEAREST)))
    blade = Image.open(ROOT / "assets/characters/blade/rainoray_idle.png").convert("RGBA").crop((0, 0, 80, 80))
    cells.append(("rainoray 2x", blade.resize((160, 160), Image.NEAREST)))
    pad, lab = 12, 14
    w = sum(c.width + pad for _, c in cells) + pad
    h = max(c.height for _, c in cells) + pad * 2 + lab
    sheet = Image.new("RGBA", (w, h), GROUND)
    draw = ImageDraw.Draw(sheet)
    x = pad
    for label, c in cells:
        y = h - pad - c.height  # bottom-align so contact lines compare
        sheet.alpha_composite(c, (x, y))
        draw.text((x, 2), label, fill=(200, 200, 200, 255))
        x += c.width + pad
    sheet.save(out)
    print("wrote", out, sheet.size)


if __name__ == "__main__":
    main()
