"""Rename a candidate to <target>-rejected-N.png or copy it to assets/props/outpost/<target>.png (verifying size).
Usage: python3 archive.py reject <candidate.png> <target> <N>
       python3 archive.py accept <candidate.png> <target> <w> <h>
"""
import shutil
import sys
from pathlib import Path
from PIL import Image

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parents[3] / "assets/props/outpost"
mode, src, target = sys.argv[1], Path(sys.argv[2]), sys.argv[3]
if mode == "reject":
    dst = HERE / f"{target}-rejected-{sys.argv[4]}.png"
    src.rename(dst)
    print("rejected ->", dst.name)
elif mode == "accept":
    w, h = int(sys.argv[4]), int(sys.argv[5])
    img = Image.open(src)
    assert img.size == (w, h), f"{src.name} is {img.size}, expected {(w, h)}"
    assert img.mode == "RGBA" and img.getchannel("A").getextrema()[0] == 0, "no real transparency"
    dst = ASSETS / f"{target}.png"
    assert not dst.exists(), f"{dst} already exists; never overwrite"
    shutil.copyfile(src, dst)
    print("accepted ->", dst, img.size)
