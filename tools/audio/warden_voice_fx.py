#!/usr/bin/env python3
"""Remaster the six existing Warden takes. No new synthesis, pitch shift or network.

Run with an output directory, or --check to compare against shipped recordings.
Keeps the direct voice in front of quiet armor modes and a 223 ms stone-room tail.
"""
import array
import hashlib
import json
import math
import re
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

from generate_forest_sfx import RATE, bands, biquad

ROOT = Path(__file__).resolve().parents[2]
SOURCE = ROOT / "assets/source/audio/companion-cast-2026-10-26"
FILES = ("warden_01", "warden_03", "bark_warden_voice_stand", "bark_warden_voice_line",
         "bark_warden_voice_gate", "bark_warden_voice_order")
REFLECTIONS = ((.032, .08), (.061, .06), (.107, .045), (.157, .028), (.223, .015))


def rms(samples):
    return math.sqrt(sum(x*x for x in samples) / max(1, len(samples)))


def treatment(dry, vulnerable=False):
    padded = list(dry) + [0.] * round(.26 * RATE)
    body = biquad(padded, "band", 145, .8)
    armor = bands(padded, [(530, 7, 1), (920, 8, .65), (1630, 6, .3)])
    # The low-health confession exposes more of the actor and less of the armor.
    gain = min(.7, rms(dry) * (.045 if vulnerable else .085) / max(1e-9, rms(armor)))
    out = [d + b*.12 + a*gain for d, b, a in zip(padded, body, armor)]
    room = biquad(padded, "low", 3200)
    for delay, level in REFLECTIONS:
        offset = round(delay * RATE)
        for i in range(len(room)-offset):
            out[i+offset] += room[i]*level
    assert all(math.isfinite(x) for x in out)
    direct = padded[:len(dry)]
    wet = [b-a for a, b in zip(direct, out)]
    assert rms(wet) < rms(dry)*.35, "Effects must remain behind intelligible speech"
    return out


def ff(args):
    return subprocess.run(["ffmpeg", "-hide_banner", "-nostdin", "-y", *args],
                          check=True, capture_output=True, text=True)


def read(path):
    with wave.open(str(path)) as src:
        assert (src.getnchannels(), src.getsampwidth(), src.getframerate()) == (1, 2, RATE)
        return [x/32768 for x in array.array("h", src.readframes(src.getnframes()))]


def render(key, out, work):
    source = SOURCE / (key+".mp3")
    dry = work/"dry.wav"
    ff(["-i", str(source), "-af", "highpass=f=65", "-ar", str(RATE), "-ac", "1",
        "-c:a", "pcm_s16le", str(dry)])
    original = read(dry)
    samples = treatment(original, key.endswith("_order"))
    peak = max(abs(x) for x in samples)
    # A single linear gain preserves the performance before final loudness matching.
    gain = min(1, .9/max(1e-9, peak))
    effected = work/"effected.wav"
    with wave.open(str(effected), "wb") as dest:
        dest.setparams((1, 2, RATE, 0, "NONE", "not compressed"))
        dest.writeframes(array.array("h", (round(x*gain*32767) for x in samples)).tobytes())
    analysis = ff(["-i", str(effected), "-af", "loudnorm=I=-18:TP=-4:LRA=9:print_format=json",
                   "-f", "null", "-"]).stderr
    data = json.loads(re.search(r'\{[^{}]+"input_i"[^{}]+\}', analysis, re.S).group())
    filt = ("loudnorm=I=-18:TP=-4:LRA=9:measured_I={input_i}:measured_TP={input_tp}:"
            "measured_LRA={input_lra}:measured_thresh={input_thresh}:offset={target_offset}:linear=true").format(**data)
    target = out/(key+".wav")
    ff(["-i", str(effected), "-af", filt, "-ar", str(RATE), "-ac", "1", "-c:a", "pcm_s16le", str(target)])
    final = read(target)
    assert abs(len(final)-len(original)-round(.26*RATE)) <= 1
    assert .05 < max(abs(x) for x in final) < .65
    assert rms(final[-round(.01*RATE):]) < .001, "Room tail must finish quietly"
    return {"file": target.name, "source": str(source.relative_to(ROOT)),
            "source_sha256": hashlib.sha256(source.read_bytes()).hexdigest(),
            "sha256": hashlib.sha256(target.read_bytes()).hexdigest(),
            "seconds": round(len(final)/RATE, 4),
            "peak_dbfs": round(20*math.log10(max(abs(x) for x in final)), 2),
            "armor_rms_ratio": .045 if key.endswith("_order") else .085,
            "room_reflections": REFLECTIONS, "tail_seconds": .26, "pitch_shift": 0}


def main():
    check = sys.argv[1:] == ["--check"]
    if not check and len(sys.argv) != 2:
        raise SystemExit("Usage: warden_voice_fx.py OUTPUT_DIRECTORY | --check")
    with tempfile.TemporaryDirectory(prefix="warden-voice-") as folder:
        work = Path(folder);out = work/"rendered" if check else Path(sys.argv[1])
        out.mkdir(parents=True, exist_ok=True)
        records = [render(key, out, work) for key in FILES]
        if check:
            for record in records:
                shipped = ROOT/"assets/audio/dialogue"/record["file"]
                assert hashlib.sha256(shipped.read_bytes()).hexdigest() == record["sha256"], shipped
        else:
            (out/"processing.json").write_text(json.dumps(records, indent=2)+"\n")
        print("Six Warden recordings verified" if check else "Six Warden recordings rendered")


if __name__ == "__main__":
    main()
