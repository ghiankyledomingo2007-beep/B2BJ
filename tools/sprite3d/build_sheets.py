"""One command: static Rainoray GLB -> rigged puppet -> every game-format blade sheet it can act out.

  python3 tools/sprite3d/build_sheets.py [--out DIR] [--install] [sheet ...]

Sheets (default: all): run idle start stop turn slash cleave charge hold dash cast guard hurt counter
transform
--install copies the results into assets/ (blade sheets to characters/blade, the morph to effects).
Each is written as rainoray_<sheet>.png in the layout src/B2BJ.java reads: 80x80 cells, one row per
facing in BladeAnimation.directionRow() order (south, east, north, west); the slash sheet stacks the
four combo moves, 16 rows. All four facings are real photographs: a mirrored west would make him
left-handed and send his cuts against the game's slash arc.
A review GIF per sheet and a clipping/repeat report come with it. No services, a minute or so.
"""
import argparse
import subprocess
import sys
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

import shutil

from PIL import Image

import transform_fx
from post_pixel import outline, palette_from, snap

HERE = Path(__file__).resolve().parent
REPO = HERE.parent.parent
SOURCE = REPO / "docs/art-review/sprite3d-run/rainoray-sprint.glb"
PALETTE = sorted((REPO / "docs/art-review/human-motion-manual").glob("canonical-*.png"))
DEFAULT_OUT = REPO / "docs/art-review/sprite3d-run/sheets"
SLIME_MORPH = REPO / "docs/art-review/sprite3d-run/source"      # the slime's own reviewed morph drawings
BLADE_ASSETS, EFFECT_ASSETS = REPO / "assets/characters/blade", REPO / "assets/effects"
TRANSFORM = "transform"                                         # composited from the idle sheet, not photographed
BLENDER = "blender"
CELL = 80
CANVAS = 3             # animate.py photographs a CANVAS x CELL square with the cell centred in it
TRAIL_CELL = 160       # trail cells are twice the sprite cell, centred on it: a swoosh is wider than he is
TRAIL_TONES = [(236, 250, 255), (120, 200, 255), (40, 110, 230)]      # trail.BANDS colours, fresh to old
# sheets whose swings leave a blade trail -> written as rainoray_<sheet>_trail.png
TRAILED = {"slash", "cleave", "cast", "counter"}
# Sheets whose cells are TRAIL_CELL wide instead of CELL. The body still stands in the middle 80 px, on the
# same foot row; the margin is room to lunge, leap and swing. In an 80 px cell arm + blade alone are 42 px,
# so every reach forced the whole body to slide back, which is exactly what made him look planted.
# B2BJ.bladeCell() reads the size off the sheet, so a sheet can be widened or narrowed here alone.
WIDE = {"slash", "cleave", "cast", "counter", "dash"}
MARGIN = 2             # sprite pixels stay this far inside the cell, leaving room for the 1 px outline
DEPTH_SLIDE = 8        # front/back facings may shift this many px up or down: that is him moving in depth
ROWS = ["south", "east", "north", "west"]      # BladeAnimation.directionRow() order, all photographed
# sheet -> [(puppet action, frames)], stacked top to bottom; frame counts must match the game code
SHEETS = {
    "run": [("run", 8)], "idle": [("idle", 8)], "start": [("start", 4)], "stop": [("stop", 4)],
    "turn": [("turn", 4)],
    # blocks in the order B2BJ.bladeBlock() expects: moves 0-3, then the dash slash (move 5)
    "slash": [("slash0", 12), ("slash1", 12), ("slash2", 12), ("slash3", 12), ("dashslash", 12)],
    # the cleave (move 4), then the full-charge spin (move 6)
    "cleave": [("cleave", 12), ("spincleave", 12)], "charge": [("charge", 8)], "hold": [("hold", 8)],
    "dash": [("dash", 8)], "cast": [("cast", 8)], "guard": [("guard", 8)], "hurt": [("hurt", 8)],
    "counter": [("counter", 8)],
}
PREVIEW_MS = {"run": 62, "idle": 167, "start": 30, "stop": 30, "turn": 30, "slash": 45, "cleave": 50,
              "charge": 44, "hold": 125, "dash": 40, "cast": 44, "guard": 38, "hurt": 40, "counter": 40}
