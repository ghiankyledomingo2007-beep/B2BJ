"""Top colours of the opaque pixels in the bottom band of an image (the ground/shadow region).
Usage: python3 colors.py <png> [rows-from-bottom]
"""
import sys
from collections import Counter
from PIL import Image

im = Image.open(sys.argv[1]).convert("RGBA")
rows = int(sys.argv[2]) if len(sys.argv) > 2 else im.height // 3
px = im.load()
c = Counter()
for y in range(im.height - rows, im.height):
    for x in range(im.width):
        r, g, b, a = px[x, y]
        if a >= 128:
            c[(r, g, b)] += 1
for col, n in c.most_common(12):
    print(col, n, "lum=%d" % int(0.3 * col[0] + 0.59 * col[1] + 0.11 * col[2]))
