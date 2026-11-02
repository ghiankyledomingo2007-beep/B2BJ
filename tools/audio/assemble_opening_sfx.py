#!/usr/bin/env python3
"""Arrange the generated opening sounds against the scene clock. Run after generate_opening_sfx.py."""
import array
import math
import sys
import wave

from generate_forest_sfx import OUT, RATE, wav_bytes

SHOTS = {
    "opening_memory": (10, [("opening_office", 0, .65), ("opening_heartbeat", 3.2, .9),
                             ("opening_collapse", 6.25, .7)]),
    "opening_reconstruct": (4.4, [("opening_form", 0, .8), ("opening_skill_analysis", .6, .6),
                                  ("opening_skill_body", 1.45, .6), ("opening_skill_absorption", 2.3, .7),
                                  ("opening_skill_regeneration", 3.15, .6)]),
}


def render():
    result = {}
    for name, (seconds, layers) in SHOTS.items():
        samples = [0.] * round(seconds * RATE)
        for source, start, gain in layers:
            raw = wav_bytes(OUT / f"{source}.wav")
            assert raw is not None, f"wrong audio format: {source}"
            offset = round(start * RATE)
            for i, value in enumerate(array.array("h", raw)[:len(samples) - offset]):
                samples[offset + i] += value * gain
        for i in range(len(samples)):
            t = i / RATE
            tail = min(1., max(0., (8.2 - t) / .9)) if name == "opening_memory" else min(1., (seconds - t) / .04)
            samples[i] *= math.sin(min(1., t / .008) * math.pi / 2) ** 2 * math.sin(tail * math.pi / 2) ** 2
        gain = min(1., .78 * 32767 / max(1., max(map(abs, samples))))
        pcm = array.array("h", (round(value * gain) for value in samples))
        assert max(map(abs, pcm)) > 100 and max(map(abs, pcm)) < 32767 * .8
        assert abs(pcm[0]) <= 1 and abs(pcm[-1]) <= 1
        if name == "opening_memory":
            assert all(value == 0 for value in pcm[round(8.2 * RATE):]), "death ends in silence"
        result[name] = pcm.tobytes()
    return result


if __name__ == "__main__":
    for name, pcm in render().items():
        path = OUT / f"{name}.wav"
        if "--check" in sys.argv:
            assert path.exists() and wav_bytes(path) == pcm, f"stale sound bed: {path}"
        else:
            with wave.open(str(path), "wb") as target:
                target.setparams((1, 2, RATE, 0, "NONE", "not compressed"));target.writeframes(pcm)
        print(f"{name}: {len(pcm) / 2 / RATE:.2f}s verified")
