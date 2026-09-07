"""Build forced-palette swatches for pixflux color_image_path from the accepted references,
dropping the grass-green tufts so the palette cannot reproduce a grass pedestal.
Usage: python3 palette.py <source.png> <out.png> [keep-green]
"""
import sys
from collections import Counter
from PIL import Image

src, out = sys.argv[1], sys.argv[2]
keep_green = len(sys.argv) > 3
img = Image.open(src).convert("RGBA")
counts = Counter(p[:3] for p in img.getdata() if p[3] > 200)


def is_grass(c):
    r, g, b = c
    return g > r + 12 and g > b + 12  # saturated green, not grey-green


colors = [c for c, n in counts.most_common() if n >= 3 and (keep_green or not is_grass(c))]
dropped = [c for c in counts if is_grass(c)]
print(f"{src}: {len(counts)} colours, kept {len(colors)}, dropped {len(dropped)} green")
sw = Image.new("RGBA", (len(colors) * 4, 4), (0, 0, 0, 0))
for i, c in enumerate(colors):
    for dx in range(4):
        for dy in range(4):
            sw.putpixel((i * 4 + dx, dy), c + (255,))
sw.save(out)
print("wrote", out, sw.size)