PREVIEW_SCALE, PREVIEW_BG = 4, (70, 70, 70, 255)


def blender(*args):
    done = subprocess.run([BLENDER, "-b", "--python", str(HERE / "animate.py"), "--", *map(str, args)],
                          capture_output=True, text=True)
    if done.returncode != 0 or "Traceback" in done.stdout + done.stderr:
        raise SystemExit(f"blender failed: {' '.join(map(str, args))}\n{done.stdout[-2000:]}\n{done.stderr[-2000:]}")


def ensure_puppet(out):
    """The rig is built once and reused until the model or the rig code changes."""
    blend = out / "puppet.blend"
    newest = max(SOURCE.stat().st_mtime, (HERE / "puppet.py").stat().st_mtime)
    if blend.exists() and blend.stat().st_mtime >= newest:
        return blend
    blend.unlink(missing_ok=True)
    blender(blend, out, "--action", "idle", "--frames", 1, "--views", "south", "--source", SOURCE)
    return blend


def wide_cell(shot, notes, where):
    """A roomy cell needs no sliding: cut it centred on the body cell and only report what still escapes."""
    pad = CELL * (CANVAS - 1) // 2
    grow = (TRAIL_CELL - CELL) // 2
    box = shot.getbbox()
    lo, hi = pad - grow + MARGIN, pad + CELL + grow - MARGIN
    if box and (box[0] < lo or box[1] < lo or box[2] > hi or box[3] > hi):
        notes.append(f"{where}: leaves even the wide cell by "
                     f"{max(lo - box[0], lo - box[1], box[2] - hi, box[3] - hi)} px")
    return shot.crop((pad - grow, pad - grow, pad + CELL + grow, pad + CELL + grow)), (pad, pad)


def fit_cell(shot, notes, where, facing):
    """Cut the 80 px cell out of the wide photograph, sliding the window just enough to keep a long
    blade inside: the trick the hand-made thrust used. A slide is the body moving over the ground, so
    it must be a move he could make. Sideways on screen is forward/back for a side facing and a side
    step for a front/back facing; up/down on screen is forward/back for a front/back facing, but for a
    side facing it would lift his feet off the foot row, so there it is never done, only reported."""
    pad = CELL * (CANVAS - 1) // 2
    box = shot.getbbox()
    x0 = y0 = pad
    if box:
        if box[2] - box[0] > CELL - 2 * MARGIN:
            notes.append(f"{where}: {box[2] - box[0]} px wide, cannot fit")
            x0 = (box[0] + box[2]) // 2 - CELL // 2
        elif box[0] < pad + MARGIN:
            x0 = box[0] - MARGIN
        elif box[2] > pad + CELL - MARGIN:
            x0 = box[2] + MARGIN - CELL
        over_top, over_bottom = pad + MARGIN - box[1], box[3] - (pad + CELL - MARGIN)
        if facing in ("south", "north") and max(over_top, over_bottom) > 0 >= min(over_top, over_bottom):
            y0 += max(-DEPTH_SLIDE, min(DEPTH_SLIDE, over_bottom if over_bottom > 0 else -over_top))
            over_top, over_bottom = y0 + MARGIN - box[1], box[3] - (y0 + CELL - MARGIN)
        if max(over_top, over_bottom) > 0:
            notes.append(f"{where}: overflows vertically by {max(over_top, over_bottom)} px")
        if (x0, y0) != (pad, pad):
            notes.append(f"{where}: slid {x0 - pad:+d},{y0 - pad:+d} px")
    return shot.crop((x0, y0, x0 + CELL, y0 + CELL)), (x0, y0)


