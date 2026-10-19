#!/usr/bin/env python3
"""
Slime movement sounds: the hop squelches (assets/audio/slime_move*.wav) and the dash (slime_dash*.wav).

Source takes: rubberduck's "40 CC0 water / splash / slime SFX" (CC0),
https://opengameart.org/content/40-cc0-water-splash-slime-sfx (water-splash-slime-sfx.zip).

A hop is a soft squelch, trimmed to its onset, pitched down a little and low-passed so it reads as wet
rather than hissy, laid over a tiny falling low thump that gives the blob weight when it lands.
The dash adds a filtered pink-noise rush and a brighter slap. Every file is 22.05 kHz mono 16-bit with
silent edges, as GameAudio expects.

Usage (from the project root, ffmpeg on PATH):
    python3 tools/audio/slime_sfx.py <unzipped pack dir> [--out assets/audio]
"""
import struct
import subprocess
import sys
import tempfile
import wave
from pathlib import Path

RATE = 22050
# (source take, pitch factor, thump level)
HOPS = [("slime_06.ogg", .90, .38), ("slime_10.ogg", .86, .34), ("slime_15.ogg", .88, .30), ("slime_09.ogg", .84, .34)]
DASHES = [("slime_14.ogg", .95), ("slime_05.ogg", .92)]
HOP_PEAK, DASH_PEAK = .40, .70


def ffmpeg(src, graph, out):
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", str(src), "-filter_complex", graph, "-map", "[out]",
                    "-ac", "1", "-ar", str(RATE), "-sample_fmt", "s16", str(out)], check=True)


def onset(rate):
    """Shared front end: resample, cut the take's leading silence, pitch it by `rate`."""
    return (f"aresample={RATE},silenceremove=start_periods=1:start_threshold=-38dB,"
            f"asetrate={RATE}*{rate},aresample={RATE}")


def hop_graph(rate, thump):
    return (f"[0:a]{onset(rate)},atrim=0:0.24,highpass=f=110,lowpass=f=2800,"
            f"afade=t=in:d=0.003,afade=t=out:st=0.11:d=0.12[sq];"
            f"aevalsrc='{thump}*sin(2*PI*(130*t-260*t*t))*exp(-t*46)':d=0.10:s={RATE}[th];"
            f"[sq][th]amix=inputs=2:weights='1 1':normalize=0[out]")


def dash_graph(rate):
    return (f"anoisesrc=d=0.32:c=pink:r={RATE}:a=0.5,highpass=f=450,lowpass=f=3000,"
            f"afade=t=in:d=0.035,afade=t=out:st=0.05:d=0.25[rush];"
            f"[0:a]{onset(rate)},atrim=0:0.26,highpass=f=150,lowpass=f=3600,adelay=28,"
            f"afade=t=out:st=0.16:d=0.12[slap];"
            f"aevalsrc='0.6*sin(2*PI*(110*t-200*t*t))*exp(-t*28)':d=0.15:s={RATE}[th];"
            f"[rush][slap][th]amix=inputs=3:weights='0.6 1 0.7':normalize=0[out]")


def finish(path, peak):
    """Trim near-silence, fade the edges clean, scale to `peak`."""
    with wave.open(str(path)) as w:
        raw = w.readframes(w.getnframes())
    s = [x / 32768 for x in struct.unpack("<%dh" % (len(raw) // 2), raw)]
    loud = [i for i, x in enumerate(s) if abs(x) > .003]
    s = s[loud[0]:loud[-1] + 1] if loud else s
    top = max(abs(x) for x in s) or 1
    s = [x * peak / top for x in s]
    fin, fout = int(RATE * .002), int(RATE * .012)
    for i in range(min(fin, len(s))):
        s[i] *= i / fin
    for i in range(min(fout, len(s))):
        s[-1 - i] *= i / fout
    with wave.open(str(path), "w") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(struct.pack("<%dh" % len(s), *(int(max(-1, min(1, x)) * 32767) for x in s)))


def main():
    if len(sys.argv) < 2:
        sys.exit(__doc__)
    pack = Path(sys.argv[1])
    out = Path(sys.argv[sys.argv.index("--out") + 1]) if "--out" in sys.argv else Path("assets/audio")
    with tempfile.TemporaryDirectory() as tmp:
        for i, (src, rate, thump) in enumerate(HOPS):
            target = out / ("slime_move.wav" if i == 0 else f"slime_move_{i + 1}.wav")
            raw = Path(tmp) / target.name
            ffmpeg(pack / src, hop_graph(rate, thump), raw)
            finish(raw, HOP_PEAK)
            target.write_bytes(raw.read_bytes())
        for i, (src, rate) in enumerate(DASHES):
            target = out / ("slime_dash.wav" if i == 0 else f"slime_dash_{i + 1}.wav")
            raw = Path(tmp) / target.name
            ffmpeg(pack / src, dash_graph(rate), raw)
            finish(raw, DASH_PEAK)
            target.write_bytes(raw.read_bytes())
    print("slime sounds written to", out)


if __name__ == "__main__":
    main()
