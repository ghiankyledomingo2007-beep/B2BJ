"""Bring a free (CC0) recording into the game's sound set, finished the same way as a generated one.

  python3 tools/audio/import_free.py <family> <source-file> [<source-file> ...] --tier action --tail hit \
      --credit "Impact Sounds by Kenney (CC0), kenney.nl"

Each source becomes assets/audio/<family>.wav, <family>_2.wav, ... (existing takes of that family are
replaced), decoded with ffmpeg to 22050 Hz mono, then trimmed, tail-capped, normalised to the tier and
given click-free edges by generate_sfx.finish. The credit line is appended to assets/audio/CREDITS.md so the
licence trail stays with the files.
"""
import argparse
import array
import subprocess
from pathlib import Path

from generate_sfx import OUT, RATE, TAILS, TIERS, finish, write_wav


def decode(path):
    """Any format ffmpeg reads -> interleaved stereo int16 at RATE, the shape finish() expects."""
    done = subprocess.run(["ffmpeg", "-v", "error", "-i", str(path), "-ac", "2", "-ar", str(RATE), "-f", "s16le", "-"],
                          capture_output=True, check=True)
    return done.stdout


def main():
    p = argparse.ArgumentParser()
    p.add_argument("family")
    p.add_argument("sources", nargs="+")
    p.add_argument("--tier", default="action", choices=sorted(TIERS))
    p.add_argument("--tail", default="hit", choices=sorted(TAILS))
    p.add_argument("--credit", required=True, help="author, pack, licence, URL")
    a = p.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    for stale in list(OUT.glob(f"{a.family}.wav")) + list(OUT.glob(f"{a.family}_[0-9].wav")):
        stale.unlink()
    for take, source in enumerate(a.sources, 1):
        samples = finish(decode(source), TIERS[a.tier], TAILS[a.tail], a.tail)
        path = OUT / (f"{a.family}.wav" if take == 1 else f"{a.family}_{take}.wav")
        write_wav(path, samples)
        print(f"{path.name}: {len(samples) / RATE:.2f}s from {Path(source).name}")
    with open(OUT / "CREDITS.md", "a") as credits:
        credits.write(f"- `{a.family}`: {a.credit} — " + ", ".join(Path(s).name for s in a.sources) + "\n")


if __name__ == "__main__":
    main()