def trail_cell(shot, window):
    """The trail photograph -> a TRAIL_CELL picture centred on the sprite's cell window (so it slides
    with him). The body was photographed in black as an occluder: black becomes transparent, and the
    trail is snapped to its three flat tones."""
    grow = (TRAIL_CELL - CELL) // 2
    box = (window[0] - grow, window[1] - grow, window[0] + CELL + grow, window[1] + CELL + grow)
    out = shot.crop(box)                     # PIL pads with transparency where the box leaves the photo
    px = out.load()
    for y in range(out.height):
        for x in range(out.width):
            r, g, b, a = px[x, y]
            if a < 128 or max(r, g, b) < 24:
                px[x, y] = (0, 0, 0, 0)
            else:
                px[x, y] = (*min(TRAIL_TONES, key=lambda t: (t[0] - r) ** 2 + (t[1] - g) ** 2 + (t[2] - b) ** 2), 255)
    return out


def strip(out, action, facing, frames, palette, cache, notes, trails, wide):
    """-> (sprite cells, trail cells or None)"""
    cells, swooshes = [], []
    for i in range(frames):
        shots = out / f"frames-{action}-{facing}"
        shot = Image.open(shots / f"{facing}_{i:02d}.png").convert("RGBA")
        where = f"{action} {facing} f{i}"
        cut, window = wide_cell(shot, notes, where) if wide else fit_cell(shot, notes, where, facing)
        cells.append(outline(snap(cut, palette, cache)))
        if trails:
            swooshes.append(trail_cell(Image.open(shots / f"trail_{facing}_{i:02d}.png").convert("RGBA"), window))
    return cells, swooshes


def save_gif(frames, path, ms):
    big = []
    for f in frames:
        bg = Image.new("RGBA", f.size, PREVIEW_BG)
        bg.alpha_composite(f)
        big.append(bg.resize((f.width * PREVIEW_SCALE, f.height * PREVIEW_SCALE), Image.NEAREST).convert("P"))
    big[0].save(path, save_all=True, append_images=big[1:], duration=ms, loop=0)


def report(name, rows, notes):
    """Clipped cells and repeated frames are the two things the game's art tests reject."""
    for note in notes:
        print("   ", note)
    clipped, repeats = [], []
    for r, cells in enumerate(rows):
        for i, cell in enumerate(cells):
            box = cell.getbbox()
            if box and (box[0] < 1 or box[1] < 1 or box[2] > cell.width - 1 or box[3] > cell.height - 1):
                clipped.append((r, i))
        unique = len({cell.tobytes() for cell in cells})
        if unique < len(cells):
            repeats.append((r, len(cells) - unique))
    print(f"{name:7} {len(rows[0])}x{len(rows)}  touching-edge cells (row, frame): {clipped or 'none'}"
          f"  rows with repeated frames (row, repeats): {repeats or 'none'}")


def pack(rows, size):
    sheet = Image.new("RGBA", (size * max(len(r) for r in rows), size * len(rows)), (0, 0, 0, 0))
    for r, cells in enumerate(rows):
        for i, cell in enumerate(cells):
            sheet.alpha_composite(cell, (i * size, r * size))
    return sheet


