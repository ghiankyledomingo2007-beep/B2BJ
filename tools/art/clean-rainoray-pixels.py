"""Cluster cleanup for Rainoray's Blender-rendered body sheets.

  /usr/bin/python3 tools/art/clean-rainoray-pixels.py SOURCE_DIR FRESH_OUT_DIR

Reads the 14 runtime body sheets (rainoray_<action>.png) and writes cleaned copies:
render anti-alias tones snap to the neighbouring flat colour, lone specks join the
material around them, small dark smudges on the mask become its black markings,
comb-like back-hair strands are thinned and the back view gets a rounded crown edge.
Alpha, the red mask dot, the sword's gold and red hilt, frame layout and effects
sheets are never changed. One pass on the rendered pixels; import the result with
rainoray-project.lua and review it before installing.
"""
from collections import Counter
from pathlib import Path
import sys

from PIL import Image

INK, BLUE, DARK, CREASE = (12, 16, 40), (55, 66, 144), (44, 51, 113), (37, 38, 69)
HAIR, SHADE, WHITE, STRAND = (218, 224, 233), (197, 214, 232), (241, 243, 244), (80, 98, 112)
SKIN, GOLD, GOLD_LIGHT = (243, 222, 188), (200, 184, 97), (225, 204, 141)
RED, HILT_RED = (224, 48, 64), (190, 68, 61)
# In-between tones from the 3D render: cloth sheen, hair/steel glints, skin shadow.
TONES = {(110, 126, 164), (170, 180, 200), (155, 157, 160), (123, 155, 188),
         (147, 128, 103), (167, 147, 122), (197, 193, 181)}
KEEP = {RED, HILT_RED, GOLD, GOLD_LIGHT}
HILT = {GOLD, GOLD_LIGHT, HILT_RED}
MATERIALS = [(BLUE, DARK, CREASE), (HAIR, SHADE, WHITE, STRAND), (SKIN,)]
SHEETS = {"idle": 80, "run": 80, "start": 80, "stop": 80, "turn": 80, "charge": 80, "hold": 80,
          "guard": 80, "hurt": 80, "slash": 160, "cleave": 160, "dash": 160, "cast": 160, "counter": 160}
NORTH = 2                                    # facing rows: south, east, north, west
N4 = [(-1, 0), (1, 0), (0, -1), (0, 1)]
N8 = N4 + [(-1, -1), (1, -1), (-1, 1), (1, 1)]


def regions(px, w, h):
    """Same-colour 4-connected regions of opaque pixels."""
    seen = set()
    for y in range(h):
        for x in range(w):
            colour = px[y][x]
            if colour is None or (x, y) in seen:
                continue
            points, i = [(x, y)], 0
            seen.add((x, y))
            while i < len(points):
                a, b = points[i]
                i += 1
                for dx, dy in N4:
                    p = (a + dx, b + dy)
                    if 0 <= p[0] < w and 0 <= p[1] < h and p not in seen and px[p[1]][p[0]] == colour:
                        seen.add(p)
                        points.append(p)
            yield colour, points


def surroundings(px, w, h, points):
    """Colour counts of the 8-connected ring around a region, and whether it touches transparency."""
    own, ring, counts, clear = set(points), set(), Counter(), False
    for a, b in points:
        for dx, dy in N8:
            p = (a + dx, b + dy)
            if p in own or p in ring:
                continue
            ring.add(p)
            if not (0 <= p[0] < w and 0 <= p[1] < h) or px[p[1]][p[0]] is None:
                clear = True
            else:
                counts[px[p[1]][p[0]]] += 1
    return counts, clear


def flatten_tones(px, w, h):
    out = [row[:] for row in px]
    for y in range(h):
        for x in range(w):
            colour = px[y][x]
            if colour not in TONES:
                continue
            if any(px[y + dy][x + dx] in HILT for dy in range(-2, 3) for dx in range(-2, 3)
                   if 0 <= x + dx < w and 0 <= y + dy < h):
                continue                     # the hilt's metal shading is drawn on purpose
            counts = Counter(px[y + dy][x + dx] for dx, dy in N8 if 0 <= x + dx < w and 0 <= y + dy < h)
            flat = [c for c in counts if c is not None and c != INK and c not in TONES and c not in KEEP]
            if not flat:
                continue
            skin = sum(px[y + dy][x + dx] == SKIN for dx, dy in N4 if 0 <= x + dx < w and 0 <= y + dy < h)
            out[y][x] = SKIN if skin >= 2 else min(
                flat, key=lambda c: (sum((p - q) ** 2 for p, q in zip(colour, c)), -counts[c]))
    return out


