"""Copy an accepted candidate to assets/props/outpost/<target>.png after asserting size + real alpha.
Usage: python3 finalize.py <candidate.png> <target-name> <width> <height>
"""
import shutil
import sys
from pathlib import Path
from PIL import Image

WT = Path("/home/ghiankylledomingo/gamedev-vault/Projects/B2BJ/.claude/worktrees/agent-a5e1019b49145f622")
src, target, w, h = sys.argv[1], sys.argv[2], int(sys.argv[3]), int(sys.argv[4])
im = Image.open(src).convert("RGBA")
assert im.size == (w, h), f"{src}: size {im.size} != {(w, h)}"
opaque = sum(1 for a in im.getchannel("A").getdata() if a >= 128)
frac = opaque / (w * h)
assert 40 <= opaque and frac <= 0.85, f"{src}: opaque {opaque} ({frac:.1%}) outside 40px..85%"
dest = WT / "assets/props/outpost" / f"{target}.png"
assert not dest.exists(), f"{dest} already exists; refusing to overwrite"
shutil.copyfile(src, dest)
print(f"{dest.name}: {w}x{h}, {opaque} opaque px ({frac:.1%})")
