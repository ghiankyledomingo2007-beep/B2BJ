#!/usr/bin/env python3
"""Remaster existing licensed slime foley locally; keep immutable inputs for repeatable exports.

Run from any directory: python3 tools/audio/remaster_slime.py
Requires ffmpeg. No service calls or credentials; original credits remain in assets/audio/CREDITS.md.
"""
from pathlib import Path
import shutil
import subprocess
import tempfile

from slime_sfx import RATE, finish

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / "assets/audio"
SOURCE = ROOT / "assets/source/audio/slime-before-2026-10-25"
# family, takes, duration, playback rate, highpass, lowpass, output peak
FAMILIES = (
    ("slime_move", 4, .14, 1.06, 150, 2400, .23),
    ("blob_attack", 3, .22, 1.10, 180, 4800, .72),
    ("water_splash", 2, .24, 1.08, 190, 4200, .65),
    ("slime_dash", 2, .28, 1.04, 140, 3400, .68),
)


def filename(family, take):
    return f"{family}{'_' + str(take) if take > 1 else ''}.wav"


def source(name):
    path = SOURCE / name
    if not path.exists():
        shutil.copy2(OUT / name, path)
    return path


def process(inputs, graph, target, peak):
    command = ["ffmpeg", "-v", "error", "-y"]
    for path in inputs:
        command += ["-i", str(path)]
    with tempfile.TemporaryDirectory() as temp:
        wave = Path(temp) / "take.wav"
        subprocess.run(command + ["-filter_complex", graph, "-map", "[out]", "-ac", "1",
                                  "-ar", str(RATE), "-sample_fmt", "s16", str(wave)], check=True)
        finish(wave, peak)
        target.write_bytes(wave.read_bytes())


def main():
    SOURCE.mkdir(parents=True, exist_ok=True)
    for family, takes, duration, rate, high, low, peak in FAMILIES:
        for take in range(1, takes + 1):
            name = filename(family, take)
            graph = (f"[0:a]asetrate={RATE}*{rate},aresample={RATE},"
                     f"highpass=f={high},lowpass=f={low},atrim=0:{duration},"
                     f"afade=t=in:d=0.002,afade=t=out:st={duration-.065}:d=0.065[out]")
            process([source(name)], graph, OUT / name, peak)
    for take in range(1, 3):
        # A wet membrane crumple with a quiet body knock; blade damage retains its own original cue.
        inputs = [source(filename("membrane_break", take)), source(filename("hurt", take))]
        graph = ("[0:a]highpass=f=110,lowpass=f=2600,atrim=0:0.22[wet];"
                 "[1:a]highpass=f=65,lowpass=f=900,atrim=0:0.12[body];"
                 "[wet][body]amix=inputs=2:weights='1 0.28':normalize=0,"
                 "afade=t=in:d=0.002,afade=t=out:st=0.14:d=0.08[out]")
        process(inputs, graph, OUT / filename("slime_hurt", take), .76)
    print("Remastered movement, shot, splash, dash and slime hurt; original takes preserved.")


if __name__ == "__main__":
    main()