def material(colour):
    return next((m for m in MATERIALS if colour in m), colour)


def cluster_sizes(px, w, h):
    """Size of each pixel's 8-connected same-material cluster: a long trim is many single-shade pieces."""
    size, seen = {}, set()
    for y in range(h):
        for x in range(w):
            if px[y][x] is None or (x, y) in seen:
                continue
            kind, points, i = material(px[y][x]), [(x, y)], 0
            seen.add((x, y))
            while i < len(points):
                a, b = points[i]
                i += 1
                for dx, dy in N8:
                    p = (a + dx, b + dy)
                    if (0 <= p[0] < w and 0 <= p[1] < h and p not in seen and px[p[1]][p[0]] is not None
                            and material(px[p[1]][p[0]]) == kind):
                        seen.add(p)
                        points.append(p)
            size.update(dict.fromkeys(points, len(points)))
    return size


def merge_specks(px, w, h):
    out, cluster = [row[:] for row in px], cluster_sizes(px, w, h)
    for colour, points in regions(px, w, h):
        limit = 4 if colour in TONES else 1 if colour == INK else 2
        if len(points) > limit or colour in KEEP:
            continue
        counts, clear = surroundings(px, w, h, points)
        if clear or colour in TONES and counts.keys() & HILT:
            continue
        opaque = sum(counts.values()) - counts[INK]
        if colour in (CREASE, DARK) and len(points) <= 3 and counts[WHITE] >= 0.8 * opaque:
            target = INK                     # a blurred mask marking becomes flat black again
        elif colour == INK and counts[WHITE] + counts[SKIN] + counts[RED]:
            continue                         # mask markings and drawn dark details stay
        else:
            choices = [(n, c) for c, n in counts.items() if c != INK and c not in KEEP]
            if not choices:
                continue
            n, target = max(choices)
            around, amount = max(((m, sum(counts[c] for c in m)) for m in MATERIALS), key=lambda q: q[1])
            if colour not in TONES and not counts[colour] and len(points) <= 2 and amount >= max(3, 0.6 * opaque):
                # No diagonal continuation, so not a fold or trim line: join the surrounding material.
                n, target = max((counts[c], c) for c in around if c != colour)
            elif n < (max(2, 0.4 * opaque) if colour in TONES else max(3, 0.6 * opaque)):
                continue
            if colour not in TONES and material(target) != material(colour) and cluster[points[0]] > 3:
                continue                     # part of a longer trim, seam or lock in another material
        for a, b in points:
            out[b][a] = target
    return out


def thin_strands(px, w, h):
    """One-pixel shadow strands only one pixel apart read as a barcode; keep the longer of each pair."""
    light = {WHITE, HAIR}

    def strand(x, y):
        return px[y][x] == SHADE and px[y][x - 1] in light and px[y][x + 1] in light

    runs = {}
    for x in range(1, w - 1):
        y = 0
        while y < h:
            if strand(x, y):
                top = y
                while y < h and strand(x, y):
                    y += 1
                runs.setdefault(x, []).append((top, y))
            else:
                y += 1
    drop = set()
    for x, spans in runs.items():
        for a0, a1 in spans:
            for b0, b1 in runs.get(x + 2, []):
                lo, hi = max(a0, b0), min(a1, b1)
                if hi - lo >= 2 and all(px[y][x + 1] in light for y in range(lo, hi)):
                    drop.add((x + 2, b0, b1) if a1 - a0 >= b1 - b0 else (x, a0, a1))
    out = [row[:] for row in px]
    for x, top, bottom in drop:
        for y in range(top, bottom):
            left, right = px[y][x - 1], px[y][x + 1]
            out[y][x] = left if left == right else HAIR
    return out


def round_crown(px, w, h):
    """Back view: a one-pixel shade down the hair's right edge keeps the flat crown round."""
    out = [row[:] for row in px]
    for y in range(h):
        for x in range(1, w - 1):
            if px[y][x] == HAIR and px[y][x + 1] == INK and px[y][x - 1] in (HAIR, SHADE):
                out[y][x] = SHADE
    return out


