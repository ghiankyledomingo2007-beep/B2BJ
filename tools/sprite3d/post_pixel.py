"""Pixel side of the 3D-to-pixel pipeline: snap rendered frames to the character palette,
add the dark sprite outline, pack a horizontal sheet and a preview GIF.

Usage: python3 post_pixel.py frames_dir palette.png [palette2.png ...] --out sheet.png
"""
import argparse
import glob
import os

from PIL import Image

ALPHA_CUT = 128          # render has no AA, but keep a hard threshold anyway
OUTLINE = (12, 16, 40, 255)
PREVIEW_SCALE = 4
PREVIEW_BG = (70, 70, 70, 255)


def palette_from(paths, min_pixels=3):
    counts = {}
    for p in paths:
        im = Image.open(p).convert("RGBA")
        for r, g, b, a in im.getdata():
            if a >= ALPHA_CUT:
                counts[(r, g, b)] = counts.get((r, g, b), 0) + 1
    return [c for c, n in counts.items() if n >= min_pixels]


def nearest(colour, palette, cache):
    if colour not in cache:
        r, g, b = colour
        cache[colour] = min(palette, key=lambda c: (c[0] - r) ** 2 + (c[1] - g) ** 2 + (c[2] - b) ** 2)
    return cache[colour]


def snap(im, palette, cache):
    out = Image.new("RGBA", im.size, (0, 0, 0, 0))
    src, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            r, g, b, a = src[x, y]
            if a >= ALPHA_CUT:
                dst[x, y] = nearest((r, g, b), palette, cache) + (255,)
    return out


def outline(im):
    """One-pixel dark rim on the transparent side, like the hand-made sprites."""
    out = im.copy()
    src, dst = im.load(), out.load()
    for y in range(im.height):
        for x in range(im.width):
            if src[x, y][3]:
                continue
            for dx, dy in ((1, 0), (-1, 0), (0, 1), (0, -1)):
                nx, ny = x + dx, y + dy
                if 0 <= nx < im.width and 0 <= ny < im.height and src[nx, ny][3]:
                    dst[x, y] = OUTLINE
                    break
    return out


def main():
    p = argparse.ArgumentParser()
    p.add_argument("frames_dir")
    p.add_argument("palette", nargs="+")
    p.add_argument("--out", required=True)
    p.add_argument("--ms", type=int, default=60)
    a = p.parse_args()

    palette = palette_from(a.palette)
    files = sorted(glob.glob(os.path.join(a.frames_dir, "*.png")))
    if not files or not palette:
        raise SystemExit("no frames or empty palette")
    cache = {}
    frames = [outline(snap(Image.open(f).convert("RGBA"), palette, cache)) for f in files]

    w, h = frames[0].size
    sheet = Image.new("RGBA", (w * len(frames), h), (0, 0, 0, 0))
    for i, f in enumerate(frames):
        sheet.alpha_composite(f, (i * w, 0))
    sheet.save(a.out)

    big = []
    for f in frames:
        bg = Image.new("RGBA", f.size, PREVIEW_BG)
        bg.alpha_composite(f)
        big.append(bg.resize((w * PREVIEW_SCALE, h * PREVIEW_SCALE), Image.NEAREST).convert("P"))
    gif = os.path.splitext(a.out)[0] + ".gif"
    big[0].save(gif, save_all=True, append_images=big[1:], duration=a.ms, loop=0)
    print(f"sheet {a.out} {sheet.size} frames={len(frames)} colours={len(palette)} gif={gif}")


if __name__ == "__main__":
    main()
