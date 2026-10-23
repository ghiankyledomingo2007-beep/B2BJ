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

# Merge near-duplicate material shades; keep the existing hair, skin, gold and coat ramps.
RAINORAY_TONES = {
    **{c: OUTLINE[:3] for c in ((4, 4, 7), (7, 6, 6), (8, 8, 14), (10, 11, 12), (16, 16, 24), (27, 26, 34))},
    (41, 46, 102): (44, 51, 113), (53, 62, 130): (55, 66, 144),
    (194, 210, 226): (197, 214, 232), (181, 198, 236): (197, 214, 232),
    (206, 214, 226): (218, 224, 233), (206, 216, 212): (218, 224, 233),
    (244, 243, 233): (241, 243, 244), (241, 236, 231): (241, 243, 244),
    **{c: (80, 98, 112) for c in ((87, 109, 123), (84, 104, 119), (89, 103, 115), (73, 85, 99), (83, 84, 97))},
}


def polish_body(im):
    """Light palette cleanup, with one red pixel per visible mask mark. Alpha and geometry stay intact."""
    out = im.copy()
    px = out.load()
    red = []
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if not a:
                continue
            px[x, y] = (*RAINORAY_TONES.get((r, g, b), (r, g, b)), a)
            if (r, g, b) == (224, 48, 64):
                red.append((x, y))
    if len(red) > 1:
        cx, cy = (sum(p[i] for p in red) / len(red) for i in (0, 1))
        keep = min(red, key=lambda p: (p[0] - cx) ** 2 + (p[1] - cy) ** 2)
        for x, y in red:
            if (x, y) != keep:
                px[x, y] = (241, 243, 244, px[x, y][3])
    return out


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
