"""Copy accepted candidates into assets/props/outpost, build contact.png, verify size + real alpha.

Run from the worktree root: python3 docs/art-review/outpost-fable/props-cleanup/finalize.py
"""
import shutil
import subprocess
import sys
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[4]
REVIEW = Path(__file__).resolve().parent
ASSETS = ROOT / "assets/props/outpost"

# (source original, accepted candidate in review dir, runtime file name)
ACCEPTED = [
    (ASSETS / "broken-cart.png", REVIEW / "cart-mud.png", "cart-mud.png"),
    (ASSETS / "dead-tree.png", REVIEW / "tree-mud.png", "tree-mud.png"),
    (ASSETS / "supply-barrel.png", REVIEW / "barrel-mud-r2.png", "barrel-mud.png"),
    (ASSETS / "barricade.png", REVIEW / "barricade-mud.png", "barricade-mud.png"),
    (ASSETS / "wall.png", REVIEW / "wall-mud.png", "wall-mud.png"),
    (REVIEW / "brazier-frame0.png", REVIEW / "brazier-unlit.png", "brazier-unlit.png"),
    (ASSETS / "shield-cache.png", REVIEW / "shield-dull.png", "shield-dull.png"),
    (ROOT / "docs/art-review/outpost-batch-27/waypost.png", REVIEW / "waypost-r2.png", "waypost.png"),
    (ROOT / "docs/art-review/outpost-foundation/debris-simple.png", REVIEW / "rubble-slate.png", "rubble-slate.png"),
]


def opaque_fraction(path):
    im = Image.open(path).convert("RGBA")
    data = im.get_flattened_data() if hasattr(im, "get_flattened_data") else im.getdata()
    return sum(1 for p in data if p[3] > 0) / (im.width * im.height)


def main():
    ok = True
    pairs = []
    for src, cand, name in ACCEPTED:
        dst = ASSETS / name
        if dst.exists() and dst.read_bytes() != cand.read_bytes():
            raise SystemExit(f"refusing to overwrite existing {dst}")
        shutil.copyfile(cand, dst)
        a, b = Image.open(src), Image.open(dst)
        frac = opaque_fraction(dst)
        good = a.size == b.size and b.mode == "RGBA" and 0.15 <= frac <= 0.85
        ok &= good
        print(f"{name}: {b.size} mode={b.mode} size-match={a.size == b.size} "
              f"opaque={frac * 100:.1f}% (source {opaque_fraction(src) * 100:.1f}%) {'ok' if good else 'FAIL'}")
        pairs += [str(src), str(dst)]
    subprocess.run([sys.executable, str(REVIEW / "inspect.py"), "contact", str(REVIEW / "contact.png"), *pairs], check=True)
    print("ALL OK" if ok else "CHECK FAILED")


if __name__ == "__main__":
    main()