def assemble(out, name, palette, cache):
    rows, swoosh_rows, notes = [], [], []
    size = TRAIL_CELL if name in WIDE else CELL
    for action, frames in SHEETS[name]:
        for facing in ROWS:
            cells, swooshes = strip(out, action, facing, frames, palette, cache, notes, name in TRAILED,
                                    name in WIDE)
            rows.append(cells)
            swoosh_rows.append(swooshes)
    pack(rows, size).save(out / f"rainoray_{name}.png")
    if name in TRAILED:
        pack(swoosh_rows, TRAIL_CELL).save(out / f"rainoray_{name}_trail.png")
        grow = (TRAIL_CELL - size) // 2
        layered = []                          # review GIF: him with the trail over him, as the game draws it
        for block in range(0, len(rows), len(ROWS)):
            for i in range(len(rows[block])):
                f = Image.new("RGBA", (TRAIL_CELL * len(ROWS), TRAIL_CELL), (0, 0, 0, 0))
                for col in range(len(ROWS)):
                    f.alpha_composite(rows[block + col][i], (col * TRAIL_CELL + grow, grow))
                    f.alpha_composite(swoosh_rows[block + col][i], (col * TRAIL_CELL, 0))
                layered.append(f)
        save_gif(layered, out / f"{name}-with-trail.gif", PREVIEW_MS[name])
        drawn = sum(1 for r in swoosh_rows for c in r if c.getbbox())
        notes.append(f"{name} trail: {drawn} of {sum(len(r) for r in swoosh_rows)} cells carry a trail")

    quad = []                                 # the four facings side by side; moves play one after another
    for block in range(0, len(rows), len(ROWS)):
        for i in range(len(rows[block])):
            f = Image.new("RGBA", (size * len(ROWS), size), (0, 0, 0, 0))
            for col in range(len(ROWS)):
                f.alpha_composite(rows[block + col][i], (col * size, 0))
            quad.append(f)
    save_gif(quad, out / f"{name}-all-directions.gif", PREVIEW_MS[name])
    report(name, rows, notes)


def transform(out):
    """Slime <-> human morph. Its human end is idle frame 0, so it is rebuilt whenever the idle is."""
    sheets = transform_fx.build(out / "rainoray_idle.png", SLIME_MORPH / "slime_morph_in.png",
                                SLIME_MORPH / "slime_morph_out.png", out)
    for name, sheet in zip(("transform_in", "transform_out"), sheets):
        rows = [[transform_fx.cell(sheet, r, i) for i in range(transform_fx.FRAMES)] for r in range(len(ROWS))]
        save_gif([f for row in rows for f in row], out / f"{name}.gif", 40)
        report(name, rows, [])


def install(out, sheets):
    for name in sheets:
        if name == TRANSFORM:
            for morph in ("transform_in.png", "transform_out.png"):
                shutil.copy2(out / morph, EFFECT_ASSETS / morph)
        else:
            shutil.copy2(out / f"rainoray_{name}.png", BLADE_ASSETS / f"rainoray_{name}.png")
            if name in TRAILED:
                shutil.copy2(out / f"rainoray_{name}_trail.png", EFFECT_ASSETS / f"rainoray_{name}_trail.png")
    print(f"installed {len(sheets)} sheet(s) into {REPO / 'assets'}; run ./build.sh")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--out", type=Path, default=DEFAULT_OUT)
    ap.add_argument("--install", action="store_true", help="copy the built sheets into assets/")
    ap.add_argument("sheets", nargs="*", default=[*SHEETS, TRANSFORM])
    a = ap.parse_args()
    unknown = [s for s in a.sheets if s not in SHEETS and s != TRANSFORM]
    if unknown:
        raise SystemExit(f"unknown sheet {unknown}; known: {[*SHEETS, TRANSFORM]}")
    out = a.out.resolve()
    out.mkdir(parents=True, exist_ok=True)
    photographed = [s for s in a.sheets if s != TRANSFORM]
    if TRANSFORM in a.sheets and "idle" not in photographed and not (out / "rainoray_idle.png").exists():
        photographed.append("idle")                           # the morph is cut from the idle sheet
    puppet = ensure_puppet(out)
    jobs = [(action, frames, ["--trail"] if s in TRAILED else []) for s in photographed for action, frames in SHEETS[s]]
    with ThreadPoolExecutor(max_workers=4) as pool:          # Blender does the work; threads just wait
        list(pool.map(lambda j: blender(puppet, out, "--action", j[0], "--frames", j[1], *j[2]), jobs))
    palette, cache = palette_from(PALETTE), {}
    for name in photographed:
        assemble(out, name, palette, cache)
    if TRANSFORM in a.sheets:
        transform(out)
    if a.install:
        install(out, a.sheets)


if __name__ == "__main__":
    main()
