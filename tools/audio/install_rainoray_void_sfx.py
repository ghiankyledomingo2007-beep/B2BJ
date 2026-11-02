#!/usr/bin/env python3
"""Install the approved cue WAVs and a sustained human charge loop; --check verifies runtime copies."""
import argparse
import hashlib
import json
import math
from pathlib import Path

from generate_rainoray_skill_sfx import VOID, cloud, count, master, pcm, to_wav

ROOT = Path(__file__).resolve().parents[2]
BANK = ROOT / "assets/source/audio/rainoray-skill-draft-2026-11-02"
OUT = ROOT / "assets/audio"


def build():
    manifest = json.loads((BANK / "manifest.json").read_text())
    files = {}
    for cue in manifest["cues"].values():
        data = (BANK / cue["file"]).read_bytes()
        assert hashlib.sha256(data).hexdigest() == cue["sha256"], cue["file"]
        files[Path(cue["file"]).name] = data
    # Held input has no fixed duration: sustain the approved chord instead of repeating its release dip.
    n, overlap = count(2.6), count(.2)
    hold = cloud(n, 82.4, VOID[:6], [1.0] * n, fm=lambda t: .65 + .18 * math.sin(t * 4))
    for i in range(overlap):
        blend = .5 - .5 * math.cos(math.pi * i / (overlap - 1))
        hold[n - overlap + i] = hold[n - overlap + i] * (1 - blend) + hold[i] * blend
    hold = master(hold[overlap:], .58, .005, .005)
    assert all(math.isfinite(v) and abs(v) <= .581 for v in hold)
    assert abs(hold[0]) < .001 and abs(hold[-1]) < .001
    files["void_hold.wav"] = to_wav(pcm(hold))
    return files


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true")
    args = parser.parse_args()
    files = build()
    for name, data in files.items():
        path = OUT / name
        if args.check:
            assert path.is_file() and path.read_bytes() == data, f"Runtime audio differs: {path}"
        else:
            path.write_bytes(data)
    print(f"{len(files)} void audio files {'verified' if args.check else 'installed'}")


if __name__ == "__main__":
    main()