def clean(px, facing=0):
    """px: rows of RGB tuples, None for transparent. Returns a new grid; the input is untouched."""
    h, w = len(px), len(px[0])
    for _ in range(3):
        px = flatten_tones(px, w, h)
    for _ in range(2):
        px = merge_specks(px, w, h)
    px = thin_strands(px, w, h)
    return round_crown(px, w, h) if facing == NORTH else px


def clean_sheet(image, cell):
    out, pixels = image.copy(), image.load()
    for top in range(0, image.height, cell):
        for left in range(0, image.width, cell):
            px = [[pixels[left + x, top + y][:3] if pixels[left + x, top + y][3] else None
                   for x in range(cell)] for y in range(cell)]
            if any(c is not None for row in px for c in row):
                result = clean(px, top // cell % 4)
                for y in range(cell):
                    for x in range(cell):
                        if result[y][x] != px[y][x]:
                            out.putpixel((left + x, top + y), result[y][x] + (255,))
    return out


def grid(rows):
    key = {".": None, "#": INK, "b": BLUE, "d": DARK, "c": CREASE, "h": HAIR, "s": SHADE, "W": WHITE,
           "g": STRAND, "k": SKIN, "G": GOLD, "R": RED, "l": (110, 126, 164)}
    return [[key[ch] for ch in row] for row in rows]


def self_check():
    cloth = grid(["bbbbbbbb", "bbbbbbbb", "bbbWbbbb", "bbbbbbbb", "bWbbbGbb", "bbhbbbbb", "bbbWbbbb", "bbbbWbbb",
                  "bbbbbbbb"])
    before = [row[:] for row in cloth]
    result = clean(cloth)
    assert cloth == before, "never mutate the source cel"
    assert result[2][3] == BLUE, "a lone white speck in the coat joins the cloth"
    assert [result[4][1], result[5][2], result[6][3], result[7][4]] == [WHITE, HAIR, WHITE, WHITE], "trim stays"
    assert result[4][5] == GOLD, "gold is never recoloured"
    mask = clean(grid(["WWWWWWW", "WWWWWWW", "WWdWW#W", "WWWWWWW", "WWWRWWW", "WWWWWWW", "WWWWWWW"]))
    assert mask[2][2] == INK and mask[2][5] == INK and mask[4][3] == RED, "flat black markings, one red dot"
    tone = clean(grid(["kkkkkk", "kkkkkk", "kklkkk", "kkkkkk", "kkkkkk"]))
    assert tone[2][2] == SKIN, "render shading tones flatten into the surrounding colour"
    hair = ["#WsWsWWW#"] * 6
    back = clean(grid(hair), NORTH)
    assert [row[2] for row in back] == [SHADE] * 6 and [row[4] for row in back] == [WHITE] * 6, "thin a barcode"
    assert back[0][7] == WHITE and clean(grid(["#hhh#"] * 5), NORTH)[2][3] == SHADE, "round only plain hair"
    assert clean(grid(["#hhh#"] * 5))[2][3] == HAIR, "only the back view gets the crown edge"
    edge = grid([".....", ".hW..", "....."])
    assert clean(edge) == edge, "transparent edges and thin blades stay"


def main():
    self_check()
    if len(sys.argv) != 3:
        raise SystemExit(__doc__)
    source, out = Path(sys.argv[1]), Path(sys.argv[2])
    targets = [out / f"rainoray_{name}.png" for name in SHEETS]
    existing = [str(p) for p in targets if p.exists()]
    if existing:
        raise SystemExit(f"Refusing to overwrite: {existing}; use a fresh output folder")
    out.mkdir(parents=True, exist_ok=True)
    total = 0
    for (name, cell), target in zip(SHEETS.items(), targets):
        image = Image.open(source / target.name).convert("RGBA")
        assert image.width % cell == 0 and image.height % (cell * 4) == 0, f"unexpected layout: {target.name}"
        result = clean_sheet(image, cell)
        assert result.getchannel("A").tobytes() == image.getchannel("A").tobytes(), "silhouette changed"
        changed = sum(a != b for a, b in zip(image.get_flattened_data(), result.get_flattened_data()))
        result.save(target)
        total += changed
        print(f"{target.name}: {changed} pixels")
    print(f"Cleaned 14 sheets, {total} pixels changed; alpha, red dot, gold and hilt untouched")


if __name__ == "__main__":
    main()
